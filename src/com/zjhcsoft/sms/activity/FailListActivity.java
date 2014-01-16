package com.zjhcsoft.sms.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.zjhcsoft.sms.DBHelper;
import com.zjhcsoft.sms.DBHelper.SendFailure;
import com.zjhcsoft.smsrobot1.R;

public class FailListActivity extends ListActivity {

//	private static final String TAG = "FailListActivity";
    // This is the Adapter being used to display the list's data
	SimpleCursorAdapter mAdapter;

    // These are the Contacts rows that we will retrieve
    static final String[] PROJECTION = new String[] {SendFailure.ID, SendFailure.REL_ID, SendFailure.STEP, SendFailure.REASON, SendFailure.SCAN_DT, SendFailure.FAIL_DT};

    // This is the select criteria
    static final String SELECTION = null;
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
		DBHelper  dbhelper = new DBHelper(this);
		SQLiteDatabase db  = dbhelper.getWritableDatabase();
		String sql = "select _id,"+ SendFailure.TARGET + "," + SendFailure.STEP +"," + SendFailure.REL_ID + ","+ SendFailure.SCAN_DT + ",  " + SendFailure.CODE + ", " + SendFailure.FAIL_DT  +"," + SendFailure.REASON+ "  from "+ SendFailure.TABLE_NAME + " order by "+ SendFailure.FAIL_DT +" desc";
		Cursor cursor = db.rawQuery(sql, null); 
		startManagingCursor(cursor);
//		long rows = cursor.getCount();
//		Log.d(TAG, "get rows="+rows);
		 mAdapter = new SimpleCursorAdapter(this, R.layout.layout_sendfailure, cursor, 
				new String[]{SendFailure.REL_ID, SendFailure.STEP, SendFailure.REASON, SendFailure.SCAN_DT, SendFailure.FAIL_DT},
				new int[]{R.id.rel_id, R.id.step,  R.id.reason, R.id.scan_dt, R.id.fail_dt});  
		setListAdapter(mAdapter);       
		
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
