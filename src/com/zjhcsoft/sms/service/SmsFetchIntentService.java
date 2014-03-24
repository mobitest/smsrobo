package com.zjhcsoft.sms.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.SendLog;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.MsgConstant;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.SuperScheduler;
import com.zjhcsoft.sms.TimeConvert;

/**
 * 短信扫描下载服务 注意：实现了睁眼服务接口，会被定时唤醒
 * 
 * @author Administrator
 */
public class SmsFetchIntentService extends WakefulIntentServiceAbs {
	public SmsFetchIntentService() {
		super("SmsFetchIntentService");
	}

	// private NotificationManager mNM;
	private boolean mDebugOn = false;
	// 配置项缺省值
	private final int DEFAULT_SCAN = 5;// 5分钟
	private final int DEFAULT_SEND = 100;// 100毫秒
	private final String DEFAULT_SERVER = "http://134.98.104.25:7777/";//

	private final String SERVICE_PATH = "sms/getsms";
	private final static String TAG = "SmsFetchIntentService";

	// 服务级变量
	private int m_interval_scan = 0;
	private int m_interval_send = 0;
	private int m_timeout_conn = 0;
	private int m_timeout_socket = 0;
	// private final String mServer="";
	private String mServiceUrl = "";
//	private int gCountGet = 0;
//	private int gCountSend = 0;
//	private int gCountScan = 0;
	private int m_limit_rows_sendlog;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	// private int NOTIFICATION = R.string.service_started;

	// private Intent broadIntent = new Intent(BROAD_UPDATE);
	private String mArea;
	private String mImei;

	@Override
	public void onCreate() {
		Log.d(TAG, "create SmsFetchIntentService!");
		if(isSlientPeriod()) LogHelper.toast(getApplicationContext(), "非工作时间，将休眠。");
		super.onCreate();
	}

	//	@Override
	//	public int onStartCommand(Intent intent, int flags, int startId) {
	//		//实际流程
	//		//1.父类的onStartCommand，获得睁眼锁
	//		//2.父父类intentService中，会执行onHandleIntent
	//		//3.父类onHandleIntent中，会进入doWakefulWork
	//		//4.父类onHandleIntent结束时，会释放睁眼锁
	//		super.onStartCommand(intent, flags, startId);
	//		return START_STICKY;
	//	}
	
		@Override
		public void onDestroy() {
			Log.d(TAG, "destroy fetch service!");
			// Tell the user we stopped.
			LogHelper.toast(this, "短信扫描完成" );
		}

	@Override
	protected void doWakefulWork(Intent intent) {
		Log.v(TAG, "wakeful work...fetch");
		init();
		
		LogHelper.msg(SmsFetchIntentService.this, MsgConstant.BROAD_ACTION_UPDATE,
				new String[] { MsgConstant.KEY_MSG_SERVICE_READY },
				new String[] { "1" });	
		// broadIntent.putExtra(KEY_MSG_CONFIG, "区域："+ mArea);
		// sendBroadcast(broadIntent);
		LogHelper
				.msg(SmsFetchIntentService.this, MsgConstant.BROAD_ACTION_UPDATE,
						new String[] { MsgConstant.KEY_MSG_AREA },
						new String[] { mArea });
//		LogHelper.toast(getApplicationContext(), "doWakefulWork，server_url="
//				+ mServiceUrl + ";interval_scan=" + m_interval_scan
//				+ "(ms);interval_send=" + m_interval_send);
	
		sayUpdate(MsgConstant.KEY_MSG_STATUS, MsgConstant.KEY_MSG_STATUS_BUSY);
	
		process();
		sayUpdate(MsgConstant.KEY_MSG_STATUS, MsgConstant.KEY_MSG_STATUS_IDLE);
	
	}

