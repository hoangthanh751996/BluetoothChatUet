package com.example.hoang.bluetoothchat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.provider.SyncStateContract;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import android.os.Handler;

/**
 * Created by hoang on 4/19/2017.
 */

public class BlutoothConnect {
    private static final String TAG = "BluetoothConnect";
    private static final String appName = "BluetoothChat";
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter uetBluetoothApdater;
    private final Handler uetHandler;
    Context uetContext;
    private AcceptThread uetSecureAcceptThread;
    private AcceptThread uetInsecureAcceptThread;
    private ConnectThread uetConnectThread;
    private ConnectedThread uetConnectedThread;
    private int uetState;
    private int uetNewState;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    /*private BluetoothDevice uetDevice;
    private UUID deviceUUID;
    ProgressDialog uetProgessDialog;*/

    public BlutoothConnect(Context context, Handler handler) {

        uetBluetoothApdater = BluetoothAdapter.getDefaultAdapter();
        uetState = STATE_NONE;
        uetNewState = uetState;
        uetHandler = handler;
    }
    // Cap nhat trang thai cuoc tro chuyen
    private  synchronized  void updateInterfaceTitle() {
        uetState = getState();
        Log.d(TAG, "Update Interface" + uetNewState + "->" + uetState);
        uetNewState = uetState;
        uetHandler.obtainMessage(MESSAGE_STATE_CHANGE, uetNewState, -1).sendToTarget();
    }
    //tra lai gia tri trang thai hien dang ket noi
    public synchronized int getState() {
        return uetState;
    }

    //Bat dau cuoc tro chuyen

    public synchronized void start() {
        Log.d(TAG, "Start Conversation");
        //Huy cac thread dang co gang tao ket noi
        if (uetConnectThread != null) {
            uetConnectThread.cancel();
            uetConnectThread = null;
        }

        //Huy cac thread dang chay chay ket noi

        if (uetConnectedThread != null) {
            uetConnectedThread.cancel();
            uetConnectedThread = null;
        }

        //BAt dau lang nghe tu bluetoothsocket

        if (uetSecureAcceptThread == null) {
            uetSecureAcceptThread = new AcceptThread(true);
            uetSecureAcceptThread.start();
        }
        if (uetInsecureAcceptThread == null) {
            uetInsecureAcceptThread = new AcceptThread(false);
            uetInsecureAcceptThread.start();
        }
        //Cap nhat tieu de vao giao dien

        updateInterfaceTitle();
    }

