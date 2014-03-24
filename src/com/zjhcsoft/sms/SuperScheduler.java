package com.zjhcsoft.sms;

import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.zjhcsoft.sms.service.SmsFetchIntentService;
import com.zjhcsoft.sms.service.SmsLaunchIntentService;
import com.zjhcsoft.sms.service.SmsTraceIntentService;

public class SuperScheduler {
	private static String TAG = SuperScheduler.class.getName();
	// 服务级变量
	private int m_interval_scan = 0;
	private int m_interval_send = 0;
	private static final int INTERVAL_STUCK_CHECK = 1000*60*10;/*检查堵塞的间隔*/
	
	private Context ctx;
	private Context baseContext;

	public SuperScheduler(Context context) {
		this.ctx = context;
		this.baseContext = context.getApplicationContext();
	}

	public void doSchedule(){
		 loadSetting();
		//采用系统时钟，开启重置、扫描服务 
		AlarmManager am=
				(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
	     
		Intent trace=new Intent(ctx.getApplicationContext(), SmsTraceIntentService.class);
		PendingIntent piTrace=PendingIntent.getService(ctx.getApplicationContext(), 0, trace, PendingIntent.FLAG_UPDATE_CURRENT);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 1000, INTERVAL_STUCK_CHECK, piTrace);				
		
		Intent fetch=new Intent(ctx.getApplicationContext(), SmsFetchIntentService.class);
		PendingIntent piFetch=PendingIntent.getService(ctx.getApplicationContext(), 0, fetch, PendingIntent.FLAG_UPDATE_CURRENT);
		
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 10000, m_interval_scan, piFetch);		
		
		
	}
	/**
	 * 执行一次服务
	 * @param serviceclass 服务类
	 * @param delay 延迟
	 * @param reqCode 请求码（用于区分不同的请求，避免合并）
	 */
	public static void oneShotNow(Class<?> serviceclass, long delay, int reqCode){
		//立即打开服务
		Context baseContext = SmsRobotApp.getInstance().getApplicationContext();
		Log.d(TAG, "open service" + serviceclass);
//		LogHelper.toast(baseContext, "立即服务...." + serviceclass);
		AlarmManager am=
				(AlarmManager)baseContext.getSystemService(Context.ALARM_SERVICE);
		 
		//系统休眠时同样执行发送
		Intent intent=new Intent(baseContext, serviceclass);
		PendingIntent pi=PendingIntent.getService(baseContext, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		am.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + delay, pi);
		return;
	}
	
	/**
	 * 执行一次性服务，允许同类任务合并执行（省电）
	 * @param serviceclass
	 * @param delay
	 */
	public static void oneShotNow(Class<?> serviceclass, long delay) {
		oneShotNow(serviceclass, delay, 0);
	}

	public void cancel(){
        Intent intent = new Intent(ctx.getApplicationContext(), SmsFetchIntentService.class);  
       /* 创建PendingIntent */  
       PendingIntent pendingIntent=PendingIntent.getBroadcast(  
               ctx.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );  
      AlarmManager am;  
      am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);  
      am.cancel(pendingIntent);  
      Log.d(TAG, "已经停止 扫描");
	}

	/**
		 * 加载设置
		 */
		private void loadSetting() {
			SharedPreferences pref;
			pref = SmsRobotApp.getInstance().getSharedPreferences();
			
			Map<String, ?> map = pref.getAll();
			Log.d(TAG, "取配置;"+map.toString());
	//		boolean useInternet = pref.getBoolean("use_internet", false);
			// String server = mPref.getString("server" + (useInternet?"1":"0" ),
			// DEFAULT_SERVER);
			// mServiceUrl = server + SERVICE_PATH;
		
			m_interval_scan = pref.getInt("interval__scan", 1) * 1000 * 60;// 分钟
			m_interval_send = pref.getInt("interval__send_1", 1000);
			// mArea = mPref.getString("area","7");
			// TelephonyManager telephonyManager =
			// (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			// mImei = telephonyManager.getSubscriberId();
			Log.i(TAG + " Service config", ";interval_scan=" + m_interval_scan
					+ "(ms);interval_send=" + m_interval_send);
		}

	
}
