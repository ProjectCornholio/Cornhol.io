package design.cornholio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SELECT_BOARD1 = 1;
    private static final int REQUEST_SELECT_BOARD2 = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    BluetoothAdapter btAdapter;

    private TextView board1;
    private TextView board2;

    private String deviceAddress1 = null;
    private String deviceAddress2 = null;

    private TextView.OnClickListener findBoard = new View.OnClickListener() {
        public void onClick(View v) {
            Intent pairIntent = new Intent(v.getContext(), PairActivity.class);
            int requestCode = (v.getId() == R.id.board1 ? REQUEST_SELECT_BOARD1 : REQUEST_SELECT_BOARD2);
            startActivityForResult(pairIntent, requestCode);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_BOARD1) {
            if (resultCode == Activity.RESULT_OK) {
                board1.setText(data.getStringExtra("name"));
                deviceAddress1 = data.getStringExtra("address");
                board2.setEnabled(true);
                View playButton = findViewById(R.id.play_button);
                playButton.setEnabled(true);
            }
        }
        if (requestCode == REQUEST_SELECT_BOARD2) {
            if (resultCode == Activity.RESULT_OK) {
                board2.setText(data.getStringExtra("name"));
                deviceAddress2 = data.getStringExtra("address");
            }
        }
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth is required for the app to function properly", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        board1 = findViewById(R.id.board1);
        board2 = findViewById(R.id.board2);
        board1.setOnClickListener(findBoard);
        board2.setOnClickListener(findBoard);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void gotoPlay(View v) {
        Intent intent = new Intent(this, PlayActivity.class);
        intent.putExtra("address1", deviceAddress1);
        intent.putExtra("address2", deviceAddress2);
        startActivity(intent);
    }

    public void gotoSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
