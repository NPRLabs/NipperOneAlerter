/**
 * SetPreferenceActivity.java
 */
package org.nprlabs.nipperone.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.nprlabs.nipperone.fragments.FullAlertFragment;
import org.nprlabs.nipperone.fragments.HelpFragment;
import org.nprlabs.nipperone.fragments.PrefsFragment;

/**
 * This Activity is a re-director to fragment activities related to configuration, help, status, etc.
 * It looks for key "NipperOneFragmentMode" in the Bundle indicating which fragment to launch.
 * We use this PreferenceActivity as the root for the different fragments to insulate the (main) NipperOneAlerter Activity
 * from unintentional user-exits---without this activity, pressing the back arrow on the action bar would exit
 * the main activity.
 *
 * @author RRarey
 *
 */
public class SetPreferenceActivity extends PreferenceActivity {
    
    // These indicate which fragment we want to display
    private final int FragmentMode_DEFAULT = 0;
    private final int FragmentMode_SETTINGS = 1;
    private final int FragmentMode_HELP = 2;
    private final int FragmentMode_SINGLE_ALERT = 3;
    private final int FragmentMode_MOREINFORMATION = 4;

    
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * We create the activity that will hold a fragment and
	 * determine which fragment to show.
	 * The default is to show "settings", because the user can get help
	 * elsewhere but can't get settings from anywhere else.
	 * REFERENCE: http://android-er.blogspot.com/2012/07/example-of-using-preferencefragment.html
	 */
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int NipperOneFragmentMode;
		
		// If the 'extra' key isn't found, use the default mode.
		NipperOneFragmentMode = getIntent().getExtras().getInt("NipperOneFragmentMode", FragmentMode_DEFAULT);
		//Log.d("---| SetPrefActivity |---", Integer.toString(NipperOneFragmentMode));
		// Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        //PrefsFragment mPrefsFragment = new PrefsFragment();
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
        switch (NipperOneFragmentMode) {
            case FragmentMode_HELP:
                HelpFragment mHelpFragment = new HelpFragment();
                mFragmentTransaction.replace(android.R.id.content, mHelpFragment);
                break;
            case FragmentMode_SETTINGS:
                PrefsFragment mPrefsFragment = new PrefsFragment();
                mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
                break;
            case FragmentMode_SINGLE_ALERT:
                FullAlertFragment mFullAlertFrag = new FullAlertFragment();
                Bundle args = new Bundle();
                args.putInt("AlertId", getIntent().getExtras().getInt("AlertId"));
                mFullAlertFrag.setArguments(args);
                mFragmentTransaction.replace(android.R.id.content, mFullAlertFrag);
                break;
            default:
                mPrefsFragment = new PrefsFragment();
                mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
                break;
        }
        mFragmentTransaction.commit();
 	}

}

