/*
 * NipperOneAlerter.java
 */

package org.nprlabs.nipperone.main;

// For USB access and control
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.nprlabs.nipperone.fragments.CustomDialog;
import org.nprlabs.nipperone.framework.DatabaseHandler;
import org.prss.nprlabs.nipperonealerter.R;
import org.nprlabs.nipperone.activities.SetPreferenceActivity;
import org.nprlabs.nipperone.framework.NipperConstants;
import org.nprlabs.nipperonealerter.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
// For clock display
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

/* TODO on NipperOne Alerter app

. Settings
    HALF DONE.Implement reading the receiver settings and writing the receiver settings.

. Features
    Implement a message recall
    Implement a message management
*/

/**
 * NipperOneAlerter
 * main class
 *
 * Purpose: Accessible user interface to Catena Radio Design "NipperOne" FM RBDS Receiver
 *
 * Author: Rich Rarey, NPR Labs and Katherine Brudos
 * Date: August 2013, Summer 2015
 * REFERENCES:
 * https://code.google.com/p/micropendous/source/browse/trunk/Micropendous/Firmware/AndroidAccessoryHost/AndroidDeviceSoftware/USBTest/src/ch/serverbox/android/usbtest/UsbTest.java?r=720
 * https://github.com/mik3y/usb-serial-for-android
 *
 * REVISIONS:
 * 20131029 - Firmware API 1.10 released and this app modified to that API version.
 * 20131102 - Moved all receiver data management to API.java, leaving this class with clock, USB management and error handling.
 * 20131112 - Firmware API 1.11 released with fixes some minor bugs.
 * 20140112 - Firmware API 1.12 released with simplified LED indicator behavior and enhanced Snooze. 
 * 20140706 - Major app updates to v0.9.21 including multiple ways to gather message text (formwatted with/without linefeeds),
 *            error handling, and more. Also created a published, key release version. pw:  npr@labs.1
 *            REFERENCE: http://developer.android.com/tools/publishing/preparing.html
 *
 *
 * The following are the key names for this application's configuration preferences, stored on the Android device in the file
 *  /data/data/org.prss.nprlabs.nipperonealerter/shared_prefs/org.prss.nprlabs.nipperonealerter_preferences.xml
 *  ---The following hold the NipperOne receiver's parameters:
 * key_VersionMinor
 * key_BeaconWaitTime
 * key_USBtimeOut
 * key_BeaconTimeOut
 * key_ConfigBlock
 * key_Volume
 * key_ValidReceiverConfig
 * key_TuningGrid
 * key_AlertTimeOut
 * key_ScanInterval_05Hz
 * key_FIPSPortionCode
 * key_SnoozeTimeOut
 * key_FIPS
 * key_FIPSStateCode
 * key_RFtimeOut
 * key_ScanInterval_2Hz
 * key_AllowRemote
 * key_AlarmTimeOut
 * key_VersionMajor
 * key_FIPSCountyCode
 *  --- The following hold values useful to CitiesActivity:
 * key_reqFIPSText
 * key_reqFIPS
 * key_currentCityIndex
 * key_currentStateIndex
 * key_reqDirty
 *  --- The following determines which method to use for gathering alert text
 * key_messagelinefeed
 */
public class NipperOneAndroid extends Activity {

    /**
     * Copyright and attribution message for the "About..." dialog.
     * This is string-formatted text with tokens for app version, NipperOne firmware version, and current FIPS Code.  
     */
    static final String msgAbout = "NipperOneAlerter app version: %s\nNipperOne Receiver Firmware Version: %s\n\nApp Copyright © 2013-2015 NPR Labs (alerting@npr.org)\nAuthor: Rich Rarey and Katherine Brudos\n\nReceiver firmware Copyright © 2013-2015 Catena Radio Design\nAuthor: Joop Beunders\n\nReceiver Location is set to:\n%s";

    String TAG = "NipperOneAlerter";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     * SystemUiHider.FLAG_PARTIALSCREEN =  Hide/Show Title bar but display a lights-out version of the action bar.
     *                                     Activated by touching the action bar only.
     * SystemUiHider.FLAG_HIDE_NAVIGATION = Hide and Show Title bar and Action bar
     *                                     Activated by touching anywhere in the activity.
     * SystemUiHider.FLAG_FULLSCREEN = For this app, same as FLAG_HIDE_NAVIGATION.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;


    private TextView mClockText;


    /** Provides methods to manage termination and methods that can produce 
     * a Future for tracking progress of one or more asynchronous tasks. 
     */
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private Calendar mCalendar;
    // These define the time display string for the 12 and 24 hour clock.
    private final static String m12 = "h:mm aa";
    private final static String m24 = "k:mm";
    private String mFormat;

    private UsbSerialDriver sDriver = null;
    private SerialInputOutputManager mSerialIoManager;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent = null;
    private IntentFilter tickReceiverIntentFilter = null;

    // These indicate which fragment we want to display
    private final int FragmentMode_SETTINGS = 1;
    private final int FragmentMode_HELP = 2;


    static String versionNipperOneAlerter = "Unknown";

    static String versionNipperOneReceiver = "Unknown";

    static RelativeLayout mStationLayout;
    static ImageView mStationLogo;
    static TextView mStationCall;
    static TextView mStationFreq;
    static TextView mBeaconStatus;


    static TextView mSignalLevel;
    static TextView mMsgOriginator;
    static TextView mEvent;
    static TextView mDuration;

    static Button msgArchive;

    static Drawable drawMessageAlarm = null; //nipperRes.getDrawable(R.drawable.bordermessagealarm);
    static Drawable drawMessageNormal = null; //nipperRes.getDrawable(R.drawable.bordermessagenormal);
    static Drawable drawStationFreqSlow = null;
    static Drawable drawStationFreqQuick = null;
    static Drawable drawStationFreqFoundStation = null;



    /**
     * Define the Listener and callback functions that interface with the Serial I/O manager.
     * The serial manager calls onNewData when it detects incoming data in its buffer.
     */
    private SerialInputOutputManager.Listener mListener;


