package org.projectproto.yuscope;

import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.widget.ToggleButton;

public class IncomingCallReceiver extends BroadcastReceiver {                                  

	public String incomingNumber;
	public SharedPreferences prefs;	
    public static final String PATIENT_NUMBER = "patient_number";
    public String prefName = "MyPref";
    public static final String DOCTOR_NUMBER = "doctor_number";
    public static final String ANA_INCOMING_NUMBER = "ana_incoming_number";
    public static final String CALL_TRIGGER = "call_trigger";
    public static final String IP_ADDRESS = "ip_address";
    public static final String OUTGOING_NUMBER = "outgoing_number";
    public static final String CALLER_SOURCE = "caller_source";
    public static final String P_MODE = "p_mode";
    public static final String INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED = "finishActivityOnSaveCompleted";
    public ToggleButton receiver_tbnspeaker;
	public String ipaddress;
    @Override
    public void onReceive(final Context context, Intent intent) {                                         
    	
    	 // for identifying outgoing call 
    	prefs = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);        	
    	
    	String doctor_number = prefs.getString(DOCTOR_NUMBER, "");
    	ipaddress = prefs.getString(IP_ADDRESS, "");
    	String patient_number = prefs.getString(PATIENT_NUMBER, "");
    	patient_number = patient_number.replace("+65", "");
    	if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")){
    		SharedPreferences.Editor edit = prefs.edit();
            edit.putString(CALL_TRIGGER, "outgoing");
            edit.putString(OUTGOING_NUMBER, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
            edit.commit();
    	}
    	String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    	//Toast.makeText(context, state, Toast.LENGTH_LONG).show();
    	 
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
        	String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);        	
        	//prefs = context.getSharedPreferences(prefName, 2);        	
        	SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ANA_INCOMING_NUMBER, incomingNumber);
            edit.putString(CALL_TRIGGER, "incoming");
            edit.commit();
        }
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
        	
        	//prefs = context.getSharedPreferences(prefName, 2);        	
        	String p_incoming_number = prefs.getString(ANA_INCOMING_NUMBER, "");
        	
        	String outgoing_number = prefs.getString(OUTGOING_NUMBER, "");
        	outgoing_number = outgoing_number.replace("+65", "");
        	String caller_source = prefs.getString(CALLER_SOURCE, "");
        	String p_mode = prefs.getString(P_MODE, "");
        	
        //	String doctor_number = prefs.getString(DOCTOR_NUMBER, "");
        	String call_trigger = prefs.getString(CALL_TRIGGER, "");
        	if(call_trigger.equalsIgnoreCase("incoming")){
        		//Toast.makeText(context,  p_mode, Toast.LENGTH_LONG).show();
        		//Toast.makeText(context,  p_mode, Toast.LENGTH_LONG).show();
        		// for identifying the mode
        		if(p_mode.equalsIgnoreCase("doctormode")){
        			if(patient_number.contains(p_incoming_number)){
        				//Toast.makeText(context, "Doctor Mode incoming call", Toast.LENGTH_LONG).show();
	    	 			Intent newitent=new Intent();	    	 		
		    	 		newitent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
		    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
		    	 		newitent.setClassName(context,"com.dukenus.edu.sg.ReceiveUdp");
		    	 		context.startActivity(newitent);
        				
        			}
        		
        		}else if(p_mode.equalsIgnoreCase("patientmode")){
        			if(doctor_number.contains(p_incoming_number)){
        				//Toast.makeText(context, "Patient Mode incoming call", Toast.LENGTH_LONG).show();
    	 				Intent newitent=new Intent();	    	 		
    	    	 		newitent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
    	    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    	    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
    	    	 		newitent.setClassName(context,"com.dukenus.edu.sg.SensorUdp");
    	    	 		context.startActivity(newitent);

        				
        			}
        			        			
        		}
        	}
        	else if(call_trigger.equalsIgnoreCase("outgoing")){
	    	 		//Toast.makeText(context, caller_source, Toast.LENGTH_LONG).show();
	    	 		// for identifying caller source mode 
	    	 		if(caller_source.equalsIgnoreCase("patient")){
	    	 			if(doctor_number.equalsIgnoreCase(outgoing_number)){
	    	 				
	    	 			//Toast.makeText(context, "Patient outgoing call", Toast.LENGTH_LONG).show();
	    	 			TimerTask task = new TimerTask() {
			    	            @Override
			    	            public void run() {
			    	                // TODO Auto-generated method stub
			    	            	Intent sensorintent = new Intent();	    	 		
				    	 			sensorintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
					    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
					    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
				    		    	sensorintent.setClassName(context,"com.dukenus.edu.sg.SensorUdp");
				    		    	context.startActivity(sensorintent);
			    	            }
	    	 			};
			    		Timer t = new Timer();
			    		t.schedule(task, 1000);	
	    	 		}
	    	 			
	    	 			
	    	 			
	    	 		} else 	if(caller_source.equalsIgnoreCase("doctor")){
	    	 			//Toast.makeText(context, caller_source, Toast.LENGTH_LONG).show();
	    	 			//Toast.makeText(context, "Patient number is "+ patient_number, Toast.LENGTH_LONG).show();
	    	 			//Toast.makeText(context, "Outgoing number is "+ outgoing_number, Toast.LENGTH_LONG).show();
	    	 			// for doctor mode
	    	 			if(patient_number.equalsIgnoreCase(outgoing_number)){
	    	 			// Using timer for foreground the activity and avoid activity conflict
		    	 			TimerTask task = new TimerTask() {
    		    	            @Override
    		    	            public void run() {
    		    	            	Intent sensorintent = new Intent();	    	 		
    			    	 			sensorintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
    				    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    				    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
    			    		    	sensorintent.setClassName(context,"com.dukenus.edu.sg.ReceiveUdp");
    			    		    	sensorintent.putExtra("text_ip", ipaddress);
    			    		    	context.startActivity(sensorintent);
    		    	         }
		    	 			};
			    		      Timer t = new Timer();
			    		      t.schedule(task, 1000);
	    	 			}
	    	 			
	    	 		}
	    	 		
	    
        	}
        }
        // for idle mode, currently deactiviate
        /*  if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
        	String call_trigger = prefs.getString(CALL_TRIGGER, "");
        	if(call_trigger.equalsIgnoreCase("outgoing")){
    			Intent sensorintent = new Intent();	    	 		
	 			sensorintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
    	 			    |Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    	 			    |Intent.FLAG_ACTIVITY_NEW_TASK);
		    	sensorintent.setClassName(context,"com.dukenus.edu.sg.SensorUdp");
		    	context.startActivity(sensorintent);
    			
        	}
    		
    	}
       */
    }
 

}