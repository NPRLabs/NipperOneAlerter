package org.nprlabs.nipperone.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import org.prss.nprlabs.nipperonealerter.R;/**
 * 
 * @author rrarey
 * REFERENCES:
 * http://www.vogella.com/code/com.vogella.android.spinner/src/com/android/example/spinner/SpinnerActivity.html
 */
public class PrefsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
	}
	
	@Override
	public void onPause(){
	    super.onPause();
	    Log.d("PrefFragment","Pausing");
	}
	
}
