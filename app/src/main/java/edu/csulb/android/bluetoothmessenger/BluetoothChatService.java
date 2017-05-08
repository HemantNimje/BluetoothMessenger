package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Hemant on 5/5/2017.
 */



public class BluetoothChatService {

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("188c5bda-d1b6-464a-8074-c5deaad3fa36");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;


    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;


    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String TOAST = "toast";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DATATYPE_IMAGE = "image";
    public static final String DATATYPE_TEXT = "text";
    public static final String DATATYPE_FILE = "file";



    private int mState;
    private int mNewState;


    public BluetoothChatService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen failed", e);
            }
            mServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        @Override
        public void run() {

            BluetoothSocket socket = null;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Accept failed", e);
                    break;
                }

                /* Handle if connection is accepted */
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "closing of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            mDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "connect thread creation failed", e);
            }
            mSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "connect thread closing failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        /**
         * Write to the connected OutStream.
         *
//         * @param buffer The bytes to write
         */

        public void run() {
            byte[] bufferData = new byte[16384];
            int numOfPackets = 0;
            int datatype = 0;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (!this.isInterrupted()) {
                try {
                    int numOfBytes = mInStream.read(bufferData);
                    byte[] trimmedBufferData = Arrays.copyOf(bufferData, numOfBytes);
                    bufferData = new byte[16384];
                    ByteBuffer tempBuffer = ByteBuffer.wrap(trimmedBufferData);
                    if (datatype == 0) {
                        datatype = tempBuffer.getInt();
                        Log.d(TAG, "Datatype: " + datatype);
                    }
                    if (numOfPackets == 0) {
                        numOfPackets = tempBuffer.getInt();
                        Log.d(TAG, "Packets size: " + numOfPackets);
                    }
                    byte[] dst = new byte[tempBuffer.remaining()];
                    tempBuffer.get(dst);
                    bos.write(dst);
                    //Following condition checks if we have received all necessary bytes to construct a message out of it.
                    if (bos.size() == numOfPackets) {
                        //For Text and Audio notes
                        if (datatype != 2) {
                            Log.d(TAG, "Data: " + new String(bos.toByteArray(), Charset.defaultCharset()));
                            Message msg = mHandler.obtainMessage(MESSAGE_READ, -1, datatype, bos.toByteArray());
                            msg.sendToTarget();
                        } else {    //For images - Special check because we have to decode the image from Base64.
                            String decodedString = new String(bos.toByteArray(), Charset.defaultCharset());
                            Log.d(TAG, "Image Base64 decoded string: " + decodedString);
                            byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                            Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                            Message msg = mHandler.obtainMessage(MESSAGE_READ, -1, datatype, bp);
                            msg.sendToTarget();
                        }
                        //Re-initialize for the next message.
                        datatype = 0;
                        numOfPackets = 0;
                        bos = new ByteArrayOutputStream();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes, String datatype) {
            try {
                Message writtenMsg = null;
                ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
                ByteBuffer tempBuffer = ByteBuffer.allocate(bytes.length + 8);
                if (datatype.equals(DATATYPE_IMAGE)) {

                    System.out.println("IMAGE WRITE");

                    tempBuffer.putInt(2);
                    ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                    imageStream.write(bytes);
                    String decodedString = new String(imageStream.toByteArray(), Charset.defaultCharset());
                    byte[] decodedStringArray = Base64.decode(decodedString, Base64.DEFAULT);
                    Bitmap bp = BitmapFactory.decodeByteArray(decodedStringArray, 0, decodedStringArray.length);
                    writtenMsg = mHandler.obtainMessage(MESSAGE_WRITE, -1, 2, bp);
                    imageStream.close();
                } else if (datatype.equals(DATATYPE_TEXT)) {
                    tempBuffer.putInt(1);
                    writtenMsg = mHandler.obtainMessage(MESSAGE_WRITE, -1, 1, bytes);
                } else if (datatype.equals(DATATYPE_FILE)) {
                    tempBuffer.putInt(3);
                    writtenMsg = mHandler.obtainMessage(MESSAGE_WRITE, -1, 3, bytes);
                }
                Log.d(TAG, "Sending size: " + bytes.length);
                tempBuffer.putInt(bytes.length);
                Log.d(TAG, "Sending data: " + new String(bytes, Charset.defaultCharset()));
                tempBuffer.put(bytes);
                tempOutputStream.write(tempBuffer.array());
                mOutStream.write(tempOutputStream.toByteArray());
                tempOutputStream.close();
                if (writtenMsg != null) {
                    writtenMsg.sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                Message writeErrorMsg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Device disconnected. Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

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
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        // Cancel the thread once connection is completed
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        bundle.putString(DEVICE_ADDRESS, device.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void start() {
        Log.d(TAG, "start");

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

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

//    /**
//     * Write to the ConnectedThread in an unsynchronized manner
//     *
//     * @param out The bytes to write
//     * @see ConnectedThread#write(byte[])
//     */
    public void write(byte[] out, String datatype) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out, datatype);
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

}
