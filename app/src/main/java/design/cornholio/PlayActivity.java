package design.cornholio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

class Team {
    static final int RED = 0;
    static final int BLUE = 1;
}

public class PlayActivity extends AppCompatActivity {
  private BluetoothClient client;
  private AlertDialog connectingDialog;
  private AlertDialog disconnectedDialog;
  private ConnectionHandler handler;

  private Boolean DEBUG;

  private int redMatchScore = 0;
  private int blueMatchScore = 0;

  private int winningScore;
  private Boolean bustingEnabled;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_play);

    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    bustingEnabled = sharedPref.getBoolean(SettingsActivity.BUSTING_PREFERENCE_KEY, true);
    winningScore = Integer.parseInt(sharedPref.getString(SettingsActivity.SCORE_LIMIT_PREFERENCE_KEY, "21"));

    handler = new ConnectionHandler(this);

    Intent intent = getIntent();
    DEBUG = intent.getBooleanExtra("debug", false);

    TextView bustScoreView = findViewById(R.id.bustScore);
    int bustScore = (winningScore * 2 / 3) + 1;
    String bustScoreText = bustingEnabled ? String.format(Locale.US, "Bust Penalty: %d", bustScore) : "";
    bustScoreView.setText(bustScoreText);

    if (DEBUG) {
      return;
    }

    View v = findViewById(R.id.randomize);
    v.setVisibility(View.GONE);

    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device = btAdapter.getRemoteDevice(intent.getStringExtra("address"));

    client = new BluetoothClient(handler, device);
    client.start();

    connectingDialog = new AlertDialog.Builder(this).setMessage("Connecting...").setCancelable(false).show();
    disconnectedDialog = getDisconnectedDialog();
  }

  @Override
  protected void onDestroy() {
    if (DEBUG) {
      super.onDestroy();
      return;
    }
    client.stop();
    connectingDialog.dismiss();
    disconnectedDialog.dismiss();
    super.onDestroy();
  }

  public static class ConnectionHandler extends Handler {
    private final int MAX_ROUND_SCORE = 12;

    private PlayActivity activity;
    private boolean reconnecting = false;

    ConnectionHandler(PlayActivity displayActivity) {
      activity = displayActivity;
    }

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == BluetoothClient.MessageType.CONNECTION_FAILED) {
        if (!reconnecting) {
          Toast.makeText(activity, "Connection failed. Make sure the board is ready to connect.", Toast.LENGTH_SHORT).show();
          activity.finish();
        }
      }
      else if (msg.what == BluetoothClient.MessageType.CONNECTION_SUCCEEDED) {
        activity.connectingDialog.dismiss();
        activity.disconnectedDialog.dismiss();
        activity.client.write("CLEAR".getBytes());
        reconnecting = false;
      }
      else if (msg.what == BluetoothClient.MessageType.DISCONNECTED) {
        activity.disconnectedDialog.show();
        reconnecting = true;
        activity.client.start();
      }
      else if (msg.what == BluetoothClient.MessageType.READ) {
        byte[] readBuf = (byte[]) msg.obj;
        String message = new String(readBuf, 0, msg.arg1);

        Log.d("PLAY", message);

        TextView redRoundView = activity.findViewById(R.id.redRoundScore);
        TextView blueRoundView = activity.findViewById(R.id.blueRoundScore);

        String[] scoreValues = message.split(",");
        int redRoundScore = Integer.parseInt(scoreValues[0]) + 3 * Integer.parseInt(scoreValues[1]);
        int blueRoundScore = Integer.parseInt(scoreValues[2]) + 3 * Integer.parseInt(scoreValues[3]);

        if ((redRoundScore > MAX_ROUND_SCORE) || (blueRoundScore > MAX_ROUND_SCORE)) {
          return;
        }

        int redNetScore = Math.max(0, redRoundScore - blueRoundScore);
        int blueNetScore = Math.max(0, blueRoundScore - redRoundScore);

        if (redNetScore > 0) {
          redRoundView.setText(String.format(Locale.US, "%+02d", redNetScore));
          blueRoundView.setText("+0");
        }
        else if (blueNetScore > 0) {
          redRoundView.setText("+0");
          blueRoundView.setText(String.format(Locale.US, "%+02d", blueNetScore));
        }
        else {
          redRoundView.setText("+0");
          blueRoundView.setText("+0");
        }

        activity.updateBustWarning();

      }
    }
  }

  public void updateBustWarning() {
    TextView redRoundView = findViewById(R.id.redRoundScore);
    TextView redBustWarning = findViewById(R.id.redBustWarning);

    TextView blueRoundView = findViewById(R.id.blueRoundScore);
    TextView blueBustWarning = findViewById(R.id.blueBustWarning);

    int redScoreAdjusted = redMatchScore + Integer.parseInt(redRoundView.getText().toString().substring(1));
    int blueScoreAdjusted = blueMatchScore + Integer.parseInt(blueRoundView.getText().toString().substring(1));


    if ((redScoreAdjusted > winningScore) && bustingEnabled) {
      redBustWarning.setText(getResources().getString(R.string.bustWarning));
    }
    else {
      redBustWarning.setText("");
    }

    if ((blueScoreAdjusted > winningScore) && bustingEnabled) {
      blueBustWarning.setText(getResources().getString(R.string.bustWarning));
    }
    else {
      blueBustWarning.setText("");
    }
  }

  public void randomize(View v) {
    Random rand = new Random();
    byte[] msg = (rand.nextInt(8) + ",0," + rand.nextInt(8) + ",0").getBytes();
    Message readMsg = handler.obtainMessage(BluetoothClient.MessageType.READ, msg.length, -1, msg);
    readMsg.sendToTarget();
  }

  public void nextRound(View v) {
    int addAmount;
    TextView redRoundView = findViewById(R.id.redRoundScore);
    TextView redBustWarning = findViewById(R.id.redBustWarning);

    TextView blueRoundView = findViewById(R.id.blueRoundScore);
    TextView blueBustWarning = findViewById(R.id.blueBustWarning);

    addAmount = Integer.parseInt(redRoundView.getText().toString().substring(1));
    changeScore(addAmount, Team.RED);

    addAmount = Integer.parseInt(blueRoundView.getText().toString().substring(1));
    changeScore(addAmount, Team.BLUE);

    redRoundView.setText("+0");
    redBustWarning.setText("");
    blueRoundView.setText("+0");
    blueBustWarning.setText("");

    if (DEBUG) {
      return;
    }

    client.write("CLEAR".getBytes());
  }

  public void endGame(View v) {
    finish();
  }

  public void win(int team) {
    getWinDialog(team).show();
  }

  public void bust(int team) {
    String bustingTeam = (team == Team.RED ? "Red" : "Blue");
    Toast.makeText(this, "The " + bustingTeam + " team busted!", Toast.LENGTH_SHORT).show();
    int bustScore = (winningScore * 2 / 3) + 1;
    int changeBy = (team == Team.RED ? bustScore - redMatchScore : bustScore - blueMatchScore);
    changeScore(changeBy, team);
  }

  protected AlertDialog getWinDialog(int team) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    String winningTeam = (team == Team.RED ? "Red" : "Blue");
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

  public void changeScore(int by, int team) {
    TextView scoreView;
    if (team == Team.RED) {
      if ((redMatchScore + by) >= 0) {
        redMatchScore = redMatchScore + by;
        scoreView = findViewById(R.id.redMatchScore);
        scoreView.setText(String.format(Locale.US, "%02d", redMatchScore));
      }
      if (redMatchScore >= winningScore) {
        if (redMatchScore > winningScore && bustingEnabled) {
          bust(Team.RED);
        }
        else {
          win(Team.RED);
        }
      }
    }
    else {
      if ((blueMatchScore + by) >= 0) {
        blueMatchScore = blueMatchScore + by;
        scoreView = findViewById(R.id.blueMatchScore);
        scoreView.setText(String.format(Locale.US, "%02d", blueMatchScore));
      }
      if (blueMatchScore >= winningScore) {
        if (blueMatchScore > winningScore && bustingEnabled) {
          bust(Team.BLUE);
        }
        else {
          win(Team.BLUE);
        }
      }
    }
    updateBustWarning();
  }

  public void incrementRed(View v) {
    changeScore(1, Team.RED);
  }

  public void decrementRed(View v) {
    changeScore(-1, Team.RED);
  }

  public void incrementBlue(View v) {
    changeScore(1, Team.BLUE);
  }

  public void decrementBlue(View v) {
    changeScore(-1, Team.BLUE);
  }
}