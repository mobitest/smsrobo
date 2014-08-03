package com.mt.sms;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mt.sms.DBHelper.AppException;

/**
 * 
 * @author Administrator
  * ---------------
 * history
 * v1.1.15 |防止出错记录过多	|rock	| 10:21 2014/2/12 
*
 */
public class LogHelper {

	private static final String TAG = "LogHelper";
	/**
	 * 广播口信
	 * @param ctx
	 * @param content
	 */
	public static void toast( Context ctx, String content){
		Intent in = new Intent(MsgConstant.BROAD_ACTION_UPDATE);
		String key = MsgConstant.KEY_MSG_TOAST;
		in.putExtra(key, content );  
		ctx.sendBroadcast(in);  
	}
	/**
	 * 广播错误
	 * @param ctx
	 * @param content
	 * @deprecated
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
		StringBuffer sb = new StringBuffer(Log.getStackTraceString(e));
		sb.insert(0, "\r\n").insert(0, content);
		toast(ctx,  content);
		Log.d(TAG, sb.toString());
		
		//写入数据库
		SQLiteDatabase db = null;
		try{
			DBHelper helper = new DBHelper(ctx);
			db = helper.getWritableDatabase();
			//		public static final String TABLE_NAME = "net_failure";
			//		public static final String ID="_id";
			//		public static final String STEP="step";//失败环节
			//		public static final String CODE = "code";
			//		public static final String TEXT="msg";
			//		public static final String REASON="reason";
			//		public static final String FAIL_DT="fail_dt";//失败时间 YYYY-MM-DD hh:mm:ss

			ContentValues  values = new ContentValues();
			values.put(AppException.MSG, content);
			values.put(AppException.STACK, sb.toString());
			values.put(AppException.RAISE_DT,  TimeConvert.getTimestamp());
			long id = db.insert(AppException.TABLE_NAME, null, values);
			
			// 每10000条清除一次
			int rows_limit = 10000;
			Log.d(TAG, "exception id="+ id);
			if ((id % rows_limit) == 0) {
				db.delete(AppException.TABLE_NAME, 
					 AppException.ID + "<?",
						new String[] { String.valueOf(id - rows_limit) });
				Log.i(TAG, "reach " + rows_limit + " rows, delete " + AppException.TABLE_NAME);
			}			
		}catch(Exception ex){
			Log.d(TAG, "异常数据写库失败！");
		}finally{
			if(db!=null) db.close();
		}
    }
    
    /**
     * 广播更新
     * @param ctx
     * @param counter
     * @param val
     * @param id 相关的标识
     */
    public static void count(Context ctx, String counter, String val , long id){
    	Intent in = new Intent(MsgConstant.BROAD_ACTION_UPDATE);
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
