package com.zjhcsoft.sms;

import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.activity.MainActivity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/*开机自启*/
public class AutoStart extends BroadcastReceiver{

    private static final String TAG = "AutoStart";
	@Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
         if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
     		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
    		boolean autoboot = pref.getBoolean("bootup_auto", false);
    		if(autoboot){
                Intent i = new Intent(context, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("auto", true);
                context.startActivity(i);
    		}else{
    			Log.d("autostart", "cancel, for pref option is off");
    		}
            }
    }

}