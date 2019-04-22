package design.cornholio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

class BluetoothClient {
  private static final String TAG = "BT_CLIENT";
  private static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private final Handler handler;
  private BluetoothSocket socket;
  private BluetoothConnection connection;

  class MessageType {
    static final int READ = 0;
    static final int WRITE = 1;
    static final int DISCONNECTED = 2;
    static final int CONNECTION_FAILED = 3;
    static final int CONNECTION_SUCCEEDED = 4;
  }

  BluetoothClient(Handler msgHandler, BluetoothDevice device) {
    handler = msgHandler;
    try {
      socket = device.createRfcommSocketToServiceRecord(serialUUID);
    } catch (IOException e) {
      Log.e(TAG, "Socket's create() method failed", e);
      Message readMsg = handler.obtainMessage(MessageType.CONNECTION_FAILED);
      readMsg.sendToTarget();
    }
  }

  void start() {
    if (connection != null) {
      connection.cancel();
    }
    connection = new BluetoothConnection();
    connection.start();
  }

  void stop() {
    if (connection != null) {
      connection.cancel();
      connection = null;
    }
  }

  void write(byte[] content) {
    connection.write(content);
  }

  private class BluetoothConnection extends Thread {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private byte[] buffer;

    BluetoothConnection() {
      BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
      connect();
      inputStream = getInputStream();
      outputStream = getOutputStream();
    }

    void write(byte[] bytes) {
      try {
        outputStream.write(bytes);
      } catch (IOException e) {
        Log.e(TAG, "Error occurred when sending data", e);
      }
    }

    void cancel() {
      try {
        socket.close();
      } catch (IOException e) {
        Log.e(TAG, "Could not close the client socket", e);
      }
    }

    public void run() {
      int numBytes;
      buffer = new byte[1024];

      while (true) {
        try {
          numBytes = inputStream.read(buffer);
          Message readMsg = handler.obtainMessage(MessageType.READ, numBytes, -1, buffer);
          readMsg.sendToTarget();
        } catch (IOException e) {
          Log.e(TAG, "Input stream was disconnected", e);
          Message readMsg = handler.obtainMessage(MessageType.DISCONNECTED);
          readMsg.sendToTarget();
          break;
        }
      }
    }

    private void connect() {
      try {
        socket.connect();
        Message readMsg = handler.obtainMessage(MessageType.CONNECTION_SUCCEEDED);
        readMsg.sendToTarget();
      } catch (IOException connectException) {
        try {
          socket.close();
        } catch (IOException closeException) {
          Log.e(TAG, "Could not close the client socket", closeException);
        }
        Message readMsg = handler.obtainMessage(MessageType.CONNECTION_FAILED);
        readMsg.sendToTarget();
      }
    }

    private InputStream getInputStream() {
      InputStream tmpIn = null;
      try {
        tmpIn = socket.getInputStream();
      } catch (IOException e) {
        Log.e(TAG, "Error occurred when creating input stream", e);
      }
      return tmpIn;
    }

    private OutputStream getOutputStream() {
      OutputStream tmpOut = null;
      try {
        tmpOut = socket.getOutputStream();
      } catch (IOException e) {
        Log.e(TAG, "Error occurred when creating output stream", e);
      }
      return tmpOut;
    }
  }

}