	/**
	 * 加载设置
	 */
	private void init() {
		mSharedPref = SmsRobotApp.getInstance().getSharedPreferences();
		Log.d(TAG, "get pref values");
		boolean useInternet = mSharedPref.getBoolean("use_internet", false);
		String server = mSharedPref.getString("server"
				+ (useInternet ? "1" : "0"), DEFAULT_SERVER);
		mServiceUrl = server + SERVICE_PATH;

		m_interval_scan = mSharedPref.getInt("interval__scan", DEFAULT_SCAN) * 1000 * 60;// 分钟
		m_interval_send = mSharedPref.getInt("interval__send_1", DEFAULT_SEND);
		m_timeout_conn = mSharedPref.getInt("interval__conn", 1) * 1000 * 60;
		m_timeout_socket = mSharedPref.getInt("timeout_socket", 3) * 1000 * 60;
		mArea = mSharedPref.getString("area", "7");
		m_limit_rows_sendlog = mSharedPref.getInt("limit_rows_sendlog", 5000);
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mImei = telephonyManager.getSubscriberId();
		Log.i(TAG + " Service config", "server_url=" + mServiceUrl
				+ ";interval_scan=" + m_interval_scan + "(ms);interval_send="
				+ m_interval_send);
	}

//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		//实际流程
//		//1.父类的onStartCommand，获得睁眼锁
//		//2.父父类intentService中，会执行onHandleIntent
//		//3.父类onHandleIntent中，会进入doWakefulWork
//		//4.父类onHandleIntent结束时，会释放睁眼锁
//		super.onStartCommand(intent, flags, startId);
//		return START_STICKY;
//	}

	private void sayUpdate(String key, Object content) {
		LogHelper.msg(SmsFetchIntentService.this, MsgConstant.BROAD_ACTION_UPDATE,
				new String[] { key }, new String[] { String.valueOf(content) });
	}

	private String JSON_NUM = "num";
	private String JSON_BODY = "body";
	private String JSON_TARGET = "target";
	private String JSON_TEXT = "text";
	private SharedPreferences mSharedPref;

	/**
	 * 扫描获取待发短信,执行发送
	 */
	private void process() {
		SmsRobotApp.getInstance().gCountScan++;
		sayUpdate(MsgConstant.KEY_MSG_COUNT_SCAN, SmsRobotApp.getInstance().gCountScan);
		DBHelper db = null;
		try {
			// if(1==1) throw(new Exception("just for test"));
			// 1.联网获取短信

			String url = mServiceUrl + "?area=" + mArea + "&IMEI=" + mImei;
			JSONObject json = null;
			String JsonResponse = mDebugOn ? genTestSms() : fetchWeb(url);
			Log.d(TAG, "get web content, url=" + url);
			if (JsonResponse != null) {
				json = new JSONObject(JsonResponse);
			}

			//2. 检测结果
			int num = json.getInt(JSON_NUM);
			if (num < 0) {
				LogHelper.exception(this, "手机未授权" + num, new Exception(
						"手机未授权, return=" + num));
				return;
			}
			if (num == 0) {
				LogHelper.toast(this, "任务数为零");
				return;
			}
			sayUpdate(MsgConstant.KEY_MSG_STATUS, "发送中");

//			 3.广播：取到数（短信总条数）
//			SmsRobotApp.getInstance().gCountGet += num;
//			sayUpdate(MsgConstant.KEY_MSG_COUNT_GET, SmsRobotApp.getInstance().gCountGet);

			//4.存数据库
			JSONArray nodes = json.getJSONArray(JSON_BODY);

			db = new DBHelper(SmsFetchIntentService.this);
			for (int i = 0; i < num; i++) {
				JSONObject node = nodes.getJSONObject(i);
				String text = node.getString(JSON_TEXT);
				JSONArray targets = node.getJSONArray(JSON_TARGET);
				int num_targets = targets.length();
				for (int j = 0; j < num_targets; j++) {
					// 保留发送日志
					String phone = targets.getString(j);
					insert(db, phone, text);// 将短信下载记录插入到数据库中

				}// end all target
			}// end all msg
			// 广播：排队短信数
			SmsRobotApp.getInstance().gCountQueue+= num;
			sayUpdate(MsgConstant.KEY_MSG_COUNT_QUEUE, SmsRobotApp.getInstance().gCountQueue);
//			LogHelper.msg(SmsFetchIntentService.this, MsgConstant.KEY_MSG_TASK_ARRIVAL,
//					new String[] { "rows" },
//					new String[] { String.valueOf(SmsRobotApp.getInstance().gCountQueue) });
			
			//5.立即打开发送服务
			Log.d(TAG, "open sms send service");
			startService(new Intent(SmsFetchIntentService.this, SmsStatsIntentService.class));
			LogHelper.toast(getApplicationContext(), "立即打开发送服务....");
			SuperScheduler.oneShotNow(SmsLaunchIntentService.class, 1000);
//			SmsRobotApp app = SmsRobotApp.getInstance();
//			AlarmManager am=
//					(AlarmManager)app.getSystemService(Context.ALARM_SERVICE);
//		     
//			//系统休眠时同样执行发送
//			Intent intent=new Intent(app.getApplicationContext(), SmsLaunchIntentService.class);
//			PendingIntent pi=PendingIntent.getService(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//			am.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 1000, pi);

		} catch (JSONException e) {
			Log.e(TAG + " sms", "可能协议有误");
			LogHelper.exception(this, "消息格式无法解析", e);
		} catch (IOException e) {
			LogHelper.exception(this, "网络异常", e);
		} catch (Exception e) {
			LogHelper.exception(this, "短信下载异常", e);
		} finally {
			if (db != null) {
				db.close();
				db = null;
			}
		}

	}

