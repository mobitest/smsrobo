package com.zjhcsoft.sms;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.EmailIntentSender;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.zjhcsoft.smsrobot1.R;


@ReportsCrashes(formKey = "", // will not be used
mailTo = "yanxw@zjhcsoft.com", customReportContent = {
		ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
		ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
		ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT }, mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)

public class SmsRobotApp extends Application {
	private static final String PREF_FILE = "defaults";
	static SmsRobotApp instance=null;

	@Override
	public void onCreate() {
		Log.d("App","on create...app");
		ACRA.init(this);
		EmailIntentSender intent = new EmailIntentSender(this);
		ACRA.getErrorReporter().setReportSender(intent);
		super.onCreate();

		instance = this;
	}
	
	
	public static SmsRobotApp getInstance(){
		return instance;
	}
	
	public  SharedPreferences getSharedPreferences(){
		return this.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
	}

}
