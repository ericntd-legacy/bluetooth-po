/***************************************
 * 
 * Android Bluetooth Oscilloscope
 * yus	-	projectproto.blogspot.com
 * September 2010
 *  
 ***************************************/

package org.projectproto.yuscope;

//import com.lit.poc.bluepulse.R;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

import com.android.internal.telephony.ITelephony;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class BluetoothOscilloscope extends Activity implements  Button.OnClickListener{
	// Logging
	private static final String TAG = "BluetoothPulseOximeter";
	
	// Run/Pause status
    private boolean bReady = false;

    // Message types sent from the BluetoothCommService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // bt-uart constants
//    private static final int MAX_SAMPLES = 640;
//    private static final int  MAX_LEVEL	= 240;
    private static final int MAX_SAMPLES = 550;
    private static final int  MAX_LEVEL	= 350;
    private static final int  DATA_START = (MAX_LEVEL + 1);
    private static final int  DATA_END = (MAX_LEVEL + 2);

    private static final byte  REQ_DATA = 0x00;
    private static final byte  ADJ_HORIZONTAL = 0x01;
    private static final byte  ADJ_VERTICAL = 0x02;
    private static final byte  ADJ_POSITION = 0x03;

    private static final byte  CHANNEL1 = 0x01;
    private static final byte  CHANNEL2 = 0x02;

    // Layout Views
    private TextView mBTStatus;
    private TextView time_per_div;
    private TextView ch1_scale, ch2_scale;
    private TextView ch1pos_label, ch2pos_label;
    private TextView pulse_rate, pulse_sat;
    private TextView txtHost, txtPort;
    private RadioButton rb1, rb2;
    private Button timebase_inc, timebase_dec;
    private Button btn_scale_up, btn_scale_down;
    private Button btn_pos_up, btn_pos_down;
    private Button mConnectButton;
    private ToggleButton run_buton;
    
    // Name of the connected device
    //private String mConnectedDeviceName = null;
    private String mConnectedDeviceName = "No Bluetooth Device Found!";
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothCommService mRfcommClient = null;
    
    protected PowerManager.WakeLock mWakeLock;
    
    public WaveformView mWaveform = null;
    
	static String[] timebase = {"500ms", "600ms", "700ms", "800ms", "900ms", "1s", "1.1s", "1.2s", "1.4s", "1.4s", "1.5s", "1.6s", "1.7s" };
	static String[] ampscale = {"1", "5", "10", "15", "20", "25", "30", "35", "40"};
	static byte timebase_index = 5;
	static byte ch1_index = 6, ch2_index = 6;
	static byte ch1_pos = 0, ch2_pos = 0;	// 0 to 60
	private int[] ch1_data = new int[MAX_SAMPLES];
	private int[] ch2_data = new int[MAX_SAMPLES];	
	
	private int dataIndex=0, dataIndex1=0, dataIndex2=0;
	private boolean bDataAvailable=false;
	
	private int frameCount = 0;
	private int syncFrame = 0;
	
	private CheckBox chkboxEnableUDP;
	private Button buttonTestUDP;
	//private ToggleButton buttonCallDoc;
	private ImageButton buttonActivateCall;
	private ImageButton buttonEndCall;
	private ImageButton buttonSms;
	private Button buttonStream2Doc;
	private String destination_host;
	private int destination_port;
	private boolean send_udp = false;
	private DatagramSocket datagramSocket;
	
	private int HR = 0;
	private int SPO2 = 0;
	
	SharedPreferences bposettings = null;
	SharedPreferences.Editor bposettingseditor = null;
	public static final String PREF_FILE = "org.projectproto.yuscope_preferences";
	public static final int PREF_INPUT_SRC_BLUETOOTH = 0;
	public static final int PREF_INPUT_SRC_UDP = 1;
	public static final int PREF_OUTPUT_TXT = 0;
	public static final int PREF_OUTPUT_RAW = 1;
	
	// Member object for the RFCOMM services
    private UdpCommService mUdpCommClient = null;
    
    private ListenSms listenSms;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.main);
        
        bposettings = PreferenceManager.getDefaultSharedPreferences(this);
        // Log.v("SharedPreferencesName", PreferenceManager.);
        bposettingseditor = bposettings.edit();
        
        mBTStatus = (TextView) findViewById(R.id.txt_btstatus);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
        	if (mBluetoothAdapter == null) {
        		Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        		//bposettingseditor.putString("selected_input_source", "1");
        		bposettingseditor.putString("selected_input_source", "0");
            	bposettingseditor.commit();
        		//finish();
        		//return;
        	}
        }
        // Prevent phone from sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
        
        // Start SMS Listener Service
        if (bposettings.getBoolean("enable_sms_listener", true)){
        	Intent smsListener = new Intent(getApplicationContext(), ListenSms.class);        
        	startService(smsListener);
        }
        
