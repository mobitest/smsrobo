package com.mt.sms.activity;

import java.util.Date;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mt.sms.DBHelper;
import com.mt.sms.TimeConvert;
import com.mt.sms.DBHelper.SendLog;
import com.mt.smsrobo.R;

public class ListSendActivity extends ListActivity {
	private String TAG = "ListSendActivity";
	private SimpleCursorAdapter mListAdapter;
	private SQLiteDatabase db = null;
	private CheckBox mCbShowTodayOnly;
	private TextView mTvRows;
	private EditText mInputMsgSearch;
	private String mStatus;
	private Spinner mSpinner1;
	private String[] status_codes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		mStatus = intent.getStringExtra("status");
		if (mStatus != null) {
			Log.d(TAG, mStatus);
		} else {
			Log.d(TAG, "no status set");

			mStatus = "all";
		}
		setContentView(R.layout.layout_sendlog_main);
		mCbShowTodayOnly = (CheckBox) findViewById(R.id.cb_show_today);
		mTvRows = (TextView) findViewById(R.id.tv_log_rows);
		mInputMsgSearch = (EditText) findViewById(R.id.input_msg_search);
		// Show the Up button in the action bar.
		setupActionBar();
		mCbShowTodayOnly
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						setListData();
					}
				});
		mInputMsgSearch.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				setListData();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});

		mSpinner1 = (Spinner) findViewById(R.id.spinner1);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.status_name,
						android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner1.setAdapter(adapter);
		mSpinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Log.w(TAG, "Spinner1: position=" + position + " id=" + id);
				String s = status_codes[position];
				if (!s.equals(mStatus)) {
					mStatus = s;
					setListData();
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				Log.w(TAG, "Spinner1: unselected");
			}
		});
		Resources res = getResources();
		status_codes = res.getStringArray(R.array.status_code);
		int i = 0;
		for (i = 0; i < status_codes.length; i++) {
			if (status_codes[i].equals(mStatus))
				break;
		}
		if (i > status_codes.length)
			i = 0;
		mStatus = status_codes[i];
		mSpinner1.setSelection(i, true);

		setListData();
	}

	@Override
	protected void onDestroy() {
		try {
			if (db != null)
				db.close();
			db = null;
		} catch (Exception e) {

		}
		super.onDestroy();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "position:" + position + ";id=" + id);
		TextView msg = (TextView) v.findViewById(R.id.msg_text);
		TextView whom = (TextView)v.findViewById(R.id.msg_phone);
		Log.d(TAG, "msg:" + msg.getText() + "; phone:"+ whom.getText());
		
		invokeSMSApp(whom.getText().toString(), msg.getText().toString());
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
	}

	public static boolean isIntegerRegex(String str) {
		return str.matches("^[0-9]+$");
	}

	@SuppressWarnings("deprecation")
	private void setListData() {
		try {
			String lastday = TimeConvert.time2Str((new Date()).getTime() - 1000
					* 60 * 60 * 24 * 1, TimeConvert.DATE_PATTERN_B);
			Log.d(TAG, "lastWeek=" + lastday + ";mstatus=" + mStatus);

			String where = mCbShowTodayOnly.isChecked() ? " where scan_dt> '"
					+ lastday + "' " : "";
			String text = mInputMsgSearch.getText().toString();
			if (text.length() > 0) {
				if (isIntegerRegex(text)) {
					text = "(_id=" + text + " or target like '%" + text + "%')";
				} else {
					text = "_id=" + text;
				}
				where = where.length() == 0 ? " where " + text : where
						+ " and " + text;
			}

			// 状态
			String whereStatus;
			if (mStatus.equals("all")) {
				whereStatus = null;
			} else {
				whereStatus = " status='" + mStatus + "'";
			}
			if (null != whereStatus)
				where = where.length() == 0 ? (" where " + whereStatus) : where
						+ " and " + whereStatus;

			if (db != null)
				db.close();
			DBHelper dbhelper = new DBHelper(this);
			db = dbhelper.getReadableDatabase();
			String sql = "select _id," + "'('|| _id ||')  ' ||"
					+ SendLog.TARGET
					+ " as phone,"
					+ SendLog.TEXT
					+ ", ifnull(((strftime('%s',"
					+ SendLog.DELI_DT
					+ ")-strftime('%s',"
					+ SendLog.SCAN_DT
					+ "))/60) ||'分钟送达','-') as delay"
					+ ",strftime('%m-%d %H:%M',"
					+ SendLog.SCAN_DT
					+ ") as "
					+ SendLog.SCAN_DT
					+ ",'派发'||strftime('%H:%M',"
					+ SendLog.SEND_DT
					+ ") as "
					+ SendLog.SEND_DT
					+ ", case "
					+ SendLog.STATUS
					+ " when 'sent' then '已寄出' when 'fail' then '丢弃' else '' end as "
					+ SendLog.STATUS + ", case " + SendLog.RETRY_TIMES
					+ " when 0 then '' else '尝试'||" + SendLog.RETRY_TIMES + ""
					+ "||'次'  end as " + SendLog.STATUS + "_fail from "
					+ SendLog.TABLE_NAME + where + " order by " + SendLog.ID
					+ " desc," + SendLog.TARGET + "";
			Log.d(TAG, "sql=" + sql);

			Cursor cursor = db.rawQuery(sql, null);
			mTvRows.setText(String.valueOf(cursor.getCount()) + "条");
			startManagingCursor(cursor);
			mListAdapter = new SimpleCursorAdapter(this, R.layout.layout_msgs,
					cursor, new String[] { "phone", SendLog.TEXT, "delay",
							SendLog.STATUS, SendLog.STATUS + "_fail",
							SendLog.SCAN_DT, SendLog.SEND_DT }, new int[] {
							R.id.msg_phone, R.id.msg_text, R.id.msg_scan_dt,
							R.id.exc_msg, R.id.msg_status_fail, R.id.msg_delay,
							R.id.msg_sent_dt });
			getListView().setAdapter(mListAdapter);
		} catch (Exception e) {
			Toast.makeText(this, "显示列表异常：" + e.getMessage(), Toast.LENGTH_LONG)
					.show();
			e.printStackTrace();
		} finally {
			// if(db!=null) db.close();
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_send, menu);
		return true;
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
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			NavUtils.navigateUpTo(this, intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void invokeSMSApp(String target, String msg) {
		Intent smsIntent = new Intent(Intent.ACTION_VIEW);

		smsIntent.putExtra("sms_body", msg);
		smsIntent.putExtra("address", target);
		smsIntent.setType("vnd.android-dir/mms-sms");

		startActivity(smsIntent);
	}
	
    public void onPopupButtonClick(View button) {
        PopupMenu popup = new PopupMenu(this, button);
        popup.getMenuInflater().inflate(R.menu.list_send, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(ListSendActivity.this, "Clicked popup menu item " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        popup.show();
    }
}
