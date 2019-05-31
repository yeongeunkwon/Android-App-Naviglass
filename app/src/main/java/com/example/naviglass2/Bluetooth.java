// Reference: https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatService.java#L361
package com.example.naviglass2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class Bluetooth {

    private BluetoothAdapter mBluetoothAdapter;
    private static final String TAG = "BLUETOOTH";
    private UUID MY_UUID = UUID.fromString("c733fac0-6030-11e9-b475-0800200c9a66"); // Unique UUID for this application
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 1;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public Bluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    // Start the Bluetooth connection. Called by the Activity onResume()
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    // Stop all threads
    public synchronized void stop() {
        Log.d("tagBt", "Bluetooth.stop()");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
    }

    public synchronized int getState() {
        return mState;
    }

    // Write to the ConnectedThread
    // @param out The bytes to write
    public synchronized void write(byte[] out) {
        ConnectedThread r;
        if (mState != STATE_CONNECTED)
            return;
        r = mConnectedThread;
        r.write(out);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device. It runs
     * straight through; the connection either succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                }
                catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                mState = STATE_NONE;
                return;
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Reset the ConnectThread because we're done
            synchronized (Bluetooth.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        // Close the socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /* Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d("tagBt", "Bluetooth.connected()");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket output streams
            try {
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {}

        /* Write to the connected OutStream.
         * @param bytes The bytes to write
         */
        public synchronized void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}