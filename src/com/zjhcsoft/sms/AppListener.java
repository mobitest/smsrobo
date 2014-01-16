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

package com.zjhcsoft.sms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class AppListener implements WakefulIntentService.AlarmListener {
//	private static final int PERIOD=100000;   // 5 minutes
	private static final String TAG = "AppListener";
	private long counter = 0;
  public void scheduleAlarms(AlarmManager mgr, PendingIntent pi,
                             Context ctxt) {
	  mPref = SmsRobotApp.getInstance().getSharedPreferences();
	  loadSetting();
//    mSharedPref.registerOnSharedPreferenceChangeListener(mListener);
//    mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                            100,
//                            100, pi);
  mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
          SystemClock.elapsedRealtime()+60000,
          m_interval_scan,
          pi);
  }
  public void sendWakefulWork(Context ctxt) {
	  Log.d(TAG, "counter-->"+ counter+ ";loadSetting, m_interval_scan-->" + m_interval_scan);
    WakefulIntentService.sendWakefulWork(ctxt, SmsTraceService.class);
    if(counter++%6 ==0){
    	WakefulIntentService.sendWakefulWork(ctxt, SmsDownloadService2.class);
    }
  }
  public long getMaxAge() {
    return(AlarmManager.INTERVAL_DAY*5);
  }
  //服务级变量
	private int m_interval_scan=0;
	private int m_interval_send=0;
	private int m_timeout_conn = 0;
	private int m_timeout_socket = 0;
//	private final String mServer="";
	private String mServiceUrl="";
	private int mCountGet = 0;
	private int mCountSend =0;
	private int mCountScan =0;
	private int m_limit_rows_sendlog;
	private SharedPreferences mPref;
	/**
	 * 加载设置
	 */
	public void loadSetting(){
		Log.d(TAG, "get pref values");
      boolean useInternet = mPref.getBoolean("use_internet",false);
//      String server = mPref.getString("server" + (useInternet?"1":"0" ), DEFAULT_SERVER);
//      mServiceUrl = server + SERVICE_PATH;
      
      m_interval_scan =  mPref.getInt("interval__scan",4) * 1000* 60;//分钟
      m_interval_send = mPref.getInt("interval__send_1", 1000) ;
      m_timeout_conn = mPref.getInt("interval__conn",1) *1000 * 60;
      m_timeout_socket = mPref.getInt("timeout_socket", 3)*1000*60;
//      mArea = mPref.getString("area","7"); 
		m_limit_rows_sendlog = mPref.getInt("limit_rows_sendlog",5000);
//      TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//      mImei = telephonyManager.getSubscriberId();		
      Log.i(TAG + " Service config",";interval_scan=" + m_interval_scan+"(ms);interval_send="+ m_interval_send); 
	}
}
