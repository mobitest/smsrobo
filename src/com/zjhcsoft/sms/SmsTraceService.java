package com.zjhcsoft.sms;


import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.zjhcsoft.sms.DBHelper.SendLog;

public class SmsTraceService extends WakefulIntentService {
	private static final int INTERVAL_STUCK_CHECK = 1000*60;/*检查堵塞的间隔*/
//	private onSmsStart msgReceiver;
//	private IntentFilter mIntentFilter;  

	static String TAG="SmsTraceService";

	private Timer timer;
	private boolean isRestored = false;
	
	public SmsTraceService() {
		super("SmsTraceService");
		Log.i(TAG, "I'm start!");
	}
	@Override
	  protected void doWakefulWork(Intent intent) {
	    Log.i(TAG, "I'm awake! I'm awake! (yawn)");
	  }
	@Override
	public void onCreate() {
		try {
//			if(!isRestored) restoreIng();
			watchStuck();
		}catch (Exception e) {
			LogHelper.exception(SmsTraceService.this, "检查堵塞异常：",e);
		}
//		if(timer!=null){
//			timer.cancel();
//			timer = null;
//		}
//		TimerTask task = new TimerTask(){    
//			public void run(){    
//			 // 在此处添加执行的代码    
//				try {
//					watchStuck();
//				} catch (Exception e) {
//					LogHelper.exception(SmsTraceService.this, "检查堵塞异常：",e);
//				}
//			}    
//		};    
//		timer = new Timer();  
//		//INTERVAL_READ
//		timer.schedule(task, 5000, INTERVAL_STUCK_CHECK);//开启定时器，delay 1s后执行task 
		super.onCreate();
	}
	private void recoverIng() {
		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
 		int limit_hours = pref.getInt("limit_hours", 2) ;
// 		final int delay_seconds = limit_hours* 60* 60*1000;
		Log.d(TAG, "create trace service!");
		
		//把所有处理中，状态改为空（未发，可以发）
		Log.i(TAG, "hi, 上次跟丢了,今天重来吧.更新数据库中");
		SQLiteDatabase db = null;
		try{
//			if(1==1) throw new Exception("test");
			DBHelper dbhelper = new DBHelper(SmsTraceService.this);
			db = dbhelper.getWritableDatabase();
			
			ContentValues values = new ContentValues();
			 values.put("status", (String)null);
			 String select = "status='ing' and (strftime('%s','now','localtime')- strftime('%s', scan_dt) )/60<"+ ( limit_hours* 60) ;
			long rows =db.update(SendLog.TABLE_NAME, values , select , null);
			LogHelper.toast(this, "恢复"+limit_hours + "小时内未发出的短信" + rows +"条");
		}catch(Exception e){
			LogHelper.exception(this,"跟踪服务开启异常：" ,e);
		}finally{
			db.close();	
		}
	}
	
	/**
	 * 观察堵塞
	 */
	public void watchStuck(){
 		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
 		int timeout_sent = pref.getInt("timeout_sent", 20) ;
 		boolean reboot_auto = pref.getBoolean("reboot_auto", false);
// 		final int delay_seconds = limit_hours* 60* 60*1000;
		Log.d(TAG, "stuck check on watch!");
		
		LogHelper.toast(this, "检查是否被阻塞，" + (INTERVAL_STUCK_CHECK/60000) +"分钟一次");
		//把所有处理中，状态改为空（未发，可以发）
		SQLiteDatabase db = null;
		try{
//			if(1==1) throw new Exception("test");
			DBHelper dbhelper = new DBHelper(SmsTraceService.this);
			db = dbhelper.getWritableDatabase();
			
			 String selectWhere = "status='ing' and (strftime('%s','now','localtime')- strftime('%s', "+ SendLog.SEND_DT0+") )>"+  timeout_sent* 60 ;
			Cursor c =db.query(SendLog.TABLE_NAME, new String[] {"1"} , selectWhere, null,null,null,null);
			long rows = c.getCount();
			c.close();
			if(rows==0) return;
			
			LogHelper.toast(this, "短信可能已阻塞，"+timeout_sent + "分钟未发出的达" + rows +"条");
			if(reboot_auto){
				try {
					Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot" });
					proc.waitFor();
				} catch (Exception ex) {
					Log.i(TAG, "无法重启, 可能没有超级用户权限。", ex);
					LogHelper.exception(SmsTraceService.this,"重启失败, 可能没有超级用户权限。" ,ex);
				}			
			}else{
				LogHelper.toast(getApplicationContext(), "如希望自动解除阻塞，可尝试【自动重启项】");
			}
		}catch(Exception e){
			LogHelper.exception(this,"跟踪服务开启异常：" ,e);
		}finally{
			db.close();	
		}
	}
	//TODO:定期检查是否需要重启
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	public class LocalBinder extends Binder {
		SmsTraceService getService() {
            return SmsTraceService.this;
        }

    }

}
