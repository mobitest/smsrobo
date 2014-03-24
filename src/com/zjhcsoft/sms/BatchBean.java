package com.zjhcsoft.sms;

import android.util.Log;

public class BatchBean{
	private final String tag="BatchBean";
	
	boolean inProcessing = false;
	private int size= 0;//总数
	public int countSent=0;//发出
	public int countFailed=0;//失败
	
	int countBatches=0;
	public void put(int nums){
		if(nums>0){//有东西
			if(!inProcessing){
				//新开始一批,重新计数
				size =nums;
				countBatches++;
				inProcessing = true;
			}else{//同一批
				inProcessing = true;
				size +=nums;
			}
		}else{//没东西
			if(!inProcessing){//上批已经没东西？
				//一轮结束
				//TODO:报告
				makeReport();
				//批内计数器清零
				size=0;
			}else{
				//一轮可能结束
				inProcessing = false;
			}
		}
		return ;
	}
	public void reset(){
		size=0;
		countSent = 0;
		countFailed =0;
		inProcessing = false;
	}
	public int getSize(){
		return this.size;
	}

	
	/**
	 * 批次报告
	 */
	private void makeReport(){
		Log.d(tag,"batches="+ countBatches+";size=" + size);
	}
}