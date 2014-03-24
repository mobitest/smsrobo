package com.zjhcsoft.sms.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.MsgConstant;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.SuperScheduler;
import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.service.SmsFetchIntentService;
import com.zjhcsoft.sms.service.SmsLaunchIntentService;
import com.zjhcsoft.sms.service.SmsStatsIntentService;
import com.zjhcsoft.sms.service.SmsTraceIntentService;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.smsrobot1.R;

public class TestcaseActivity extends Activity {

	private EditText test_nums;
	private EditText test_target;
	private EditText test_msg;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_testcase);
		
		Button btn_add_reboot_row  = (Button)findViewById(R.id.btn_add_reboot_row);
		Button btn_test_cleardata = (Button)findViewById(R.id.btn_test_cleardata);
		Button btn_test_batch_new = (Button)findViewById(R.id.btn_test_batch_new);		
		test_nums = (EditText)findViewById(R.id.test_nums);
		test_target = (EditText)findViewById(R.id.test_target);
		test_msg = (EditText)findViewById(R.id.test_msg);
		Button btn_test_resetcounter =  (Button)findViewById(R.id.btn_test_resetcounter);
				
		OnClickListener lis = new MyClickListener();
		btn_add_reboot_row.setOnClickListener( lis);
		btn_test_cleardata.setOnClickListener(lis);
		btn_test_batch_new.setOnClickListener(lis);
		btn_test_resetcounter.setOnClickListener(lis);
	}
	class MyClickListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			Button btn = (Button)v;
			int id = v.getId();
			switch (id) {
			case R.id.btn_add_reboot_row:
				makeRebootRow();
				break;
			case R.id.btn_test_cleardata:
				Toast.makeText(getApplicationContext(), "清除数据" , Toast.LENGTH_SHORT).show();
				clearData();
				break;
			case R.id.btn_test_batch_new:
				Toast.makeText(getApplicationContext(), "批量短信" , Toast.LENGTH_SHORT).show();
				genBatchSms();
				break;
			case R.id.btn_test_resetcounter:
				increaseCounter();
				break;
			default:
				Toast.makeText(getApplicationContext(), "unknow button" + btn.getText(), Toast.LENGTH_SHORT).show();
				break;
			}
		}		
	}
	private SQLiteDatabase db;
	private void increaseCounter(){
		SmsRobotApp app = SmsRobotApp.getInstance();
		app.gCountPacked +=900000000;
		Context ctx = app;
		SuperScheduler.oneShotNow(SmsTraceIntentService.class, 1000);
		Toast.makeText(getApplicationContext(), "已经增大（派送）计数器至"+ app.gCountPacked , Toast.LENGTH_SHORT).show();
		TestcaseActivity.this.finish();
	}
	/**
	 * 重启短信；100分钟前，发送状态，
	 */
	private void makeRebootRow(){
		DBHelper  dbhelper = new DBHelper(this);
		db  = dbhelper.getWritableDatabase();
		
		String target = test_target.getText().toString();
		String msg = "失败测试;" + test_msg.getText().toString();
		final String status = "ing";
		ContentValues values = new ContentValues();
        values.put(SendLog.TARGET, target);
        values.put(SendLog.TEXT, msg);
        values.put(SendLog.SCAN_DT, TimeConvert.getTimeDiff(-110));
        values.put("status", status);
        values.put(SendLog.SEND_DT0, TimeConvert.getTimeDiff(-100));
        values.put(SendLog.SEND_DT, TimeConvert.getTimeDiff(-100));
		db.insert(SendLog.TABLE_NAME, null, values);
		db.close();
		Toast.makeText(getApplicationContext(), "已经添加一条100分钟前下载的任务，一直未发送成功" , Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 产生一批测试记录,并且开始发送
	 * 内容格式：编号-正文-时间戳
	 */
	private void genBatchSms(){
		int nums = Integer.parseInt(test_nums.getText().toString());
		String target = test_target.getText().toString();
		target = target.replace(" ", "");
		String msg =  test_msg.getText().toString();
		final String status=null;
		
		DBHelper  dbhelper = new DBHelper(this);
		db  = dbhelper.getWritableDatabase();		
		for(int i=0; i< nums; i++){
			ContentValues values = new ContentValues();
	        values.put(SendLog.TARGET, target);
	        String test = (i+1) + "-"+ msg  ;//+" <生成于:"+TimeConvert.getCurrTime()+">";
	        values.put(SendLog.TEXT, test);
	        values.put(SendLog.SCAN_DT, TimeConvert.getTimestamp());
	        values.put("status", status);
//	        values.put(SendLog.SEND_DT0, TimeConvert.getTimeDiff(-100));
			db.insert(SendLog.TABLE_NAME, null, values);			
		}
		db.close();
//		SmsRobotApp.getInstance().gCountGet = nums;
		SmsRobotApp.getInstance().gCountQueue+= nums;
		LogHelper.msg(SmsRobotApp.getInstance(), MsgConstant.BROAD_ACTION_UPDATE, 
				new String[] {MsgConstant.KEY_MSG_COUNT_QUEUE}, new String[] {""+SmsRobotApp.getInstance().gCountQueue});
		startService(new Intent(TestcaseActivity.this, SmsStatsIntentService.class));
		LogHelper.toast(getApplicationContext(), "立即打开发送服务....");
		SuperScheduler.oneShotNow(SmsLaunchIntentService.class, 1000);
		TestcaseActivity.this.finish();
		
	}
	/**
	 * 清除所有数据（重建表）
	 */
	private void clearData(){
		DBHelper  dbhelper = new DBHelper(this);
		db  = dbhelper.getWritableDatabase();		
		dbhelper.clearAllTables(db);
		db.close();
		Toast.makeText(getApplicationContext(), "已经清除所有历史记录" , Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.testcase, menu);
		return true;
	}

}
