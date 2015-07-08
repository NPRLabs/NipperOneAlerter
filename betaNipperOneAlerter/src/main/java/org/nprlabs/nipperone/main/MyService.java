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

    boolean isRunning = false;
    private UsbManager mUsbManager;
    Thread backgroundThread;

    private final Messenger mMessenger = new Messenger(new msgHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "S:onCreate: Service Started.");

        this.isRunning = true;
        //this.backgroundThread = new Thread(myTask);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        if(!this.isRunning){
            this.isRunning = true;
            this.backgroundThread.start();
        }
        return START_STICKY;
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