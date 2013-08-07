package org.projectproto.yuscope;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MyPhoneStateListener extends PhoneStateListener {

	public void onCallStateChanged(int state,String incomingNumber){
		  switch(state){
		    case TelephonyManager.CALL_STATE_IDLE:
		      Log.e("DEBUG", "IDLE");
		      //Log.e("ReceiverThread", "failed to open datagram socket.");
		    	//Toast.makeText(getBasecontext(), "IDLE", Toast.LENGTH_LONG).show();
		    break;
		    case TelephonyManager.CALL_STATE_OFFHOOK:
		      Log.e("DEBUG", "OFFHOOK");
		    break;
		    case TelephonyManager.CALL_STATE_RINGING:
		      Log.e("DEBUG", "RINGING");
		    break;
		    }
	} 
}
