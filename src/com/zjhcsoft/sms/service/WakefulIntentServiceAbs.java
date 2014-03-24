/***
  Copyright (c) 2009-11 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.zjhcsoft.sms.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.TimeConvert;

abstract public class WakefulIntentServiceAbs extends IntentService {
	protected static String TAG = "WakefulIntentService";
  static final String NAME=
      "com.commonsware.cwac.wakeful.WakefulIntentService";
  static final String LAST_ALARM="lastAlarm";
  private static volatile PowerManager.WakeLock lockStatic=null;

  synchronized private static PowerManager.WakeLock getLock(Context context) {
    if (lockStatic == null) {
      PowerManager mgr=
          (PowerManager)context.getSystemService(Context.POWER_SERVICE);

      lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
      lockStatic.setReferenceCounted(true);
    }

    return(lockStatic);
  }

  


//  public static void cancelAlarms(Context ctxt) {
//    AlarmManager mgr=
//        (AlarmManager)ctxt.getSystemService(Context.ALARM_SERVICE);
//    Intent i=new Intent(ctxt, AlarmReceiver.class);
//    PendingIntent pi=PendingIntent.getBroadcast(ctxt, 0, i, 0);
//
//    mgr.cancel(pi);
//  }

  public WakefulIntentServiceAbs(String name) {
    super(name);
    setIntentRedelivery(true);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

	  super.onStartCommand(intent, flags, startId);
    return(START_REDELIVER_INTENT);
  }

@Override
protected void onHandleIntent(Intent intent) {
	if(isSlientPeriod()){
		Log.w(TAG, "静默时间，不执行任何工作！");
		return;
	}
	
    PowerManager.WakeLock lock=getLock(this.getApplicationContext());

    if (!lock.isHeld() ) {
      lock.acquire();
    }

    try {
        doWakefulWork(intent);
      } finally {
//    	lock=getLock(this.getApplicationContext());
        if (lock.isHeld()) {
        	lock.release();     
        }
      }
	
}
//如果在凌晨，则不执行睁眼任务

protected boolean isSlientPeriod(){
	SmsRobotApp app = SmsRobotApp.getInstance();
	SharedPreferences pref = app.getSharedPreferences();
	long now = System.currentTimeMillis();
	final String timestop= pref.getString("timestop","2330").replaceAll(":", "").replaceAll("：", "") +"00";
	final String timestart= pref.getString("timestart","0600").replaceAll(":", "").replaceAll("：", "")  +"00";
	String now_str = TimeConvert.time2Str(now, "HHmmss");
	Log.w(TAG, "time2-->" + timestart+";(int)-->"+ Integer.valueOf(timestart) 
		+ "; now_str-->" + now_str + "; now int-->" + Integer.valueOf(now_str)
		+ "; is slient-->" + (Integer.valueOf(now_str) >Integer.valueOf(timestart))
		);
	return (Integer.valueOf(now_str) <Integer.valueOf(timestart) || Integer.valueOf(now_str) >Integer.valueOf(timestop));
	
}

abstract protected void doWakefulWork(Intent intent);

@Override
/**
 * 不许绑定
 */
public IBinder onBind(Intent intent) {
	return null;
}


}
