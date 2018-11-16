package design.cornholio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

public class DebugActivity extends AppCompatActivity {
    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        TextView rssiMsg = findViewById(R.id.textView);
        rssiMsg.setText("who");
    }

    public void enableBluetooth(View enableButton) {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth already on", Toast.LENGTH_LONG).show();
        }
    }

    public void disableBluetooth(View disableButton) {
        btAdapter.disable();
        Toast.makeText(getApplicationContext(), "Bluetooth off", Toast.LENGTH_LONG).show();
    }

    public void discoverBluetooth(View discoverButton) {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Must start Bluetooth first", Toast.LENGTH_LONG).show();
        }
        else {
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }
            btAdapter.startDiscovery();
        }
    }

}