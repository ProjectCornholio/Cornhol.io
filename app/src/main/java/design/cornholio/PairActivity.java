package design.cornholio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PairActivity extends AppCompatActivity {
    public static final String TAG = "PairActivity";
    public static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<String> pairedDevices = new ArrayList<>();
    private ArrayList<String> discoveredDevices = new ArrayList<>();

    private BluetoothSocket btSocket = null;

    private ListView pairedView;
    private ListView discoveredView;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveredDevices.add(device.getName() + "\n" + device.getAddress());
                discoveredView.setAdapter(new ArrayAdapter<>(context,
                        android.R.layout.simple_list_item_1, discoveredDevices));
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "ass", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private AdapterView.OnItemClickListener deviceClickedListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String targetAddress = parent
                    .getItemAtPosition(position).toString().split("\n")[1];
            BluetoothDevice targetDevice = btAdapter.getRemoteDevice(targetAddress);
            ConnectThread thread = new ConnectThread(targetDevice);
            thread.start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        pairedView = findViewById(R.id.pairedView);
        pairedView.setOnItemClickListener(deviceClickedListener);
        discoveredView = findViewById(R.id.discoveredView);
        discoveredView.setOnItemClickListener(deviceClickedListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            searchForDevices(null);
        }
        else {
            Toast.makeText(getApplicationContext(), getString(R.string.locDeniedMsg),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void searchForDevices(View v) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getString(R.string.locPermMsg));
        } else {
            if (btAdapter == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.btDisabledMsg),
                        Toast.LENGTH_LONG).show();
            }
            else {
                if (!btAdapter.isEnabled()){
                    btAdapter.enable();
                }
                pairedDevices.clear();
                for (BluetoothDevice device : btAdapter.getBondedDevices()) {
                    pairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                pairedView.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, pairedDevices));

                discoveredDevices.clear();
                if (btAdapter.isDiscovering()) {
                    btAdapter.cancelDiscovery();
                }
                btAdapter.startDiscovery();
            }
        }
    }

    protected void requestPermission(String permission, String rationaleMsg) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            getPermissionRationaleDialog(permission, rationaleMsg).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, 0);
        }
    }

    protected AlertDialog getPermissionRationaleDialog(String permission, String msg) {
        final String perm = permission;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg).setTitle(R.string.permissions);
        builder.setPositiveButton(R.string.grant, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(PairActivity.this,
                        new String[]{perm}, 0);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(serialUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }
                Toast.makeText(getApplicationContext(), "fail", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start the connected thread
            ConnectedThread thread = new ConnectedThread(mmSocket);
            thread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            long time = System.currentTimeMillis();

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    if (System.currentTimeMillis() - time >= 1000) {
                        final byte[] printBuf = buffer;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), new String(printBuf), Toast.LENGTH_SHORT).show();
                            }
                        });
                        time = System.currentTimeMillis();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    Toast.makeText(getApplicationContext(), "disconnect", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}