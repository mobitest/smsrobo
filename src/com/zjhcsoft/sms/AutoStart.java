package com.zjhcsoft.sms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.zjhcsoft.sms.activity.MainActivity;

/*开机自启*/
public class AutoStart extends BroadcastReceiver{

    private static final String TAG = "AutoStart";
	@Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
         if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
        	 SmsRobotApp app = SmsRobotApp.getInstance();
        	 SharedPreferences pref = app.getSharedPreferences();
        	 boolean autoboot = pref.getBoolean("bootup_auto", false);
        	 if(autoboot){
        		 app.gIsRebooted = true;
        		 Intent i = new Intent(context, MainActivity.class);
        		 i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		 i.putExtra("auto", true);
        		 context.startActivity(i);
        		 
        		 //定时服务（自动恢复、阻塞检测、扫描下载、发送），
        	 }else{
        		 Log.d(TAG, "cancel, for pref option is off");
        	 }
         }
    }

}