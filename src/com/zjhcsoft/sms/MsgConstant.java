package com.zjhcsoft.sms;

public class MsgConstant {
    //广播的关键字
    public final static String KEY_MSG_STATUS="status";
    public final static String KEY_MSG_STATUS_IDLE="侦听中";
    public final static String KEY_MSG_STATUS_BUSY="扫描中";
    public final static String KEY_MSG_STATUS_HALT="关闭";
    public final static String KEY_MSG_COUNT_GET="count.get";
    public final static String KEY_MSG_COUNT_QUEUE="count.queue";    
    public final static String KEY_MSG_COUNT_PACK="count.pack";  
    public final static String KEY_MSG_COUNT_TRACE_DELIVERED="count.delivered";
    public final static String KEY_MSG_COUNT_TRACE_SENT="count.sent";
    public static final String KEY_MSG_COUNT_TRACE_FAIL = "count.fail";
    public final static String KEY_MSG_COUNT_TRACE_SENT_AGAIN="count.sent_again";
    public static final String KEY_MSG_COUNT_PACK_AGAIN = "count.packed_again";
    public final static String BROAD_ACTION_UPDATE="com.zjhcsoft.sms.communication.reciver";
    public final static String KEY_DELIVERED="com.zjhcsoft.sms.communication.delivered";
    public static final String KEY_MSG_COUNT_SCAN = "count.scan";
    public static final String KEY_MSG_SERVICE_READY="ready";
    public static final String KEY_MSG_AREA="config";
	public static final String KEY_MSG_TOAST = "toast";

    public final static String BROAD_SENT = "com.zjhcsoft.sms.SENT";
    public final static String BROAD_DELIVERED = "com.zjhcsoft.sms.DELI";
	public static final String BROAD_NEWROW = "SMS_NEW";
	public static final String KEY_MSG_TASK_ARRIVAL = "wait_sending";	
	
	/**
	 * 发送状态
	 */
	public final static String STATUS_PROCESS ="ing";
	public final static String STATUS_TOTAL ="total";
	public final static String STATUS_QUEUE =null;
	public final static String STATUS_FAIL ="fail";
	public final static String STATUS_SENT ="sent";
	//统计广播
	public static final String STATUS_DELAY = "delay";
	public final static String STATUS_DELIVERED ="delivered";
	public static final String BROAD_ACTION_STATS = "com.zjhcsoft.sms.stats.today";

}
