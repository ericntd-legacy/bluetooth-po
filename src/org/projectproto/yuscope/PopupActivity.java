package org.projectproto.yuscope;

import android.app.Activity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.apache.http.conn.util.InetAddressUtils;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class PopupActivity extends Activity {
	private String senderPhone;
	String senderPhonevalue;
	Button btnok;
	Button btncancel;
	ListenSms listemsms;
	
    public SharedPreferences prefs;	
    public static final String PATIENT_NUMBER = "patient_number";
    public String prefName = "MyPref";
    
 
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		
		super.onCreate(savedInstanceState);        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popupview);
        
                
        
        // get patient phone number
        Bundle extras = getIntent().getExtras();
		if (extras == null) {			
			return;
		}
		senderPhonevalue = extras.getString("sender_ph");
		prefs = getSharedPreferences(prefName, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
    	SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PATIENT_NUMBER, senderPhonevalue);
        edit.commit();
    	
	}
	
	
	public void btnOkclick(View view) {		
    	String ipaddress=getipAddress();    
    	String replymessage;
    	if(ipaddress == null){
    		replymessage = "doctorbusy";
    		sendSMS(senderPhonevalue, replymessage);
    		 Intent busyintent = new Intent(Intent.ACTION_CALL); 
    	     busyintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
    	     busyintent.setClass(this,DialogActivity.class);
    	     busyintent.putExtra("error_type", 2);
    	     startActivity(busyintent);
    		//Toast.makeText(getBaseContext(), "ipaddress is nothing", Toast.LENGTH_LONG).show();
    	}else{
    		replymessage = "startip" + ipaddress + "stopip";
    		sendSMS(senderPhonevalue, replymessage);
    		Intent callIntent = new Intent(); 
   		    //callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		callIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
	 			    |Intent.FLAG_ACTIVITY_SINGLE_TOP 
	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
   		    callIntent.setClass(this, BluetoothPulseOximeter.class);
   		    callIntent.putExtra("input_source_pref", "1");
   		    startActivity(callIntent);
    		//Toast.makeText(getBaseContext(), "ipaddress is not nothing", Toast.LENGTH_LONG).show();
    	}
    	//Toast.makeText(this, ipaddress, Toast.LENGTH_LONG).show();
    	finish(); 
}
	public void btnonCancel(View view) {
		
    	String reply_message= "doctorbusy";
		sendSMS(senderPhonevalue, reply_message);
		finish();
    }
	 
	// for send sms
	//---sends an SMS message to another device---
  
    private void sendSMS(String phoneNumber, String message)
    {        
        Log.v("phoneNumber",phoneNumber);
        Log.v("MEssage",message);
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, Object.class), 0);                
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, pi, null);        
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
    public void onResume()
    {
    	super.onResume();
    	setContentView(R.layout.popupview);
    }
    public void onPause()
    {
    	super.onPause();
    }
    public void onDestroy()
    {
    	super.onDestroy();
    }
}

