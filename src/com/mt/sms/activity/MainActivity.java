package com.mt.sms.activity;

import java.util.HashMap;
import java.util.Map;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mt.sms.LogTextBox;
import com.mt.sms.MsgConstant;
import com.mt.sms.SmsRobotApp;
import com.mt.sms.SuperScheduler;
import com.mt.sms.TimeConvert;
import com.mt.sms.actionbar.ActionBarActivity;
import com.mt.sms.service.SmsStatsIntentService;
import com.mt.smsrobo.R;

//TODO:缺陷：长期运行，全局计数器过大时的处理
//TODO:午夜静默,OK
//TODO:充电警告短信
//TODO:发送完成通知
//TODO:重发显示
//TODO:测试功能1：清除历史数据（OK）
//TODO:测试功能2：发送一批短信（OK）
//TODO:测试功能3：立即重启设置
//TODO:高级参数，分屏进入,TO TEST
//TODO:侧滑菜单
//TODO:日志定期保存
//TODO:缺陷：点亮屏幕时日志被清除
//TODO:增强：异常表的自行清理


/**
 * 
 * @author Administrator
 *
 */
public class MainActivity extends ActionBarActivity {

	private MsgReceiver mMsgReceiver;
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
//	private Button mBtnSetting;
	private ToggleButton mBtnStart;
	private IntentFilter mIntentFilter;
	private TextView mTvConfig;
	private TextView mCountSent;
	private TextView mCountDelivered;
	private TextView mCountFail;
	private LogTextBox mLogText;
	private ProgressBar mProgressBar1;
	private TextView mCountPackAgain;
	private TextView mStatsTime ;

	private SuperScheduler mSchedule;
	private StatsReceiver mStatsReceiver;
	private IntentFilter mIntentFilterStats;
	private TextView mStatsSpeed;

	@Override
	protected void onResume() {
		registerReceiver(mMsgReceiver, mIntentFilter);
		registerReceiver(mStatsReceiver, mIntentFilterStats);
		
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
		unregisterReceiver(mMsgReceiver);
		unregisterReceiver(mStatsReceiver);
//		stopService(new Intent(MainActivity.this, SmsLaunchService.class));
		super.onDestroy();
	}

