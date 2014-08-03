package com.mt.sms.receiver;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import com.mt.sms.DBHelper;
import com.mt.sms.LogHelper;
import com.mt.sms.MsgConstant;
import com.mt.sms.SmsRobotApp;
import com.mt.sms.SuperScheduler;
import com.mt.sms.TimeConvert;
import com.mt.sms.DBHelper.SendFailure;
import com.mt.sms.DBHelper.SendLog;
import com.mt.sms.service.SmsLaunchIntentService;
import com.mt.sms.service.SmsStatsIntentService;

/**
     * 短信送发的消息处理
     * @author Administrator
     *
     */
    public final class SentReceiver extends BroadcastReceiver {
		/**
		 * 
		 */
		private final String TAG = "SentReceiver!!!";


		@Override
		public void onReceive(final Context context, Intent intent) {
			SmsRobotApp app = SmsRobotApp.getInstance();
			
			String failure = null;
			Bundle b=intent.getExtras();
			final int num = b.getInt("num");
			final int num_max = b.getInt("num_max");
			final long rel_id= b.getLong("id");
			final boolean print_debug = b.getBoolean("print_debug", true);
			final int code = getResultCode();
			final int times = b.getInt("times");
			
			if(print_debug) LogHelper.toast(app.getApplicationContext(), "---反馈：" + rel_id);
			Log.w(TAG + " rec:" , "get something! id=" +rel_id + "; num/max-->" + num + "/" + num_max);
		    switch (code)
		    {
		        case Activity.RESULT_OK:
		        	//到短信的最末一部分时，更新发送状态
		        	if(num == num_max){
		        		ContentValues values = new ContentValues();
		        		values.put(SendLog.SEND_DT, TimeConvert.getTimestamp());
//		        		values.put(SendLog.STATUS, "sent");
		        		values.put(SendLog.STATUS, MsgConstant.STATUS_SENT);
		        		DBHelper db = new DBHelper(app);
		        		db.update(SendLog.TABLE_NAME, values, SendLog.ID + "=?", new String[]{String.valueOf(rel_id)});
		        		db.close();
		        		//重发成功，也计到总送达数
		        		//排队数=送达+失败+发送中
		        		app.gCountSent++;
		        		app.gCountReportLast++;
		        		LogHelper.count(app, MsgConstant.KEY_MSG_COUNT_TRACE_SENT, String.valueOf(app.gCountSent) , rel_id);//通知短信已经发出
		        		if(times==0){
		        		}else{
		        			app.gCountSentAgain++;
		        			LogHelper.count(app, MsgConstant.KEY_MSG_COUNT_TRACE_SENT_AGAIN, String.valueOf(app.gCountSentAgain) , rel_id);
		        		}
		        		
		        		if(app.gCountSent == app.gCountQueue){
		        			//TODO:发送完成。。。
		        		}
//						Intent intentB = new Intent(SmsSendService.BROAD_UPDATE);
//						intentB.putExtra(SmsSendService.KEY_MSG_COUNT_TRACE_SENT, String.valueOf(count_sent) );  
//					    sendBroadcast(intentB);//通知短信已经发出
					    
		        		Log.i(TAG + " sent:" , "OK! id=" +rel_id);
		        	}else{
		        		if(print_debug) LogHelper.toast(app, "*****分拆短信送出部分; parts"+ num +"/"+ num_max +";id=" + rel_id);//通知短信短信部分发出
		        	}
		            break;
		        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
		        	failure = "常规失败";
		            break;
		        case SmsManager.RESULT_ERROR_NO_SERVICE:
		        	failure = "无可用服务";
		            break;
		        case SmsManager.RESULT_ERROR_NULL_PDU:
		        	failure = "元数据单元";
		            break;
		        case SmsManager.RESULT_ERROR_RADIO_OFF:
		        	failure = "无线信号被关闭";
		            break;
		        default:
		        	failure="未知错误码-" +code;
		    }//-switch
		    
		    //异常处理
		    if(code!=Activity.RESULT_OK){
		    	LogHelper.exception(app,  "寄出失败" + failure, new Exception("短信寄出失败" +";id="+ rel_id +";code="+ code+ ";"+ failure));
		    	Log.w(TAG, "send failure:code=" + code +",msg="+ failure);
		    	SQLiteDatabase db = null;
		    	Cursor c = null;
		    	try{
		    		DBHelper dbhelper = new DBHelper(app);
		    		db = dbhelper.getWritableDatabase();
		    		//取出发送信息
		    		String[] columns = {SendLog.SCAN_DT, SendLog.TARGET, SendLog.TEXT, SendLog.RETRY_TIMES};
		    		String selection = SendLog.ID+"=?";
		    		String[] selectionArgs = {String.valueOf(rel_id)};
		    		c = db.query(SendLog.TABLE_NAME,columns,selection, selectionArgs,null,null,null);
		    		boolean suc = c.moveToFirst();
		    		String scan_dt="", target="", text ="";
		    		int retry_times=0;
		    		if(suc) {
		    			scan_dt = c.getString(c.getColumnIndexOrThrow(SendLog.SCAN_DT));
		    			target = c.getString(c.getColumnIndexOrThrow(SendLog.TARGET));
		    			text = c.getString(c.getColumnIndexOrThrow(SendLog.TEXT));
		    			retry_times =  c.getInt(c.getColumnIndexOrThrow(SendLog.RETRY_TIMES));
		    		}else{
		    			LogHelper.exception(app, "重发准备异常", new Exception("未找到原始记录,id=" + rel_id));
		    		}
		    		Log.w(TAG, "send failure:id=" + rel_id +", times="+ retry_times);
		    		c.close();
		    		c = null;
		    		//写失败记录
		    		ContentValues values = new ContentValues();
		    		values.put(SendFailure.REL_ID, rel_id);
		    		
		    		values.put(SendFailure.SCAN_DT, scan_dt);
		    		values.put(SendFailure.TARGET, target);
		    		values.put(SendFailure.TEXT, text);
		    		values.put(SendFailure.CODE, code);
		    		values.put(SendFailure.REASON, failure);
		    		
		    		values.put(SendFailure.STEP, "发送");
		    		values.put(SendFailure.FAIL_DT, TimeConvert.getTimestamp());
		    		long id_failure = db.insert(SendFailure.TABLE_NAME, null, values);
		    		long m_limit_rows_sendlog = 10000;
		    		//每10000条清除一次(留一半） id<20000- 10000/2
		    		if((id_failure % m_limit_rows_sendlog ) ==0){
		    			db.delete(SendFailure.TABLE_NAME,  SendFailure.ID + "< ? ", new String[]{ String.valueOf(id_failure - m_limit_rows_sendlog/2)});
		    			Log.i(TAG, "reach 10000 rows, delete " + SendFailure.TABLE_NAME);
		    		}
		    		
		    		//更新发送记录
		    		if(retry_times>=3){
		    			//三而竭,不再发送;标为死短信
		    			app.gCountReportLast++;
		    			
		    			//如果已经部分失败，计数器是否加1？失败。。。
						ContentValues valuesLog = new ContentValues();
						valuesLog.put(SendLog.STATUS, "fail");
						int rtn = db.update(SendLog.TABLE_NAME, valuesLog, "_id=? and " + SendLog.STATUS + "='ing'", new String[]{String.valueOf(rel_id)});	
						if(rtn ==1){
							app.gCountFail++;
							LogHelper.count(app, MsgConstant.KEY_MSG_COUNT_TRACE_FAIL, String.valueOf(app.gCountFail) , rel_id);
							LogHelper.toast(app,  "丢弃！已发："+retry_times +"次;id="+ rel_id);
							Log.w(TAG, "终止！已重发次数："+retry_times +";id="+ rel_id);
						}else{
							LogHelper.toast(app,  "失败; 但该记录状态已经不是发送中："+retry_times +"次;id="+ rel_id +";parts="+ num);
						}
//						LogHelper.toast(app, "部分失败，回滚发送计数：parts"+ num +"/"+ num_max +";id=" + rel_id);

		    		}else{
		    			if(print_debug) LogHelper.toast(app,  "准备重发,id=" +rel_id);
		    			//重试次数加1
		    			
		    			retry_times++;
		    			ContentValues valuesLog = new ContentValues();
		    			valuesLog.put(SendLog.RETRY_TIMES, retry_times);
		    			valuesLog.put(SendLog.STATUS, (String)null);//空，表示可以重试了。
		    			int rtn = db.update(SendLog.TABLE_NAME, valuesLog, "_id=? and " +SendLog.STATUS +"='ing'", new String[]{String.valueOf(rel_id)});
		    			//如果已经被重试，就算了
		    			if(rtn ==0){
		    				LogHelper.toast(app,  "...无需重发，id=" +rel_id +";part="+ num);
		    				retry_times--;
		    			}else{
		    				LogHelper.toast(app,  "准备重发,id=" +rel_id +";part="+ num);
		    				
		    				LogHelper.toast(app.getApplicationContext(), "需要重新发送，稍后开始....");
		    				int reqCode = Integer.parseInt(rel_id +"" + retry_times);
		    				//系统休眠时同样执行发送（15分钟后执行，-----太快重发，失败难免）
		    				SuperScheduler.oneShotNow(SmsLaunchIntentService.class,  AlarmManager.INTERVAL_FIFTEEN_MINUTES, reqCode);
		    			}
		    		}
		    	}catch(Exception e){
		    		LogHelper.exception(app.getApplicationContext(), "发送通知处理失败", e);
		    	}finally{
		    		if(c!=null) c.close();
		    		if(db!=null) db.close();
		    	}
		    }
		    //可能发送完成，处理计数变化、通知
		    if(app.gCountQueue - app.gCountSent - app.gCountFail==0){
		    	SuperScheduler.oneShotNow(SmsStatsIntentService.class, 1000);
		    }
		}
	}