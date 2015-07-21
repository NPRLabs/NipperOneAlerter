package org.nprlabs.nipperone.main;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.nprlabs.nipperone.framework.DatabaseHandler;
import org.nprlabs.nipperone.framework.NipperConstants;

import java.util.Arrays;
import java.util.List;

/**
 * Created by kbrudos on 7/8/2015.
 *
 * This is the
 */
public class MyService extends Service {

    private static final String TAG = "MyService";

    final static int SET_CLIENT = 0;
    static final int REMOVE_CLIENT = 1;
    static final int UPDATE_BANDSCAN = 2;
    static final int UPDATE_TABLET_TEXT_VIEW = 3;
    static final int NEW_ALERT = 4;
    static final int ALERT_DONE = 5;


    static boolean isRunning = false;

    private AlertImpl myMsg = new AlertImpl();
    private DatabaseHandler dbHandler;

    final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "SerialInputOutputManagerListener Runner stopped.");
        }
        @Override
        public void onNewData(final byte[] data) {
            new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "serial I/O manager started");
                    processReceivedData(data);  //NipperOneAlerter.this.updateReceivedData(data);
                }
            };
        }
    };

    private SerialInputOutputManager mSerialIoManager;
    private PendingIntent mPermissionIntent = null;
    private Messenger mClient = null;
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    private String displayFreqString = "";



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "S:onCreate: Service Started.");

        dbHandler = DatabaseHandler.getInstance(this);
    }

    public static boolean getIsRunning(){
        return isRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "service bound");
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.d(TAG, "Service Started, On Start command");
        if(!this.isRunning){
            this.isRunning = true;
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
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

        Log.d(TAG, "processing receiver data.");

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
                    System.out.println("The tablet views were updated");
                    sendMessageToUI(UPDATE_TABLET_TEXT_VIEW);

                    break;
                case NipperConstants.RECEIVER_MODE_TEXT:
                    myMsg = NipperConstants.myReceiver.updateReceiverAlertMessage(data, false, myMsg);
//                    System.out.println("#1THE MESSAGE THAT WAS RETURNED: " + myMsg.ShortMsgtoString());

                    NipperConstants.expectingMoreAlertText = NipperConstants.myReceiver.get_expectingMoreAlertText();

                    if(dbHandler.msgExists(NipperConstants.messageCount)){
                        dbHandler.updateMessage(myMsg);
                        System.out.println("#1I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                    }else{
//                        System.out.println("DID NOT UPDATE THE DATABASE, CASE WAS FALSE");
                    }
                    System.out.println("UPDATE");
                    break;
                case NipperConstants.RECEIVER_MODE_BANDSCAN:
                    updateReceiverBandscan(data);
                    sendMessageToUI(UPDATE_BANDSCAN);
                    break;
                case NipperConstants.RECEIVER_MODE_EASDATA:
                    myMsg = NipperConstants.myReceiver.updateReceiverEASData(data, myMsg);
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
                    NipperConstants.versionNipperOneReceiver = NipperConstants.myReceiver.receiveReceiverVersion(data);
                    //Log.d("versionNipperOneReceiver", versionNipperOneReceiver);
                    break;
                case NipperConstants.RECEIVER_MODE_CONFIGURATION:
                    NipperConstants.myReceiver.receiveReceiverConfiguration(data, this);
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
                    myMsg = NipperConstants.myReceiver.updateReceiverAlertMessage(data, true, myMsg);
                    if(dbHandler.msgExists(NipperConstants.messageCount)){
                        dbHandler.updateMessage(myMsg);
                        //System.out.println("#3I updated the message!!!!!! message is now: " + myMsg.ShortMsgtoString());
                    }
                }
            } else  {

            }
        }

    } // END updateReceivedData()

    /**
     *
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
    private void updateTabletTextViews(byte[] data){
        // Rdata[13..16] PI code: format hhhh (hex format)
        // Receiver returns a PI code of FFFF when scanning.
        // In this notification mode, the receiver is slow scanning
        // its stored stations, looking for our beacon.

        // Rdata[8..12] Tuned frequency: format: nnnnn
        // Note that data[12] is always zero in the USA.

        // Frequency is in data[8..12] inclusive
        this.displayFreqString = NipperConstants.myReceiver.displayFrequency(Arrays.copyOfRange(data, 8, 13));
        // Display the signal level
        // data 32, 33, 34 is the Signal Level in this notification mode
//        mSignalLevel.setText(myReceiver.displaySignalLevel(Arrays.copyOfRange(data, 32, 35)));


        NipperConstants.versionNipperOneReceiver = Byte.toString(data[54]) + "." + Byte.toString(data[55]);

        if ( data[13]== 'F' ) {


            // This is also a good place to extract our NIPPER ONE firmware version from the byte array
            // and store it for the About box use.
            //Log.d("versionNipperOneReceiver: update Tablet Text View ",Byte.toString(data[54]) + "." + Byte.toString(data[55]) );
            NipperConstants.versionNipperOneReceiver = Byte.toString(data[54]) + "." + Byte.toString(data[55]);


        } else {
            NipperConstants.isAlarm = (data[47] & NipperConstants.FLAG_HAVE_ALARM) == NipperConstants.FLAG_HAVE_ALARM  ? true : false;
            //if ((data[47] & FLAG_HAVE_ALARM) == FLAG_HAVE_ALARM) {
            //    isAlarm = true;
            //} else isAlarm = false;


            // Rdata[17..20] Call sign: format: CCCC
            // The receiver back-calculates the call letters from the PI Code.
            // Note that for ClearChannel-owned stations, the call letters will be
            // nonsensical because they transmit TMC and adhere to PI Euro standard.
            //mStationCall.setText(new String(Arrays.copyOfRange(data, 17, 21)));
            // New: Show the Call Letters in the BeaconStatus text.
//            String tmp = nipperRes.getString(R.string.defaultTextStationFound) + new String(Arrays.copyOfRange(data, 17, 21));
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

        }
    }

    private void updateReceiverBandscan(byte[] data){
        this.displayFreqString = NipperConstants.myReceiver.displayFrequency(Arrays.copyOfRange(data, 8, 13));
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
        for (final UsbDevice device : NipperConstants.mUsbManager.getDeviceList().values()) {
            if (device.getVendorId() == 0x1320) {
                Log.d(TAG, "Found Catena (NipperOne) USB device: " + device);
                //mMessage.append("Found the NipperOne receiver.\n");
                // Make sure we have permission to access the Receiver, if not, ask the user
                // if it's OK to access the receiver.
                if (NipperConstants.mUsbManager.hasPermission(device) ) {
                    final List<UsbSerialDriver> drivers =
                            UsbSerialProber.probeSingleDevice(NipperConstants.mUsbManager, device);
                    if (drivers.isEmpty()) {
                        Log.d(TAG, "  - No UsbSerialDriver available.");
                        //mMessage.append("\nThe NipperOne receiver does NOT have its software driver assigned. \nPlease Press the Receiver's Reset button.\n");
                    } else {
                        // This simply confirms to the user that we're connected to the receiver;
                        // there is nothing to call here because mListener will begin to get
                        // the incoming receiver data, once we've set sDriver to the proper CDC driver.
                        for (UsbSerialDriver driver : drivers) {
                            // Most important line in the function: Assigns the driver to sDriver.
                            NipperConstants.sDriver = driver;
                            Log.d(TAG, "  + " + driver);
                            //mMessage.append("Ready...\n");
                        }
                    }
                } else {
                    // Let's get permission from the user.
                    mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("org.prss.nprlabs.nipperonealerter.USBPERMISSION"),0);
                    NipperConstants.mUsbManager.requestPermission(device, mPermissionIntent);
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
        if (NipperConstants.sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(NipperConstants.sDriver, mListener);
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
     *
     * @param valueToSend
     */
    private void sendMessageToUI(int valueToSend){

        Log.i(TAG, "Sending a message to the UI");
        try{
            Bundle msgBundle = new Bundle();

            Message msg = new Message();

            switch (valueToSend) {

                case UPDATE_TABLET_TEXT_VIEW:
                    msg.what = valueToSend;
                    msgBundle.putString("freqString", displayFreqString);
                    msg.setData(msgBundle);
                    break;
                case UPDATE_BANDSCAN:
                    msg.what = valueToSend;
                    msgBundle.putString("freqString", displayFreqString);
                    msg.setData(msgBundle);
                    break;
                default:
                    break;
            }
            mClient.send(msg);
        }
        catch(RemoteException e){

        }

    }

    private class IncomingHandler extends PauseHandler{

        @Override
        public void processMessage(Activity activity,Message msg){

            if(activity != null)
            switch(msg.what){

                case SET_CLIENT:
                    Log.d(TAG, "Set the service client!");
                    mClient = msg.replyTo;
                    break;
                case REMOVE_CLIENT:
                    Log.d(TAG, "removed the service client");
                    mClient = null;
                    break;
                case UPDATE_BANDSCAN:
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }//end of IncomingHandler
}