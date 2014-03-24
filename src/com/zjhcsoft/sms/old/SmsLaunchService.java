package com.zjhcsoft.sms.old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.sms.DBHelper.SendLog;

public class SmsLaunchService extends Service {
	private static String TAG = "SmsLauchService";
	Timer timer;
	Timer timerSend;
	private int mCountDone = 0;
	int count_sent = 0;
	int count_fail = 0;
	public int count_delivered = 0;
	private boolean m_print_debug;/* 有多少日志都打印 */
	private int m_interval_send; /* 发送间隔 */
	private final int INTERVAL_READ=1000 * 60* 2;//扫读一次数据库的间隔
	private final int INTERVAL_READ_SHOW = INTERVAL_READ/ 1000/60 ;
	private boolean m_append_timestamp; /* 在信息加时间戳来看延时 */

	private int m_limit_hours;
	private SharedPreferences mSharedPref;
	private IntentFilter mIntentFilter;
	private TaskReceiver msgReceiver;

	@Override
	public void onCreate() {
		// 动态注册广播接收器，在下载结束后可立即开始发送
		msgReceiver = new TaskReceiver();
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(SmsDownloadService.KEY_MSG_TASK_ARRIVAL);
		// mIntentFilter.addAction(action)
		registerReceiver(msgReceiver, mIntentFilter);
		
    	mSharedPref = SmsRobotApp.getInstance().getSharedPreferences();
        mSharedPref.registerOnSharedPreferenceChangeListener(mListener);
        loadSetting();
        
		Log.i(TAG, "create service!");
		scheduleStart();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(msgReceiver);
		Log.d(TAG, "exit...");
		timer.cancel();
		LogHelper.toast(getApplicationContext(), "已停止派发服务");
		super.onDestroy();
	}