//        Bundle extras = getIntent().getExtras();
//		if(extras != null){
//			String remote_ip = extras.getString("remote_ip");					
//			bposettingseditor.putString("destination_host", remote_ip);
//			bposettingseditor.putBoolean("enable_udp_stream", true);
//			bposettingseditor.commit();					
//		}
        
		// Initialize views
        // ch1_scale = (TextView) findViewById(R.id.txt_ch1_scale);
        // ch1_scale.setText(ampscale[ch1_index]);
        pulse_rate = (TextView) findViewById(R.id.txt_hr_value);
        pulse_sat = (TextView) findViewById(R.id.txt_spo_value);
        txtHost = (TextView) findViewById(R.id.txt_host_value);
        txtPort = (TextView) findViewById(R.id.txt_port_value);
        chkboxEnableUDP = (CheckBox) this.findViewById(R.id.chkbox_enable_udp);
		buttonTestUDP = (Button) this.findViewById(R.id.button_udp);
		//buttonCallDoc = (ToggleButton) this.findViewById(R.id.button_call);
		buttonActivateCall = (ImageButton) findViewById(R.id.btn_r_activatecall);
		buttonEndCall = (ImageButton) findViewById(R.id.btn_r_endcall);
		buttonSms = (ImageButton) findViewById(R.id.btn_sms);
		buttonStream2Doc = (Button) this.findViewById(R.id.button_data);
		mConnectButton = (Button) findViewById(R.id.button_connect);
		mWaveform = (WaveformView)findViewById(R.id.WaveformArea);
		
		SetListeners();

    }
    
    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupOscilloscope() will then be called during onActivityResult
        Bundle extras = getIntent().getExtras();
		if(extras != null){
			String remote_ip = extras.getString("remote_ip");
			if (remote_ip != null){
				Log.v("BluetoothOscilloscope", "receive message from service: remote_ip="+remote_ip);
				bposettingseditor.putString("destination_host", remote_ip);
				bposettingseditor.putBoolean("enable_udp_stream", true);
				bposettingseditor.commit();
			}
			String input_source = extras.getString("input_source_pref");
			if (input_source != null){
				Log.v("BluetoothOscilloscope", "receive message from service: input_source="+input_source);
				bposettingseditor.putString("selected_input_source", input_source);
				bposettingseditor.putBoolean("enable_udp_stream", false);
				bposettingseditor.commit();
			}
		}
        if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
        	if (!mBluetoothAdapter.isEnabled()) {
        		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        		// Otherwise, setup the Oscillosope session
        	} else {
        		if (mRfcommClient == null) setupOscilloscope();
        	}
        } else {
        	if (mUdpCommClient == null) setupOscilloscope();
        }
        RefreshSettings();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
        	if (mRfcommClient != null) {
        		// Only if the state is STATE_NONE, do we know that we haven't started already
        		if (mRfcommClient.getState() == BluetoothCommService.STATE_NONE) {
        			// Start the Bluetooth  RFCOMM services
        			mRfcommClient.start();
        		}
        	}
        } else {
        	if (mUdpCommClient.getState() == UdpCommService.STATE_NONE){
        		mUdpCommClient.start();
        	}
        }
        frameCount = 0;
        RefreshSettings();
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    private void setupOscilloscope() {

        // Initialize the BluetoothCommService to perform bluetooth connections
    	if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
    		if (mRfcommClient == null) {
    			mRfcommClient = new BluetoothCommService(this, mHandler);
    		}
    	} else {
    		if (mUdpCommClient == null){
    			mUdpCommClient = new UdpCommService(this, mHandler);
    		}
    	}
