package org.projectproto.yuscope;

import java.io.IOException;

import org.projectproto.yuscope.UdpCommService;
import org.projectproto.yuscope.UdpCommService.ReceiverThread;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class does all the work for setting up and managing UDP
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class UdpCommService {
	// Debugging
    private static final String TAG = "UdpCommService";
    private static final boolean D = true;

    // Member fields
    private final Handler mHandler;
    private ReceiverThread mReceiverThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    private SharedPreferences bposettings = null;
    
    /**
     * Constructor. Prepares a new UDPChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public UdpCommService(Context context, Handler handler) {
        mState = STATE_NONE;
        mHandler = handler;
        
        bposettings = PreferenceManager.getDefaultSharedPreferences(context);
        
    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        // mHandler.obtainMessage(BluePulse.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        mHandler.obtainMessage(BluetoothPulseOximeter.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Start the comm service. Specifically start ReceiverThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Start the thread to listen on a UDP Socket
        if (mReceiverThread == null) {
            mReceiverThread = new ReceiverThread();
            mReceiverThread.start();
        }
        setState(STATE_LISTEN);
    }
    
    /**
    * Stop all threads
    */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mReceiverThread != null) {mReceiverThread.cancel(); mReceiverThread = null;}
        setState(STATE_NONE);
    }
    
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothPulseOximeter.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothPulseOximeter.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    class ReceiverThread extends Thread {
		DatagramSocket datagram_socket = null;
		private int PORT = Integer.parseInt(bposettings.getString("destination_port", "12345"));

		public ReceiverThread(){
			super();
			try {
				datagram_socket = new DatagramSocket(PORT);
			} catch (SocketException e) {
				Log.e("ReceiverThread", "failed to open datagram socket.");
			}
		}

		public void run() {
			byte buffer[] = new byte[5];
			DatagramPacket datagram_packet = new DatagramPacket(buffer,	buffer.length);
			int bytes = 0;

			while (true) {
				try {
					datagram_socket.receive(datagram_packet);
					bytes = datagram_packet.getLength();
					mHandler.obtainMessage(BluetoothPulseOximeter.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					e.printStackTrace();
					Log.e("UDPReceiverThread",	"failed to receive datagram packet.");
				}
			}

		}
		
		public void cancel() {
            datagram_socket.close();
        }
		
	}

    
}
