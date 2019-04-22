package design.cornholio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SELECT_BOARD = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    BluetoothAdapter btAdapter;

    private TextView board;

    private String deviceAddress = null;

    private TextView.OnClickListener findBoard = new View.OnClickListener() {
        public void onClick(View v) {
            Intent pairIntent = new Intent(v.getContext(), PairActivity.class);
            startActivityForResult(pairIntent, REQUEST_SELECT_BOARD);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_BOARD) {
            if (resultCode == Activity.RESULT_OK) {
                board.setText(data.getStringExtra("name"));
                deviceAddress = data.getStringExtra("address");
                View playButton = findViewById(R.id.play_button);
                playButton.setEnabled(true);
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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        board = findViewById(R.id.board);
        board.setOnClickListener(findBoard);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void gotoPlay(View v) {
        Intent intent = new Intent(this, PlayActivity.class);
        intent.putExtra("address", deviceAddress);
        intent.putExtra("debug", false);
        startActivity(intent);
    }

    public void gotoDebug(View v) {
      Intent intent = new Intent(this, PlayActivity.class);
      intent.putExtra("address", "");
      intent.putExtra("debug", true);
      startActivity(intent);
    }

    public void gotoSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
