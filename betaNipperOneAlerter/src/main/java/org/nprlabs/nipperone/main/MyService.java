package org.nprlabs.nipperone.main;

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

import org.prss.nprlabs.nipperonealerter.R;

import java.util.List;

/**
 * Created by kbrudos on 7/8/2015.
 */
public class MyService extends Service {

    int GET_MSG = 1;


    private static final String TAG = "MyService";
    private NotificationManager mNotificationManager;
    private UsbManager mUsbManager;

    private final Messenger mMessenger = new Messenger(new msgHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "S:onCreate: Service Started.");
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        if ( Intent.ACTION_TIME_TICK.equals(action) ) {
//
//            mCalendar.setTimeInMillis(System.currentTimeMillis());
//            mClockText.setText(DateFormat.format(mFormat, mCalendar));
//
//        }  else if ( UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ) {
//            //mMessage.append("\nThe Receiver is plugged in!\n");
//
//
//        } else if ( UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) ) {
//            // Either the receiver has been rebooted or physically disconnected from the tablet.
//            // Bring this to the user's attention.
//            receiverNotConnected();
//
//        } else if ( "org.prss.nprlabs.nipperonealerter.USBPERMISSION".equals(action) ) {
//            for (final UsbDevice device : mUsbManager.getDeviceList().values()) {
//                if (device.getVendorId() == 0x1320) {
//                    Log.d(TAG, "Found Catena USB device: " + device);
//                    //mMessage.append("Found the NipperOne Radio receiver\n");
//
//                    if ( mUsbManager.hasPermission(device) ) {
//                        final List<UsbSerialDriver> drivers =
//                                UsbSerialProber.probeSingleDevice(mUsbManager, device);
//                        if (drivers.isEmpty()) {
//                            Log.d(TAG, "  - No UsbSerialDriver available.");
//                            //mMessage.append("\nThe Nipper One receiver does NOT have its driver assigned.\nPlease Press the Receiver's Reset button.\n");
//                        } else {
//                            for (UsbSerialDriver driver : drivers) {
//                                Log.d(TAG, "  + " + driver);
//                                //mMessage.append("tickReceiver:The NipperOne receiver will be using the following driver:\n" + driver + "\n");
//                            }
//                        }
//                    } else {
//                        //mMessage.append("\nThis is distressing:\nWe still don't have permission to access the Catena Receiver.\n");
//                    }
//                }
//            }
//        }
//    }

    private void showNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, NipperOneAndroid.class), 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("NipperOne Alerter")
                .setContentText("Started Alert System Monitor")
                .setSmallIcon(R.drawable.nprlabslogo)
                .setContentIntent(contentIntent).build();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        //mNotificationManager.notify(0,notification);
    }

    @Override
    public void onDestroy() {

    }


    /**
     * inner class that handles the communication between the client and the service.
     */
    private static class msgHandler extends Handler {

        private final int GET_MSG = 1;
        private final int GET_MSG_RESPONSE = 11;

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