package com.zjhcsoft.sms;

import android.content.Context;
import android.content.Intent;
public class LogHelper {

	/**
	 * 广播口信
	 * @param ctx
	 * @param content
	 */
	public static void toast( Context ctx, String content){
		Intent in = new Intent(SmsDownloadService.BROAD_UPDATE);
		String key = SmsDownloadService.KEY_MSG_TOAST;
		in.putExtra(key, content );  
		ctx.sendBroadcast(in);  
	}
	/**
	 * 广播错误
	 * @param ctx
	 * @param content
	 */
    public static void error( Context ctx, String content){
    	toast(ctx, "!!" + content); 
    }
    /**
     * 异常的详细广播
     * @param ctx
     * @param content
     * @param e
     */
    public static void exception(Context ctx, String content, Exception e){
		StackTraceElement[] ste = e.getStackTrace();
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<ste.length; i++){
			sb.append( ste[i].toString() +"\r\n");
		}
		toast(ctx, "!!"+ content +"\r\n" + e.getMessage() + "\r\n" +sb.toString());
    }
    
    /**
     * 广播更新
     * @param ctx
     * @param counter
     * @param val
     * @param id 相关的标识
     */
    public static void count(Context ctx, String counter, String val , long id){
    	Intent in = new Intent(SmsDownloadService.BROAD_UPDATE);
    	in.putExtra(counter, val );  
    	in.putExtra("id", String.valueOf(id) );
    	ctx.sendBroadcast(in);  
    }
    
    /**
     * 广播普通消息
     * @param ctx
     * @param action
     * @param keys
     * @param vals
     */
    public static void msg(Context ctx, String action, String keys[], String vals[]){
    	Intent in = new Intent(action);
    	for(int i=0;i< keys.length; i++){
    		in.putExtra(keys[i], vals[i]);
    	}
    	ctx.sendBroadcast(in);     	
    }
}
