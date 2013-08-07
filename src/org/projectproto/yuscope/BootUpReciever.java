package org.projectproto.yuscope;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootUpReciever extends BroadcastReceiver {

	SharedPreferences bposettings = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		bposettings = PreferenceManager.getDefaultSharedPreferences(context);
		if (bposettings.getBoolean("enable_sms_listener", true)){
			if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
				Intent i = new Intent();
				i.setAction("org.projectproto.yuscope.ListenSms");
				context.startService(i);
			}
		}
	}
}
