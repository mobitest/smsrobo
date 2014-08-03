package com.mt.sms;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

public class IntEditTextPreference extends EditTextPreference {

	   public IntEditTextPreference(Context context) {
	        super(context);
	    }

	    public IntEditTextPreference(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }

	    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
	        super(context, attrs, defStyle);
	    }

	    @Override
		protected void showDialog(Bundle state) {
			// TODO Auto-generated method stub
			super.showDialog(state);
		    Handler delayedRun = new Handler();
		    delayedRun.post(new Runnable() {
		      @Override
		      public void run() {
		        EditText textBox = getEditText();
		        textBox.setSelection(textBox.getText().length());
		      }
		    });
		}

		@Override
	    protected String getPersistedString(String defaultReturnValue) {
	        return String.valueOf(getPersistedInt(-1));
	    }

	    @Override
	    protected boolean persistString(String value) {
	    	   if (shouldPersist()) {
	    	        return persistInt(Integer.valueOf(value));
	    	    } else {
	    	        if (isPersistent())
	    	            Log.w("myapp", "shouldPersist() returned false. Check if this preference has a key.");
	    	        return false;
	    	    }	    	
	    }
}
