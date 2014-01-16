package com.zjhcsoft.sms.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.smsrobot1.R;

public class TestcaseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_testcase);
		
		Button btn_add_reboot_row  = (Button)findViewById(R.id.btn_add_reboot_row);
		Button btn_test_del = (Button)findViewById(R.id.btn_test_del);
		OnClickListener lis = new MyClickListener();
		btn_add_reboot_row.setOnClickListener( lis);
		btn_test_del.setOnClickListener(lis);
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
			case R.id.btn_test_del:
				Toast.makeText(getApplicationContext(), "测试清除" , Toast.LENGTH_SHORT);
				break;

			default:
				Toast.makeText(getApplicationContext(), "unknow button" + btn.getText(), Toast.LENGTH_SHORT);
				break;
			}
		}		
	}
	private SQLiteDatabase db;
	private void makeRebootRow(){
		DBHelper  dbhelper = new DBHelper(this);
		db  = dbhelper.getReadableDatabase();
		
		ContentValues values = new ContentValues();
		String num = "15372095937";
		String text = "失败测试";
		String status = "ing";
        //ID自增，只存放名称和URL
        values.put(SendLog.TARGET, num);
        values.put(SendLog.TEXT, text);
        values.put(SendLog.SCAN_DT, TimeConvert.getTimestamp());
        values.put("status", status);
        values.put(SendLog.SEND_DT0, TimeConvert.getTimeDiff(-100));
		db.insert(SendLog.TABLE_NAME, null, values);
		db.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.testcase, menu);
		return true;
	}

}
