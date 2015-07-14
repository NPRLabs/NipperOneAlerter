package org.nprlabs.nipperone.main;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.format.DateFormat;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.nprlabs.nipperone.framework.DatabaseHandler;
import org.nprlabs.nipperone.framework.NipperConstants;
import org.prss.nprlabs.nipperonealerter.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kbrudos on 7/8/2015.
 *
 * This is the
 */
public class MyService extends Service {

    private static final String TAG = "MyService";

    static final int GET_MSG = 1;
    static final int NEW_ALERT = 2;
    static final int ALERT_DONE = 3;

    boolean isRunning = false;

    private Receiver myReceiver = new Receiver();
    private MessageImpl myMsg = new MessageImpl();
    private DatabaseHandler dbHandler;
    private SerialInputOutputManager.Listener mListener;

    private UsbSerialDriver sDriver = null;
    private SerialInputOutputManager mSerialIoManager;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent = null;



    private final Messenger mMessenger = new Messenger(new msgHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "S:onCreate: Service Started.");

        this.isRunning = true;
        //this.backgroundThread = new Thread(myTask);

        dbHandler = DatabaseHandler.getInstance(this);

        mListener = new SerialInputOutputManager.Listener() {
            @Override
            public void onRunError(Exception e) {
                Log.d(TAG, "SerialInputOutputManagerListener Runner stopped.");
            }
            @Override
            public void onNewData(final byte[] data) {
                new Runnable() {
                    @Override
                    public void run() {
                        processReceivedData(data);  //NipperOneAlerter.this.updateReceivedData(data);
                    }
                };
            }
        };
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.d(TAG, "Service Started");
        if(!this.isRunning){
            this.isRunning = true;
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {

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

        if(NipperConstants.messageCount > 0 && dbHandler.getMessageCount()> 0 && dbHandler.msgExists(NipperConstants.messageCount)){
            myMsg= dbHandler.getMessage(NipperConstants.messageCount);
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

                    NipperConstants.expectingMoreAlertText = myReceiver.get_expectingMoreAlertText();

                    if(dbHandler.msgExists(NipperConstants.messageCount)){
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
                    if(NipperConstants.isAlarm && dbHandler.msgExists(NipperConstants.messageCount)){
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
                    NipperConstants.versionNipperOneReceiver = myReceiver.receiveReceiverVersion(data);
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
            if (NipperConstants.expectingMoreAlertText) {
                if (NipperConstants.isAlarm){
                    myMsg = myReceiver.updateReceiverAlertMessage(data, true, myMsg);
                    if(dbHandler.msgExists(NipperConstants.messageCount)){
                        dbHandler.updateMessage(myMsg);
                        System.out.println("#3I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                    }
                }
            } else  {

            }
        }

    } // END updateReceivedData()

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
           // mExecutor.submit(mSerialIoManager);
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
     * inner class that handles the communication between the client and the service.
     */
    private static class msgHandler extends Handler {

        //final int GET_MSG = 1;
        final int GET_MSG_RESPONSE = 11;

        private final int SET_MSG = 2;

        @Override
        public void handleMessage(Message msg) {
            //this is the action
            int msgType = msg.what;

            switch (msgType) {
                case GET_MSG:
                    try {
                        //Incoming Data
                        String data = msg.getData().getString("data");
                        Message resp = Message.obtain(null, GET_MSG_RESPONSE);
                        Bundle bResp = new Bundle();
                        bResp.putString("respData", data.toUpperCase());
                        resp.setData(bResp);

                        msg.replyTo.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}