    /**
     *  Define the broadcast receiver that handles incoming broadcast messages.
     *  Our clock tick and USB connections are monitored here.
     */
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( Intent.ACTION_TIME_TICK.equals(action) ) {

                mCalendar.setTimeInMillis(System.currentTimeMillis());
                mClockText.setText(DateFormat.format(mFormat, mCalendar));

            }  else if ( UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ) {
                //mMessage.append("\nThe Receiver is plugged in!\n");


            } else if ( UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) ) {
                // Either the receiver has been rebooted or physically disconnected from the tablet.
                // Bring this to the user's attention.
                receiverNotConnected();

            } else if ( "org.prss.nprlabs.nipperonealerter.USBPERMISSION".equals(action) ) {
                for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
                    if (device.getVendorId() == 0x1320) {
                        Log.d(TAG, "Found Catena USB device: " + device);
                        //mMessage.append("Found the NipperOne Radio receiver\n");

                        if ( mUsbManager.hasPermission(device) ) {
                            final List<UsbSerialDriver> drivers =
                                    UsbSerialProber.probeSingleDevice(mUsbManager, device);
                            if (drivers.isEmpty()) {
                                Log.d(TAG, "  - No UsbSerialDriver available.");
                                //mMessage.append("\nThe Nipper One receiver does NOT have its driver assigned.\nPlease Press the Receiver's Reset button.\n");
                            } else {
                                for (UsbSerialDriver driver : drivers) {
                                    Log.d(TAG, "  + " + driver);
                                    //mMessage.append("tickReceiver:The NipperOne receiver will be using the following driver:\n" + driver + "\n");
                                }
                            }
                        } else {
                            //mMessage.append("\nThis is distressing:\nWe still don't have permission to access the Catena Receiver.\n");
                        }
                    }
                }
            }
        }
    };