//        mWaveform = (WaveformView)findViewById(R.id.WaveformArea);
        
        frameCount = 0;
        
        for(int i=0; i<MAX_SAMPLES; i++){
        	ch1_data[i] = 0;
        	ch2_data[i] = 0;
        }
        
        RefreshSettings();
        
    }
    
    private void RefreshSettings() {
    	if (!bposettings.contains("enable_udp_stream")){
        	Log.v("setupOscilloscope", "PREF_FILE does not contain 'enable_udp_stream' key");
        	bposettingseditor.putBoolean("enable_udp_stream", false);
        	bposettingseditor.commit();
        }
        send_udp = bposettings.getBoolean("enable_udp_stream", false);
        chkboxEnableUDP.setChecked(send_udp);
        if (!bposettings.contains("destination_host")){
        	Log.v("setupOscilloscope", "PREF_FILE does not contain 'destination_host' key");
        	bposettingseditor.putString("destination_host", "127.0.0.1");
        	bposettingseditor.commit();
        }
        destination_host = bposettings.getString("destination_host", "127.0.0.1");
        txtHost.setText(destination_host);
//        editTextHost.setText(destination_host);
        if (!bposettings.contains("destination_port")){
        	Log.v("setupOscilloscope", "PREF_FILE does not contain 'destination_port' key");
        	bposettingseditor.putString("destination_port", "12345");
        	bposettingseditor.commit();
        }
        destination_port = Integer.parseInt(bposettings.getString("destination_port", "12345"));
        txtPort.setText(bposettings.getString("destination_port", "12345"));
//        editTextPort.setText(bposettings.getString("destination_port", "12345"));
        if (!bposettings.contains("enable_sms_listener")){
        	Log.v("setupOscilloscope", "PREF_FILE does not contain 'enable_sms_listener' key");
        	bposettingseditor.putBoolean("enable_sms_listener", true);
        	bposettingseditor.commit();
        }
        ChangeDestination();
        Log.v("SharedPreferences", "selected_input_source="+bposettings.getString("selected_input_source", "1"));
        Log.v("SharedPreferences", "selected_output_format="+bposettings.getString("selected_output_format", "0"));
    }
    
    private void SetListeners() {
		chkboxEnableUDP.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				send_udp = isChecked;
				bposettingseditor.putBoolean("enable_udp_stream", isChecked);
				bposettingseditor.commit();
			}
		});
		buttonTestUDP.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SendLiteralByUdp();
				Log.v("TestUdp#onClick", "ButtonSendDebugMessage");
			}
		});
		buttonActivateCall.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ActivateCall();
				Log.v("TestCallDoc#onClick", "ButtonActivateCall");
			}
		});
		buttonEndCall.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EndCall();
				Log.v("TestCallDoc#onClick", "ButtonEndCall");
			}
		});
		buttonSms.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SmsHrSpo2Values();
				Log.v("TestCallDoc#onClick", "ButtonEndCall");
			}
		});
		buttonStream2Doc.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Stream2Doc();
				Log.v("TestStream2Doc#onClick", "ButtonStream2Doc");
			}
		});
		mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				BTConnect();
			}
		});
	}
    
    void ActivateCall(){
    	String phoneNo = bposettings.getString("phone_num", "");
    	AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		   
    	if (phoneNo.length()>0)   {             
    		Intent callIntent = new Intent(Intent.ACTION_CALL);
    		// callIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
    		callIntent.setData(Uri.parse("tel:" + phoneNo));
    		startActivity(callIntent);
    		mAudioManager.setSpeakerphoneOn(true);
    	} else {
    		Toast.makeText(getBaseContext(), 
    				"Please enter doctor's phone number in 'Settings' menu.", 
    				Toast.LENGTH_SHORT).show();
    	}
		   
    }
    
    void EndCall(){
    	TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
		try {
		    // Java reflection to gain access to TelephonyManager's
		    // ITelephony getter
		    Class c = Class.forName(tm.getClass().getName());
		    Method m = c.getDeclaredMethod("getITelephony");
		    m.setAccessible(true);
		    com.android.internal.telephony.ITelephony telephonyService = (ITelephony) m.invoke(tm);
		    telephonyService.endCall();
		} catch (Exception e) {
		    e.printStackTrace(); 
		}
		Toast.makeText(getBaseContext(),"Call is ended",Toast.LENGTH_SHORT).show();
    }
    
    void SmsHrSpo2Values(){
    	String phoneNo = bposettings.getString("phone_num", "");
    	String msg = "From joe; @HR@ = "+HR+"; @spO2@ = "+SPO2+";";
        if (phoneNo.length()>0)   {             
        	sendSMS(phoneNo, msg);   
        } else {
        	Toast.makeText(getBaseContext(), 
                "Please enter doctor's phone number in 'Settings' menu.", 
                Toast.LENGTH_SHORT).show();
    	}
    }
    
    void Stream2Doc(){
    	String phoneNo = bposettings.getString("phone_num", "");
        if (phoneNo.length()>0)   {             
        	sendSMS(phoneNo, "wakeup");   
        } else {
        	Toast.makeText(getBaseContext(), 
                "Please enter doctor's phone number in 'Settings' menu.", 
                Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void sendSMS(String phoneNumber, String message)
    {      
    	
    	String SENT = "SMS_SENT";
    	String DELIVERED = "SMS_DELIVERED";
    	
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
        
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
    	
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				    case Activity.RESULT_OK:
					    Toast.makeText(getBaseContext(), "SMS sent", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					    Toast.makeText(getBaseContext(), "Generic failure", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_NO_SERVICE:
					    Toast.makeText(getBaseContext(), "No service", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_NULL_PDU:
					    Toast.makeText(getBaseContext(), "Null PDU", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_RADIO_OFF:
					    Toast.makeText(getBaseContext(), "Radio off", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				}
			}
        }, new IntentFilter(SENT));
        
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				    case Activity.RESULT_OK:
					    Toast.makeText(getBaseContext(), "SMS delivered", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case Activity.RESULT_CANCELED:
					    Toast.makeText(getBaseContext(), "SMS not delivered", 
					    		Toast.LENGTH_SHORT).show();
					    break;					    
				}
			}
        }, new IntentFilter(DELIVERED));        
    	
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);               
    }
    
    void ChangeDestination() {
		try {
			datagramSocket = null;
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			Log.e(TAG, e.toString());
		}
	}
    
    @Override
    public void  onClick(View v){

    }
    
    private void BTConnect(){
    	if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
    		Intent serverIntent = new Intent(this, DeviceListActivity.class);
    		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    	}
    }
    
    private int toScreenPos(byte position){
    	return ( (int)MAX_LEVEL - (int)position*7 - 8);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) mRfcommClient.stop();
        if (mUdpCommClient != null) mUdpCommClient.stop();
        // release screen being on
        if (mWakeLock.isHeld()) { 
            mWakeLock.release();
        }
        unregisterReceiver(smsReceiver);
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    
    private void SendMessageByUdp(String string_to_be_sent) {
		try {
			byte[] byte_array = string_to_be_sent.getBytes();
			InetAddress inet_address = InetAddress.getByName(destination_host);
			DatagramPacket datagram_packet = new DatagramPacket(byte_array,
					byte_array.length, inet_address, destination_port);
			if (datagramSocket==null) {
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.send(datagram_packet);		
		} catch (IOException io_exception) {
			datagramSocket = null;
			Log.e(TAG, io_exception.toString());
		}
	}
    
    private void SendLiteralByUdp() {
		SendMessageByUdp("[" + destination_host + ":" + destination_port + "] "
				+ mConnectedDeviceName+"\n");
	}
    
    private void SendBytesByUdp(byte[] byte_array) {
		try {
			InetAddress inet_address = InetAddress.getByName(destination_host);
			DatagramPacket datagram_packet = new DatagramPacket(byte_array,
					byte_array.length, inet_address, destination_port);
			if (datagramSocket==null) {
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.send(datagram_packet);		
		} catch (IOException io_exception) {
			datagramSocket = null;
			Log.e(TAG, io_exception.toString());
		}
	}

    // The Handler that gets information back from the BluetoothCommService
    private final Handler mHandler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothCommService.STATE_CONNECTED:
                    mBTStatus.setText(R.string.title_connected_to);
                    mBTStatus.append("\n" + mConnectedDeviceName);
                    break;
                case BluetoothCommService.STATE_CONNECTING:
                	mBTStatus.setText(R.string.title_connecting);
                    break;
                case BluetoothCommService.STATE_NONE:
                	mBTStatus.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_READ:
            	int raw, data_length, x;
                byte[] readBuf = (byte[]) msg.obj;
                data_length = msg.arg1;
                
                x = frameCount % MAX_SAMPLES;
                raw = UByte(readBuf[2]);
                ch1_data[x] = raw;
                mWaveform.set_data(ch1_data, ch2_data);
                
                if((readBuf[1] & 0x01)== 1){
                	syncFrame = 1;
                } else {
                	syncFrame++;
                }

                switch(syncFrame){
                case 2:
                case 15:
                case 21:
                case 23:
                	pulse_rate.setText(""+UByte(readBuf[3]));
                	HR = UByte(readBuf[3]);
                	break;
                case 3:
//                case 9:
//                case 16:
//                case 17:
                	pulse_sat.setText(""+UByte(readBuf[3]));
                	SPO2 = UByte(readBuf[3]);
                	break;
                }
                
                if(send_udp){
                	switch(Integer.parseInt(bposettings.getString("selected_output_format", "0"))){
                	case PREF_OUTPUT_TXT:
                		Date date = new Date();
                		//String udpMessage = String.format("%d, %d, %d, %d, %d", UByte(readBuf[0]), UByte(readBuf[1]),
                		//		UByte(readBuf[2]), UByte(readBuf[3]), UByte(readBuf[4]));
                		String udpMessage = String.format("%d, %d, %d", UByte(readBuf[2]), HR, SPO2);
                		SendMessageByUdp("OX, " + frameCount + ", " + date.getTime() + ", " + udpMessage + "\n");
                		break;
                	case PREF_OUTPUT_RAW:
                		SendBytesByUdp(readBuf);
                		break;
                	}
                }
                frameCount++;
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
        private int UByte(byte b){
        	if(b<0) // if negative
        		return (int)( (b&0x7F) + 128 );
        	else
        		return (int)b;
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
        	if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
        		// When DeviceListActivity returns with a device to connect
        		if (resultCode == Activity.RESULT_OK) {//1 device is discovered - pair or not pair?
        			// Get the device MAC address
        			String address = data.getExtras()
        			.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        			// Get the BLuetoothDevice object
        			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        			// Attempt to connect to the device
        			mRfcommClient.connect(device);
        		}
        	}
            break;
        case REQUEST_ENABLE_BT:
        	if (Integer.parseInt(bposettings.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH){
        		// When the request to enable Bluetooth returns
        		if (resultCode == Activity.RESULT_OK) {
        			// Bluetooth is now enabled, so set up the oscilloscope
        			setupOscilloscope();
        		} else {
        			// User did not enable Bluetooth or an error occured
        			Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
        			finish();
        		}
        	}
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.mainmenu, menu);
    	Log.v("BluetoothOscilloscope", "onCreateOptionsMenu");
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	Log.v("BluetoothOscilloscope#MenuItem", "" + item.getItemId());
    	Log.v("BluetoothOscilloscope#R.id.settings", "" + R.id.settings);
    	if (item.getItemId() == R.id.settings){
    		Intent intent = new Intent().setClass(this, MenuSettingsActivity.class);
    		this.startActivityForResult(intent, 0);
    	}
    	return true;
    }
    
    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
		
    	@Override
    	public void onReceive(Context context, Intent intent) 
    	{
    		if(!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
    		{
    			return;
    		}
    		SmsMessage msg[] = getMessagesFromIntent(intent);

    		for(int i=0; i < msg.length; i++)
    		{
    			String message = msg[i].getDisplayMessageBody();
    			if(message != null && message.length() > 0)
    			{
    				Log.i("MessageListener:",  message);
    				// to check sms keyword.. need to define the keyword
    				if(message.startsWith("startip"))
    				{
    					//String senderphone = msg[i].getOriginatingAddress();
    					String remote_ip = message.replaceFirst("(?i)(startip)(.+?)(stopip)", "$2");
    					bposettingseditor.putString("destination_host", remote_ip);
    					bposettingseditor.putBoolean("enable_udp_stream", true);
    					bposettingseditor.putString("selected_output_format", "1");
    					bposettingseditor.commit();
    					destination_host = bposettings.getString("destination_host", "127.0.0.1");
    			        txtHost.setText(destination_host);
    			        send_udp = bposettings.getBoolean("enable_udp_stream", false);
    			        chkboxEnableUDP.setChecked(send_udp);
    				}
    			}
    		}
    	}
    	
    	private SmsMessage[] getMessagesFromIntent(Intent intent)
		{
			SmsMessage retMsgs[] = null;
			Bundle bdl = intent.getExtras();
			try{
				Object pdus[] = (Object [])bdl.get("pdus");
				retMsgs = new SmsMessage[pdus.length];
				for(int n=0; n < pdus.length; n++)
				{
					byte[] byteData = (byte[])pdus[n];
					retMsgs[n] = SmsMessage.createFromPdu(byteData);
				}	
				
			}catch(Exception e)	{
				Log.e("GetMessages", "fail", e);
			}
			return retMsgs;
		}
    	
    };


}
