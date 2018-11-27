package design.cornholio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class PlayActivity extends AppCompatActivity {
    private BluetoothClient client;
    private AlertDialog connectingDialog;
    private AlertDialog disconnectedDialog;

    private int redMatchScore = 0;
    private int blueMatchScore = 0;
    private int winningScore = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        Handler handler = new ConnectionHandler(this);
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent intent = getIntent();
        BluetoothDevice device1 = btAdapter.getRemoteDevice(intent.getStringExtra("address1"));
        //BluetoothDevice device2 = btAdapter.getRemoteDevice(intent.getStringExtra("address2"));

        client = new BluetoothClient(device1, handler);
        client.start();

        connectingDialog = new AlertDialog.Builder(this).setMessage("Connecting...").setCancelable(false).show();
        disconnectedDialog = getDisconnectedDialog();
    }

    @Override
    protected void onDestroy() {
        client.cancel();
        connectingDialog.dismiss();
        disconnectedDialog.dismiss();
        super.onDestroy();
    }

    public static class ConnectionHandler extends Handler {
        private PlayActivity activity;
        ConnectionHandler(PlayActivity displayActivity) {
            activity = displayActivity;
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothClient.MessageType.CONNECTION_FAILED) {
                Toast.makeText(activity, "Connection failed. Make sure the board is ready to connect.", Toast.LENGTH_SHORT).show();
                activity.finish();
            }
            else if (msg.what == BluetoothClient.MessageType.CONNECTION_SUCCEEDED) {
                activity.connectingDialog.dismiss();
            }
            else if (msg.what == BluetoothConnection.MessageType.DISCONNECTED) {
                activity.disconnectedDialog.show();
            }
            else if (msg.what == BluetoothConnection.MessageType.READ) {
                activity.disconnectedDialog.dismiss();
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);

                TextView redRoundView = activity.findViewById(R.id.redRoundScore);
                TextView blueRoundView = activity.findViewById(R.id.blueRoundScore);
                String[] scoreValues = message.split(",");
                int redRoundScore = Integer.parseInt(scoreValues[0]) + 3*Integer.parseInt(scoreValues[1]);
                int blueRoundScore = Integer.parseInt(scoreValues[2]) + 3*Integer.parseInt(scoreValues[3]);

                if (redRoundScore > blueRoundScore) {
                    redRoundView.setText(String.format(Locale.US, "%+02d", redRoundScore - blueRoundScore));
                    blueRoundView.setText("");
                }
                else if (blueRoundScore > redRoundScore) {
                    redRoundView.setText("");
                    blueRoundView.setText(String.format(Locale.US, "%+02d", blueRoundScore - redRoundScore));
                }
                else {
                    redRoundView.setText("");
                    blueRoundView.setText("");
                }
            }
        }
    }

    public void nextRound(View v) {
        TextView redMatchView = findViewById(R.id.redMatchScore);
        TextView redRoundView = findViewById(R.id.redRoundScore);
        TextView blueMatchView = findViewById(R.id.blueMatchScore);
        TextView blueRoundView = findViewById(R.id.blueRoundScore);
        if (redRoundView.getText() != "") {
            int addAmount = Integer.parseInt(redRoundView.getText().toString().substring(1));
            redMatchScore += addAmount;
            redMatchView.setText(String.format(Locale.US, "%02d", redMatchScore));
        }
        else if (blueRoundView.getText() != "") {
            int addAmount = Integer.parseInt(blueRoundView.getText().toString().substring(1));
            blueMatchScore += addAmount;
            blueMatchView.setText(String.format(Locale.US, "%02d", blueMatchScore));
        }
        redRoundView.setText("");
        blueRoundView.setText("");
        client.write("CLEAR".getBytes());
        if (redMatchScore >= winningScore || blueMatchScore >= winningScore) {
            win();
        }
    }

    public void endGame(View v) {
        finish();
    }

    public void win() {
        getWinDialog().show();
    }

    protected AlertDialog getWinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String winningTeam = redMatchScore == 21 ? "Red" : "Blue";
        builder.setMessage("The " + winningTeam + " team won!").setTitle("Congratulations!");
        builder.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        return builder.create();
    }

    protected AlertDialog getDisconnectedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have been disconnected. Make sure you are in range of the board and the board has power. If you do not automatically re-connect, quit the game and retry.").setTitle("Disconnected");
        builder.setNegativeButton("Quit Game", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        return builder.create();
    }

    public void incrementRed(View v) {
        TextView redMatchView = findViewById(R.id.redMatchScore);
        redMatchView.setText(String.format(Locale.US, "%02d", ++redMatchScore));
        if (redMatchScore >= winningScore) {
            win();
        }
    }

    public void decrementRed(View v) {
        TextView redMatchView = findViewById(R.id.redMatchScore);
        if (redMatchScore > 0) {
            redMatchView.setText(String.format(Locale.US, "%02d", --redMatchScore));
        }
    }

    public void incrementBlue(View v) {
        TextView blueMatchView = findViewById(R.id.blueMatchScore);
        blueMatchView.setText(String.format(Locale.US, "%02d", ++blueMatchScore));
        if (blueMatchScore >= winningScore) {
            win();
        }
    }

    public void decrementBlue(View v) {
        TextView blueMatchView = findViewById(R.id.blueMatchScore);
        if (blueMatchScore > 0) {
            blueMatchView.setText(String.format(Locale.US, "%02d", --blueMatchScore));
        }
    }
}