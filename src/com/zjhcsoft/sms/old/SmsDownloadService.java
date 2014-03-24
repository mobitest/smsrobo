package com.zjhcsoft.sms.old;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.LogHelper;
import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.sms.DBHelper.SendLog;

public class SmsDownloadService extends android.app.Service {
//    private NotificationManager mNM;
    private boolean mIsOn = true;
    private boolean mDebugOn = false;
    //配置项缺省值
    private final int DEFAULT_SCAN=5;//5分钟
    private final int DEFAULT_SEND=100;//100毫秒
    private final String DEFAULT_SERVER="http://134.98.104.25:7777/";//
    
    private final String SERVICE_PATH="sms/getsms";
    private final static String TAG="SmsSendService";
    
    //广播的关键字
    public final static String KEY_MSG_STATUS="status";
    public final static String KEY_MSG_STATUS_IDLE="侦听中";
    public final static String KEY_MSG_STATUS_BUSY="扫描中";
    public final static String KEY_MSG_COUNT_GET="get";
    public final static String KEY_MSG_COUNT_SEND="send";    
    public final static String KEY_MSG_COUNT_PACK="pack";  
    public final static String KEY_MSG_COUNT_TRACE_DELIVERED="count_delivered";
    public final static String KEY_MSG_COUNT_TRACE_SENT="sent";
    public static final String KEY_MSG_COUNT_TRACE_FAIL = "count_fail";
    public final static String BROAD_UPDATE="com.zjhcsoft.sms.communication.reciver";
    public final static String KEY_DELIVERED="com.zjhcsoft.sms.communication.delivered";
    public static final String KEY_MSG_COUNT_SCAN = "scan";
    public static final String KEY_MSG_SERVICE_READY="ready";
    public static final String KEY_MSG_AREA="config";
	public static final String KEY_MSG_TOAST = "toast";

    public final static String BROAD_SENT = "SENT";
    public final static String BROAD_DELIVERED = "DELI";
	public static final String BROAD_NEWROW = "SMS_NEW";
	static final String KEY_MSG_TASK_ARRIVAL = "wait_sending";
    
    //服务级变量
	private int m_interval_scan=0;
	private int m_interval_send=0;
	private int m_timeout_conn = 0;
	private int m_timeout_socket = 0;
//	private final String mServer="";
	private String mServiceUrl="";
	private int mCountGet = 0;
	private int mCountSend =0;
	private int mCountScan =0;
	private int m_limit_rows_sendlog;
	
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
//    private int NOTIFICATION = R.string.service_started;
    private final IBinder mBinder = new LocalBinder();
//    private Intent broadIntent = new Intent(BROAD_UPDATE);
	private String mArea;
	private String mImei;
    private static SmsDownloadService sInstance;
    public SmsDownloadService() {
	}

	public static SmsDownloadService getInstance(){
    	return sInstance;
    }

	OnSharedPreferenceChangeListener	mListener
		= new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			Log.d(TAG, "changed:key="+key);
			loadSetting();
			// Implementation
		}
	};
	private Timer timer;
    @Override
    public void onCreate() {
    	sInstance = this;
//        broadIntent.putExtra(KEY_MSG_SERVICE_READY, 1);  
//        sendBroadcast(broadIntent);  
    	
        LogHelper.msg(SmsDownloadService.this, BROAD_UPDATE, new String[]{KEY_MSG_SERVICE_READY}, new String[]{"1"});
    }
    
    @Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	

	public class LocalBinder extends Binder {
    	SmsDownloadService getService() {
            return SmsDownloadService.this;
        }
    }
    
	/**
	 * 加载设置
	 */
	public void loadSetting(){
		Log.d(TAG, "get pref values");
        boolean useInternet = mSharedPref.getBoolean("use_internet",false);
        String server = mSharedPref.getString("server" + (useInternet?"1":"0" ), DEFAULT_SERVER);
        mServiceUrl = server + SERVICE_PATH;
        
        m_interval_scan =  mSharedPref.getInt("interval__scan",DEFAULT_SCAN) * 1000* 60;//分钟
        m_interval_send = mSharedPref.getInt("interval__send_1",DEFAULT_SEND) ;
        m_timeout_conn = mSharedPref.getInt("interval__conn",1) *1000 * 60;
        m_timeout_socket = mSharedPref.getInt("timeout_socket", 3)*1000*60;
        mArea = mSharedPref.getString("area","7"); 
		m_limit_rows_sendlog = mSharedPref.getInt("limit_rows_sendlog",5000);
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mImei = telephonyManager.getSubscriberId();		
        Log.i(TAG + " Service config","server_url="+ mServiceUrl +";interval_scan=" + m_interval_scan+"(ms);interval_send="+ m_interval_send); 
	}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//    	mSharedPref = SmsRobotApp.getContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    	mSharedPref = SmsRobotApp.getInstance().getSharedPreferences();
        mSharedPref.registerOnSharedPreferenceChangeListener(mListener);
        loadSetting();
        