	/**
	 * 将下载短信逐条插入数据库
	 * 
	 * @param num
	 * @param text
	 * @return
	 */
	private long insert(DBHelper db, String num, String text) throws Exception {
		ContentValues values = new ContentValues();

		// ID自增，只存放名称和URL
		values.put(SendLog.TARGET, num);
		values.put(SendLog.TEXT, text);
		values.put(SendLog.SCAN_DT, TimeConvert.getTimestamp());

		long id = db.insert(SendLog.TABLE_NAME, values);
		// 每10000条清除一次已发送记录
		if ((id % m_limit_rows_sendlog) == 0) {
			db.delete(SendLog.TABLE_NAME, SendLog.STATUS + " ='sent' and "
					+ SendLog.ID + "<?",
					new String[] { String.valueOf(id - m_limit_rows_sendlog) });
			Log.i(TAG, "reach 10000 rows, delete " + SendLog.TABLE_NAME);
		}
		return id;
	}

	/**
	 * 测试串
	 * 
	 * @return
	 */
	private String genTestSms() {
		String s = "{'num':1,'body':[{'id':4630100,'target':['13335718048'],'text':'2014-01-04:合约机发展量通报如下(按季度累计完成率排名)：\r\n县市/当日发展量/日均指标/季度累计完成率：\r\n全区:191/235/122.55%\r\n,浦江:42/18/206.82%\r\n,金东:27/19/157.44%\r\n,婺城:18/31/143.83%\r\n,义乌:51/71/116.07%\r\n,东阳:28/29/114.55%\r\n,武义:6/13/110.16%\r\n,兰溪:11/18/95.85%\r\n,永康:8/28/87.15%\r\n,磐安:0/7/41.66%--IT支撑中心'}]}";
		return s;
	}

	private String fetchWeb(String url) throws IOException {
		HttpParams httpParams = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used.
		final int timeoutConnection = m_timeout_conn;
		HttpConnectionParams
				.setConnectionTimeout(httpParams, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		final int timeoutSocket = m_timeout_socket;
		HttpConnectionParams.setSoTimeout(httpParams, timeoutSocket);

		HttpClient httpclient = new DefaultHttpClient(httpParams);
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader("User-Agent", "sms robot");
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 200) {
				Log.e(TAG, "status=" + status.toString());
				throw new IOException("服务器错误" + status.getStatusCode());
			}
			// Log.i(TAG,response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream is = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();

				String line = null;
				try {
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// Log.d("get web content", sb.toString());
				return sb.toString();
			}
		} catch (ClientProtocolException e) {
			throw new IOException("客户端协议异常，" + e.getMessage());
		}
		return null;
	}

}
