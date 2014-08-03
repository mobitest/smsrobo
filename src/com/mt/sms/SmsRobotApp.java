package com.mt.sms;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.EmailIntentSender;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.mt.sms.receiver.SentReceiver;
import com.mt.sms.service.SmsStatsIntentService;
import com.mt.smsrobo.R;


@ReportsCrashes(formKey = "", // will not be used
mailTo = "yanxw@zjhcsoft.com", customReportContent = {
		ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
		ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
		ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT }, mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)

public class SmsRobotApp extends Application {
	private static final String PREF_FILE = "defaults";
	static SmsRobotApp instance=null;
	public int gCountSent =0 ;//寄出数（到达运营商网关）
	public int gCountPackedAgain=0;//重发
	public int gCountSentAgain = 0;//重发成功
	public int gCountFail=0;//失败数
	public int gCountDelivered=0;//送达数
	public int gCountGet = 0;//下载条数
	public int gCountQueue = 0;//排队条数
	
	public int gCountScan = 0;//扫描次数
	public String gLastReport;
	/**
	 * 从一次报告之后，已经累计发送的
	 * @see SentReceiver
	 *    失败、寄出时++
	 * @see SmsStatsIntentService. 
	 */
	public int gCountReportLast = 0;
	public boolean gBatchDoing = false;//发送中：一次扫描有内容，真；连续两次扫描无内容，报告后，置假；
	public int gBatchCount =0;//批次
	public int gBatchNthCount=0;
	public int gBatchSthCount = 0;
	
	/*
	 * 是否新重启
	 */
	public boolean gIsRebooted = false;
	public int gCountPacked=0; //打包
	
	@Override
	public void onCreate() {
		Log.d("App","on create...app");
		ACRA.init(this);
		EmailIntentSender intent = new EmailIntentSender(this);
		ACRA.getErrorReporter().setReportSender(intent);
		instance = this;
		super.onCreate();
	}
	
	
	public static SmsRobotApp getInstance(){
		return instance;
	}
	
	public  SharedPreferences getSharedPreferences(){
		return this.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
	}

	/**
	 * 重置计数器
	 */
	public  void resetCounter(){
		gCountSent =0 ;//寄出数（到达运营商网关）
		gCountPackedAgain=0;//重发
		gCountSentAgain = 0;//重发成功
		gCountFail=0;//失败数
		gCountDelivered=0;//送达数
		gCountGet = 0;//下载条数
		gCountQueue = 0;//排队条数

		gCountScan = 0;//扫描次数
		gCountPacked=0; //打包		
	}
}
