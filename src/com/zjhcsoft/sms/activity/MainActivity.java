package com.zjhcsoft.sms.activity;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zjhcsoft.sms.LogTextBox;
import com.zjhcsoft.sms.SmsDownloadService;
import com.zjhcsoft.sms.SmsLaunchService;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.SmsTraceService;
import com.zjhcsoft.sms.actionbar.ActionBarActivity;
import com.zjhcsoft.smsrobot1.R;

public class MainActivity extends ActionBarActivity {

	private MsgReceiver msgReceiver;
	private TextView mStatus;
	private static final int DIALOG_AOUT = 2;
	private final String TAG = "MainActivity";

	// @Override
	// protected void onSaveInstanceState(Bundle outState) {
	// // TODO Auto-generated method stub
	// outState.putString("scan", (String)mCountScan.getText());
	// outState.putString("get", (String)mCountGet.getText());
	// outState.putString("send", (String)mCountSend.getText());
	// outState.putString("done", (String)mCountDone.getText());
	// outState.putBoolean("start", mBtnStart.isChecked());
	// super.onSaveInstanceState(outState);
	// Log.d("onSaveInstanceState","save....");
	// }

	protected boolean mSendStatus = false;
	private TextView mCountScan;
	private TextView mCountGet;
	private TextView mCountSend;
	private TextView mCountDone;
	private Button mBtnSetting;
	private ToggleButton mBtnStart;
	private IntentFilter mIntentFilter;
	private TextView mTvConfig;
	private TextView mCountSent;
	private TextView mCountDelivered;
	public TextView mCountFail;
	private LogTextBox mLogText;

