package org.nprlabs.nipperone.main;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.nprlabs.nipperone.framework.NipperConstants;
import java.util.List;

/**
 * Created by kbrudos on 7/15/2015.
 *
 */
public class MyBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "BroadcastReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        switch(intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                NipperActivity.receiverConnected();
                break;

            case UsbManager.ACTION_USB_DEVICE_DETACHED :
                // Either the receiver has been rebooted or physically disconnected from the tablet.
                // Bring this to the user's attention.

                NipperActivity.receiverNotConnected();
                break;
            case "org.prss.nprlabs.nipperonealerter.USBPERMISSION":
                for (final UsbDevice device : NipperConstants.mUsbManager.getDeviceList().values()) {
                    if (device.getVendorId() == 0x1320) {
                        Log.d(TAG, "Found Catena USB device: " + device);
                        //mMessage.append("Found the NipperOne Radio receiver\n");

                        if (NipperConstants.mUsbManager.hasPermission(device)) {
                            final List<UsbSerialDriver> drivers =
                                    UsbSerialProber.probeSingleDevice(NipperConstants.mUsbManager, device);
                            if (drivers.isEmpty()) {
                                Log.d(TAG, "  - No UsbSerialDriver available.");
                                //mMessage.append("\nThe Nipper One receiver does NOT have its driver assigned.\nPlease Press the Receiver's Reset button.\n");
                            } else {
                                for (UsbSerialDriver driver : drivers) {
                                    Log.d(TAG, "The NipperOne Receiver will be using this driver: " + driver);
                                }
                            }
                        } else {
                            Log.d(TAG, "This is distressing:We still don't have permission to access the Catena Receiver.");
                        }
                    }
                }
                break;
        }//end of switch
    } //end onReceive()
}