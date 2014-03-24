package com.zjhcsoft.sms.service;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.SmsManager;
import android.util.Log;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.MsgConstant;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.TimeConvert;

public class SmsLaunchIntentService extends WakefulIntentServiceAbs {
	public SmsLaunchIntentService() {
		super("SmsLaunchIntentService");
	}

	private static String TAG = "SmsLaunchIntentService";
	private boolean m_print_debug;/* 有多少日志都打印 */
	private int m_interval_send; /* 发送间隔 */
	private boolean m_append_timestamp; /* 在信息加时间戳来看延时 */
//	private final int INTERVAL_READ=1000 * 60* 2;//扫读一次数据库的间隔
//	private final int INTERVAL_READ_SHOW = INTERVAL_READ/ 1000/60 ;
	private SmsRobotApp mApp = SmsRobotApp.getInstance();

	private int m_limit_hours;

	@Override
	public void onCreate() {
		if(isSlientPeriod()) LogHelper.toast(getApplicationContext(), "非工作时间，将休眠。");
        
		Log.i(TAG, "create launch intent service!");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "exit...");
		LogHelper.toast(getApplicationContext(), "已结束派发");
		super.onDestroy();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
//		LogHelper.toast(getApplicationContext(), "开始派发");
		try {
			init();
			ArrayList<SendLog> rowsWait = getSmsQueue();
			sendAllSms(rowsWait);
		} catch (Exception e) {
			LogHelper.exception(getApplicationContext(), "派发异常", e);
			e.printStackTrace();
		}
		
	}

	/**
	 * 从库中取出短信，并做发送
	 * @throws Exception
	 */
	private ArrayList<SendLog> getSmsQueue() throws Exception {
		SQLiteDatabase db = null;
		Cursor c = null;
		ArrayList<SendLog> rowsWait = new ArrayList<SendLog>();
		try {
			DBHelper dbhelper = new DBHelper(this);
			db = dbhelper.getWritableDatabase();
			
			//TODO占位符居然不能正确替换，SQL里也变字符串了? localtime!!
			String[] columns = { SendLog.ID, SendLog.TARGET, SendLog.TEXT,
					SendLog.RETRY_TIMES , " (strftime('%s', 'now', 'localtime') - strftime('%s', scan_dt))/60 as diff"};
			String selection ="((strftime('%s', 'now','localtime')- strftime('%s', scan_dt)) )<" + (m_limit_hours *3600)  + " and "+ SendLog.STATUS + " is null" ;
					
			String[] selectionArgs = null;// new String[]{String.valueOf(m_limit_hours)};
			if(m_print_debug) LogHelper.toast(getApplicationContext(), "fetch: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
			
			Log.d(TAG, "fetch: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
			String orderBy = "retry_times, _id";
			c = db.query(SendLog.TABLE_NAME, columns, selection, selectionArgs,
					null, null, orderBy);

			// Log.d(TAG, "取未发的短信" );
			if (c != null && c.moveToFirst()) {
				Log.d(TAG, "有待发短信！");
				while(!c.isAfterLast()){
					long id = c.getLong(c.getColumnIndex(SendLog.ID));
					String target = c.getString(c.getColumnIndex(SendLog.TARGET));
					String text = c.getString(c.getColumnIndex(SendLog.TEXT));
					int times = c.getInt(c.getColumnIndex(SendLog.RETRY_TIMES));
					SendLog row = new SendLog(id, times, target, text);
					rowsWait.add(row);

					// 标记为发送中
					ContentValues valuesLog = new ContentValues();
					// valuesLog.put(SendLog.RETRY_TIMES, times);
					valuesLog.put(SendLog.STATUS, "ing");// 暂不要发了，这条
					valuesLog.put(SendLog.SEND_DT0, TimeConvert.getTimestamp());
					db.update(SendLog.TABLE_NAME, valuesLog, "_id=?",
							new String[] { String.valueOf(id) });
					
					c.moveToNext();
				}

			}// -if cursor has row
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null)	c.close();
			if (db != null)	db.close();
		}
		return rowsWait;
	}

	/**
	 * 发掉队列中的短信
	 * @param tosend TODO
	 */
	private void sendAllSms(ArrayList<SendLog> rows) throws Exception{
		int size = rows.size();
		if(size==0) return;
		LogHelper.toast(getApplicationContext(), "将派发："+ size + "条") ;
		
		for(int i=0; i<size; i++){
			SendLog row = rows.get(i);
			String text = row.text;
			long id = row._id;
			String target = row.target;
			int times = row.retry_times;
			
			LogHelper.toast(getApplicationContext(), ">>>派发：id=" + id + ",times=" + times);
			Log.d(TAG, "发送中, id=" + id + ";target=" + target);
			SmsManager sm = SmsManager.getDefault();
			// 增加发送时间戳
			if (m_append_timestamp) {
				text = text + "--" + TimeConvert.getCurrTime();
				Log.d(TAG, "append timestamp, text=" + text);
			}
			ArrayList<String> msgs = sm.divideMessage(text);
			
			/*//试用静态注册，以下去掉
			int parts = msgs.size();
			
			for (int n = 0; n < parts; n++) {
				String[] actions = {
						MsgConstant.BROAD_SENT + id + "-" + n,
						MsgConstant.BROAD_DELIVERED + id + "-" + n };
				// ---when the SMS has been sent---
				registerReceiver(new HandleSentNew(this), new IntentFilter(
						actions[0]));
				// ---when the SMS has been delivered---
				registerReceiver(new HandleDeliveredNew(this),
						new IntentFilter(actions[1]));
			}//-for
	*/
			sendSingleSms(id, target, msgs, sm, times, SmsRobotApp.getInstance().getApplicationContext());
			// if(m_print_debug) LogHelper.toast(getApplicationContext(),
			// "++去盯:"+tracers);
	
			if (times == 0) {
				mApp.gCountPacked++;
				LogHelper.count(getBaseContext(),
						MsgConstant.KEY_MSG_COUNT_PACK,
						String.valueOf(mApp.gCountPacked), id);
			}else{
				//重新打包
				mApp.gCountPackedAgain ++;
				LogHelper.count(getBaseContext(),
						MsgConstant.KEY_MSG_COUNT_PACK_AGAIN,
						String.valueOf(mApp.gCountPackedAgain), id);				
			}
			// -if times==0
			
			//每条的间歇
			Thread.sleep(m_interval_send);
		}//- for send one row
	}

	/**
	 * 发送单条短信
	 * @param rowid 标识
	 * @param phoneNumber 手机号
	 * @param msgs 短信内容（已经分拆好）
	 * @param sm 短信管理器
	 * @param times 
	 * @param ctx 
	 * @return
	 */
	private int sendSingleSms(long rowid, String phoneNumber, ArrayList<String> msgs,
			SmsManager sm, int times, Context ctx) {
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
//			String bid1 = MsgConstant.BROAD_SENT + rowid + "-" + i;
			int reqCode = Integer.valueOf(""+rowid +i);//
			String bid1 = MsgConstant.BROAD_SENT;
			Intent sent = new Intent(bid1);
			sent.putExtra("id", rowid);
			sent.putExtra("num", i + 1);
			sent.putExtra("num_max", parts);
			sent.putExtra("print_debug", m_print_debug);
			sent.putExtra("times", times);

//			String bid2 = MsgConstant.BROAD_DELIVERED + rowid + "-" + i;
			String bid2 = MsgConstant.BROAD_DELIVERED;
			Intent deli = new Intent(bid2);
			deli.putExtra("id", rowid);
			deli.putExtra("num", i + 1);
			deli.putExtra("num_max", parts);
			deli.putExtra("print_debug", m_print_debug);
			deli.putExtra("times", times);
			Log.i(TAG, "prepare broadcast :" + bid1 + ";" + bid2);
			// PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
			// new Intent(mContext, SmsSentReceiver.class), 0);
			sentPI = PendingIntent.getBroadcast(ctx, reqCode, sent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			deliveredPI = PendingIntent.getBroadcast(ctx, reqCode, deli,
					PendingIntent.FLAG_UPDATE_CURRENT);
			sbTracer.append(bid1 + ";");
			sbTracer.append(bid2);
			// }
			sentPIs.add(sentPI);
			deliPIs.add(deliveredPI);
		}
		// sms.sendMultipartTextMessage(phoneNumber, null, msgs, null,null);
		sm.sendMultipartTextMessage(phoneNumber, null, msgs, sentPIs, deliPIs);
		Log.d(TAG, "do send finish, phonenumber=" + phoneNumber + ",msg="
				+ msgs.toString() + ",parts=" + parts);
		return parts;
	}


	/**
	 * 加载发送设置
	 */
	private void init() {
		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
		m_print_debug = pref.getBoolean("print_debug", false);
		m_interval_send = pref.getInt("interval__send_1",1000) ;
 		m_limit_hours = pref.getInt("limit_hours", 2) ;
 		
		m_append_timestamp = pref.getBoolean("append_timestamp", false);
	}

}
