package com.mt.sms.service;


import java.util.ArrayList;
import java.util.Arrays;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import com.mt.sms.DBHelper;
import com.mt.sms.LogHelper;
import com.mt.sms.MsgConstant;
import com.mt.sms.SmsRobotApp;
import com.mt.sms.DBHelper.SendLog;
import com.mt.sms.activity.MainActivity;
import com.mt.smsrobo.R;

/**
 * 统计服务
 * @author Administrator
 * 
 */
public class SmsStatsIntentService extends Service {
//	private onSmsStart msgReceiver;
//	private IntentFilter mIntentFilter;  

	static String TAG="SmsStatsIntentService";

	private SmsRobotApp mApp = SmsRobotApp.getInstance();

	public SmsStatsIntentService() {
		Log.i(TAG, "start......>>>>>");
	}
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		statsUpdate();
		return super.onStartCommand(intent, flags, startId);
	}
	/**
	 * 更新统计信息
	 */
	private void statsUpdate(){
		SQLiteDatabase db = null;
		try{
//			if(1==1) throw new Exception("test");
			DBHelper dbhelper = new DBHelper(SmsStatsIntentService.this);
			db = dbhelper.getWritableDatabase();
//			String[] columns = { "count(*)", SendLog.STATUS};
			String selection = SendLog.SCAN_DT + ">datetime('now','localtime','start of day') ";// and "+ SendLog.SCAN_DT + "<datetime('now','localtime','+25 hours','start of day')";
			String[] selectionArgs = null;
			
			Log.d(TAG, "stats: selection-->"+ selection +"; arg-->"+ Arrays.toString(selectionArgs));
//			String orderBy = SendLog.STATUS;
			String groupBy = SendLog.STATUS;
			String sql = "select count(*) as cnt, " + SendLog.STATUS +" from " + SendLog.TABLE_NAME 
					+ " where " + selection
					+ " group by " + groupBy;
			Cursor c = db.rawQuery(sql, selectionArgs);
			
			int rows = c.getCount();
			if(rows>0){
				String vals[] = new String[rows +2];
				String keys[] = new String[rows +2];
				int ii=0;
				int total = 0;
				int fail=0;
				int suc=0;
				while(c.moveToNext()){
					Log.d(TAG, "vals-->" + c.getInt(0) );
					Log.d(TAG, "keys-->" + c.getString(1) );
					int cnt =  c.getInt(0);
					vals[ii] = String.valueOf( cnt);
					keys[ii] = c.getString(1);
					if(keys[ii] ==null) {
						keys[ii] = MsgConstant.STATUS_PROCESS;
					}
					//对失败数| 成功数（寄出+送达）进行统计
					if(keys[ii].equals(MsgConstant.STATUS_FAIL)){
						fail += cnt;
					}else if(keys[ii].equals(MsgConstant.STATUS_SENT) || keys[ii].equals(MsgConstant.STATUS_DELIVERED)){
						suc += cnt;
					}
					
					total +=cnt;
					ii++;
				}
				//加汇总信息
				vals[ii] =String.valueOf( total );
				keys[ii] = MsgConstant.STATUS_TOTAL;
				
				//统计速度
				selection = SendLog.STATUS +" in('delivered','failed','sent') ";
				String sql2 = "select avg((strftime('%s', " + SendLog.SEND_DT +")- strftime('%s', "+ SendLog.SCAN_DT +")) )/60 as delay from " + SendLog.TABLE_NAME 
						+ " where " + selection;
				Cursor c2 = db.rawQuery(sql2, selectionArgs);
				if(c2.moveToFirst()){
					float delay = Math.round(c2.getFloat(0) *100)/100;
					vals[ii +1] = String.valueOf(delay);
					keys[ii +1] = MsgConstant.STATUS_DELAY;
				}
				c2.close();
				
				
				LogHelper.msg(mApp, MsgConstant.BROAD_ACTION_STATS, keys, vals);
				
				//报告条件：
				//0.今天有发送
				//1,最近有发送，且未发送过报告//
				//2.全部发完：无发送中，无待发
				if(total>0 
						&& (total - fail - suc==0)
						&& (mApp.gCountReportLast>0)
					){
					reportSummary(total, suc, fail);
						
					//报告结束时，计数器置0
					mApp.gCountReportLast =0;
				}
			}else{
				Log.d(TAG, "无统计信息。");
			}
			c.close();

			
		}catch(Exception e){
			LogHelper.exception(this,"统计异常" ,e);
		}finally{
			db.close();	
		}			
		
	}
	
	private void reportSummary(int total, int suc, int fail){
		//报告内容：赞，已处理完短信XX条，全部发送成功。
		//报告内容2：已处理完短信XX条，有XX条发送失败，请核查。
		StringBuffer sb =new StringBuffer("今天已处理短信");
		sb.append(total).append("条，");
		if(fail==0){
			sb.insert(0, "赞！").append("全部成功。");
		}else{
			sb.append("有").append(fail).append("发送失败。");
		}
		addNotificaction("发送完成",sb.toString());
		
		SharedPreferences sp = mApp.getSharedPreferences();
		boolean finish_sms = sp.getBoolean("finish_sms", true);
		String admin_number = sp.getString("admin_number", "");
		if(finish_sms && admin_number.length()>0){
			SmsManager sm = SmsManager.getDefault();
			ArrayList<String> msgs = sm.divideMessage(sb.toString());
			sm.sendMultipartTextMessage(admin_number, null, msgs, null, null);
		}
	}
	
	/** 
     * 添加一个notification 
     */  
    @SuppressWarnings("deprecation")
	private void addNotificaction(String contentTitle, String contentText) {
        NotificationManager manager = (NotificationManager) this  
        .getSystemService(Context.NOTIFICATION_SERVICE);  
        // 创建一个Notification  
        Notification noti = new Notification();  
        // 设置显示在手机最上边的状态栏的图标  
        noti.icon = R.drawable.ic_launcher;  
        // 当当前的notification被放到状态栏上的时候，提示内容  
        noti.tickerText = "短信发送结束";  
          
        // 添加声音提示  
        noti.defaults=Notification.DEFAULT_SOUND;  
        // audioStreamType的值必须AudioManager中的值，代表着响铃的模式  
        noti.audioStreamType= android.media.AudioManager.ADJUST_LOWER;  
        noti.flags |=Notification.FLAG_AUTO_CANCEL  ;
        noti.flags |= Notification.FLAG_SHOW_LIGHTS;
          
        //下边的两个方式可以添加音乐  
        //notification.sound = Uri.parse("file:///sdcard/notification/ringer.mp3");   
        //notification.sound = Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, "6");   
        Intent toLaunch = new Intent(this, MainActivity.class);  
        toLaunch .setAction("android.intent.action.MAIN");
        toLaunch .addCategory("android.intent.category.LAUNCHER");
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, toLaunch, PendingIntent.FLAG_ONE_SHOT);  
        
        noti.setLatestEventInfo(this, contentTitle + "-" +getString(R.string.app_name), contentText, pendingIntent);
        manager.notify(1, noti);  
    }
          
}
