package com.zjhcsoft.sms;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper{
    private static final String TAG = "DBHelper";
    
    /**
     * 数据库版本号
     * 只在需要升级数据库时，需改变该版本号。
     */
    private static final int DATABASE_VERSION = 5;
    /**
     * 数据库名称
     */
    private static final String DATABASE_NAME = "smsrobot";

//	private static final String TABLE_SENDLOG = "send_log";
    
    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建客户信息表
        createTable(db);
    }

    /**
     * 数据库升级
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade from version:" + oldVersion + " to version:" + newVersion);
        if(oldVersion<newVersion){
        	//直接重建数据库
        	clearAllTables(db);
//        	//备份数据
//        	String sql1 = "ALTER TABLE " + SendLog.TABLE_NAME +" RENAME TO temp_" + SendLog.TABLE_NAME;
//        	execSQL(sql1);
//        	
//        	//建新表
//        	createTable(db);
//        	
//        	//恢复备份数据到新表中
//        	String columns =  SendLog.TARGET + "," + SendLog.TEXT + "," + SendLog.SCAN_DT + "," + SendLog.SEND_DT + "," + SendLog.DELI_DT + "," + SendLog.STATUS ; 
//        	String sql2 = "insert into "+ SendLog.TABLE_NAME + "(" + columns + ")"
//        			+ " select " + columns + " from temp_" + SendLog.TABLE_NAME  ;
//        	execSQL(sql2);
        	
        }
    }
    
    /**
     * 执行SQL语句
     * @param sql
     */
    public void execSQL(String sql){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);
    }
    
    /**
     * 清除数据库（删表重建）
     */
    public void clearAllTables(SQLiteDatabase db){
//        SQLiteDatabase db = getWritableDatabase();
    	String tables[] = {SendLog.TABLE_NAME, SendFailure.TABLE_NAME, AppException.TABLE_NAME};
    	for(int i=0; i< tables.length; i++){
    		db.execSQL("DROP TABLE IF EXISTS " +tables[i]);
    		Log.d("drop table", tables[i]);
    	}
        onCreate(db);
    }

    /**
     * 插入
     * @param table
     * @param values
     * @return the row ID of the newly inserted row, or -1 if an error occurred 
     */
    public long insert(String table, ContentValues values) throws SQLiteException {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(table, null, values);
//        Log.d(TAG, "insert into " + table + ", at position " + id + " where content values is "
//                + values);
        return id;
    }
    
    /**
     * 更新
     * @param table
     * @param values
     * @param whereClause
     * @param whereArgs
     * @return the number of rows affected 
     */
    public long update(String table, ContentValues values, String whereClause, String[] whereArgs) throws SQLiteException{
        SQLiteDatabase db = getWritableDatabase();
        long id = db.update(table, values, whereClause, whereArgs);
        return id;
    }
    
    /**
     * 查询
     * @param tableName
     * @param projectionMap
     * @param projections
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param sortOrder
     * @return a cursor, or null
     */
    public Cursor query(String tableName, HashMap<String, String> projectionMap,
            String[] columns, String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder, String limit) throws SQLiteException {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(tableName, columns, selection, selectionArgs, groupBy, having, sortOrder, limit);
        if (c == null) {
            return null;
        } else if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    /**
     * 根据条件删除
     * @param table
     * @param whereClause
     * @param args
     * @return the number of rows affected if a whereClause is passed in, 0 otherwise. 
     *         To remove all rows and get a count pass "1" as the whereClause.
     */
    public int delete(String table, String whereClause, String[] args) throws SQLiteException {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(table, whereClause, args);
    }

    /**
     * 删除
     * @param table
     */
    public void deleteAll(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, null, null);
    }
    
    /**
     * 创建服务器信息表
     * @param db
     */
    private void createTable(SQLiteDatabase db){
        final String createSendLogSql = "CREATE TABLE " +SendLog.TABLE_NAME + " (" 
                + SendLog.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," //改名
                + SendLog.TARGET + " TEXT,"
                + SendLog.TEXT + " TEXT,"
                + SendLog.SCAN_DT + " TEXT,"
                + SendLog.SEND_DT0 + " TEXT,"
                + SendLog.SEND_DT + " TEXT,"
                + SendLog.DELI_DT + " TEXT,"
                + SendLog.STATUS + " TEXT," 
                + SendLog.RETRY_TIMES + " smallint DEFAULT 0, " //新增
                + SendLog.ORG_ID + " integer " //新增
                +");";
        db.execSQL(createSendLogSql);
        
        Log.d(TAG, "onCreate, CREATE TABLE " + SendLog.TABLE_NAME);
        
        final String createSendFailureSql = 
        		"CREATE TABLE " +SendFailure.TABLE_NAME + " (" 
                        + SendFailure.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
                        + SendFailure.REL_ID + " integer,"
                        + SendFailure.TARGET + " TEXT,"
                        + SendFailure.CODE + " integer,"
                        + SendFailure.TEXT + " TEXT,"
                        + SendFailure.SCAN_DT + " TEXT,"
                        + SendFailure.FAIL_DT + " TEXT,"
                        + SendFailure.STEP + " TEXT,"
                        + SendFailure.REASON + " TEXT" 
                        +");";
        db.execSQL(createSendFailureSql);
        
        Log.d(TAG, "onCreate, CREATE TABLE " + SendFailure.TABLE_NAME);

        final String createAppExceptionSql = 
        		"CREATE TABLE " +AppException.TABLE_NAME + " (" 
                        + AppException.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," //改名
                        + AppException.STEP + " TEXT,"
                        + AppException.MSG + " TEXT,"
                        + AppException.RAISE_DT + " TEXT,"
                        + AppException.STACK + " TEXT" 
                        +");";
        db.execSQL(createAppExceptionSql);
        
        Log.d(TAG, "onCreate, CREATE TABLE " + AppException.TABLE_NAME);

    }

    /*
     * 发送日志
     */
	public static class SendLog {
		public static final String TABLE_NAME = "send_log";
		public static final String ID="_id";
		public static final String TARGET="target";
		public static final String TEXT="msgtext";
		public static final String SCAN_DT="scan_dt";
		public static final String SEND_DT0 = "send_dt0";
		public static final String SEND_DT="send_dt";
		public static final String DELI_DT = "deli_dt";
		public static final String STATUS="status"; //status: sent/delivered
		public static final String RETRY_TIMES = "retry_times";//重试次数
		public static final String ORG_ID = "org_id"; //原始ID
		public long _id;
		public String target;
		public String text;
		public String scan_dt;
		public String send_dt;
		public String status;
		public int retry_times;
		public long org_id;
		public String send_dt0;
		
		public SendLog(long id, int times, String target, String text) {
			this._id = id;
			this.retry_times = times;
			this.target = target;
			this.text = text;
			// TODO Auto-generated constructor stub
		}

		public static ContentValues genInsertValues(String id, String target, String text){
			 ContentValues  values = new ContentValues();
			 values.put(ID, id);
			 values.put(TARGET, target);
			 values.put(TEXT, text);
			 return values;
		}
	
	}//-SendLog
	
	/*
	 * 发送失败
	 */
	public static class SendFailure{
		public static final String TABLE_NAME = "send_failure";
		public static final String ID="_id";
		public static final String REL_ID="rel_id";
		public static final String TARGET="target";
		public static final String CODE = "code";
		public static final String TEXT="msgtext";
		public static final String SCAN_DT="scan_dt";//扫描时间
		public static final String FAIL_DT="fail_dt";//失败时间 YYYY-MM-DD hh:mm:ss
		public static final String STEP="step";//失败环节,发出、送达
		public static final String REASON="reason";
		
	}
	
	/**
	 * 网络失败
	 */
	public static class AppException{
		public static final String TABLE_NAME = "app_exception";
		public static final String ID="_id";
		public static final String STEP="step";//失败环节
		public static final String CODE = "code";
		public static final String MSG="msg";
		public static final String STACK="stack";
		public static final String RAISE_DT="raise_dt";//失败时间 YYYY-MM-DD hh:mm:ss
		
	}
}
