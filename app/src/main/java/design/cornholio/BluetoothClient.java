package design.cornholio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothClient extends Thread {
    private static final String TAG = "BT_CLIENT";
    private static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Handler handler;
    private BluetoothSocket socket;
    private BluetoothConnection connection;

    class MessageType {
        static final int CONNECTION_FAILED = 3;
        static final int CONNECTION_SUCCEEDED = 4;
    }

    BluetoothClient(BluetoothDevice device, Handler connHandler) {
        handler = connHandler;
        try {
            socket = device.createRfcommSocketToServiceRecord(serialUUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
            Message readMsg = handler.obtainMessage(MessageType.CONNECTION_FAILED, -1, -1, null);
            readMsg.sendToTarget();
        }
    }

    public void run() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.cancelDiscovery();
        try {
            socket.connect();
        } catch (IOException connectException) {
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        connection = new BluetoothConnection(socket, handler);
        connection.start();

        Message readMsg = handler.obtainMessage(MessageType.CONNECTION_SUCCEEDED, -1, -1, null);
        readMsg.sendToTarget();
    }

    void write(byte[] bytes) {
        connection.write(bytes);
    }

    // Closes the client socket and causes the thread to finish.
    void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}