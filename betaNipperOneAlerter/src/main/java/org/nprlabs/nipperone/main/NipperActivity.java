/*
 * NipperOneAlerter.java
 */

package org.nprlabs.nipperone.main;

// For USB access and control
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Arrays;
// For clock display
import java.util.Calendar;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;

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
public class NipperActivity extends Activity {

    /**
     * Copyright and attribution message for the "About..." dialog.
     * This is string-formatted text with tokens for app version, NipperOne firmware version, and current FIPS Code.  
     */
    static final String msgAbout = "NipperOneAlerter app version: %s\nNipperOne Receiver Firmware Version: %s\n\nApp Copyright © 2013-2015 NPR Labs (alerting@npr.org)\nAuthor: Rich Rarey and Katherine Brudos\n\nReceiver firmware Copyright © 2013-2015 Catena Radio Design\nAuthor: Joop Beunders\n\nReceiver Location is set to:\n%s";

    String TAG = "NipperOneAlerter";


    private String mFormat;

    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private IntentFilter tickReceiverIntentFilter = null;

    // These indicate which fragment we want to display
    private final int FragmentMode_SETTINGS = 1;
    private final int FragmentMode_HELP = 2;
    private final int FragmentMode_SINGLE_ALERT = 3;

    static String versionNipperOneAlerter = "Unknown";

    static RelativeLayout mStationLayout;
    static ImageView mStationLogo;
    static TextView mStationCall;
    static TextView mStationFreq;
    static TextView mBeaconStatus;


    static TextView mSignalLevel;
    static TextView mMsgOriginator;
    static TextView mEvent;
    static TextView mDuration;

    static TextView txtBanner;
    static Button msgArchive;
    static ListView myListView;

    static Drawable drawMessageAlarm = null; //nipperRes.getDrawable(R.drawable.bordermessagealarm);
    static Drawable drawMessageNormal = null; //nipperRes.getDrawable(R.drawable.bordermessagenormal);
    static Drawable drawStationFreqSlow = null;
    static Drawable drawStationFreqQuick = null;
    static Drawable drawStationFreqFoundStation = null;



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
//
//    /**
//     * List containing help strings for each of the EAS Short Code TextViews.
//     * API.mEASCodesUrgency.setTag(1);
//     * API.mEASCodesCertainty.setTag(2);
//     * API.mEASCodesResponse.setTag(3);
//     * API.mEASCodesMessageType.setTag(4);
//     * API.mEASCodesEvent.setTag(5);
//     * API.mEASCodesSeverity.setTag(6);
//     * API.mEASCodesCategory.setTag(7);
//     * API.mEASCodesDuration.setTag(8)
//     */
//    private final List<String> codeHelpText = Arrays.asList(
//            " describes the 'urgency' of this message.",
//            " describes the 'certainty' of the message.",
//            " is the recommended action you should take for this event.",
//            " describes this Alert Message type.",
//            " is a short description of this event.",
//            " describes the severity of this event.",
//            " describes the category of this event.",
//            " is how long this alert is in effect.");



    // Our local resources, such as stored colors
    private Resources nipperRes;

//     /**
     //     * Receiver Alarm Status Flag (set and cleared in updateReceiverStatus())
     //     * Set = ALARM, Cleared = Normal
     //     */

    /**
     * This is the context variable set in onCreate()
     * It is used by the Toast widget.
     */
    private static Context parentContext;
    private static AlertImpl myMsg = new AlertImpl();
    private AlertImpl newMsg = new AlertImpl();
    private static int messageCount = 0;
    boolean messageComplete = false;


    //variables having to do with the Service and it's connection.
    //mService is used to send messages
    private static Messenger mService = null;
    private static Messenger mMessenger = new Messenger(new MessageHandler());
    private static boolean mIsBound = false;
    private  MessageHandler msgHandler = new MessageHandler();

