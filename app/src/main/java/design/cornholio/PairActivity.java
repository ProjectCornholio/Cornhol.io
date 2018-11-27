package design.cornholio;

import android.Manifest;
import android.app.Activity;
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
import java.util.UUID;

public class PairActivity extends AppCompatActivity {
    public static final String TAG = "PairActivity";
    public static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<String> pairedDevices = new ArrayList<>();
    private ArrayList<String> discoveredDevices = new ArrayList<>();

    private ListView pairedView;
    private ListView discoveredView;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().contains("Cornhol.io")) {
                    discoveredDevices.add(device.getName() + "\n" + device.getAddress());
                    discoveredView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, discoveredDevices));
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Search complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private AdapterView.OnItemClickListener deviceClickedListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String targetName = parent.getItemAtPosition(position).toString().split("\n")[0];
            String targetAddress = parent.getItemAtPosition(position).toString().split("\n")[1];

            Intent returnIntent = new Intent();
            returnIntent.putExtra("name", targetName);
            returnIntent.putExtra("address", targetAddress);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getString(R.string.locPermMsg));
        }
        else {
            View searchButton = findViewById(R.id.searchButton);
            searchButton.setEnabled(true);
            getPairedDevices();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        btAdapter.cancelDiscovery();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            View searchButton = findViewById(R.id.searchButton);
            searchButton.setEnabled(true);
            getPairedDevices();
        } else {
            Toast.makeText(getApplicationContext(), "Cannot search for devices without location permission", Toast.LENGTH_SHORT).show();
        }
    }

    public void getPairedDevices() {
        pairedDevices.clear();
        for (BluetoothDevice device : btAdapter.getBondedDevices()) {
            if (device.getName() != null && device.getName().contains("Cornhol.io")) {
                pairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        pairedView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pairedDevices));
    }

    public void searchForDevices(View v) {
        discoveredDevices.clear();
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
        Toast.makeText(getApplicationContext(), "Searching...", Toast.LENGTH_SHORT).show();
    }

    public void clearDevice(View v) {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }

    protected void requestPermission(String permission, String rationaleMsg) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            getPermissionRationaleDialog(permission, rationaleMsg).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
    }

    protected AlertDialog getPermissionRationaleDialog(String permission, String msg) {
        final String perm = permission;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg).setTitle(R.string.permissions);
        builder.setPositiveButton(R.string.grant, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(PairActivity.this, new String[]{perm}, 0);
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
}