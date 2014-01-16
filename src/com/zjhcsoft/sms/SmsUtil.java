package com.zjhcsoft.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.telephony.SmsManager;
import android.util.Log;

/**
 * 短信工具
 * @author Administrator
 *
 */
public class SmsUtil {
	private int INTERVAL_SEND=100;
	private String SERVICE_PATH="sms/getsms?area=32518";
	private String SERVICE_CONTEXT="http://10.80.12.101:8080/";
	
	private String JSON_NUM="num";
	private String JSON_BODY="body";
	private String JSON_TARGET="target";
	private String JSON_TEXT = "text";

	/**
	 * 扫描获取待发短信,执行发送
	 */
	public void doSend(){
		//获取
		JSONObject json = getSmsWaiting();
//		JSONObjectjson = null;
		
		//拆解、发送
		try {
			int num =json.getInt(JSON_NUM);
			if(num<=0) return ;
			
			SmsManager sms = SmsManager.getDefault();   
			JSONArray nodes = json.getJSONArray(JSON_BODY);
			for(int i=0; i<num; i++){
				JSONObject node = nodes.getJSONObject(i);
				String text = node.getString(JSON_TEXT);
				JSONArray targets = node.getJSONArray(JSON_TARGET);
				int num_targets = targets.length();
				for(int j=0; j<num_targets; j++){
					ArrayList<String> msgs =  sms.divideMessage( text.toString());
					Log.d("send:", targets.getString(j)+",msg="+ text);
					sms.sendMultipartTextMessage ( targets.getString(j), null,msgs,null,null);   
					Thread.sleep(INTERVAL_SEND);
				}
			}
		} catch (JSONException e) {
			Log.e("sms","可能协议有误，信息原始内容:" + json.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private JSONObject getTestMsg() throws JSONException{
		String s= "{'body':[{'target':['15372095937','905937'],'id':1,'text':'新增用户 120，2013年3月5日'},{'target':['15372095937'],'id':2,'text':'test msg1:today we get 120 new customers'}],'num':2}";
		return new JSONObject(s);
	}
	/**
	 * 获取待发送的待信
	 * @return
	 */
	private JSONObject getSmsWaiting(){
		String url = SERVICE_CONTEXT + SERVICE_PATH;
		url = "http://134.100.5.30:8888/sms/getsms?area=5";
		JSONObject json= new JSONObject();
		Log.d("smsutil","get web content, url="+url);
		String JsonResponse = getWeb(url);
//		Log.d("smsutil","http result:"+JsonResponse);
		if(JsonResponse==null) return json;
		try {
			json=new JSONObject(JsonResponse);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;
	}

	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
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
		return sb.toString();
	}
	private static String getWeb(String url)
	{
		HttpClient httpclient = new DefaultHttpClient();
//		httpclient.
		HttpGet httpget = new HttpGet(url); 
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			//Log.i(TAG,response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				String result= convertStreamToString(instream);
				instream.close();
				return result;
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		return null;
	}	
}
