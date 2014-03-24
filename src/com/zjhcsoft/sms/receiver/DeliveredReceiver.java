package com.zjhcsoft.sms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.MsgConstant;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.TimeConvert;

/**
 * 短信送达的消息处理
 * 
 * @author Administrator
 * 
 */
public final class DeliveredReceiver extends BroadcastReceiver {
	private SmsRobotApp caller = SmsRobotApp.getInstance();
	private final String TAG = "DeliveredReceiver!!!";


	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Bundle b = arg1.getExtras();
		int num = b.getInt("num");
		int num_max = b.getInt("num_max");
		long id = b.getLong("id");
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			// Toast.makeText(getBaseContext(), "SMS delivered",
			// Toast.LENGTH_SHORT).show();
			// 到短信的最末一部分时，更新发送状态
			if (num == num_max) {
				ContentValues values = new ContentValues();
				values.put(SendLog.DELI_DT, TimeConvert.getTimestamp());
				values.put(SendLog.STATUS, MsgConstant.STATUS_DELIVERED);
				SQLiteDatabase db = null;
				try {
					DBHelper dbhelper = new DBHelper(this.caller);
					db = dbhelper.getWritableDatabase();
					db.update(SendLog.TABLE_NAME, values, SendLog.ID + "=?",
							new String[] { String.valueOf(id) });
				} catch (SQLiteException e) {
					LogHelper.exception(this.caller.getApplicationContext(),
							"处理回执失败", e);
				} finally {
					if (db != null)
						db.close();
					db = null;
				}

				this.caller.gCountDelivered++;
				// Intent intent = new Intent(SmsSendService.BROAD_UPDATE);
				// intent.putExtra((SmsSendService.KEY_MSG_COUNT_TRACE_DELIVERED),
				// String.valueOf(count_delivered) );
				// sendBroadcast(intent);//通知新的短信送达
				// 通知发送OK
				LogHelper.count(this.caller,
						MsgConstant.KEY_MSG_COUNT_TRACE_DELIVERED,
						String.valueOf(this.caller.gCountDelivered), id);

				Log.w(TAG + " sms status deliver:", "deliver ed!" + id);
			} else {
				LogHelper.toast(this.caller, "******分拆部分送达; parts" + num + "/"
						+ num_max + ";id=" + id);
			}

			break;
		case Activity.RESULT_CANCELED:
			Toast.makeText(this.caller.getBaseContext(), "SMS not delivered, ",
					Toast.LENGTH_SHORT).show();
			Log.i(TAG + "deliver:", "canceled!");
			LogHelper.count(this.caller,
					MsgConstant.KEY_MSG_COUNT_TRACE_FAIL,
					String.valueOf(this.caller.gCountFail), id);
			break;
		}// -switch
//		Log.d(TAG, "unregisterReciver:" + arg1.getAction());
//		this.caller.unregisterReceiver(this);
	}
}