package design.cornholio;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnection extends Thread {
    private static final String TAG = "BT_CONNECTION";
    private Handler handler;
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private byte[] buffer;

    class MessageType {
        static final int READ = 0;
        static final int WRITE = 1;
        static final int DISCONNECTED = 2;
    }

    BluetoothConnection(BluetoothSocket connSocket, Handler connHandler) {
        socket = connSocket;
        handler = connHandler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    public void run() {
        buffer = new byte[1024];
        int numBytes;

        while (true) {
            try {
                numBytes = inputStream.read(buffer);
                Message readMsg = handler.obtainMessage(MessageType.READ, numBytes, -1, buffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Input stream was disconnected", e);
                //Message disconnectMsg = handler.obtainMessage(MessageType.DISCONNECTED, -1, -1, null);
                //disconnectMsg.sendToTarget();
                break;
            }
        }
    }

    void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
            Message writeMsg = handler.obtainMessage(MessageType.WRITE, -1, -1, buffer);
            writeMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
        }
    }

    void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}