    //Bat dau ket noi voi mot thiet bi tu xa

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "Connect to: " + device);

        //Huy cac thread dang co tao ket noi

        if (uetState == STATE_CONNECTING) {
            if (uetConnectThread != null) {
                uetConnectThread.cancel();
                uetConnectThread=null;
            }
        }
        if (uetConnectedThread != null) {
            uetConnectedThread.cancel();
            uetConnectedThread = null;
        }

        // Bat dau ket noi voi thiet bi

        uetConnectThread = new ConnectThread(device,secure);
        uetConnectThread.start();

        // Cap nhat len giao dien
        updateInterfaceTitle();
    }

    //Quan ly sau khi da ket noi

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        Log.d(TAG,"Connected, Socket Type: " + socketType);

        //Huy cac thread da hoan thanh ket noi

        if(uetConnectThread != null) {
            uetConnectThread.cancel();
            uetConnectThread = null;
        }

        //Huy cac thread dang chay ket noi

        if (uetConnectedThread != null) {
            uetConnectedThread.cancel();
            uetConnectedThread = null;
        }

        //Huy cac thiet bi muon chap nhan ket noi

        if (uetSecureAcceptThread != null) {
            uetSecureAcceptThread.cancel();
            uetSecureAcceptThread = null;
        }
        if (uetInsecureAcceptThread != null) {
            uetInsecureAcceptThread.cancel();
            uetInsecureAcceptThread = null;
        }

        //Bat dau quan lly ket noi va hieu suat truyen du lieu

        uetConnectedThread = new ConnectedThread(socket,socketType);
        uetConnectedThread.start();
        //Gui ten thiet bi da ket noi va cap nhat len giao dien
        Message msg = uetHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());;
        msg.setData(bundle);
        uetHandler.sendMessage(msg);

        //cap nhat giao dien
        updateInterfaceTitle();
    }

    //Dung thread

    public synchronized void stop() {
        Log.d(TAG, "Stop");

        if (uetConnectThread != null) {
            uetConnectThread.cancel();
            uetConnectThread = null;

        }
        if (uetConnectedThread != null) {
            uetConnectedThread.cancel();
            uetConnectedThread = null;
        }

        if (uetSecureAcceptThread != null) {
            uetSecureAcceptThread.cancel();
            uetSecureAcceptThread = null;

        }
        if (uetInsecureAcceptThread != null) {
            uetInsecureAcceptThread.cancel();
            uetInsecureAcceptThread = null;

        }
        uetState = STATE_NONE;
        updateInterfaceTitle();
    }

    //write

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (uetState != STATE_CONNECTED) return;
            r = uetConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    //thong bao khi ket noi looi
    private void connectionFailed() {
        //send message back UI
        Message msg = uetHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        uetHandler.sendMessage(msg);

        uetState = STATE_NONE;

        updateInterfaceTitle();
        BlutoothConnect.this.start();
    }

    //ket noi lost

    private void connectLost() {
        //gui thong baos UI

        Message msg = uetHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        uetHandler.sendMessage(msg);

        uetState = STATE_NONE;

        updateInterfaceTitle();
        BlutoothConnect.this.start();
    }

    //chat thread trong khi socket lang nghe ket noi
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket uetServerSocket;
        private String uetSocketType;
        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            uetSocketType = secure ? "Secure" : "Insecure";
            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = uetBluetoothApdater.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_SECURE);
                    Log.d(TAG, "AccpectThread: Setting up Server: " + MY_UUID_SECURE);
                }
                else {
                    tmp = uetBluetoothApdater.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                    Log.d(TAG, "AccpectThread: Setting up Server: " + MY_UUID_INSECURE);
                }

            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + uetSocketType + "listen() failed", e);
            }
            uetServerSocket = tmp;
            uetState = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + uetSocketType + "AcceptThread Running." + this);
            setName("Accpect "+ uetSocketType);
            BluetoothSocket socket = null;
            while (uetState != STATE_CONNECTED) {
                try {
                    //ket noi thanh cong
                    socket = uetServerSocket.accept();

                }
                catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + uetSocketType + "accept() failed", e);
                    break;
                }
                if (socket != null) {
                    synchronized (BlutoothConnect.this) {
                        switch (uetState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        uetSocketType);
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
            Log.i(TAG, "END, Socket Type: " + uetSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Cancel: Cancel AcceptThread");
            try {
                uetServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Cancel: Close of AcceptThread ServerSocket");
            }
        }
    }

    private class ConnectThread extends Thread {
        // The local server socket
        private final BluetoothSocket uetSocket;
        private final BluetoothDevice uetDevice;
        private String uetSocketType;
        public ConnectThread(BluetoothDevice device, boolean secure) {
            uetDevice = device;
            BluetoothSocket tmp = null;
            uetSocketType = secure ? "Secure" : "Insecure";
            // Create a new listening server socket
            try {
                if (secure) {
                  tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);

                }
                else {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);

                }

            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + uetSocketType + "create() failed", e);
            }
            uetSocket = tmp;
            uetState = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + uetSocketType);
            setName("connect "+ uetSocketType);
            uetBluetoothApdater.cancelDiscovery();
            BluetoothSocket socket = null;

                try {
                    //ket noi thanh cong
                    uetSocket.connect();

                }
                catch (IOException e) {
                    try {
                        uetSocket.close();
                    }
                    catch (IOException e2) {
                        Log.e(TAG, "unable to close() " + uetSocketType +
                                " socket during connection failure", e2);
                    }
                    connectionFailed();
                    return;

                }
                //reset connectthread
            synchronized (BlutoothConnect.this){
                uetConnectThread = null;
            }
            //batau ket noi
            connected(uetSocket,uetDevice,uetSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Cancel: Cancel AcceptThread");
            try {
                uetSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Cancel: Close of AcceptThread ServerSocket");
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket uetSocket;
        private final InputStream uetInStream;
        private final OutputStream uetOutStream;
        public ConnectedThread(BluetoothSocket socket, String sockeType) {
            Log.d(TAG, "ConnectedThread: Starting");
            uetSocket = socket;
            InputStream uetIn = null;
            OutputStream uetOut = null;



            try {
                uetIn = uetSocket.getInputStream();
                uetOut = uetSocket.getOutputStream();

            }
            catch (IOException e) {
                e.printStackTrace();
            }
            uetInStream = uetIn;
            uetOutStream = uetOut;
            uetState = STATE_CONNECTED;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            //giu lang nghe du lieu den trong khi da ket noi
            while (uetState == STATE_CONNECTED) {
                try {
                    bytes = uetInStream.read(buffer);
                    uetHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                }
                catch (IOException e) {
                    Log.e(TAG, "Disconnected", e);
                    connectLost();

                    break;
                }
            }

        }

        public void write(byte[] buffer) {
            try {
                uetOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                uetHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                uetSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}