//    /**
//     * Listens for a click on the EAS Short Codes TextViews, and displays a Toast object with a
//     * description of the clicked EAS Short Code.
//     */
//    private final OnClickListener EASCodesClickListener = new OnClickListener() {
//        @Override
//        public void onClick(View v){
//            String msg = "";
//            int m = (Integer) v.getTag();
//            TextView tv = (TextView) v; 
//            if (tv.getText().length() > 0) msg = "\"" + tv.getText() + "\"" ;
//            msg = msg + codeHelpText.get(m);
//            //Toast.makeText(Receiver.parentContext, msg, Toast.LENGTH_SHORT).show();
//        }       
//    };

    /**
     * List containing help strings for each of the EAS Short Code TextViews. 
     * API.mEASCodesUrgency.setTag(1);
     * API.mEASCodesCertainty.setTag(2);
     * API.mEASCodesResponse.setTag(3);
     * API.mEASCodesMessageType.setTag(4);
     * API.mEASCodesEvent.setTag(5);
     * API.mEASCodesSeverity.setTag(6);
     * API.mEASCodesCategory.setTag(7);
     * API.mEASCodesDuration.setTag(8)
     */
    private final List<String> codeHelpText = Arrays.asList(
            " describes the 'urgency' of this message.",
            " describes the 'certainty' of the message.",
            " is the recommended action you should take for this event.",
            " describes this Alert Message type.",
            " is a short description of this event.",
            " describes the severity of this event.",
            " describes the category of this event.",
            " is how long this alert is in effect.");

    private Receiver myReceiver = new Receiver();


    // Our local resources, such as stored colors
    private Resources nipperRes;

    /**
     * Indicates if we're showing the alarm screen
     */
    private static Boolean HaveSetAlarmScreen = false;

    /**
     * Receiver Alarm Status Flag (set and cleared in updateReceiverStatus())
     * Set = ALARM, Cleared = Normal
     */
    private static boolean isAlarm = false;

    /**
     * The message string to display when the receiver is disconnected or is rebooting.
     */
    private final String messageReceiverDisconnected = "\n------------------------------------------------------\nWHOA! The receiver has been unplugged or is rebooting.\n------------------------------------------------------\n";

    private boolean expectingMoreAlertText = false;
    /**
     * This is the context variable set in onCreate()
     * It is used by the Toast widget.
     */
    private Context parentContext;

    private static MessageImpl myMsg = new MessageImpl();

    private MessageImpl[] messageArray = new MessageImpl[10];

    private Button button;
    private Button button2;


    DatabaseHandler dbHandler;
    int messageCount = 0;

    boolean messageComplete = false;

    MessageImpl newMsg = new MessageImpl();

    /*
     * (non-Javadoc)
     * First method upon creation.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This will manually hide the activity bar and nav bar, but only until the user touches the screen.
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN );

//        newMsg.setEvent("Fire Warning");
//        newMsg.setMsgUrgency("Something");
//        newMsg.setMsgCertainty("blah");
//        newMsg.setMsgSeverity("very severe");
//        newMsg.setMsgOriginator("NPR LABS");
//        newMsg.setMsgString("This is a trial message. Created to be added to the dataBase in onCreate()");

        setContentView(R.layout.main);

        dbHandler = DatabaseHandler.getInstance(this);

//        dbHandler.addMessage(newMsg);

        //message count is set like this only once. Here. This way if you are restarting
        // the app you get the number of messages currently in the database.
        //Also if there is a current database the most recent message is retrieved.
        messageCount = dbHandler.getMessageCount();
        Log.d("Message Count onCreate", Integer.toString(messageCount));

        if(dbHandler.getMessageCount() == 0){}
        else{myMsg= dbHandler.getMessage(dbHandler.getMessageCount());}


        // We get the local resources instance so we can get our stored colors
        // and other attributes. REQUIRED by API.initializeAPI();
        nipperRes = getResources();

        // Initialize the API class.
        myReceiver.initializeReceiver();

        // Share the activity and context for Toast and the preferences activity
        parentContext = getApplicationContext();
        //parentActivity = this;


        mStationFreq = (TextView)findViewById(R.id.txt_station_freq);
        msgArchive = (Button)findViewById(R.id.btn_msg_archive);



        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionNipperOneAlerter = pInfo.versionName;
        } catch (NameNotFoundException e) {
            Log.e(TAG,"Could not determine the NipperOneAlerter version.");
            versionNipperOneAlerter = "Unknown";
        }


        /// -----------| Begin the set up for the clock |----------------------
        // Match the user's preference for 12 or 24 hour clock display.
        mCalendar = Calendar.getInstance();
        if (android.text.format.DateFormat.is24HourFormat(getBaseContext()) ){
            mFormat = m24;}
        else {
            mFormat = m12;
        }

        mClockText = (TextView) findViewById(R.id.txtClock);

        // Display the clock time immediately
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        mClockText.setText(DateFormat.format(mFormat, mCalendar));

        // Set the IntentFilter to listen for the top of the minute actions, 
        // and listen for problems with the USB connection.
        tickReceiverIntentFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        tickReceiverIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        tickReceiverIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        tickReceiverIntentFilter.addAction("org.prss.nprlabs.nipperonealerter.USBPERMISSION");


        // Register the broadcast receiver (defined above) to receive 
        // TIME_TICK and other broadcasts of interest.
        // This will happen in onResume()
        //registerReceiver(tickReceiver, tickReceiverIntentFilter);
        /// -----------| END Clock setup  |------------------------------------

        /// -----------| System Nav Bar + Title Bar hiding and revealing |-----
        // This section deals with hiding and revealing the System Navigation (bottom of screen) and System Title bar (top of screen).
        // We use it here to automatically return to our full-screen state after the user has touched the Navigation bar. We also
        // use it to hide our app's clock when the Nav bar is visible, because the system displays a clock in the Nav Bar.
        final View contentView =  findViewById(R.id.fraLayout);
        final View controlsView = mClockText;



        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
//        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
//        mSystemUiHider.setup();

//        mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
//            // Cached values.
//            int mContentHeight;
//            int mShortAnimTime;
//
//            @Override
//            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//            public void onVisibilityChange(boolean visible) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//                    // If the ViewPropertyAnimator API is available
//                    // (Honeycomb MR2 and later), use it to animate the
//                    // in-layout UI controls at the bottom of the
//                    // screen.
//                    if (mContentHeight == 0) {
//                        mContentHeight = contentView.getHeight();
//                    }
//                    if (mShortAnimTime == 0) {
//                        mShortAnimTime = getResources().getInteger(
//                                android.R.integer.config_shortAnimTime);
//                    }
//                    // Reference: http://stackoverflow.com/questions/16282294/show-title-bar-from-code
//                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//                    //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//
//
//                    controlsView.animate()
//                            .translationY(visible ? mContentHeight : 0)
//                            .setDuration(mShortAnimTime);
//                } else {
//                    // If the ViewPropertyAnimator APIs aren't
//                    // available, simply show or hide the in-layout UI
//                    // controls.
//                    controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
//                }
//
//                if (visible && AUTO_HIDE) {
//                    // Schedule a hide().
//                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
//                }
//            }
//        });

        // Set up the user interaction to manually show or hide the system UI.
//        contentView.setOnClickListener(new View.OnClickListener() {
//            //API.mStationLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (TOGGLE_ON_CLICK) {
//                    mSystemUiHider.toggle();
//                } else {
//                    mSystemUiHider.show();
//                }
//            }
//        });

//        /**
//         * Touch listener to use for in-layout UI controls to delay hiding the
//         * system UI.<br>This is to prevent the jarring behavior of controls going away
//         * while interacting with activity UI.
//         */
//        mMessage.setOnTouchListener(new TextView.OnTouchListener(){
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//               
//                if ( view != mMessage ) return false;
//                
//                //delayedEnableScroll(5000);
//                delayedHide(AUTO_HIDE_DELAY_MILLIS);
//                return false;
//            }   
//        });

        /// -----------| END System Nav Bar + Title Bar hiding and revealing |-

        // Set up the USB Manager and look for the NipperOne receiver. We create it here, once,
        // so onResume can use it.
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //probeUSBforReceiver();

        // DEBUG TEXT TO PLAY WITH SCROLLING:
        //API.appendTextAndScroll("Home is a customizable space that houses app shortcuts, folders and widgets. Navigate between different home screen panels by swiping left and right.The Favorites Tray at the bottom always keeps your most important shortcuts and folders in view regardless of which panel is currently showing. Access the entire collection of apps and widgets by touching the All Apps button at the center of the Favorites Tray.Home is a customizable space that houses app shortcuts, folders and widgets. Navigate between different home screen panels by swiping left and right.The Favorites Tray at the bottom always keeps your most important shortcuts and folders in view regardless of which panel is currently showing. Access the entire collection of apps and widgets by touching the All Apps button at the center of the Favorites Tray.Home is a customizable space that houses app shortcuts, folders and widgets. Navigate between different home screen panels by swiping left and right.The Favorites Tray at the bottom always keeps your most important shortcuts and folders in view regardless of which panel is currently showing. Access the entire collection of apps and widgets by touching the All Apps button at the center of the Favorites Tray.");

        // Load any app preferences (not receiver config, but only app stuff).
        loadPrefs();

        mListener = new SerialInputOutputManager.Listener() {
            @Override
            public void onRunError(Exception e) {
                Log.d(TAG, "SerialInputOutputManagerListener Runner stopped.");
            }
            @Override
            public void onNewData(final byte[] data) {
                NipperOneAndroid.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processReceivedData(data);  //NipperOneAlerter.this.updateReceivedData(data);
                    }
                });
            }
        };


    } // END onCreate()


    /// -----------| Variables, functions for Nav + title hiding |------------

    /* onPostCreate
     * Called when activity start-up is complete. Not typically implemented
     * in applications. 
     * (non-Javadoc)
     * @see android.app.Activity#onPostCreate(android.os.Bundle)
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(500);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     * Clean up running processes to prepare for the activity to pause
     * We unregister the tickReceiver, stop the serialIOManager, 
     * and null sDriver.
     * Any incoming alert messages will not be seen by the app, although
     * the receiver will continue to monitor the station.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(tickReceiver != null) unregisterReceiver(tickReceiver);
        Log.d(TAG,"---OnPause---");
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            } catch (IOException e) {
                Log.e (TAG,"Error trying to close sDriver in onPause().");
            }
            sDriver = null;
        }
    }

    /// -----------| END API and USB support functions |-----------------------

    /// -----------| Action Bar Support functions |----------------------------

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        //res/menu/main_activity_actions.xml
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
        // TODO Or should we return "true" ?
    }

    /*
    * (non-Javadoc)
    * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
    */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        myReceiver.requestReceiverConfiguration(sDriver);
        myReceiver.requestReceiverVersion(sDriver);
        // Handles presses on the action bar items
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
            //API.mMessage.append("Pressed Settings\n");
            return true;
        } else if (id == R.id.action_help) {

            openHelp();
            return true;
        } else if (id == R.id.action_about) {
            openAbout();
            return true;
        } else if (id == R.id.action_clearscreen) {
            clearText();
            return true;
        } else if (id == R.id.action_restore_receiver_defaults){
            // @TODO Finish the Alert dialog.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Reset the NipperOne receiver to its factory settings");
            builder.setMessage("You can reset the NipperOne to its original factory settings");
            // Add the buttons
            builder.setPositiveButton("Reset Receiver to Default Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    myReceiver.writeReceiverConfigurationDefault(sDriver);
                }
            });
            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            // Set other dialog properties
            //...

            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else  return super.onOptionsItemSelected(item);

    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     * We're here either after onCreate() or coming back to the main activity from showing Help, 
     * User Settings, etc, and normally this wouldn't be a big deal, but we have a USB connection
     * that has to be managed, complicating the restart.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"---onResume---");
        Log.d(TAG, "Resumed, sDriver=" + sDriver);

        // If we're returning from another activity and have left during an Alert, clear the flag
        // so the screen will eventually be put back into alarm.
        HaveSetAlarmScreen = false;

        // When we left this activity to show help|settings|etc., we set 
        // sDriver to null, so we need to probe for the device and driver again.
        if (sDriver == null) {
            //mMessage.append("Now looking for the NipperOne Receiver...\n");
            probeUSBforReceiver();
        }

        // We've probed and hopefully found the receiver.
        // If so, we'll set up the USB connection, register the tickReceiver, 
        // and start the IO Manager.
        if (sDriver != null) {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
                // Stop and restart the IOManager
                onDeviceStateChange();

                // @TODO Put the configuration check here to see if our FIPS or any other receiver configuration has changed.
                if (compareFIPS()) updateReceiverFIPS();
                loadPrefs();

            } catch (IOException e) {
                Log.e(TAG, "Error setting up NipperOne: " + e.getMessage(), e);
                //mMessage.setText("Error opening NipperOne: " + e.getMessage() + "\n");
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    Log.e("onResume",e2.getMessage());   // Ignore.
                }
                sDriver = null;
                return;
            }
        }
        // Register the broadcast receiver to receive TIME_TICK and other broadcasts of interest.
        // We register regardless if the NipperOne is found or not.
        registerReceiver(tickReceiver, tickReceiverIntentFilter);
    }

    /// -----------| USB Connection Probing |---------------------------

    /**
     * This function uses the usbserial library to probe for the 
     * Catena device's VendorID (0x1320) and check that we have a driver for it.<br>
     * If found, ensures we have user permission to access the device and 
     * ask the user if we don't have permission.
     * If we already have permission, assign the driver to sDriver.
     *
     */
    private void probeUSBforReceiver() {
        for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (device.getVendorId() == 0x1320) {
                Log.d(TAG, "Found Catena (NipperOne) USB device: " + device);
                //mMessage.append("Found the NipperOne receiver.\n");
                // Make sure we have permission to access the Receiver, if not, ask the user 
                // if it's OK to access the receiver.
                if ( mUsbManager.hasPermission(device) ) {
                    final List<UsbSerialDriver> drivers =
                            UsbSerialProber.probeSingleDevice(mUsbManager, device);
                    if (drivers.isEmpty()) {
                        Log.d(TAG, "  - No UsbSerialDriver available.");
                        //mMessage.append("\nThe NipperOne receiver does NOT have its software driver assigned. \nPlease Press the Receiver's Reset button.\n");
                    } else {
                        // This simply confirms to the user that we're connected to the receiver;
                        // there is nothing to call here because mListener will begin to get 
                        // the incoming receiver data, once we've set sDriver to the proper CDC driver.
                        for (UsbSerialDriver driver : drivers) {
                            // Most important line in the function: Assigns the driver to sDriver.
                            sDriver = driver;
                            Log.d(TAG, "  + " + driver);
                            //mMessage.append("Ready...\n");
                        }
                    }
                } else {
                    // Let's get permission from the user.
                    mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("org.prss.nprlabs.nipperonealerter.USBPERMISSION"),0);
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }
            }
        }
    }
    /// -----------| END USB Connection Probing |---------------------------


