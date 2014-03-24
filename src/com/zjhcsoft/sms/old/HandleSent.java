package com.zjhcsoft.sms.old;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.sms.DBHelper.SendFailure;
import com.zjhcsoft.sms.DBHelper.SendLog;

/**
     * 短信送发的消息处理
     * @author Administrator
     *
     */
    public final class HandleSent extends BroadcastReceiver {
		/**
		 * 
		 */
		private final SmsLaunchService caller;
		private final String TAG = "BroadcastRecSent";

		/**
		 * @param caller
		 */
		HandleSent(SmsLaunchService caller) {
			this.caller = caller;
		}

		@Override
		public void onReceive(final Context context, Intent intent) {
			String failure = null;
			Bundle b=intent.getExtras();
			final int num = b.getInt("num");
			final int num_max = b.getInt("num_max");
			final long rel_id= b.getLong("id");
			final boolean print_debug = b.getBoolean("print_debug", true);
			final int code = getResultCode();
			
			if(print_debug) LogHelper.toast(this.caller.getApplicationContext(), "---反馈：" + rel_id);
			Log.w(TAG + " rec:" , "get something! id=" +rel_id);
		    switch (code)
		    {
		        case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), "SMS sent",
//                                Toast.LENGTH_SHORT).show();
		            //当未知Intent包含的内容，则需要通过以下方法来列举
		        	//到短信的最末一部分时，更新发送状态
		        	if(num == num_max){
		        		ContentValues values = new ContentValues();
		        		values.put(SendLog.SEND_DT, TimeConvert.getTimestamp());
		        		values.put(SendLog.STATUS, "sent");
		        		DBHelper db = new DBHelper(this.caller);
		        		db.update(SendLog.TABLE_NAME, values, SendLog.ID + "=?", new String[]{String.valueOf(rel_id)});
		        		db.close();
		        		this.caller.count_sent++;
		        		LogHelper.count(this.caller, SmsDownloadService.KEY_MSG_COUNT_TRACE_SENT, String.valueOf(this.caller.count_sent) , rel_id);//通知短信已经发出
//						Intent intentB = new Intent(SmsSendService.BROAD_UPDATE);
//						intentB.putExtra(SmsSendService.KEY_MSG_COUNT_TRACE_SENT, String.valueOf(count_sent) );  
//					    sendBroadcast(intentB);//通知短信已经发出
					    
		        		Log.w(TAG + " sent:" , "OK! id=" +rel_id);
		        	}else{
		        		if(print_debug) LogHelper.toast(this.caller, "*****分拆短信送出部分; parts"+ num +"/"+ num_max +";id=" + rel_id);//通知短信短信部分发出
//		        		Intent intentB = new Intent(SmsSendService.BROAD_UPDATE);
//		        		intentB.putExtra(SmsSendService.KEY_MSG_TOAST, "分拆短信送出部分; parts"+ num +"/"+ num_max);
//		        		sendBroadcast(intentB);//通知短信部分发出
//		        		killMe = false;
		        	}
		            break;
		        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
		        	failure = "Generic failure";
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
		    	LogHelper.error(this.caller,  "--失败;id="+ rel_id +";code="+ code+ ";"+ failure);
		    	SQLiteDatabase db = null;
		    	Cursor c = null;
		    	try{
		    		Log.w(TAG, "send failure:code=" + code +",msg="+ failure);
		    		DBHelper dbhelper = new DBHelper(this.caller);
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
		    		values.put(SendFailure.STEP, "发送");
		    		values.put(SendFailure.CODE, code);
		    		values.put(SendFailure.REASON, failure);
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
		    			//三而竭,不再发送
		    			
		    			//如果已经部分失败，计数器是否加1？失败。。。
						ContentValues valuesLog = new ContentValues();
						valuesLog.put(SendLog.STATUS, "fail");
						int rtn = db.update(SendLog.TABLE_NAME, valuesLog, "_id=? and " + SendLog.STATUS + "='ing'", new String[]{String.valueOf(rel_id)});	
						if(rtn ==1){
							this.caller.count_fail++;
							LogHelper.count(this.caller, SmsDownloadService.KEY_MSG_COUNT_TRACE_FAIL, String.valueOf(this.caller.count_fail) , rel_id);
							LogHelper.toast(this.caller,  "停止重发！已发："+retry_times +"次;id="+ rel_id);
							Log.w(TAG, "重发不再进行！已重发次数："+retry_times +";id="+ rel_id);
						}else{
							LogHelper.toast(this.caller,  "失败; 但该记录状态已经不是发送中："+retry_times +"次;id="+ rel_id +";parts="+ num);
						}
//						LogHelper.toast(this.caller, "部分失败，回滚发送计数：parts"+ num +"/"+ num_max +";id=" + rel_id);

		    			//TODO:标为死短信
		    		}else{
		    			if(print_debug) LogHelper.toast(this.caller,  "准备重发,id=" +rel_id);
		    			//重试次数加1
		    			
		    			retry_times++;
		    			ContentValues valuesLog = new ContentValues();
		    			valuesLog.put(SendLog.RETRY_TIMES, retry_times);
		    			valuesLog.put(SendLog.STATUS, (String)null);//空，表示可以重试了。
		    			int rtn = db.update(SendLog.TABLE_NAME, valuesLog, "_id=? and " +SendLog.STATUS +"='ing'", new String[]{String.valueOf(rel_id)});
		    			//如果已经被重试，就算了
		    			if(rtn ==0){
		    				LogHelper.toast(this.caller,  "...无需重发，id=" +rel_id +";part="+ num);
		    				retry_times--;
		    			}else{
		    				LogHelper.toast(this.caller,  "准备重发,id=" +rel_id +";part="+ num);
		    			}

		    			//					//广播：我要重发了，请准备跟踪！
		    			//					Intent broadIntent = new Intent(SmsSendService.BROAD_NEWROW);
		    			//			        broadIntent.putExtra("rowid", rel_id+ "-"+(retry_times));  
		    			//			        sendBroadcast(broadIntent);  
		    			//			        Log.d(TAG, "准备重发！"+broadIntent.getAction());
		    			//			        Thread thread = new SendThread(rel_id, target, text, context, retry_times);
		    			//					//启动重发
		    		}
		    	}catch(Exception e){
		    		LogHelper.exception(this.caller.getApplicationContext(), "发送通知处理失败", e);
		    	}finally{
		    		if(c!=null) c.close();
		    		if(db!=null) db.close();
		    	}
		    }
		    Log.d(TAG, "unregisterReciver:"+ intent.getAction() );
		    if(print_debug) LogHelper.toast(this.caller.getApplicationContext(), "--停追" +intent.getAction());
		    this.caller.unregisterReceiver(this);
		}
	}