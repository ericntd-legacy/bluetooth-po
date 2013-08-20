package org.projectproto.yuscope;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import org.apache.http.conn.util.InetAddressUtils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class ListenSms extends Service {

	private static final String TAG = "SmsService";
	private boolean exitingKeyguardSecurely = false;
	private NotificationManager mNM;
	private final IBinder mBinder = new LocalBinder();
	private static final Object mStartingServiceSync = new Object();
	
	public SharedPreferences prefs;
	public String prefName = "MyPref";
	public static final String PATIENT_NUMBER = "patient_number";
	public static final String DOCTOR_NUMBER = "doctor_number";
	public static final String IP_ADDRESS = "ip_address";
	public static final String CALL_TRIGGER = "call_trigger";
	public static final String P_MODE = "p_mode";
	//for sound playing  
	private SoundPool soundPool;
	private HashMap<Integer, Integer> soundsMap;
	int SOUND1=1;
	int SOUND2=2;
	
	public class LocalBinder extends Binder {
		ListenSms getService() {
	            return ListenSms.this;
	        }
	 }
	
	@Override
	public IBinder onBind(Intent intent) {		
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		 
		final int myID = 1234;

		//The intent to launch when the user clicks the expanded notification
		Intent serviceintent = new Intent(this, ListenSms.class);
		serviceintent.setFlags(serviceintent.FLAG_ACTIVITY_CLEAR_TOP | serviceintent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

		//This constructor is deprecated. Use Notification.Builder instead
		Notification notice = new Notification(R.drawable.androiddukelogo2, "Tele Health Service(Listen SMS)", System.currentTimeMillis());

		//This method is deprecated. Use Notification.Builder instead.
		notice.setLatestEventInfo(this, "Tele Health SMS Service", "This service is listening sms message.", pendIntent);

		notice.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(myID, notice);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");
		filter.addAction("android.intent.action.BOOT_COMPLETED");
		filter.addAction("android.intent.action.USER_PRESENT");
        registerReceiver(myReceiver, filter);   
		
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundsMap = new HashMap<Integer, Integer>();
        soundsMap.put(SOUND1, soundPool.load(this, R.raw.sound2, 1));        
		Toast.makeText(this, "Tele Health SMS service is started.", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onCreate");
	}
	
	 public void playSound(int sound, float fSpeed) {
	        AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	        float volume = streamVolumeCurrent / streamVolumeMax;
	        soundPool.play(soundsMap.get(sound), volume, volume, 1, 0, fSpeed);
   }

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Tele Health SMS service is Stopped", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");		
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "Tele Health SMS service is started", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onStart");		
	}
	 private void triggerAppLaunch(Context context)
		{
			Intent broadcast = new Intent("com.dukenus.DocTeleHealth.WAKE_UP");					
			broadcast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 	
			context.startActivity(new Intent(broadcast));
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
				
			}catch(Exception e)
			{
				Log.e("GetMessages", "fail", e);
			}
			return retMsgs;
		}
	    
	    public String getSenderno(SmsMessage inMessage){
	    	return inMessage.getOriginatingAddress();
	    }
		private void sendPhoneDetail(Context context, Intent intent, SmsMessage inMessage){
			//String replymsg = "Hi from Android";
			SmsManager mng= SmsManager.getDefault();
			PendingIntent dummyEvent = PendingIntent.getBroadcast(context, 0, new Intent("com.example.SMSExample.IGNORE_ME"), 0);
			String addr = inMessage.getOriginatingAddress();

			if(addr == null)
			{
				Log.i("SmsIntent", "Unable to get Address from Sent Message");
			}
			String ipaddress=getipAddress();
			//String ipaddress=getLocalIpAddress();
			
			String replymsg;
			if(ipaddress == null){
				replymsg = "null";
			}else{
				replymsg = "startip" + ipaddress + "stopip"; 
			
			}
		
			try{
				mng.sendTextMessage(addr, null, replymsg, dummyEvent, dummyEvent);
			}catch(Exception e){
				Log.e("SmsIntent","SendException", e );
			}
		
		}
	    
	    public String getipAddress() {
		    try {
		    	String ipv4;
		        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		            NetworkInterface intf = en.nextElement();
		            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
		                InetAddress inetAddress = enumIpAddr.nextElement();
		               if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4=inetAddress.getHostAddress())) {
		                   //return inetAddress.getHostAddress().toString();	            	  	                   
		                   
		                   return ipv4;
		                   	                	//return inetAddress.getAddress().toString();
		                	//return inetAddress.getHostAddress();
		               }
		            }
		        }
		    } catch (SocketException ex) {
		        Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
		    }
		    return null;
		}
	    
	   public void soundPlay(int index){

	         playSound(index, 1.0f);
	         Log.d("SOUND1","hi1");

	    }	
	    
	    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
		
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
					if(message.startsWith("kaung"))
					{
						triggerAppLaunch(context);						
					}
					else if(message.startsWith("wakeup"))
					{
						
						prefs = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
				    	Editor prefsPrivateEditor = prefs.edit();
						prefsPrivateEditor.putString(P_MODE, "doctormode");
						prefsPrivateEditor.commit();
						//to pass the KeyguardManager
						ManageKeyguard.initialize(context);
						ManageWakeLock.acquireFull(context);
					    
						// to start the Receiver activity
						Intent callIntent = new Intent(Intent.ACTION_CALL); 
					    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
					    callIntent.setClass(context, PopupActivity.class);
					    
					    // get the sender phone and pass to destination app 
					    String senderphone = getSenderno(msg[i]);
					    callIntent.putExtra("sender_ph", senderphone);					     
					    startActivity(callIntent);
					    
					    // Play the notification sound and vibration
					    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						v.vibrate(600);
						soundPlay(1);
					 }
					else if(message.startsWith("startip"))
					{
						String senderphone = getSenderno(msg[i]);
						String remote_ip = message.replaceFirst("(?i)(startip)(.+?)(stopip)", "$2");
						prefs = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
				    	Editor prefsPrivateEditor = prefs.edit();
						//prefsPrivateEditor.putString(P_MODE, "patientmode");
						//prefsPrivateEditor.putString(IP_ADDRESS, message);
						//prefsPrivateEditor.putString("phone_num", senderphone);
						prefsPrivateEditor.putString("destination_host", remote_ip);
						prefsPrivateEditor.putBoolean("enable_udp_stream", true);
						prefsPrivateEditor.commit();
						Intent callIntent = new Intent(Intent.ACTION_CALL);
						callIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
		    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		    	 			    |Intent.FLAG_ACTIVITY_SINGLE_TOP 
		    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
						callIntent.setClass(context, BluetoothPulseOximeter.class);
						    
						// get the sender phone and pass to destination app 
						//String senderphone = getSenderno(msg[i]);
						callIntent.putExtra("remote_ip", message);
						//callIntent.putExtra("sender_ph", senderphone);
						
						
						startActivity(callIntent);
						
						Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						v.vibrate(600);
						soundPlay(1);
											
					}
					else if(message.startsWith("doctorbusy"))
					{
						 // for BUSY message	
					     Intent busyintent = new Intent(Intent.ACTION_CALL); 
					     busyintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
					     busyintent.setClass(context,DialogActivity.class);
					     busyintent.putExtra("error_type", 1);
					     startActivity(busyintent);
					     
					     // for vibration and sound
					     Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);						 
						 v.vibrate(600);
						 soundPlay(1);
											
					}

					
				}
			}

		}
	}; 


}