	class TaskReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent intent) {
			Bundle b = intent.getExtras();
//			if (intent.getAction().equals(SmsDownloadService.KEY_MSG_TASK_ARRIVAL)) {
				if (b == null) {
					LogHelper.toast(getApplicationContext(), "收到空的通知！！");
					Log.w(TAG, intent.getAction() + ";intent extras empty!");
					return;
				}else{
					String rows = b.getString("rows");
					LogHelper.toast(getApplicationContext(), "立即唤醒发送，条数：" + rows );
					scheduleStart();
				}
		}
		
	}
	/**
	 * 启动定时扫描
	 */
	private void scheduleStart() {
		if(timer!=null){
			timer.cancel();
			timer = null;
		}
		TimerTask task = new TimerTask(){    
			public void run(){    
			 // 在此处添加执行的代码    
				try {
					fetchSms();
				} catch (Exception e) {
					LogHelper.exception(SmsLaunchService.this, "发送服务异常：",e);
//					timer.cancel();
					e.printStackTrace();
				}
			}    
		};    
		timer = new Timer();  
		//INTERVAL_READ
		timer.schedule(task, 5000, INTERVAL_READ);//开启定时器，delay 1s后执行task 
		LogHelper.toast(getApplicationContext(), "派发启动："+ INTERVAL_READ_SHOW+"分钟一次");
	}
	
	/**
	 * 发掉缓存中的短信
	 * @param tosend TODO
	 */
	private void sendFromBuffer(ArrayList<SendLog> rows) throws Exception{
		int size = rows.size();
		if(size==0) return;
		LogHelper.toast(getApplicationContext(), "将派发："+ size + "条") ;
		
		for(int i=0; i<size; i++){
			SendLog row = rows.get(i);
			String text = row.text;
			long id = row._id;
			String target = row.target;
			int times = row.retry_times;
			
			LogHelper.toast(getApplicationContext(), ">>>>派发：id=" + id + ",times=" + times);
			Log.d(TAG, "发送中, id=" + id + ";target=" + target);
			SmsManager sms = SmsManager.getDefault();
			// 增加发送时间戳
			if (m_append_timestamp) {
				text = text + "--" + TimeConvert.getCurrTime();
				Log.d(TAG, "append timestamp, text=" + text);
			}
			ArrayList<String> msgs = sms.divideMessage(text);

			int parts = msgs.size();
			for (int n = 0; n < parts; n++) {
				String[] actions = {
						SmsDownloadService.BROAD_SENT + id + "-" + n,
						SmsDownloadService.BROAD_DELIVERED + id + "-" + n };
				// ---when the SMS has been sent---
				registerReceiver(new HandleSent(this), new IntentFilter(
						actions[0]));
				// ---when the SMS has been delivered---
				registerReceiver(new HandleDelivered(this),
						new IntentFilter(actions[1]));
			}//-for

			send(id, target, msgs, sms, this);
			// if(m_print_debug) LogHelper.toast(getApplicationContext(),
			// "++去盯:"+tracers);

			if (times == 0) {
				mCountDone++;
				LogHelper.count(getBaseContext(),
						SmsDownloadService.KEY_MSG_COUNT_PACK,
						String.valueOf(mCountDone), id);
			}// -if times==0
			
			//每条的间歇
			Thread.sleep(m_interval_send);
		}//- for send one row
	}

	/**
	 * 从库中取出短信，并做发送
	 * @throws Exception
	 */
	private void fetchSms() throws Exception {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			DBHelper dbhelper = new DBHelper(this);
			db = dbhelper.getWritableDatabase();
			
			//TODO占位符居然不能正确替换，SQL里也变字符串了? localtime!!
			String[] columns = { SendLog.ID, SendLog.TARGET, SendLog.TEXT,
					SendLog.RETRY_TIMES , " (strftime('%s', 'now', 'localtime') - strftime('%s', scan_dt))/60 as diff"};
			String selection ="((strftime('%s', 'now','localtime')- strftime('%s', scan_dt))/60 )<" + (m_limit_hours *60)  + " and "+ SendLog.STATUS + " is null" ;
					
			String[] selectionArgs = null;// new String[]{String.valueOf(m_limit_hours)};
			if(m_print_debug) LogHelper.toast(getApplicationContext(), "fetch: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
			
			Log.d(TAG, "fetch: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
			String orderBy = "retry_times, _id";
			c = db.query(SendLog.TABLE_NAME, columns, selection, selectionArgs,
					null, null, orderBy);

			ArrayList<SendLog> rowsWait = new ArrayList<SendLog>();
			// Log.d(TAG, "取未发的短信" );
			if (c != null && c.moveToFirst()) {
				Log.d(TAG, "有待发短信！");
				while(!c.isAfterLast()){
					long id = c.getLong(c.getColumnIndex(SendLog.ID));
					String target = c.getString(c.getColumnIndex(SendLog.TARGET));
					String text = c.getString(c.getColumnIndex(SendLog.TEXT));
					int times = c.getInt(c.getColumnIndex(SendLog.RETRY_TIMES));
					long diff = c.getLong(c.getColumnIndex("diff"));
					SendLog row = new SendLog(id, times, target, text);
					rowsWait.add(row);

					c.moveToNext();
					// 标记为发送中
					ContentValues valuesLog = new ContentValues();
					// valuesLog.put(SendLog.RETRY_TIMES, times);
					valuesLog.put(SendLog.STATUS, "ing");// 暂不要发了，这条
					valuesLog.put(SendLog.SEND_DT0, TimeConvert.getTimestamp());
					db.update(SendLog.TABLE_NAME, valuesLog, "_id=?",
							new String[] { String.valueOf(id) });
				}
				sendFromBuffer(rowsWait);


			}// -if cursor has row
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null)
				c.close();
			if (db != null)
				db.close();
		}
	}

	private int send(long rowid, String phoneNumber, ArrayList<String> msgs,
			SmsManager sms, Context ctx) {
		// final String TAG = "SendThread";
		Log.i(TAG + " ", "ask sms to work, rowid=" + rowid);

		StringBuffer sbTracer = new StringBuffer();

		ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
		ArrayList<PendingIntent> deliPIs = new ArrayList<PendingIntent>();
		int parts = msgs.size();
		for (int i = 0; i < parts; i++) {

			PendingIntent sentPI = null;
			PendingIntent deliveredPI = null;
			// if(i==parts -1){
			String bid1 = SmsDownloadService.BROAD_SENT + rowid + "-" + i;
			Intent sent = new Intent(bid1);
			sent.putExtra("id", rowid);
			sent.putExtra("num", i + 1);
			sent.putExtra("num_max", parts);
			sent.putExtra("print_debug", m_print_debug);

			String bid2 = SmsDownloadService.BROAD_DELIVERED + rowid + "-" + i;
			Intent deli = new Intent(bid2);
			deli.putExtra("id", rowid);
			deli.putExtra("num", i + 1);
			deli.putExtra("num_max", parts);
			deli.putExtra("print_debug", m_print_debug);
			Log.i(TAG, "prepare broadcast :" + bid1 + ";" + bid2);
			// PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
			// new Intent(mContext, SmsSentReceiver.class), 0);
			sentPI = PendingIntent.getBroadcast(ctx, 0, sent,
					PendingIntent.FLAG_CANCEL_CURRENT);
			deliveredPI = PendingIntent.getBroadcast(ctx, 0, deli,
					PendingIntent.FLAG_CANCEL_CURRENT);
			sbTracer.append(bid1 + ";");
			sbTracer.append(bid2);
			// }
			sentPIs.add(sentPI);
			deliPIs.add(deliveredPI);
		}
		// sms.sendMultipartTextMessage(phoneNumber, null, msgs, null,null);
		sms.sendMultipartTextMessage(phoneNumber, null, msgs, sentPIs, deliPIs);
		Log.d(TAG, "do send finish, phonenumber=" + phoneNumber + ",msg="
				+ msgs.toString() + ",parts=" + parts);
		return parts;
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		SmsLaunchService getService() {
			return SmsLaunchService.this;
		}
	}

	OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			Log.d(TAG, "changed:key=" + key);
			loadSetting();
			if(key.equals("interval__send_1") || key.equals("limit_hours")){
				scheduleStart();
			}
		}

	};
	/**
	 * 加载设置
	 */
	private void loadSetting() {
		Log.d(TAG, "get pref values");
		m_print_debug = mSharedPref.getBoolean("print_debug", false);
		m_interval_send = mSharedPref.getInt("interval__send_1",1000) ;
 		m_limit_hours = mSharedPref.getInt("limit_hours", 2) ;
 		
		m_append_timestamp = mSharedPref.getBoolean("append_timestamp", false);
	}
}
