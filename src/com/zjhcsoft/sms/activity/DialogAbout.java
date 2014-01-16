package com.zjhcsoft.sms.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zjhcsoft.sms.SmsRobotApp;
import com.zjhcsoft.smsrobot1.R;

public class DialogAbout {

    private Context mContext;

    private SmsRobotApp app;

    public DialogAbout(Context mContext, SmsRobotApp app) {
        this.mContext = mContext;
        this.app = app;
    }

    public void init(AlertDialog.Builder builder) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(), 0);
            
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.alert_dialog_about, null);
        TextView version = (TextView) layout.findViewById(R.id.text_version);
        TextView name = (TextView) layout.findViewById(R.id.text_package_name);
        TextView rights = (TextView) layout.findViewById(R.id.text_rights);
        TextView copyright = (TextView) layout.findViewById(R.id.text_copyright);
        name.setText(mContext.getString(R.string.app_name));
        String ver = "版本："+ (packageInfo == null ? "未知" : "V"+ packageInfo.versionName) +  (packageInfo == null ? "" : "("+packageInfo.versionCode) +")";
        version.setText(ver);
        copyright.setText(mContext.getString(R.string.copyright_info));
        rights.setText(mContext.getString(R.string.login_bottom_text));
        builder.setView(layout);
        builder.setTitle("关于");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setNegativeButton("确定", null);
        
        
        TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getSubscriberId();
        TextView tvImei = (TextView)layout.findViewById(R.id.edit_imei);
        tvImei.setText(imei);        
    }

}
