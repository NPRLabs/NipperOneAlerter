/**
 * CitiesActivity.java
 */

package org.nprlabs.nipperone.activities;

import org.prss.nprlabs.nipperonealerter.R;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.AbstractWheelTextAdapter;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * @author RRarey
 * Based on Wheel widget examples by Yuri Kanivets (https://code.google.com/p/android-wheel/)
 *   
 * Implements the State/County selector wheels for the user to choose their location.
 * <pre>R.array.USStates</pre> is formatted in the following way:
 * <pre>&lt;item>Alabama&lt;/item></pre>
 * The County files,<pre>R.array.County(name)</pre> are formatted in the following way:
 * <pre>&lt;item>Athens,OH,039009&lt;/item></pre>
 * The comma-delimited fields are: the county (or city) name, the state abbreviation, and the corresponding 6 digit FIPS code.
 * 
 * Settings are saved to the default preferences file, available from anywhere in the application.
 * The file location on the device is
 *   /data/data/org.prss.nprlabs.nipperonealerter/shared_prefs/org.prss.nprlabs.nipperonealerter_preferences.xml
 * 
 * <br<REFERENCES:<br>
 * Android Wheel Widget:
 *<pre><a href="http://code.google.com/p/android-wheel/">http://code.google.com/p/android-wheel/</a></pre>
 * How to use the wheel:
 * <pre><a href="http://tolkianaa.blogspot.mx/2012/03/do-not-try-to-reinvent-wheel.html">http://tolkianaa.blogspot.mx/2012/03/do-not-try-to-reinvent-wheel.html</a></pre>
 * Launching activity from within a Preferences fragment:
 * <pre><a href="http://www.coderanch.com/t/543114/Android/Mobile/Launching-Activity-Preferences">http://www.coderanch.com/t/543114/Android/Mobile/Launching-Activity-Preferences</a></pre>
 */
public class CitiesActivity extends Activity implements OnClickListener{
    // Scrolling flag
    private boolean scrolling = false;
    TextView txtCurrentSelection;
    Button btnSave;
    Button btnCancel;
    String currentFIPS="000000";
    String currentState;
    String currentCity;
    int currentStateIndex;
    int currentCityIndex;
    SharedPreferences preferences;
    final String key_reqFIPS = "key_reqFIPS";
    final String key_currentStateIndex = "key_currentStateIndex";
    final String key_currentCityIndex = "key_currentCityIndex";
    final String defValue_FIPS = "000000";
    final String key_reqFIPSText = "key_reqFIPSText";
    final String key_reqDirty = "key_reqDirty";
    final int defValue_currentStateIndex = 0;
    final int defValue_currentCityIndex = 0;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cities_layout);
        
        txtCurrentSelection = (TextView) findViewById(R.id.txtCurrentSel);
        txtCurrentSelection.setText("");
        
        // Set up the Save and Cancel button handling
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);	
        
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);
        
        // NOTE:This preference file is the default for this application, available to all activities.
        // It is on the Android device at:
        // /data/data/org.prss.nprlabs.nipperonealerter/shared_prefs/org.prss.nprlabs.nipperonealerter_preferences.xml
        preferences =  PreferenceManager.getDefaultSharedPreferences(this); 
        currentFIPS = preferences.getString(key_reqFIPS, defValue_FIPS);
        currentStateIndex = preferences.getInt(key_currentStateIndex, defValue_currentStateIndex);
        currentCityIndex = preferences.getInt(key_currentCityIndex, defValue_currentCityIndex);
        
        //Log.d("getPreferences",String.format("%s State:%d  City:%d", currentFIPS,currentStateIndex,currentCityIndex));
        
        // Clear key_reqDirty config so we are making a clean start.
        setkey_reqDirty(false);
        
        final WheelView state = (WheelView) findViewById(R.id.state);
        state.setVisibleItems(3);
        state.setViewAdapter(new StateAdapter(this));
        
        final String ourStates[] = getResources().getStringArray(R.array.USStates);

        final String cities[][] = new String[][] {
                getResources().getStringArray(R.array.CountyOther),
        		getResources().getStringArray(R.array.CountyNational),
        		getResources().getStringArray(R.array.CountyAlabama),
				getResources().getStringArray(R.array.CountyFlorida),
	    		getResources().getStringArray(R.array.CountyLouisiana),
				getResources().getStringArray(R.array.CountyMississippi),
				getResources().getStringArray(R.array.CountyTexas),
        		};
        
        final WheelView city = (WheelView) findViewById(R.id.city);
        city.setVisibleItems(3);

        
        state.addChangingListener(new OnWheelChangedListener() {
			/**
			 * This is the initializer listener that sets up the wheel
			 */
        	public void onChanged(WheelView wheel, int oldValue, int newValue) {
			    if (!scrolling) {
			        updateCities(city, cities, newValue, -1);
			        updateCurrentItemView(txtCurrentSelection, city, state, cities, ourStates);
			    }
			}
		});
        
        
        city.addChangingListener(new OnWheelChangedListener() {
            /**
             * This moves the city wheel to the city we need to highlight. 
             */
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (!scrolling) {
                    currentCityIndex = newValue;
                }
            } 
        });
        
        state.addScrollingListener( new OnWheelScrollListener() {
            /**
             * Add the scrolling start|stop listener to the state WheelView
             */
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
                // Blank the current selection text during the scroll.
                txtCurrentSelection.setText("");
            }
            public void onScrollingFinished(WheelView wheel) {
                scrolling = false;
                updateCities(city, cities, state.getCurrentItem(),-1);
		        updateCurrentItemView(txtCurrentSelection, city, state, cities, ourStates);
            }
        });
        
        city.addScrollingListener(new OnWheelScrollListener(){
            /**
             * Add the scrolling start|stop listener to the city WheelView
             */
        	public void onScrollingStarted(WheelView wheel){
        		// Blank the current selection text during the scroll.
        		txtCurrentSelection.setText("");
        	}
        	public void onScrollingFinished(WheelView wheel){
		        updateCurrentItemView(txtCurrentSelection, city, state, cities, ourStates);
        	}
        });
        
        // Hopefully the receiver has given us a FIPS code in the main activity
        String rcvrFIPS = preferences.getString("key_FIPS", null);
        if (rcvrFIPS != null) {
            if (reconcileFIPS(cities,rcvrFIPS)) {
                // Move the wheels to point to the stored FIPS code (if any) BEFORE we
                // add the change listeners.
                // currentCityIndex is changed when we set a state, so preserve its value.
                int tmpCityIndex = currentCityIndex;
                state.setCurrentItem(currentStateIndex);
                city.setCurrentItem(tmpCityIndex);
                updateCities(city, cities, currentStateIndex,tmpCityIndex);
                updateCurrentItemView(txtCurrentSelection, city, state, cities, ourStates);
            } else {
                // The receiver's FIPS code is not in our State/County array, so the user must
                // have programmed it for another area. Don't change it here; the user will change it if desired
                int tmpCityIndex = currentCityIndex;
                state.setCurrentItem(currentStateIndex);
                city.setCurrentItem(tmpCityIndex);
                updateCities(city, cities, currentStateIndex,tmpCityIndex);
                currentFIPS = rcvrFIPS;                
                txtCurrentSelection.setText(String.format("The NipperOne's FIPS code, %s, is not in our state coverage area.\nYou can keep the current FIPS code by pressing Cancel,\nor scroll the state and county list and enter your location.",currentFIPS));
            }
            saveFIPS();
            
        } else {
            txtCurrentSelection.setText("The NipperOne doesn't have a FIPS code selected.\n Please scroll to your location and touch Save."); 
        }
   
 
       
    } // END onCreate()
    
    /**
     * If the stored FIPS does not match that from the receiver, we need to reconcile to find
     * the FIPS city and state in our cities[][] array. This situation occurs if the user has 
     * programmed the receiver's FIPS using another app or PC.
     * If there is no reconciliation it's still OK, because the user may have programmed the
     * receiver for another location and the receiver will still work fine in that area.
     * @param cities The state and county array.
     * @param fipsToFind The FIPS code from the receiver.
     * @return true if fipsToFind was found in the array (currentStateIndex, currentCityIndex, currentFIPS are updated), 
     * false otherwise ('current' variables unchanged).
     */
    private Boolean reconcileFIPS(String cities[][], String fipsToFind) {
        // Get the State FIPS code to find
        String statefipsToFind = fipsToFind.substring(1, 3);
        //Log.d("statefipsToFind",statefipsToFind);
        for (int n = 0; n < cities.length; n++){
            // Get the State FIPS code from the first cities[n] element and
            // compare to the StateFIPS for which we're looking.
            // If no match, don't bother looking through the remaining counties
            // in cities[n].
            String[] tmp = cities[n][0].split(",");
            //Log.d("poop",String.format("%s %s %s",tmp[0],tmp[1],tmp[2]));
            String tmpState = tmp[2].substring(1, 3);
            if (!statefipsToFind.contentEquals(tmpState)) continue;
            
            for (int z = 0; z < cities[n].length; z++) {
            // Search for our FIPS code anywhere in an array element.
            // Important because our County array has comma delimited text. 
                if ( cities[n][z].indexOf(fipsToFind) > -1 ) {
                    //Log.d("FOUND",String.format("Cities [%d][%d]", n,i));
                    // Found a match to fipsToFind at offset i, so
                    // write the indices variables and return true.
                    currentStateIndex = n;
                    currentCityIndex = z;
                    currentFIPS = fipsToFind;
                    return true;
                }
            }
        }
        currentStateIndex = 0;
        currentCityIndex = 0;
        currentFIPS = fipsToFind;
        return false;
    }


    /**
     * Updates the city wheel when we've changed U.S. States
     * @param city The WheelView that displays 'cities'.
     * @param cities The big string array that holds all county FIPS codes.
     * @param currentStateIndex The current index of WheelView "state"
     */
    private void updateCities(WheelView city, String cities[][], int currentStateIndex, int currentCityIndex) {
        ArrayWheelAdapter<String> adapter =
            new ArrayWheelAdapter<String>(this, cities[currentStateIndex]);
        adapter.setTextSize(26);
        city.setViewAdapter(adapter);
        if (currentCityIndex == -1) {
            city.setCurrentItem(cities[currentStateIndex].length / 2);
        } else {
            city.setCurrentItem(currentCityIndex);
        }
    }
  
    
    /**
     * Simple function to format and display our current county,state,fips selection to the screen.
     * We use parameters to ensure everything we need to make the display is in scope.
     * @param tv The TextView that will display our current selected information.
     * @param city The WheelView that displays 'cities'.
     * @param state The WheelView that displays 'states'.
     * @param cities The big string array that holds all county FIPS codes.
     * @param ourStates The string small array that holds our state name. 
     */
    private void updateCurrentItemView(TextView tv, WheelView city, WheelView state, String cities[][],String ourStates[]){
        currentStateIndex = state.getCurrentItem();
        currentCityIndex = city.getCurrentItem();
        // The array element in cities[][] contains a comma-delimited string of "countyName,stateAbbreviation,FIPScode"
		String tmp[] = cities[currentStateIndex][currentCityIndex].split(",");
		currentFIPS=tmp[2];
		tv.setText(String.format("%s, %s   FIPS CODE: %s", 
				tmp[0],
				ourStates[currentStateIndex],
				currentFIPS));
		
	}
    
    /**
     * Override of onClick() to capture clicks of our Save and Cancel buttons.  
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnCancel) {
            //Log.d("btnCancel","CANCELLED PRESSED");
            setkey_reqDirty(false);
            finish();
            
        } else if (id == R.id.btnSave) {
            //Log.d("---btnSave---",String.format("SAVE PRESSED, FIPS: %s State: %d City: %d",currentFIPS,currentStateIndex, currentCityIndex));
            saveFIPS();
            finish();
        } 
    }
    
    /**
     * Write the FIPS code, State and County index to the preferences file.
     * <b>NOTE:</b>This preference file is the default for this application, available to all activities.
     * It is on the Android device at:
     * <pre>/data/data/org.prss.nprlabs.nipperonealerter/shared_prefs/org.prss.nprlabs.nipperonealerter_preferences.xml</pre>
     */
    private void saveFIPS() {
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putString(key_reqFIPS, currentFIPS);
        prefEditor.putInt(key_currentStateIndex, currentStateIndex);
        prefEditor.putInt(key_currentCityIndex, currentCityIndex);
        prefEditor.putString(key_reqFIPSText,txtCurrentSelection.getText().toString());
        prefEditor.apply();
        setkey_reqDirty(true);
    }
    
    /**
     * Writes a boolean value to the key_reqDirty configuration parameter.
     * @param bAction Writes a value to key_reqDirty ( either true or false ) 
     */
    private void setkey_reqDirty (boolean bAction){
        if ( preferences != null) {
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putBoolean(key_reqDirty,bAction);
        prefEditor.apply();
        }
     }

    // -------------------------------------------------------------------------------
    
    /**
     *  Adapter class for WheelView states
     *  
     *
    */
    private class StateAdapter extends AbstractWheelTextAdapter {
        // State names
        private String states[] = getResources().getStringArray(R.array.USStates);
        // State flags
        private int flags[] =
            new int[] {R.drawable.usa,
                       R.drawable.usa,
        		       R.drawable.alabama, 
        		       R.drawable.florida, 
        		       R.drawable.louisiana, 
        		       R.drawable.mississippi,
        		       R.drawable.texas};
        

        /**
         * StateAdapter Constructor
         * @param context 
         */
        protected StateAdapter(Context context) {
            super(context, R.layout.state_layout, NO_RESOURCE);
            
            setItemTextResource(R.id.state_name);
        }

        /**
         * Override the getItem method specifically to draw the state flag.
         */
        @Override
        public View getItem(int index, View cachedView, ViewGroup parent) {
            View view = super.getItem(index, cachedView, parent);
            ImageView img = (ImageView) view.findViewById(R.id.flag);
            img.setImageResource(flags[index]);
            return view;
        }
        
        /**
         * 
         */
        @Override
        public int getItemsCount() {
            return states.length;
        }
        
        /**
         * 
         */
        @Override
        protected CharSequence getItemText(int index) {
            return states[index];
        }
    } // END class StateAdapter
	
} // END Class CitiesActivity