    public ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "mConnection on Service Connected");
            mService = new Messenger(service);

            try{
                Message msg = Message.obtain(null, MyService.SET_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }catch(RemoteException e){

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private static boolean receiverConnection = false;
    private ArrayAdapter<String> simpleAdpt;


    /*
     * (non-Javadoc)
     * First method upon creation.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "------onCreate------");

        // This will manually hide the activity bar and nav bar, but only until the user touches the screen.
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN );

        setContentView(R.layout.main);

        //this should be the only way that the database is accessed. The constructor should never be used
        NipperConstants.dbHandler = DatabaseHandler.getInstance(this);

        //For testing purposes an alert can be manually added to the database. An alert is added everytime
//        newMsg.setEvent("Fire Warning");
//        newMsg.setMsgUrgency("Something");
//        newMsg.setMsgCertainty("blah");
//        newMsg.setMsgSeverity("very severe");
//        newMsg.setMsgOriginator("NPR LABS");
//        newMsg.setMsgString("This is a trial message. Created to be added to the dataBase in onCreate()");
//        dbHandler.addMessage(newMsg);

        //message count is set like this only once. Here. This way if you are restarting
        // the app you get the number of messages currently in the database.
        //Also if there is a current database the most recent message is retrieved.
        messageCount = NipperConstants.dbHandler.getMessageCount();
        Log.d("Message Count onCreate", Integer.toString(messageCount));

        if(NipperConstants.dbHandler.getMessageCount() == 0){}
        else{myMsg= NipperConstants.dbHandler.getMessage(NipperConstants.dbHandler.getMessageCount());}


        // We get the local resources instance so we can get our stored colors
        // and other attributes. REQUIRED by API.initializeAPI();
        nipperRes = getResources();

        // Initialize the API class.
        NipperConstants.myReceiver.initializeReceiver();

        // Share the activity and context for Toast and the preferences activity
        parentContext = getApplicationContext();
        //parentActivity = this;


        mStationFreq = (TextView)findViewById(R.id.txt_station_freq);
        msgArchive = (Button)findViewById(R.id.btn_msg_archive);
        txtBanner = (TextView)findViewById(R.id.txt_banner);
        myListView = (ListView) findViewById(R.id.alert_list_view);



        String[] values = new String[]{ "Alert 1", "Alert 2", "Alert 3"};
        simpleAdpt = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, values);

        myListView.setAdapter(simpleAdpt);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionNipperOneAlerter = pInfo.versionName;
        } catch (NameNotFoundException e) {
            Log.e(TAG,"Could not determine the NipperOneAlerter version.");
            versionNipperOneAlerter = "Unknown";
        }


        tickReceiverIntentFilter = new IntentFilter();
        tickReceiverIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        tickReceiverIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        tickReceiverIntentFilter.addAction("org.prss.nprlabs.nipperonealerter.USBPERMISSION");

        //registerReceiver(myBroadcastReceiver, tickReceiverIntentFilter);

        /// -----------| END Clock setup  |------------------------------------

        /// -----------| System Nav Bar + Title Bar hiding and revealing |-----
        // This section deals with hiding and revealing the System Navigation (bottom of screen) and System Title bar (top of screen).
        // We use it here to automatically return to our full-screen state after the user has touched the Navigation bar. We also
        // use it to hide our app's clock when the Nav bar is visible, because the system displays a clock in the Nav Bar.
//        final View contentView =  findViewById(R.id.fraLayout);
//        final View controlsView = mClockText;



        // Load any app preferences (not receiver config, but only app stuff).
        loadPrefs();



        //bind to the service that was started in the broadcast receiver
        Intent myIntent = new Intent(this, MyService.class);
        startService(myIntent);
        bindService(myIntent, mConnection, Context.BIND_AUTO_CREATE);
        //bind to the service
        mIsBound = true;
        sendMessageToService(MyService.SET_CLIENT);


    } // END onCreate()

    @Override
    public void onStart(){
        super.onStart();
        NipperConstants.isActivityRunning = true;
    }
    @Override
    public void onStop(){
        super.onStop();
        NipperConstants.isActivityRunning = false;
    }

    public void doUnbindService(){
        if(mIsBound){
            //if we have received the service and hence registered with it, then now is the time
            // to unregister
            if(mService != null){

                Message msg = Message.obtain(null, MyService.REMOVE_CLIENT);
                msg.replyTo = mMessenger;
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    //You could print the stack track, but it's not necessary
                    //e.printStackTrace();
                }
            }
            Log.d(TAG, "unbinding the service from client");
            unbindService(mConnection);
            mIsBound = false;

        }
    }


    private static void sendMessageToService(int valueToSend){
        if(mIsBound){
            if(mService != null){
                try{
                    Message msg = Message.obtain(null, valueToSend);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                    Log.d("SEND_MESSAGE_TO_SERVICE", "msg was sent to the service!");
                }catch (RemoteException e){
                }
            }else{
                Log.d("NipperActivity", "mService is null, no message sent.");
            }
        }

    }

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

    @Override
    public void onPause(){
        Log.d(TAG, "---onPause---");
        super.onPause();
        msgHandler.pause();

        if(myBroadcastReceiver != null) unregisterReceiver(myBroadcastReceiver);
        receiverConnection = false;
    }

    @Override
    public void onResume(){
        Log.d(TAG, "---onResume---");
        super.onResume();
        msgHandler.resume();

        if (myBroadcastReceiver != null) registerReceiver(myBroadcastReceiver, tickReceiverIntentFilter);

        //when the receiver is re-connected the activity is paused and resumed (if the activity is
        //already going). If not then the Broadcast receiver starts the main activity.and On Resume is still called
        if(!receiverConnection){sendMessageToService(MyService.RECEIVER_CONNECTED);}
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        doUnbindService();
        stopService(new Intent(this, MyService.class));
    }

    /*
    * (non-Javadoc)
    * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
    */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
//        NipperConstants.myReceiver.requestReceiverConfiguration(NipperConstants.sDriver);
//        NipperConstants.myReceiver.requestReceiverVersion(NipperConstants.sDriver);
        // Handles presses on the action bar items
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_help) {
            openHelp();
            return true;
        } else if (id == R.id.action_about) {
            openAbout();
            return true;
        } else if (id == R.id.action_clearscreen) {
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
//                    NipperConstants.myReceiver.writeReceiverConfigurationDefault(NipperConstants.sDriver);
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



    /**
     * Handler for Action Bar item "About"
     * Displays the app version, Receiver firmware version, copyright information, and receiver FIPS code in a message box.
     */
    private void openAbout() {
//        NipperConstants.myReceiver.requestReceiverVersion(NipperConstants.sDriver);

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
        String msg = String.format(msgAbout,versionNipperOneAlerter,NipperConstants.versionNipperOneReceiver,msgFIPS);


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
        intent.setClass(NipperActivity.this, SetPreferenceActivity.class);
        startActivity(intent);
    }

    /**
     * Helper class that calls the openHelp() method from the shortcut button
     * @param view
     */
    public void openHelp(View view){
        openHelp();
    }


    /**
     * Handler for Action Bar item "Settings"
     */
    private void openSettings() {
        //    API.mMessage.append("Pressed Settings\n");
        // Display the fragment as the main content.
        Intent intent = new Intent();

        // Pass the parameter to indicate we want to show the settings fragment.
        intent.putExtra("NipperOneFragmentMode", FragmentMode_SETTINGS);
        intent.setClass(NipperActivity.this, SetPreferenceActivity.class);
        startActivity(intent);
    }


    public void viewMostRecent(View view){

        messageCount = NipperConstants.dbHandler.getMessageCount();
        String tmpString = "";

        if(messageCount == 0){
            //do nothing
        }else {
//            CustomDialog d = new CustomDialog(this);
//            d.show();

            //Display the fragment as the main content
            Intent intent = new Intent();

            // Pass the parameter to indicate we want to show the settings fragment.
            intent.putExtra("NipperOneFragmentMode", FragmentMode_SINGLE_ALERT);
            intent.setClass(NipperActivity.this, SetPreferenceActivity.class);
            startActivity(intent);
        }
    }


    public void viewMessageArchive(View view){
//        Toast toast = Toast.makeText(getApplicationContext(), "Clicked to view the current Message!", Toast.LENGTH_SHORT );
//        toast.show();
        messageCount = NipperConstants.dbHandler.getMessageCount();
        String tmpString = "";
        List<AlertImpl> messageList = NipperConstants.dbHandler.getAllMessages();

        if(messageCount == 0){tmpString = "There are currently no messages in the database to display.";}
        else {

            int i = 0;
            for (AlertImpl msg : messageList) {
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
//            NipperConstants.myReceiver.writeReceiverConfigurationFIPS(NipperConstants.sDriver, reqFIPS);

            // If we have descriptive text about the location, show it.
            // Else just show the user the new FIPS code.
            String msg = prefs.getString("key_reqFIPSText", null);
            if (msg == null)  msg = reqFIPS;
            msg = "The receiver location has been changed to\n" + msg;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * The UI side of this method. The other part deals with the data
     * in the service.class with a method of the same name.
     */
    //TODO clean up this method.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    protected static void updateTabletTextViews() {

        // If the receiver is in slow scan, the PI code is ASCII "FFFF", so
        // we indicate we're looking for our station.
            //mStationFreq.setBackgroundColor(nipperRes.getColor(R.color.defaultBackgroundRelStationLayout));



            if (NipperConstants.isAlarm){
                txtBanner.setText(R.string.new_alert);

            } else {
                txtBanner.setText(R.string.no_alert);

            }

    } // END updateReceiverStatus()


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
    protected static void updateReceiverBandScan(String data){
        // Frequency is in data[8]...data[12] inclusive
        mStationFreq.setText(data);
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
     * Returns the tablet display to a non-alarm display,<br>
     * Note: Does not check HaveSetAlarmScreen, the calling function should do that to save a call to this function.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    private static void clearAlarmScreen() {

        resetAlertStructure();
        NipperConstants.HaveSetAlarmScreen = false;
    }

    /**
     * Clear all flags and null strings that are used in updateReceiverAlertMessage()<br>
     * so that we can minimize the occurrences of previous, old message data appearing<br>
     * at the beginning of a new alert message.
     */
    private static void resetAlertStructure() {
        NipperConstants.myReceiver.resetAlertStructure();
    }


    /**
     * Method that is a work around to trigger the service to look for the receiver if it has been plugged in
     * after the service/app has been started.
     * Called from 'MyBroadcastReceiver' when the usb is plugged in.
     */
    public static void receiverConnected(){
        Log.d("Main Activity", "Receiver was connected again! Sending Message to service!!!!!");
        sendMessageToService(MyService.RECEIVER_CONNECTED);
    }


    /**
     * Clears the display when the receiver has been disconnected from the tablet,
     * Clears alarm screen (if shown) and clears callletters/frequency/etc.
     * Shows a warning on the screen.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    public static void receiverNotConnected() {

        if (NipperConstants.HaveSetAlarmScreen) {}
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
        sendMessageToService(MyService.RECEIVER_DISCONNECTED);
        receiverConnection = true;
    }

    static class MessageHandler extends PauseHandler {

        protected Activity activity;


        final protected void processMessage(android.os.Message msg){

            //final Activity activity = this.activity;
            byte[] data = msg.getData().getByteArray("byteArray");

                switch (msg.what){

                    case MyService.UPDATE_TABLET_TEXT_VIEW:
                        mStationFreq.setText(msg.getData().getString("freqString"));
                        //updateReceiverBandScan(msg.getData().getString("freqString"));
                        updateTabletTextViews();
                        break;
                    case MyService.UPDATE_BANDSCAN:
                        mStationFreq.setText(msg.getData().getString("freqString"));
                        break;
                    case MyService.TEST:
                        mStationFreq.setText("Searching");
                        break;
                    case MyService.ALERT_DONE:
                        clearAlarmScreen();
                        break;
                    default:
                        break;
                }
            }

    }


} // END Class NipperOneAlerter 

  