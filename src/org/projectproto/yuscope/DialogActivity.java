package org.projectproto.yuscope;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;



public class DialogActivity extends Activity {	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity);
        int error_type;
        String n_message = "";
        Bundle extras = getIntent().getExtras();
		if(extras != null){
			error_type = extras.getInt("error_type");		
			
		
			switch (error_type) {
			   case 1:
				   n_message = "Doctor show Busy signal, Please try again later."; 
				   break;
			   case 2:
				   n_message = "Sorry, Your mobile is not connected to internet, Please try again later.";
				   break;
			   case 3:
				   n_message = "Sorry, Your mobile is not connected to internet.";
				   break;
			   
			}

		
        
        AlertDialog alertDialog = new AlertDialog.Builder(DialogActivity.this).create();
	    alertDialog.setTitle("TeleHealth Notification");
	    alertDialog.setMessage(n_message);
	    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	   
	         //here you can add functions
	        	finish();
	   
	      } }); 
	    alertDialog.setIcon(R.drawable.androiddukelogo2);
	    alertDialog.show();
         
	}
	}
    public void onResume()
    {
    	super.onResume();
    	setContentView(R.layout.dialog_activity);
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
