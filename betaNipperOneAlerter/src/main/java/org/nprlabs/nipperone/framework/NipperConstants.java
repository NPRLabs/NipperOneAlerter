package org.nprlabs.nipperone.framework;

import org.nprlabs.nipperone.main.NipperOneAndroid;

public class NipperConstants {
    
    // These are bits in mydata[46] and correspond to the
    // receiver's LED indicators (which remain ON until the receiver
    // 'times out').
    public static final byte FLAG_LED_BEACON = 1;
    public static final byte FLAG_LED_NOSIGNAL = 2;
    public static final byte FLAG_LED_POWER = 4;
    public static final byte FLAG_LED_ALERT = 8;
    public static final byte FLAG_LED_ALARM = 16;
    // These are bits in data[47] and have no 'time out'  
    public static final byte FLAG_HAVE_BEACON = 1;
    public static final byte FLAG_HAVE_WABIT = 2;
    public static final byte FLAG_HAVE_ALARM = 4;
    public static final byte FLAG_HAVE_SNOOZE = 8;
  
     
    public static final byte RECEIVER_MODE_STATUS = 2; 
    public static final byte RECEIVER_MODE_TEXT = 1; 
    public static final byte RECEIVER_MODE_BANDSCAN = 7;
    public static final byte RECEIVER_MODE_EASDATA = 6;
    
    public static final byte RECEIVER_MODE_CONFIGURATION = 0;
    public static final byte RECEIVER_MODE_VERSION = 8;
    public static final byte RECEIVER_MODE_STOREDMESSAGES = 9;
    
    // Byte[5] indicates the message return type: Status or Notification
    public static final byte receiverByteReturnType = 5;
    // Byte[7] indicates the mode of the message return type
    public static final byte receiverByteReturnMode = 7;
    public static final byte receiverFrequency = 8;
    public static final byte receiverLEDStatus = 46;
    public static final byte receiverTimerStatus = 47;

    // 0xE (-18 unsigned) in ReturnType indicates a Status message
    public static final byte receiverReturnTypeStatus = -18;
    // 0xED (-19 unsigned) in ReturnType indicates a Notification message
    public static final byte receiverReturnTypeNotification = -19;

}