//        broadIntent.putExtra(KEY_MSG_CONFIG,  "区域："+ mArea);
//        sendBroadcast(broadIntent);  
        LogHelper.msg(SmsDownloadService.this, BROAD_UPDATE, new String[]{KEY_MSG_AREA}, new String[]{ mArea });
        LogHelper.toast(getApplicationContext(), "server_url="+ mServiceUrl +";interval_scan=" + m_interval_scan+"(ms);interval_send="+ m_interval_send);
        
        //发送Action为com.example.communication.RECEIVER的广播  
//        broadIntent.putExtra(KEY_MSG_STATUS, KEY_MSG_STATUS_BUSY);  
//        sendBroadcast(broadIntent);  
        
        Log.i(TAG+ " LocalService", "Received start id " + startId + ": " + intent);
        
        scheduleStart();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
//        mIsOn=true;
//        Thread t = new Thread(){
//
//			@Override
//			public void run() {
//				while(mIsOn /*&&counter++<3*/){
//					broadUpate(KEY_MSG_STATUS, KEY_MSG_STATUS_BUSY);  
//		            
//					download();
//					broadUpate(KEY_MSG_STATUS, KEY_MSG_STATUS_IDLE);  
//					try {
//						Thread.sleep(m_interval_scan);
//					} catch (InterruptedException e) {
//						broadUpate(KEY_MSG_TOAST,"调用中断失败!");
//						e.printStackTrace();
//					}
//				}
//				
//			}
//        	
//        };
//        t.start();
        
        return START_STICKY;
    }
	/**
	 * 启动定时发送
	 */
	private void scheduleStart() {
		if(timer!=null){
			timer.cancel();
			timer = null;
		}
		TimerTask task = new TimerTask(){    
			public void run(){    
			 // 在此处添加执行的代码    
				try {
					broadUpate(KEY_MSG_STATUS, KEY_MSG_STATUS_BUSY);  
		            
					download();
					broadUpate(KEY_MSG_STATUS, KEY_MSG_STATUS_IDLE);  
				} catch (Exception e) {
					LogHelper.exception(SmsDownloadService.this, "下载异常：",e);
//					timer.cancel();
//					e.printStackTrace();
				}
			}    
		};    
		timer = new Timer();  
		timer.schedule(task, 0, m_interval_scan);//开启定时器，delay 1s后执行task 
		int m_interval_scan_min = m_interval_scan/1000/60 ;
		LogHelper.toast(getApplicationContext(), "定时扫描启动，"+ m_interval_scan_min+"分钟一次");
	}   
    @Override
    public void onDestroy() {
        mIsOn= false;
        if(timer!=null) timer.cancel();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(mListener);

        // Tell the user we stopped.
        Toast.makeText(this, "停止短信扫描", Toast.LENGTH_SHORT).show();
    }
    

    private void broadUpate(String key, Object content){
//    	broadIntent = new Intent(BROAD_UPDATE);
//        broadIntent.putExtra(key, String.valueOf(content) ); 
//        sendBroadcast(broadIntent); 
        LogHelper.msg(SmsDownloadService.this, SmsDownloadService.BROAD_UPDATE, new String[]{key}, new String[]{String.valueOf(content) });
    }

    	private String JSON_NUM="num";
    	private String JSON_BODY="body";
    	private String JSON_TARGET="target";
    	private String JSON_TEXT = "text";
		private SharedPreferences mSharedPref;

    	/**
    	 * 扫描获取待发短信,执行发送
    	 */
    	private void download(){
    		mCountScan++;
    		broadUpate(KEY_MSG_COUNT_SCAN, mCountScan);
    		DBHelper db = null;
    		try {
//    			if(1==1) throw(new Exception("just for test"));
    			//获取
    			JSONObject json = getSmsWaiting();
    			
    			//拆解、发送
    			int num =json.getInt(JSON_NUM);
    			if(num<0) {
    				LogHelper.error( this, "请检查此手机是否被授权！返回码=" + num);
//    				broadMsg(KEY_MSG_TOAST, "请检查此手机是否被授权！返回码=" + num);
    				return ;
    			}
    			if(num==0) {
    				LogHelper.toast(this, "任务数为零");
    				return ;
    			}
    			broadUpate(KEY_MSG_STATUS, "发送中");  
    			
    			//广播：取到数（短信总条数）
    			mCountGet+=num;
    			broadUpate(KEY_MSG_COUNT_GET, mCountGet);
//    			SmsManager sms0 = SmsManager.getDefault();   
    			JSONArray nodes = json.getJSONArray(JSON_BODY);
    			
    			db = new DBHelper(SmsDownloadService.this);
    			for(int i=0; i<num; i++){
    				JSONObject node = nodes.getJSONObject(i);
    				String text = node.getString(JSON_TEXT);
    				JSONArray targets = node.getJSONArray(JSON_TARGET);
    				int num_targets = targets.length();
    				for(int j=0; j<num_targets; j++){
    					//保留发送日志
    					String phone = targets.getString(j);
//    					long rowid = 
    							insert(db, phone, text);//将短信下载记录插入到数据库中
    					
//    					//广播：要发！
//    		        	Intent newrow = new Intent(BROAD_NEWROW);
//    		        	newrow.putExtra("rowid", rowid +"-0");//ID+次数  
//    		        	sendBroadcast(newrow);//通知新的短信发送中
//    		        	//真正开始发送
//    		        	Thread thread = new SendThread(rowid, phone, text, this, 0);
//    		        	thread.start();
//    		        	sendSMS(rowid, targets.getString(j), text, SmsSendService.this);
    		        	
//    					ArrayList<String> msgs =  sms0.divideMessage( text.toString() );
//    					Log.d("send0:", targets.getString(j)+",msg="+ text);
//    					sms0.sendMultipartTextMessage ( targets.getString(j), null,msgs,null,null);   
    	    			
    		        	//TODO:好象计数有问题（与处理数口径不一致？当短信有多个接收人时）
    		        	//广播：已安排条数（发出、送达状态在跟踪服务中更新）
//    					mCountDone++;
//    					broadMsg(KEY_MSG_COUNT_DONE,  mCountDone);
//    					Thread.sleep(m_interval_send);
    				}//end all target
    				//广播：处理短信数
    				mCountSend++;
    				broadUpate(KEY_MSG_COUNT_SEND,  mCountSend);
    			}//end all msg
    			LogHelper.msg(SmsDownloadService.this, SmsDownloadService.KEY_MSG_TASK_ARRIVAL, new String[]{"rows"}, new String[]{String.valueOf(mCountSend) });
    			
    		} catch (JSONException e) {
    			Log.e(TAG +" sms","可能协议有误");
    			LogHelper.error( this, "消息无法解析;"+ e.getMessage());
//    			broadMsg(KEY_MSG_TOAST, "消息无法解析;"+ e.getMessage());
    			// TODO Auto-generated catch block
    			e.printStackTrace();
//    		} catch (InterruptedException e) {
//    			broadMsg(KEY_MSG_TOAST, "线程中断失败;"+ e.getMessage());
//    			e.printStackTrace();
    		} catch (IOException e) {
    			LogHelper.error( this, "联网失败；请检查手机的网络设置，及服务器位置。\r\n"+ e.getMessage());
				e.printStackTrace();
    		}catch(Exception e){
    			LogHelper.error( this,  "7怪的异常。\r\n"+ e.getMessage());
				e.printStackTrace();    			
			}finally{
				if(db!=null) {
					db.close();
					db=null;
				}
			}
    		
    		
    	}
    	
    	/**
    	 * 将下载短信逐条插入数据库
    	 * @param num
    	 * @param text
    	 * @return
    	 */
    	private long insert(DBHelper db, String num, String text) throws Exception{
    		ContentValues values = new ContentValues();
            
            //ID自增，只存放名称和URL
            values.put(SendLog.TARGET, num);
            values.put(SendLog.TEXT, text);
            values.put(SendLog.SCAN_DT, TimeConvert.getTimestamp());
            
    		long id =  db.insert(SendLog.TABLE_NAME, values);
    		//每10000条清除一次已发送记录
    		if((id % m_limit_rows_sendlog) ==0){
    			db.delete(SendLog.TABLE_NAME, SendLog.STATUS + " ='sent' and " + SendLog.ID + "<?", new String[]{ String.valueOf(id - m_limit_rows_sendlog)});
    			Log.i(TAG, "reach 10000 rows, delete " + SendLog.TABLE_NAME);
    		}
    		return id;
    	}
    	private JSONObject getTestMsg() throws JSONException{
    		String s = "{'num':1,'body':[{'id':4630100,'target':['13335718048'],'text':'2014-01-04:金华合约机发展量通报如下(按季度累计完成率排名)：\r\n县市/当日发展量/日均指标/季度累计完成率：\r\n全区:191/235/122.55%\r\n,浦江:42/18/206.82%\r\n,金东:27/19/157.44%\r\n,婺城:18/31/143.83%\r\n,义乌:51/71/116.07%\r\n,东阳:28/29/114.55%\r\n,武义:6/13/110.16%\r\n,兰溪:11/18/95.85%\r\n,永康:8/28/87.15%\r\n,磐安:0/7/41.66%--IT支撑中心'}]}";
//    		String s= "{'body':[{'target':['15372095937','905937'],'id':1,'text':'新增用户 120，2013年3月5日'},{'target':['15372095937'],'id':2,'text':'test msg1:today we get 120 new customers'}],'num':2}";
    		return new JSONObject(s);
    	}
    	/**
    	 * 获取待发送的待信
    	 * @return
    	 * @throws IOException 
    	 * @throws JSONException 
    	 */
    	private JSONObject getSmsWaiting() throws IOException, JSONException{
//    		String url = SERVICE_CONTEXT + SERVICE_PATH;
//    		url = "http://134.100.5.30:8888/sms/getsms?area=5";
    		JSONObject json= null;
    		if(mDebugOn){
    			try {
					json = getTestMsg();
					return json;
				} catch (JSONException e) {
					e.printStackTrace();
				}
    		}
//    		mImei = "460030986520725";
    		String url = mServiceUrl +"?area=" + mArea + "&IMEI="+mImei;
    		String JsonResponse = getWebContent(url);
    		Log.d(TAG ,"get web content, url="+url);
//    		Log.d("smsutil","http result:"+JsonResponse);
//    		JsonResponse = getWebContent(url);
    		if(JsonResponse==null) return json;
    		json=new JSONObject(JsonResponse);
    		
    		return json;
    	}

    	private  String getWebContent(String url) throws IOException
    	{
    		HttpParams httpParams = new BasicHttpParams();
    		// Set the timeout in milliseconds until a connection is established.
    		// The default value is zero, that means the timeout is not used. 
    		final int timeoutConnection = m_timeout_conn;
    		HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
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
    				Log.e(TAG, "status="+ status.toString());
    				throw new IOException("服务器错误，代码=" + status.getStatusCode());
    			}
    			//Log.i(TAG,response.getStatusLine().toString());
    			HttpEntity entity = response.getEntity();
    			if (entity != null) {
    				InputStream is = entity.getContent();
    				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
//    	    		Log.d("get web content", sb.toString());
    				return sb.toString();
    			}
    		} catch (ClientProtocolException e) {
    			throw new IOException("呀，请通知管理员，可能服务器宕机了。客户端协议异常，" + e.getMessage());
    		}
    		return null;
    	}	


}
