package com.zjhcsoft.sms.activity;

import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.AppException;
import com.zjhcsoft.sms.DBHelper.SendFailure;
import com.zjhcsoft.sms.TimeConvert;
import com.zjhcsoft.smsrobot1.R;

public class ExceptionListActivity extends ListActivity {

//	private static final String TAG = "FailListActivity";
    // This is the Adapter being used to display the list's data
	SimpleCursorAdapter mAdapter;

    // These are the Contacts rows that we will retrieve
    static final String[] PROJECTION = new String[] {SendFailure.ID, SendFailure.REL_ID, SendFailure.STEP, SendFailure.REASON, SendFailure.SCAN_DT, SendFailure.FAIL_DT};

    // This is the select criteria
    static final String SELECTION = null;

	private static final String TAG = "ExceptionListActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fail_list, menu);
		return true;
	}
	@SuppressWarnings("deprecation")
	private void setListData(){
		try {
			
			DBHelper  dbhelper = new DBHelper(this);
			SQLiteDatabase db  = dbhelper.getWritableDatabase();
			String lastWeek = TimeConvert.time2Str((new Date()).getTime() - 1000*60*60 *24* 5, TimeConvert.DATE_PATTERN_B);
			Log.d("exceptionActivity", "lastWeek="+lastWeek);
			String sql = "select _id,"+ AppException.MSG + "," + AppException.STACK +"," + AppException.RAISE_DT + "  from "+ AppException.TABLE_NAME 
					+" where " + AppException.RAISE_DT +"> ? " 
					+ " order by "+ AppException.RAISE_DT +" desc";
			Log.d(TAG, "sql="+sql);
			Cursor cursor = db.rawQuery(sql , new String[]{lastWeek}); 
			startManagingCursor(cursor);
			long rows = cursor.getCount();
			Log.d("exceptionActivity", "get rows="+rows);
			mAdapter = new SimpleCursorAdapter(this, R.layout.layout_exception, cursor, 
					new String[]{ AppException.MSG, AppException.STACK, AppException.RAISE_DT},
					new int[]{R.id.exc_msg,  R.id.exc_track, R.id.exc_raise_dt});  
			setListAdapter(mAdapter);       
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
		

    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			Intent intent = NavUtils.getParentActivityIntent(this); 
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP); 
			NavUtils.navigateUpTo(this, intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
