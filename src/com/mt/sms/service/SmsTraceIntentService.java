package com.mt.sms.service;


import java.util.Arrays;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mt.sms.DBHelper;
import com.mt.sms.LogHelper;
import com.mt.sms.MsgConstant;
import com.mt.sms.SmsRobotApp;
import com.mt.sms.SuperScheduler;
import com.mt.sms.DBHelper.SendLog;

/**
 * 发送情况跟踪（重启后的状态重置、阻塞检查）
 * @author Administrator
 * ---------------
 * history
 * v1.1.15 |修改重启后，延迟超限的状态未重置，导致反复重启	|rock	| 10:21 2014/2/12
 */

public class SmsTraceIntentService extends WakefulIntentServiceAbs {
//	private onSmsStart msgReceiver;
//	private IntentFilter mIntentFilter;  

	static String TAG="SmsTraceIntentService";

	private SmsRobotApp mApp = SmsRobotApp.getInstance();

	private int m_limit_hours;

	private int m_timeout_sent;
	
	public SmsTraceIntentService() {
		super("SmsTraceService");
		Log.i(TAG, "I'm start!");
	}
	@Override
	  protected void doWakefulWork(Intent intent) {
		Log.i(TAG, "I'm awake! I'm awake! (yawn)");
		//开机后未执行过恢复,则恢复一次
		if(mApp.gIsRebooted){
			resetStatusAfterReboot();
			mApp.gIsRebooted = false;
		}
		startService(new Intent(SmsTraceIntentService.this, SmsStatsIntentService.class));
		
		watchStuck();
	  }
	@Override
	public void onCreate() {
		if(isSlientPeriod()) LogHelper.toast(getApplicationContext(), "非工作时间，将休眠");
		super.onCreate();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	/**
	 * 将未发送完成的短信状态重置
	 */
	private void resetStatusAfterReboot() {
		
		SharedPreferences pref = mApp.getSharedPreferences();
 		m_limit_hours = pref.getInt("limit_hours", 3) ;
// 		final int delay_seconds = limit_hours* 60* 60*1000;
		
		//把所有处理中，状态改为空（未发，可以发）
		Log.i(TAG, "hi, 上次跟丢了,重置发送中的短信状态");
		SQLiteDatabase db = null;
		try{
//			if(1==1) throw new Exception("test");
			DBHelper dbhelper = new DBHelper(SmsTraceIntentService.this);
			db = dbhelper.getWritableDatabase();
			String selectWhereNosense = "status='ing' and (strftime('%s','now','localtime')- strftime('%s', "+ SendLog.SEND_DT0+") )>"+  m_timeout_sent* 60  +" and (strftime('%s','now','localtime')- strftime('%s', scan_dt) )>="+ ( m_limit_hours* 3600);
			ContentValues values1 = new ContentValues();
			values1.put("status", MsgConstant.STATUS_FAIL);
			int rows1 = db.update(SendLog.TABLE_NAME, values1 , selectWhereNosense , null);
			LogHelper.toast(this, "丢弃"+m_limit_hours + "未发出的短信" + rows1 +"条");
			 
			ContentValues values = new ContentValues();
			 values.put("status", (String)null);
			 String select = "status='ing' and (strftime('%s','now','localtime')- strftime('%s', scan_dt) )<"+ ( m_limit_hours* 3600) ;
			int rows =db.update(SendLog.TABLE_NAME, values , select , null);
			LogHelper.toast(this, "恢复"+m_limit_hours + "小时内未发出的短信" + rows +"条");
			
			if(rows>0){
				mApp.gCountQueue += rows;
				sendNow();
			}
		}catch(Exception e){
			LogHelper.exception(this,"重置发送状态时发生异常" ,e);
		}finally{
			db.close();	
		}
	}
	private void sendNow() {
		//5.立即打开发送服务
		SuperScheduler.oneShotNow(SmsLaunchIntentService.class, 1000);
		Log.d(TAG, "open sms send service");
		LogHelper.toast(getApplicationContext(), "立即打开发送服务....");
//		AlarmManager am=
//				(AlarmManager)mApp.getSystemService(Context.ALARM_SERVICE);
//		 
//		//系统休眠时同样执行发送
//		Intent intent=new Intent(mApp.getApplicationContext(), SmsLaunchIntentService.class);
//		PendingIntent pi=PendingIntent.getService(mApp, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//		am.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 1000, pi);
	}
	
	/**
	 * 观察堵塞
	 */
	private void watchStuck(){
 		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
 		m_timeout_sent = pref.getInt("timeout_sent", 20);
 		boolean reboot_auto = pref.getBoolean("reboot_auto", false);
 		m_limit_hours = pref.getInt("limit_hours", 3) ;
// 		final int delay_seconds = limit_hours* 60* 60*1000;
		Log.d(TAG, "stuck check on watch!");
		
		LogHelper.toast(this, "检查是否被阻塞" );
		//把所有处理中，状态改为空（未发，可以发）
		SQLiteDatabase db = null;
		try{
//			if(1==1) throw new Exception("test");
			DBHelper dbhelper = new DBHelper(SmsTraceIntentService.this);
			db = dbhelper.getWritableDatabase();
			
			 String selectWhere = "status='ing' and (strftime('%s','now','localtime')- strftime('%s', "+ SendLog.SEND_DT0+") )>"+  m_timeout_sent* 60 ;
			Cursor c =db.query(SendLog.TABLE_NAME, new String[] {"1"} , selectWhere, null,null,null,null);
			long rows = c.getCount();
			c.close();
			if(rows>0){
			
				LogHelper.toast(this, "短信可能已阻塞，" + rows +"条超过"+m_timeout_sent + "分钟仍未发出");
				if(reboot_auto){
					try {
						LogHelper.toast(this, "15秒后执行重启");
						int time = 15000;
						Thread.sleep(time);
						Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot" });
						proc.waitFor();
					} catch (Exception ex) {
						Log.i(TAG, "无法重启, 可能没有超级用户权限。", ex);
						LogHelper.exception(SmsTraceIntentService.this,"执行重启失败" ,ex);
					}			
				}else{
					LogHelper.toast(getApplicationContext(), "如希望自动解除阻塞，可尝试设置为【自动重启】");
				}//- if reboot_auto 
			}else{//- if rows>0
				LogHelper.toast(mApp, "未阻塞");
				
				//是否有需要发送的
				//TODO占位符居然不能正确替换，SQL里也变字符串了? localtime!!
				String[] columns = { SendLog.ID, SendLog.TARGET, SendLog.TEXT,
						SendLog.RETRY_TIMES , " (strftime('%s', 'now', 'localtime') - strftime('%s', scan_dt))/60 as diff"};
				String selection ="((strftime('%s', 'now','localtime')- strftime('%s', scan_dt)) )<" + (m_limit_hours *3600)  + " and "+ SendLog.STATUS + " is null" ;
						
				String[] selectionArgs = null;// new String[]{String.valueOf(m_limit_hours)};
				
				Log.d(TAG, "fetch: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
				String orderBy = "retry_times, _id";
				c = db.query(SendLog.TABLE_NAME, columns, selection, selectionArgs,
						null, null, orderBy);
				if(c.getCount()>0){
					sendNow();
				}
				
			}//- rows
		}catch(Exception e){
			LogHelper.exception(this,"堵塞跟踪异常：" ,e);
		}finally{
			db.close();	
		}
	}
}