	@Override
	protected void onResume() {
		registerReceiver(msgReceiver, mIntentFilter);
		Log.d(TAG, " onResume...");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
//		LogHelper.toast(getApplicationContext(), "pause...");
		Log.d(TAG, " pause...");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, " onDestroy...");
		// stopService(new Intent(MainActivity.this,
		// SMSService.class));
		// //停止服务
		// stopService(mIntent);
		// 注销广播
		unregisterReceiver(msgReceiver);
//		stopService(new Intent(MainActivity.this, SmsLaunchService.class));
		super.onDestroy();
	}

	public class MsgReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 显示进度消息
			// int progress = intent.getIntExtra("progress", 0);
			// mStatus.setText(String.valueOf(progress));

			Bundle b = intent.getExtras();
			if (intent.getAction().equals(SmsDownloadService.BROAD_UPDATE)) {
				if (b == null) {
					mLogText.appendLog("收到空的通知！！");
					Log.w(TAG, intent.getAction() + ";intent extras empty!");
					return;
				}
				Object[] lstName = b.keySet().toArray();
				for (int i = 0; i < lstName.length; i++) {
					String key = lstName[i].toString();
					// Log.i(key,String.valueOf(b.get(key)));
					if (key.equals(SmsDownloadService.KEY_MSG_COUNT_SCAN)) {
						mCountScan.setText(b.getString(key));
						mLogText.appendLog("扫描：" + b.getString(key));
					} else if (key.equals(SmsDownloadService.KEY_MSG_COUNT_GET)) {
						mCountGet.setText(b.getString(key));
						mLogText.appendLog("下载短信：" + b.getString(key));
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_COUNT_SEND)) {
						mLogText.appendLog("处理短信：" + b.getString(key));
						mCountSend.setText(b.getString(key));
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_COUNT_PACK)) {
						mLogText.appendLog(">>>派发：" + ";id="
								+ b.getString("id"));
						mCountDone.setText(b.getString(key));
					} else if (key.equals(SmsDownloadService.KEY_MSG_STATUS)) {
						String sStatus = b.getString(key);
						mLogText.appendLog(sStatus);
						mStatus.setText(sStatus);
						// 变忙
						if (sStatus
								.equals(SmsDownloadService.KEY_MSG_STATUS_BUSY)) {
							mBtnStart.setEnabled(false);
							// mCountFail.setText("---");
						} else if (sStatus
								.equals(SmsDownloadService.KEY_MSG_STATUS_IDLE)) {
							// mBtnStart.setChecked(false);//按过变为可用
							mBtnStart.setEnabled(true);
							Log.d(TAG, "done deal the msg, now list update...");
						}
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_SERVICE_READY)) {
						mBtnStart.setChecked(true);
					} else if (key.equals(SmsDownloadService.KEY_MSG_AREA)) {
						String areaCode = b.getString(key);
						String areaName = areaCode2Name(areaCode);
						mLogText.appendLog("配置信息：" +areaName);
						mTvConfig.setText(areaName);
					} else if (key.equals(SmsDownloadService.KEY_MSG_TOAST)) {
						mLogText.appendLog(b.getString(key));
						// Toast.makeText(context, b.getString(key) ,
						// Toast.LENGTH_SHORT).show();
						// mCountFail.setText( b.getString(key) );
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_COUNT_TRACE_SENT)) {
						mLogText.appendLog("<<<寄出短信：" + ";id="
								+ b.getString("id"));
						mCountSent.setText(b.getString(key));
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_COUNT_TRACE_DELIVERED)) {
						mLogText.appendLog("<<<送达：" + ";id="
								+ b.getString("id"));
						mCountDelivered.setText(b.getString(key));
					} else if (key
							.equals(SmsDownloadService.KEY_MSG_COUNT_TRACE_FAIL)) {
						mLogText.appendLog("<<<失败：" + ";id="
								+ b.getString("id"));
						mCountFail.setText(b.getString(key));
					}
				}// -for
			} else {

			}
			// mProgressBar.setProgress(progress);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "activity create !");
		super.onCreate(savedInstanceState);
		Intent in = getIntent();

		setContentView(R.layout.layout_main);
		// 动态注册广播接收器
		msgReceiver = new MsgReceiver();
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(SmsDownloadService.BROAD_UPDATE);
		// mIntentFilter.addAction(action)
		registerReceiver(msgReceiver, mIntentFilter);
		

		mBtnStart = (ToggleButton) findViewById(R.id.toggleButton1);
		mBtnSetting = (Button) findViewById(R.id.setting);
		mStatus = (TextView) findViewById(R.id.status);
		mCountScan = (TextView) findViewById(R.id.count_scan);
		mCountGet = (TextView) findViewById(R.id.count_get);
		mCountSend = (TextView) findViewById(R.id.count_send);
		mCountDone = (TextView) findViewById(R.id.count_done);
		mTvConfig = (TextView) findViewById(R.id.config);
		mCountSent = (TextView) findViewById(R.id.count_sent);
		mCountDelivered = (TextView) findViewById(R.id.count_delivered);
		mCountFail = (TextView) findViewById(R.id.count_fail);
		mLogText = (LogTextBox) findViewById(R.id.logTextBox);
		mTvConfig.setText(getCurAreaName());
		mLogText.setMaxLines(getLimitedLines());

		// if(savedInstanceState!=null){
		// mBtnStart.setChecked(savedInstanceState.getBoolean("start"));
		// mCountScan.setText(savedInstanceState.getString("scan"));
		// mCountSend.setText(savedInstanceState.getString("send"));
		// mCountDone.setText(savedInstanceState.getString("done"));
		// mCountGet.setText(savedInstanceState.getString("get"));
		//
		// }
		if(in.getBooleanExtra("auto", false)){
			mLogText.appendLog( "开机自启动！");
			startTheService(SmsDownloadService.class);
		}		
		if (isMyServiceRunning()) {
			mBtnStart.setChecked(true);
			mStatus.setText("运行中...");
		}
//		startTheService(SmsTraceService.class);
		startTheService(SmsLaunchService.class);