	@Override
		protected void onCreate(Bundle savedInstanceState) {
			Log.d(TAG, "activity create !");
			super.onCreate(savedInstanceState);
			Intent in = getIntent();
	
			setContentView(R.layout.layout_main);
			// 动态注册广播接收器
			mMsgReceiver = new MsgReceiver();
			mIntentFilter = new IntentFilter();
			mIntentFilter.addAction(MsgConstant.BROAD_ACTION_UPDATE);
			// mIntentFilter.addAction(action)
			registerReceiver(mMsgReceiver, mIntentFilter);
			
			//统计更新接收器
			mStatsReceiver = new StatsReceiver();
			mIntentFilterStats = new IntentFilter();
			mIntentFilterStats.addAction(MsgConstant.BROAD_ACTION_STATS);
			registerReceiver(mStatsReceiver,mIntentFilterStats);
			mApp = (SmsRobotApp) getApplication();		
	
			mStatsSpeed = (TextView)findViewById(R.id.stats_speed);
			mBtnStart = (ToggleButton) findViewById(R.id.toggleButton1);
//			mBtnSetting = (Button) findViewById(R.id.setting);
			mStatus = (TextView) findViewById(R.id.status);
			mCountScan = (TextView) findViewById(R.id.count_scan);
			mCountGet = (TextView) findViewById(R.id.count_get);
			mCountSend = (TextView) findViewById(R.id.count_send);
			mCountDone = (TextView) findViewById(R.id.count_done);
			mTvConfig = (TextView) findViewById(R.id.config);
			mCountSent = (TextView) findViewById(R.id.count_sent);
			mCountDelivered = (TextView) findViewById(R.id.count_delivered);
			mCountFail = (TextView) findViewById(R.id.count_fail);
			mCountPackAgain = (TextView)findViewById(R.id.tv_pack_again);
			mLogText = (LogTextBox) findViewById(R.id.logTextBox);
			mProgressBar1 = (ProgressBar)findViewById(R.id.progressBar1);
			
			TextView stats1 = (TextView)findViewById(R.id.textView1);
			TextView stats2 = (TextView)findViewById(R.id.textView2);
			TextView stats3 = (TextView)findViewById(R.id.textView3);
			TextView stats4 = (TextView)findViewById(R.id.textView4);
			
			//---------统计信息的初始化
			mStatsTexts = new HashMap<String, TextView>();
			mStatsTexts.put(MsgConstant.STATUS_DELIVERED, stats1);
			mStatsTexts.put(MsgConstant.STATUS_SENT, stats2);
			mStatsTexts.put(MsgConstant.STATUS_FAIL, stats3);
			mStatsTexts.put(MsgConstant.STATUS_PROCESS, stats4);
			mRootStats = (LinearLayout)findViewById(R.id.stats_root);
			mStatsTime = (TextView)findViewById(R.id.stats_time);
			resetStats();
			mLabelSend = (TextView)findViewById(R.id.label_send);
			//---------结束统计信息的初始化
			
			View containerSent = findViewById(R.id.container_sent);
			containerSent.setOnTouchListener(new StatsTouchListener());
			View containerDeli = findViewById(R.id.container_delivered);
			containerDeli.setOnTouchListener(new StatsTouchListener());
			View containerQueue = findViewById(R.id.container_queue);
			containerQueue.setOnTouchListener(new StatsTouchListener());
			View containerFail = findViewById(R.id.container_fail);
			containerFail.setOnTouchListener(new StatsTouchListener());
			mTvConfig.setText(getCurAreaName());
			mLogText.setMaxLines(getLimitedLines());
			mProgressBar1.setMax(1);
			mProgressBar1.setVisibility(View.GONE);
			mCountPackAgain.setText("0");
			
			
			mSchedule = new SuperScheduler(this.getBaseContext());
			if(in.getBooleanExtra("auto", false)){
				mLogText.appendLog( "随开机启动，自动开启服务！");
				mSchedule.doSchedule();
	
				mBtnStart.setChecked(true);
				mStatus.setText("已运行...");
				mProgressBar1.setProgress(1);
			}else{
				mLogText.appendLog( "准备就绪，等待人工开启服务。");
			}
			startService(new Intent(MainActivity.this, SmsStatsIntentService.class));
			
	//		startTheService(SmsTraceService.class);
	//		startTheService(SmsLaunchService.class);
	//		startService(new Intent(MainActivity.this, SmsLaunchService.class));
	//		mLogText.appendLog("跟踪服务开启...");
	//		mLogText.appendLog("发送服务开启...");
	
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

	private class  StatsTouchListener implements OnTouchListener{

		@Override
		public boolean onTouch(View v, MotionEvent arg1) {
			String status;
			switch (v.getId()) {
			case R.id.container_sent:
				status = MsgConstant.STATUS_SENT;
				break;
			case R.id.container_fail:
				status = MsgConstant.STATUS_FAIL;
				break;
			case R.id.container_delivered:
				status = MsgConstant.STATUS_DELIVERED;
				break;
			case R.id.container_queue:
				status = MsgConstant.STATUS_QUEUE;
				break;
			default:
				status = "all";
				break;
			}
			Intent in = new Intent(MainActivity.this, ListSendActivity.class);
			in.putExtra("status", status);
			startActivity(in);
			return false;
		}
	}
	/**
	 * 重置统计数字
	 */
	private void resetStats() {
		for(TextView tv: mStatsTexts.values()){
			tv.setText("");
			tv.setWidth(0);
			tv.setHeight(10);
		}
	}

	public class MsgReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 显示进度消息
			// int progress = intent.getIntExtra("progress", 0);
			// mStatus.setText(String.valueOf(progress));

			Bundle b = intent.getExtras();
			if (intent.getAction().equals(MsgConstant.BROAD_ACTION_UPDATE)) {
				if (b == null) {
					mLogText.appendLog("收到空的通知！！");
					Log.w(TAG, intent.getAction() + ";intent extras empty!");
					return;
				}
				Object[] lstName = b.keySet().toArray();
				for (int i = 0; i < lstName.length; i++) {
					String key = lstName[i].toString();
					// Log.i(key,String.valueOf(b.get(key)));
					if (key.equals(MsgConstant.KEY_MSG_COUNT_SCAN)) {
						mCountScan.setText(b.getString(key));
						mLogText.appendLog("扫描：" + b.getString(key));
					} else if (key.equals(MsgConstant.KEY_MSG_COUNT_GET)) {
						mCountGet.setText(b.getString(key));
						mLogText.appendLog("下载短信：" + b.getString(key));
					} else if (key
							.equals(MsgConstant.KEY_MSG_COUNT_QUEUE)) {
						mLogText.appendLog("处理短信：" + b.getString(key));
						mCountSend.setText(b.getString(key));
						mProgressBar1.setMax(mApp.gCountQueue);
						mProgressBar1.setVisibility(View.VISIBLE);
					} else if (key
							.equals(MsgConstant.KEY_MSG_COUNT_PACK)) {
//						mLogText.appendLog(">>>派发：" + ";id="
//								+ b.getString("id"));
						mCountDone.setText(b.getString(key));
					} else if (key.equals(MsgConstant.KEY_MSG_STATUS)) {
						String sStatus = b.getString(key);
						mLogText.appendLog(sStatus);
						mStatus.setText(sStatus);
						// 变忙
						if (sStatus
								.equals(MsgConstant.KEY_MSG_STATUS_BUSY)) {
							mBtnStart.setEnabled(false);
							// mCountFail.setText("---");
						} else if (sStatus
								.equals(MsgConstant.KEY_MSG_STATUS_IDLE)) {
							// mBtnStart.setChecked(false);//按过变为可用
							mBtnStart.setEnabled(true);
							Log.d(TAG, "done deal the msg, now list update...");
						}else if(sStatus.equals(MsgConstant.KEY_MSG_STATUS_HALT)){
							mBtnStart.setEnabled(true);
							mBtnStart.setText(MsgConstant.KEY_MSG_STATUS_HALT);
							mBtnStart.setChecked(false);
						}
					} else if (key
							.equals(MsgConstant.KEY_MSG_SERVICE_READY)) {
						mBtnStart.setChecked(true);
					} else if (key.equals(MsgConstant.KEY_MSG_AREA)) {
//						String areaCode = b.getString(key);
//						String areaName = areaCode2Name(areaCode);
//						mLogText.appendLog("配置信息：" +areaName);
//						mTvConfig.setText(areaName);
					} else if (key.equals(MsgConstant.KEY_MSG_TOAST)) {
						mLogText.appendLog(b.getString(key));
						// Toast.makeText(context, b.getString(key) ,
						// Toast.LENGTH_SHORT).show();
						// mCountFail.setText( b.getString(key) );
					} else if (key
							.equals(MsgConstant.KEY_MSG_COUNT_TRACE_SENT)) {
						mLogText.appendLog("<<<寄出短信：" + ";id="
								+ b.getString("id"));
						mCountSent.setText(b.getString(key));
						mCountPackAgain.setText(""+mApp.gCountPackedAgain);
						//进度条
						mProgressBar1.setProgress(mApp.gCountSent);
						if(mApp.gCountQueue== mApp.gCountSent){
							mProgressBar1.setVisibility(View.GONE);
						}
					} else if (key
							.equals(MsgConstant.KEY_MSG_COUNT_TRACE_DELIVERED)) {
						mLogText.appendLog("<<<送达：" + ";id="
								+ b.getString("id"));
						mCountDelivered.setText(b.getString(key));
					} else if (key
							.equals(MsgConstant.KEY_MSG_COUNT_TRACE_FAIL)) {
						mLogText.appendLog("<<<失败：" + ";id="
								+ b.getString("id"));
						mCountFail.setText(b.getString(key));
					}
				}// -for
			} else {

			}
		}


	}
	
	/**
	 * 更新统计信息（主要是各种状态的占比条）
	 * @author Administrator
	 *
	 */
	private class StatsReceiver  extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			
			if (b == null) {
				return;
			}
			Object[] lstName = b.keySet().toArray();
			mStatsTime.setText(TimeConvert.getCurrTime()+"更新");
			int widthTotal = (mRootStats.getWidth() -  mLabelSend.getWidth() - mStatsTime.getWidth() -10)/2;
			int widthRuler =( (LinearLayout)findViewById(R.id.speed_now)).getWidth();

			int total = Integer.parseInt(b.getString(MsgConstant.STATUS_TOTAL));
			if(total==0) return;
			resetStats();
			for (int i = 0; i < lstName.length; i++) {
				String key  = lstName[i].toString();
//				LogHelper.toast(mApp, "key-->" + key+";val=" + b.getString(key));
				if(mStatsTexts.containsKey(key)){
					int n = Integer.parseInt(b.getString(key));
					TextView tv = mStatsTexts.get(key);
//					tv.setText(b.getString(key));
					int pixels = widthTotal *n/total;
					tv.setWidth(pixels);
				}
				if(MsgConstant.STATUS_DELAY.equals(key)){
					float curF = Float.parseFloat(b.getString(key));
					int cur = Math.round(curF);
					
					//延时
					ImageView marker = (ImageView)findViewById(R.id.imageView1);
					int max = 60;
					if(max<cur){
						max = Math.round(cur) +5;
					}
//					seekbar.setMax(max);
//					seekbar.setProgress(cur);
//					seekbar.setSecondaryProgress(30);
					mStatsSpeed.setText(String.valueOf(cur));
					int leftMarker = (widthRuler -10)*cur/widthRuler -10;
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.setMargins(leftMarker, 5, 0, 0);
					marker.setLayoutParams(lp);
					
				}
			}
		}
		
	}

	//开关扫描服务
	private OnClickListener mSendClicker = new OnClickListener() {
		public void onClick(View v) {
			ToggleButton tb = (ToggleButton)v;
			if(!tb.isChecked()){
				mStatus.setText("已停止");
				mStatus.setTextColor(getResources().getColor(R.color.text_color_gray));
				mLogText.appendLog("扫描服务收到停止指令");
				
				mSchedule.cancel();				
			}else{
				mSchedule.doSchedule();
				mStatus.setText("开启中");
				mStatus.setTextColor(getResources().getColor(R.color.text_color_blue));
				mLogText.appendLog("开启扫描服务...");
			}
		}
	};
	private SmsRobotApp mApp;
	private Map<String, TextView> mStatsTexts;
	private LinearLayout mRootStats;
	private TextView mLabelSend;
	// 检查服务是否在运行
	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo rs : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(
					rs.service.getClassName())) {
				Log.d(TAG, serviceClass.getName()+ "检测到已运行");
				return true;
			}
		}
		Log.d(TAG, serviceClass.getName()+ "未检测到");
		return false;
	}

	@SuppressWarnings("unused")
	private void startTheService(Class<?> serviceClass){
		Log.d(TAG, "hi..." +serviceClass.getName());
		if(isServiceRunning(serviceClass)) {
			Log.d(TAG, serviceClass.getName()+ "早已运行");
			return;			
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
		case R.id.menu_exception:
			Intent exception = new Intent(MainActivity.this, ExceptionListActivity.class);
			startActivity(exception);
			break;			

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		android.app.AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_AOUT:
			builder = new AlertDialog.Builder(MainActivity.this);
			DialogAbout about = new DialogAbout(MainActivity.this, mApp);
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
