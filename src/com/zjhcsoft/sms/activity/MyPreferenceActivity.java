/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zjhcsoft.sms.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.zjhcsoft.smsrobot1.R;

/**
 * This activity is an example of a simple settings screen that has default
 * values.
 * <p>
 * In order for the default values to be populated into the
 * {@link SharedPreferences} (from the preferences XML file), the client must
 * call
 * {@link PreferenceManager#setDefaultValues(android.content.Context, int, boolean)}.
 * <p>
 * This should be called early, typically when the application is first created.
 * An easy way to do this is to have a common function for retrieving the
 * SharedPreferences that takes care of calling it.
 */
public class MyPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    // This is the global (to the .apk) name under which we store these
    // preferences.  We want this to be unique from other preferences so that
    // we do not have unexpected name conflicts, and the framework can correctly
    // determine whether these preferences' defaults have already been written.
    static final String PREFS_NAME = "defaults";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPrefs(this);
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        addPreferencesFromResource(R.xml.preference);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    static SharedPreferences getPrefs(Context context) {
        PreferenceManager.setDefaultValues(context, PREFS_NAME, MODE_PRIVATE,
                R.xml.preference, false);
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory) p;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                initSummary(pCat.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    /**
     * 定制参数的说明，合并显示当前设置的值和summary属性。
     * @param p
     */
    private void updatePrefSummary(Preference p) {
    	//"1；说明：";
    	final String split = "  ";
    	CharSequence sum0 = p.getSummary();
    	String sum1;
    	if(sum0==null){
    		sum1 = "";
    	}else{
    		sum1 = sum0.toString();
    		int pos = sum1.indexOf(split);
    		if(pos>0) sum1 =sum1.substring(pos); 
    		else sum1 = split + sum1;
    	}
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            sum1 = listPref.getEntry() + sum1;
            p.setSummary( sum1);
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            sum1 = editTextPref.getText() + sum1;
            p.setSummary(sum1);
        }
    }    
}