//		startService(new Intent(MainActivity.this, SmsLaunchService.class));
		mLogText.appendLog("跟踪服务开启...");
		mLogText.appendLog("发送服务开启...");

		mBtnStart.setOnClickListener(mSendClicker);
		mTvConfig.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						MyPreferenceActivity.class);
				startActivity(intent);
			}

		});


	}

	private OnClickListener mSendClicker = new OnClickListener() {
		public void onClick(View v) {

			// SharedPreferences sharedPref =
			// PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			// final String server_url = sharedPref.getString("server_url",
			// "http://134.100.5.30/");

			if (!isMyServiceRunning()) {
				startService(new Intent(MainActivity.this,
						SmsDownloadService.class));
				startTheService(SmsTraceService.class);
				mStatus.setText("开启中");
				mStatus.setTextColor(getResources().getColor(R.color.text_color_blue));
//				mCountSent.setText("-");
//				mCountSend.setText("-");
//				mCountScan.setText("-");
//				mCountDone.setText("-");
//				mCountGet.setText("-");
//				mCountDelivered.setText("-");
				mLogText.appendLog("开启下载服务...");
			} else {
				stopService(new Intent(MainActivity.this,
						SmsDownloadService.class));
				// stopService(new Intent(MainActivity.this,
				// SmsTraceService.class));
				mStatus.setText("已停止");
				mStatus.setTextColor(getResources().getColor(R.color.text_color_gray));
				mLogText.appendLog("下载服务已停止！");
			}
		}
	};

	// 检查服务是否在运行
	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (SmsDownloadService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void startTheService(Class serviceClass){
		Log.d(TAG, "hi..." +serviceClass.getName());
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(
					service.service.getClassName())) {
				Log.d(TAG, serviceClass.getName()+ "早已运行");
				return;
			}
		}
		startService(new Intent(MainActivity.this, serviceClass));
		Log.d(TAG,serviceClass.getName()+ "启动！");
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			showDialog(DIALOG_AOUT);
			break;

		case android.R.id.home:
			// Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_reboot:
			try {
		        Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot" });
		        proc.waitFor();
		    } catch (Exception ex) {
		        Log.i(TAG, "Could not reboot", ex);
		    }
			break;
		case R.id.menu_test:
			startActivity(new Intent(MainActivity.this, TestcaseActivity.class));
			break;
			

		case R.id.menu_history:
			// Toast.makeText(this, "Fake refreshing...",
			// Toast.LENGTH_SHORT).show();
			// refreshList();
			startActivity(new Intent(MainActivity.this, ListSendActivity.class));
			break;

		case R.id.menu_setting:
			// Toast.makeText(this, "Tapped setting",
			// Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(MainActivity.this,
					MyPreferenceActivity.class);
			startActivity(intent);
			break;
		case R.id.menu_failure:
			Intent list = new Intent(MainActivity.this, FailListActivity.class);
			startActivity(list);
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		android.app.AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_AOUT:
			SmsRobotApp app = (SmsRobotApp) getApplication();
			builder = new AlertDialog.Builder(MainActivity.this);
			DialogAbout about = new DialogAbout(MainActivity.this, app);
			about.init(builder);
			Dialog dialog = builder.create();
			return dialog;
		default:
			return null;
		}
	}

	/**
	 * 代码（配置项）转区域名
	 * @param code
	 * @return
	 */
	private String areaCode2Name(String code){
		Resources res =getResources();
		String[] areas=res.getStringArray(R.array.area_list_preference);
		String[] areaCodes = res.getStringArray(R.array.areavalues_list_preference);
		int i=0;
//		int code_ = Integer.parseInt(code);
		for(i=0; i<areaCodes.length; i++){
			if(areaCodes[i].equals(code)) break;
		}
		return areas[i] +"";
	}
	
	/**
	 * 取配置文件中的当前区域名
	 * @return
	 */
	private String getCurAreaName(){
		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
		String code = pref.getString("area", "7");
		String name = areaCode2Name(code);
		return name;
	}
    private int getLimitedLines(){
		SharedPreferences pref = SmsRobotApp.getInstance().getSharedPreferences();
		int lines = pref.getInt("limit_lines_runlog", 50000);
		return lines;
    }
}