//    /**
//     * Schedules a call to enable the scrolling on API.mMessage,
//     * [delayMillis] milliseconds, canceling any previously scheduled calls
//     * @param delayMillis Delay in milliseconds.
//     */
//    private void delayedEnableScroll(int delayMillis){
//        mEnableScrollHandler.removeCallbacks(mEnableScrollRunnable);
//        mEnableScrollHandler.postDelayed(mEnableScrollRunnable, delayMillis);
//    
//    }
//
//
//    Handler mHideHandler = new Handler();
//    Runnable mHideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            mSystemUiHider.hide();
//        }
//    };
//
//
//    /**
//     * Schedules a call to hide() in [delayMillis] milliseconds, canceling any
//     * previously scheduled calls.
//     * @param delayMillis Delay in milliseconds.
//     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    /// -----------| END Variables, functions for Nav + title hiding |---------


    /// -----------| API and USB support functions |---------------------------

    /**
     * Stops the serialIOManager and NULLs its member
     */
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    /**
     * If a USB Driver is assigned, creates a new serialIOManager and
     * starts its async process.
     */
    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
            // Not strictly necessary but nice to have the firmware version available.
        }
    }

    /**
     * Stop and re-start the serialIOManager
     */
    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    /**
     * Handler for Action Bar item "About"
     * Displays the app version, Receiver firmware version, copyright information, and receiver FIPS code in a message box.
     */
    private void openAbout() {
        myReceiver.requestReceiverVersion(sDriver);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyfips = prefs.getString("key_FIPS","");
        String keyreqfips = prefs.getString("key_reqFIPS","");

        Log.d("KEYFIPS: ", keyfips);
        Log.d("KEYREQ_FIPS: ", keyreqfips);

        String msgFIPS = prefs.getString("key_reqFIPSText", null);
        if (msgFIPS == null)  {
            msgFIPS = prefs.getString("key_FIPS","Location cannot be determined! Please go to settings and set the receiver to your current location.");
        } else {
            // If the user has restored the factory defaults, then the two FIPS sources will disagree.
            // We want the user to have some confidence in the About information.
            if (!keyfips.equalsIgnoreCase(keyreqfips)){
//                Log.d("KEYFIPS: ", keyfips);
//                Log.d("KEYREQ_FIPS: ", keyreqfips);
                msgFIPS = keyfips + " (A description of the location isn't available at the moment.)";
            }
        }
        String msg = String.format(msgAbout,versionNipperOneAlerter,versionNipperOneReceiver,msgFIPS);


        // Instead of making a Toast, display an "OK"-only dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About this app and the NipperOne Receiver...");
        // Set our icon for the title.
        builder.setIcon(R.drawable.nprlabslogo);
        // Set our copyright and info message.
        builder.setMessage(msg);
        // Add the OK button. Dialog box will disappear when OK is touched.
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Handler for Action Bar item "Help"
     */
    private void openHelp() {
        Intent intent = new Intent();
        // Pass the parameter to indicate we want to show the settings fragment.
        intent.putExtra("NipperOneFragmentMode",FragmentMode_HELP);
        intent.setClass(NipperOneAndroid.this, SetPreferenceActivity.class);
        startActivity(intent);
    }


    /**
     * Handler for Action Bar item "Settings"
     */
    private void openSettings() {
        //    API.mMessage.append("Pressed Settings\n");
        // Display the fragment as the main content.
        Intent intent = new Intent();

        // Pass the parameter to indicate we want to show the settings fragment.
        intent.putExtra("NipperOneFragmentMode",FragmentMode_SETTINGS);
        intent.setClass(NipperOneAndroid.this, SetPreferenceActivity.class);
        startActivity(intent);
    }


    public void viewMostRecent(View view){

        messageCount = dbHandler.getMessageCount();

        String tmpString = "";

        if(messageCount == 0){
            tmpString = "There are currently no messages in the database to display.";
        }else {

            CustomDialog d = new CustomDialog(this);
            d.show();
        }
//            MessageImpl msg = dbHandler.getMessage(messageCount);
//            tmpString = String.format("ID: %d\nEvent: %s\nCertainty: %s\nSeverity: %s\nUrgency: %s\nCategory: %s\nDuration: %s\nOrigin time: %s\nOriginator: %s\nType: %s\nMessage: %s\n", msg.getId(), msg.getEventString(), msg.getMsgCertainty(), msg.getMsgSeverity(), msg.getMsgUrgency(), msg.getMsgCategory(), msg.getMsgDuration(), msg.getMsgOrgTime(), msg.getMsgOriginator(), msg.getMsgType(), msg.getMsgString());
//        }
//
//        // Instead of making a Toast, display an "OK"-only dialog.
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("More information about this alert");
//        // Set our icon for the title.
//        builder.setIcon(R.drawable.nprlabslogo);
//        // Set our message.
//        builder.setMessage(tmpString);
//        // Add the OK button. Dialog box will disappear when OK is touched.
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // User clicked OK button
//            }
//        });
//        // Create the AlertDialog
//        AlertDialog dialog = builder.create();
//        dialog.show();
    }


    public void viewMessageArchive(View view){
//        Toast toast = Toast.makeText(getApplicationContext(), "Clicked to view the current Message!", Toast.LENGTH_SHORT );
//        toast.show();
        messageCount = dbHandler.getMessageCount();
        String tmpString = "";
        List<MessageImpl> messageList = dbHandler.getAllMessages();

        if(messageCount == 0){tmpString = "There are currently no messages in the database to display.";}
        else {

            int i = 0;
            for (MessageImpl msg : messageList) {
                tmpString += String.format("ID: %d\n EVENT: %s ACTION: %s\n\n", msg.getId(), msg.getEventString(), msg.getMsgAction());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert Database");
        // Set our icon for the title.
        builder.setIcon(R.drawable.nprlabslogo);
        // Set our copyright and info message.
        builder.setMessage(tmpString);
        // Add the OK button. Dialog box will disappear when OK is touched.
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void loadPrefs(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // This determines which method of gathering alert messages; with linefeeds or without 
        Receiver.disableupdateReceiverAlertMessage = prefs.getBoolean("key_messagelinefeed", false);
    }


    /**
     * Check if key_reqDirty setting is true, validate FIPS and reqFIPS and check if (FIPS != reqFIPS). 
     * @return true, if the user has changed the FIPS code and it is different that the FIPS the receiver has stored in it.
     */
    public boolean compareFIPS() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String reqFIPS;
        String FIPS;
        if (prefs.getBoolean("key_reqDirty", false)) {
            // If key_reqDirty is true, the user has changed the FIPS code in the configuration activity.
            // Clear key_reqDirty.
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putBoolean("key_reqDirty",false);
            prefEditor.apply();
            // Validate the existence of the two FIPS keys.
            reqFIPS = prefs.getString("key_reqFIPS", null);
            if (reqFIPS != null) {
                FIPS = prefs.getString("key_FIPS", null);
                if (FIPS != null) {
                    if (!FIPS.contentEquals(reqFIPS)) {
                        return true;
                    } else {
                        // If no change, then still show the balloon for the user's confidence.
                        String msg = "The receiver location has not been changed. Just so you know.\n";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Write the FIPS code from prefs "key_reqFIPS" into the receiver, and show a Toast of the change.
     */
    public void updateReceiverFIPS() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String reqFIPS = prefs.getString("key_reqFIPS", null);
        if (reqFIPS != null) {
            myReceiver.writeReceiverConfigurationFIPS(sDriver, reqFIPS);

            // If we have descriptive text about the location, show it.
            // Else just show the user the new FIPS code.
            String msg = prefs.getString("key_reqFIPSText", null);
            if (msg == null)  msg = reqFIPS;
            msg = "The receiver location has been changed to\n" + msg;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Int assignments assigned in configValue[]
     *  0 - Configuration block number [API 1.10, 1.11, 1.12 only returns block 0]
     *  1 - Size of byte array holding configuration block, excluding Message Length bytes.
     *  2 - Scan Interval_2Hz [ms]
     *  3 - Scan Interval_05Hz [ms]
     *  4 - RFtimeOut [sec]
     *  5 - USBtimeOut [sec]
     *  6 - BeaconTimeOut [sec]
     *  7 - AlertTimeOut [sec]
     *  8 - AlarmTimeOut [sec]
     *  9 - SnoozeTimeOut [sec]
     * 10 - AllowRemote [1 - true; 0 - false]
     * 11 - Tuning Grid [1 – 200kHz; 0 - 100 kHz]
     * 12 - Volume [0..63]
     * 13 - FIPS portion code [0..9]
     * 14 - FIPS state code [0..99]
     * 15 - FIPS county code [0..999]
     * 16 - Version [Major]
     * 17 - Version [Minor]
     * 18 - Beacon wait time[ms]
     * 19 - Validity for other functions to use: 0 = Request in process, don't use. 1 = Valid configuration array.
     */

    /// -----------| END Action Bar Support functions |------------------------

    /**
     * Processes the incoming receiver data based on its content defined in the receiver API.
     * We expect a multi-byte header defining the data and its purpose, but there are two cases
     * where non-header data must be expected:
     *    1) The continuation of an alert message from Text Notification API, or
     *    2) The continuation of Stored Messages made in response to a requestStoredMessages API call.
     * It is not possible to determine the source of non-header messages if a request for returning stored messages 
     * is made during the period when alert message text is being returned.
     *
     * NOTE: All graphical TextView,ImageView,etc. members in this class must be initialized
     *       prior to calling this function.
     * @param data A byte array containing data sent from the NPRLabs accessible FM RDS Receiver.
     */
    public void processReceivedData(byte[] data) {


        // 0xED = -19d = Notification from the receiver
        // 0xEE = -18d = Return of requested data
        // (All Java bytes are signed)
        // REFERENCE: http://stackoverflow.com/questions/11088/what-is-the-best-way-to-work-around-the-fact-that-all-java-bytes-are-signed

        if(messageCount > 0 && dbHandler.getMessageCount()> 0 && dbHandler.msgExists(messageCount)){
            myMsg= dbHandler.getMessage(messageCount);
        }

        if (data[NipperConstants.receiverByteReturnType] == NipperConstants.receiverReturnTypeNotification) {
            switch (data[NipperConstants.receiverByteReturnMode]){
                case NipperConstants.RECEIVER_MODE_STATUS:
                    updateTabletTextViews(data);
                    //System.out.println("The tablet views were updated");
                    break;
                case NipperConstants.RECEIVER_MODE_TEXT:
                    myMsg = myReceiver.updateReceiverAlertMessage(data, false, myMsg);
//                    System.out.println("#1THE MESSAGE THAT WAS RETURNED: " + myMsg.ShortMsgtoString());

                    this.expectingMoreAlertText = myReceiver.get_expectingMoreAlertText();

                    if(dbHandler.msgExists(messageCount)){
                        dbHandler.updateMessage(myMsg);
                        System.out.println("#1I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                    }else{
//                        System.out.println("DID NOT UPDATE THE DATABASE, CASE WAS FALSE");
                    }
//                    System.out.println("UPDATE");
                    break;
                case NipperConstants.RECEIVER_MODE_BANDSCAN:
                    updateReceiverBandScan(data);
                    break;
                case NipperConstants.RECEIVER_MODE_EASDATA:
                    myMsg = myReceiver.updateReceiverEASData(data, myMsg);
                    //System.out.println("This is what Message String is currently: " + myMsg.getMsgString());
                    displayCurrentMessage();
                   if(isAlarm && dbHandler.msgExists(messageCount)){
                        dbHandler.updateMessage(myMsg);
//                        System.out.println("#2I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                        //System.out.println("Did not add a new alert message, updated existing one.");
                    }
                    break;
                default:
                    break;
            }

        } else if  (data[NipperConstants.receiverByteReturnType] == NipperConstants.receiverReturnTypeStatus) {
            switch (data[NipperConstants.receiverByteReturnMode]){
                case NipperConstants.RECEIVER_MODE_VERSION:
                    versionNipperOneReceiver = myReceiver.receiveReceiverVersion(data);
                    //Log.d("versionNipperOneReceiver", versionNipperOneReceiver);
                    break;
                case NipperConstants.RECEIVER_MODE_CONFIGURATION:
                    myReceiver.receiveReceiverConfiguration(data, this);
                    break;
                case NipperConstants.RECEIVER_MODE_STOREDMESSAGES:
                    break;
                default:
                    break;
            }
        }
        else {
            // Data without a header is handled here.
            // It is either data containing Stored Messages, or it's from Text Notification API.
            // If we're in alarm but we haven't had a text notification with a header, ignore it.
            if (expectingMoreAlertText) {
                if (isAlarm){
                    myMsg = myReceiver.updateReceiverAlertMessage(data, true, myMsg);
                    if(dbHandler.msgExists(messageCount)){
                        dbHandler.updateMessage(myMsg);
                        System.out.println("#3I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                    }
                }
            } else  {

            }
        }

    } // END updateReceivedData()

    /**
     * Displays the receiver's status to the tablet screen.
     * <pre>
     * This function requires data[7] = 2 ("normal status") AND data[5] = 0xED ("Notification")
     * --------- 
     * data[0..1] Message Length(excluding these two bytes) Least significant byte first. 
     *            Status Message length is typically 0x36 bytes.
     * data[2..3]  Group   Application 01 01
     * data[4] Function    NPR-EAS 20
     * data[5] Return  Notification = ED  Notification ED is important data for immediate use. No request needed.
     * data[6] Message Number  01..FF  Only odd numbers from receiver
     * data[7] Receiver Mode: 
     *         2 = Normal Status
     * data[8..12] Tuned frequency: format: nnnnn
     * data[13..16] PI code: format hhhh (hex format), returns FFFF when scanning.
     * data[17..20] Call sign: format: CCCC
     * data[21..28] PS name. Format: cccccccc
     * data[29..31] SNR Quality in dB. Format: nnn (0..100)
     * data[32..34] Signal level. Format : nnn (0..100)
     * data[35] Mono / Stereo: 0-Mono; 1-Stereo
     * data[36..41] Date: format: ddmmyy
     * data[42..45] Time: format: HHMM
     * data[46] LED ON status: Format is bit-encoded.
     *             Beacon = 0000 0001
     *          No Signal = 0000 0010
     *              Power = 0000 0100
     *              Alert = 0000 1000
     *              ALARM = 0001 0000
     * Data[47] Alarm: Format is bit-encoded:
     *            No Beacon = 0000 0000
     *          Have Beacon = 0000 0001
     *          Have WA Bit = 0000 0010
     *           Have ALARM = 0000 0100
     *             Snoozing = 0000 1000
     * data[48] Volume Status 
     * data[49..53]  Receiver's stored FIPS Code: nnnnnn as portion (0-9),state (00-99), County (000-999)
     * data[54..55] Firmware version nn (major - minor) 
     * </pre>
     * @param data A byte array containing status data from the NPR Labs FM RDS Receiver.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void updateTabletTextViews(byte[] data) {
        // Rdata[13..16] PI code: format hhhh (hex format)
        // Receiver returns a PI code of FFFF when scanning.
        // In this notification mode, the receiver is slow scanning
        // its stored stations, looking for our beacon.

        // Rdata[8..12] Tuned frequency: format: nnnnn
        // Note that data[12] is always zero in the USA.

        // Frequency is in data[8..12] inclusive
        mStationFreq.setText(myReceiver.displayFrequency(Arrays.copyOfRange(data, 8, 13)));
        // Display the signal level
        // data 32, 33, 34 is the Signal Level in this notification mode
//        mSignalLevel.setText(myReceiver.displaySignalLevel(Arrays.copyOfRange(data, 32, 35)));

        // If the receiver is in slow scan, the PI code is ASCII "FFFF", so
        // we indicate we're looking for our station.
        if ( data[13]== 'F' ) {
            if (Build.VERSION.SDK_INT >= 16) {
                mStationFreq.setBackground(drawStationFreqSlow);
            }
            else {
                mStationFreq.setBackground(drawStationFreqSlow);
            }

//            mBeaconStatus.setText(nipperRes.getString(R.string.defaultTextSlowScan));
            // This is also a good place to extract our NIPPER ONE firmware version from the byte array
            // and store it for the About box use.
            //Log.d("versionNipperOneReceiver: update Tablet Text View ",Byte.toString(data[54]) + "." + Byte.toString(data[55]) );
            versionNipperOneReceiver = Byte.toString(data[54]) + "." + Byte.toString(data[55]);


        } else {
            isAlarm = (data[47] & NipperConstants.FLAG_HAVE_ALARM) == NipperConstants.FLAG_HAVE_ALARM  ? true : false;
            //if ((data[47] & FLAG_HAVE_ALARM) == FLAG_HAVE_ALARM) {
            //    isAlarm = true;
            //} else isAlarm = false;

            // We've found our station!
            if (Build.VERSION.SDK_INT >= 16) {
                mStationFreq.setBackground(drawStationFreqFoundStation);
            }
            else {
                mStationFreq.setBackground(drawStationFreqFoundStation);
            }
            //mStationFreq.setBackgroundColor(nipperRes.getColor(R.color.defaultBackgroundRelStationLayout));

            // Rdata[17..20] Call sign: format: CCCC
            // The receiver back-calculates the call letters from the PI Code.
            // Note that for ClearChannel-owned stations, the call letters will be
            // nonsensical because they transmit TMC and adhere to PI Euro standard.
            //mStationCall.setText(new String(Arrays.copyOfRange(data, 17, 21)));
            // New: Show the Call Letters in the BeaconStatus text.
            String tmp = nipperRes.getString(R.string.defaultTextStationFound) + new String(Arrays.copyOfRange(data, 17, 21));
//            mBeaconStatus.setText(tmp);

            // Rdata[21...28] PS code, format: CCCCCCCC
            // The Program Service field, an 8 character friendly name of the station.
            // It is more reliable than back calculating the Call letters from the PI code.
//            mStationCall.setText(new String(Arrays.copyOfRange(data, 21, 29)));

            // WA bit activated. This blinks the display.
            // For steady Alert indication use (data[47] & FLAG_HAVE_WABIT) == FLAG_HAVE_WABIT
//            if ((data[NipperConstants.receiverLEDStatus] & NipperConstants.FLAG_LED_ALERT) == NipperConstants.FLAG_LED_ALERT){
//                // If changing text color, be sure to set Alpha level too:  0xAARRGGBB
//                mAlert.setVisibility(TextView.VISIBLE);
//            } else {
//                mAlert.setVisibility(TextView.INVISIBLE);
//            }

            // ALARM activated. This is a blinking ALARM indication.
            // For steady ALARM indication use (data[47] & FLAG_HAVE_ALARM) == FLAG_HAVE_ALARM
//            if ((data[NipperConstants.receiverLEDStatus] & NipperConstants.FLAG_LED_ALARM) == NipperConstants.FLAG_LED_ALARM){
//                // If changing text color, be sure to set Alpha level too:  0xAARRGGBB
//                mAlarm.setVisibility(TextView.VISIBLE);
//            } else {
//                mAlarm.setVisibility(TextView.INVISIBLE);
//            }

            // Change the background message box color on ALARM,
            // Return to normal otherwise.
            // TODO If a new message comes in while HaveSetAlarmScreen == true, then display the time stamp again.
            if (isAlarm){

                if (!HaveSetAlarmScreen ) {

                    messageCount++;
                    dbHandler.addMessage(new MessageImpl());
                    myMsg = dbHandler.getMessage(messageCount);
                    System.out.println("NEW MESSAGE ADDED! " + myMsg.ShortMsgtoString());

                    Log.d("Message Count in UTTV", Integer.toString(messageCount));

//                    messageLayout.setVisibility(RelativeLayout.VISIBLE);
//
//                    if (Build.VERSION.SDK_INT >= 16) {
//                        messageLayout.setBackground(NipperOneAndroid.drawMessageAlarm);
//                    }
//                    else {
//                        messageLayout.setBackground(NipperOneAndroid.drawMessageAlarm);
//                    }

                    //mEASEvent.setTextColor(nipperRes.getColor(R.color.defaultTextColorMessageAlarm));
                    //messageLayout.setBackgroundColor(nipperRes.getColor(R.color.defaultBackgroundMessageAlarm));


                    android.text.format.Time now = new android.text.format.Time();
                    now.setToNow();
                    SimpleDateFormat formatter;
                    formatter = new SimpleDateFormat("MMMMM dd,yyyy hh:mm aaa");
                    //mMessage.append("\n\n---| ALARM Message received " + formatter.format(new Date()) + " |---\n");
                    Toast.makeText(parentContext, "An Alert Transmission is starting.", Toast.LENGTH_SHORT).show();

                    HaveSetAlarmScreen = true;
                }
            } else {
                // If we're in a Alarm screen mode, then we'll clear it.
                if (HaveSetAlarmScreen) {
                    // Clean up the display and flush any remaining alert message text.
                    dbHandler.updateMessage(myMsg);

                    clearAlarmScreen();
                    Toast.makeText(parentContext, "The Alert Transmission has ended.", Toast.LENGTH_SHORT).show();
                }
            }
        }

    } // END updateReceiverStatus()

    /**
     * a helper method used by updateTabletScreen()
     * it updates the variables in
     */
    private void displayCurrentMessage(){

    }

    /**
     * Displays the tuned frequency when the receiver is
     * performing a fast scan of the entire FM band.<br>
     * Affects:
     * <pre>
     *  mStationFreq
     *  mBeaconStatus
     *  mStationCall
     *  </pre>
     * @param data A byte array containing status data from the NPR Labs FM RDS Receiver.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void updateReceiverBandScan(byte[] data){
        // Frequency is in data[8]...data[12] inclusive
        mStationFreq.setText(myReceiver.displayFrequency(Arrays.copyOfRange(data, 8, 13)));
        // Signal Level is in data[13]...data[15] inclusive
//        mSignalLevel.setText(myReceiver.displaySignalLevel(Arrays.copyOfRange(data, 13, 16)));
        //      mStationFreq.setBackgroundColor(nipperRes.getColor(R.color.defaultBackgroundQuickScan));
        if ( mStationFreq.getBackground() != drawStationFreqQuick ) {
            if (Build.VERSION.SDK_INT >= 16) {
                mStationFreq.setBackground(drawStationFreqQuick);
            }
            else {
                mStationFreq.setBackgroundDrawable(drawStationFreqQuick);
            }
        }
//        mBeaconStatus.setText (nipperRes.getString(R.string.defaultTextQuickScan));
//        mStationCall.setText ("");

    } // END updateReceiverBandScan()

    /// -----------| Utility Functions |-------------------------------------------


    /**
     * Clears mMessage of all text and sets the printing line to 0,0.
     */
    public void clearText(){
    }

    /**
     * Returns the tablet display to a non-alarm display,<br>
     * Note: Does not check HaveSetAlarmScreen, the calling function should do that to save a call to this function.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void clearAlarmScreen() {

        resetAlertStructure();

        HaveSetAlarmScreen = false;

    }

    /**
     * Clear all flags and null strings that are used in updateReceiverAlertMessage()<br>
     * so that we can minimize the occurrences of previous, old message data appearing<br>
     * at the beginning of a new alert message.
     */
    private void resetAlertStructure() {
        myReceiver.resetAlertStructure();
    }


    /**
     * Clears the display when the receiver has been disconnected from the tablet,
     * Clears alarm screen (if shown) and clears callletters/frequency/etc.
     * Shows a warning on the screen.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private void receiverNotConnected() {
        if (HaveSetAlarmScreen) clearAlarmScreen();
        //mMessage.append(messageReceiverDisconnected);
//        mBeaconStatus.setText("");
//        mSignalLevel.setText("");
//        mStationCall.setText("");
        if ( mStationFreq.getBackground() != drawStationFreqQuick ) {
            if (Build.VERSION.SDK_INT >= 16) {
                mStationFreq.setBackground(drawStationFreqQuick);
            }
            else {
                mStationFreq.setBackground(drawStationFreqQuick);
            }
        }
        mStationFreq.setText("DISCONNECTED FROM RECEIVER");
    }


} // END Class NipperOneAlerter 

  