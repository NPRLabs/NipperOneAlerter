
/* API.java
 * 
 * REFERENCES:
 * code folding: http://kosiara87.blogspot.com/2011/12/how-to-install-coffee-bytes-plugin-in.html
 */

package org.nprlabs.nipperone.main;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
//import android.view.View;
//import android.widget.ScrollView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
//import com.hoho.android.usbserial.util.SerialInputOutputManager;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
//import java.util.Locale;
import java.util.Map;

/**
* This class manages the incoming receiver data by processing it and displaying 
* the information on the various TextView controls.
* The logic for displaying the information is in this class.<br>
* <p><b>REQUIRES:</b><br>
* 1. NipperOneAlerter must initialize the TextView,ImageView, etc. members with a valid reference.<br>
* 2. NipperOneAlerter must call API.initialize() to initialize the EAS Event Code hashmap.<br>
* 3. NipperOneAlerter is the source of the raw data from the receiver.<br>
* 
* <p><b>REFERENCES:</b><br>
* For understanding the FM RDS Transmissions, see NPR Labs/Jump2Go's  <i>Modernized Emergency Alerting ODA v 0.9.3</i><br>
* For the definitive guide to the NIPPER ONE FM RDS Receiver, see Catena Radio Design's <i>CDC NPR-EAS API v. 1.11</i><br>
* 
* <p>@author RRarey -  20131102
* <p>REVISIONS:
* 20140702 RRAREY - Fixed the scrolling to be smoother. Implemented an RDS message error-reduction scheme to reduce garblem messages.
* 
*/
public class Receiver {
    
    String TAG = "NipperOneAlerter";
   
   // Variables for managing stored messages from the receiver
   static ByteArrayOutputStream outputStream;
   static byte rawStoredMessages[];
   static int StoredMessageMap[];
   static String StoredMessages[];

    private String msg = "";

   
   /**
    * Alert Message Type, enumerated in text equivalent
    */
   static final List<String> typeMessage = Arrays.asList(
                           "End of Transmission",
                           "New Alert",
                           "Update Previous Alert",
                           "Cancel Previous Alert",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Private Data");
   
   /**
    * CAP Certainty Codes enumerated in text equivalent
    */
   static final List<String> codeCertainty = Arrays.asList(
                           "Not Set",  
                           "Observed",    
                           "Likely",  
                           "Possible",    
                           "Unlikely",    
                           "Unassigned",
                           "Unassigned",
                           "Unknown"); 

   /**
    * CAP Severity Codes enumerated in text equivalent
    */
   static final List<String> codeSeverity = Arrays.asList(
                           "Not Set",     
                           "Extreme",
                           "Severe",
                           "Moderate",
                           "Minor",
                           "Unassigned",
                           "Unassigned",
                           "Unknown");     
        
   /**
    * CAP Urgency Codes enumerated in text equivalent
    */
   static final List<String> codeUrgency = Arrays.asList(        
                           "Not Set",
                           "Immediate",
                           "Expected",
                           "Future",
                           "Past",
                           "Unassigned",
                           "Unassigned",
                           "Unknown");
   
   /**
    * CAP Response Codes enumerated in text equivalent
    */
   static final List<String> codeResponse = Arrays.asList(
                           "Not Set",
                           "Shelter",
                           "Evacuate",
                           "Prepare",
                           "Execute",
                           "Avoid",
                           "Monitor",
                           "Assess",
                           "All Clear",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "None");
   
   /**
    * CAP Category Codes enumerated in text equivalent
    */
   static final List<String> codeCategory = Arrays.asList(
                           "Not Set",
                           "Geophysical",
                           "Meteorological",
                           "Safety",
                           "Security",
                           "Rescue",
                           "Fire",
                           "Health",
                           "Environmental",
                           "Transportation",
                           "Infrastructure",
                           "CBRNE",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Other");
   
   /**
    * CAP Origination Codes enumerated in text equivalent
    */
   static final List<String> codeOrg = Arrays.asList(
                           "EAS-Participant (Broadcast level test message)",
                           "CIV-Civil Authorities (Governor / State / Local)",
                           "WXR-National Weather Service",
                           "PEP-Primary Entry Point Station (President / National)",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned",
                           "Unassigned");

   /**
    * CAP Event Codes hashmap
    *
    */
   static Map<String,String> codeEvent = new HashMap<String,String>();
   static {
       codeEvent.put("ADR","Administrative Message");
       codeEvent.put("AVA","Avalanche Watch");
       codeEvent.put("AVW","Avalanche Warning");
       codeEvent.put("BZW","Blizzard Warning");
       codeEvent.put("CAE","Child Abduction Emergency");
       codeEvent.put("CDW","Civil Danger Warning");
       codeEvent.put("CEM","Civil Emergency Message");
       codeEvent.put("CFA","Coastal Flood Watch");
       codeEvent.put("CFW","Coastal Flood Warning");
       codeEvent.put("DMO","Demonstration Message");
       codeEvent.put("DSW","Dust Storm Warning");
       codeEvent.put("EAN","Emergency Action Notification");
       codeEvent.put("EQW","Earthquake Warning");
       codeEvent.put("EVI","Evacuation Immediate");
       codeEvent.put("FFA","Flash Flood Watch");
       codeEvent.put("FFW","Flash Flood Warning");
       codeEvent.put("FLA","Flood Watch");
       codeEvent.put("FLW","Flood Warning");
       codeEvent.put("FRW","Fire Warning");
       codeEvent.put("HMW","Hazardous Materials Warning");
       codeEvent.put("HUA","Hurricane Watch");
       codeEvent.put("HUW","Hurricane Warning");
       codeEvent.put("HWW","High Wind Warning");
       codeEvent.put("LAE","Local Area Emergency");
       codeEvent.put("LEW","Law Enforcement Warning");
       codeEvent.put("NAT","National Audible Test");
       codeEvent.put("NIC","National Information Center");
       codeEvent.put("NMN","Network Message Notification");
       codeEvent.put("NPT","National Periodic Test");
       codeEvent.put("NUW","Nuclear Power Plant Warning");
       codeEvent.put("RHW","Radiological Hazard Warning");
       codeEvent.put("RMT","Required Monthly Test");
       codeEvent.put("RWT","Required Weekly Test");
       codeEvent.put("SMW","Special Marine Warning");
       codeEvent.put("SPW","Shelter In-place Warning");
       codeEvent.put("SVA","Severe Thunderstorm Watch");
       codeEvent.put("SVR","Severe Thunderstorm Warning");
       codeEvent.put("TOA","Tornado Watch");
       codeEvent.put("TOE","911 Telephone Outage Emergency");
       codeEvent.put("TOR","Tornado Warning");
       codeEvent.put("TRA","Tropical Storm Watch");
       codeEvent.put("TRW","Tropical Storm Warning");
       codeEvent.put("TSA","Tsunami Watch");
       codeEvent.put("TSW","Tsunami Warning");
       codeEvent.put("VOW","Volcano Warning");
       codeEvent.put("WSW","Winter Storm Watch");
   }
    
   /**
   * Holds the current receiver configuration converted to an Int array. 
   */
   private int configValue[] = new int[20];
   
   //private Context parentContext;   
   
   /**
    * The total number of text segments in the alert message<br>
    * Range: 0 to 63, where 0 indicates no text segment with the alert.
    */
   static int alerttextCount = 0;
   
   /**
    * The text segment currently being transmitted<br>, used by updateReceiverEASData().
    * Range: 0 to 63 where 0 indicates no text is currently being transmitted. 
    */
   static int alerttextSegment = 0;
   static int msgID = 0;
   static int msgDigest = 0;

   static StringBuilder alertMessageText = new StringBuilder();
   
   
   /**
    * 
    */
   static boolean prepareAlertTextSegment = true;   
   
   /**
    *  For tracking textSegment changes, used by updateReceiverEASData()
    */
   private static int alerttextSegmentPrevious = 0;

   /**
    * Flag indicating we expect a headerless data chunk containing the remaining chars 
    * of an alert text segment, used by updateReceiverAlertMessage().
    */
   private static boolean expectingMoreAlertText = false;
   
   /**
    * Byte array that holds the current Alerting message segment, used by updateReceiverAlertMessage().
    */
    private static byte[] currentTextNotification = new byte[64];

    /**
     * Byte array that holds the current Alerting message segment, used by updateReceiverEASData().
     */
    static byte[] alertMessageTextTemp;
    
   /**
    * Length of the 1st chunk of alert text data, used by updateReceiverAlertMessage().
    */
   private static int initialMessageLen = 0;
   
   /**
    * Segment tracking used by updateReceiverAlertMessage(); this holds the current segment
    * reported by data[8] in the byte array passed in the header to the function, the source of this
    * segment number is in the header of data passed to updateReceiverAlertMessage().
    */
   static int updateReceiverAlertMessage_Segment = 0;
   
   /**
    * Segment tracking used by updateReceiverAlertMessage(), this holds the previous segment number that was
    * passed in the header of data passed to updateReceiverAlertMessage().
    */
   private int updateReceiverAlertMessage_SegmentPrevious = 0;

   /**
    * Repeated message tracking, used by updateReceiverAlertMessage() AND updateReceiverEASData() (but not simultaneously).
    */
   private byte messageRepeatCount = 0;
   /**
    * The Class that implements Seth Stroh's error-reduction scheme for repeated messages, used by 
    * updateReceiverAlertMessage() AND updateReceiverEASData() (but not simultaneously).
    */
   private rdsValidation rdsvalidation = new rdsValidation();
   
   /**
    * Bool that determines which method to use to gather the alert text:<br>
    * If true, then updateReceiverAlertMessage() is disabled and we use the updateReceiverEASData()<br>
    * If false, then we use updateReceiverAlertMessage() and updateReceiverEASData() is disabled from displaying text.
    */
   static Boolean disableupdateReceiverAlertMessage;
   
   private String appendString; 
   private int printCount = 0;
   

   ///------------------------| Initializer |-----------------------------------
   
   /**
    * Initialize the EAS codeEvent HashMap with the values.
 * @return 
    */
   public void initializeReceiver(){
       
       // Fill the alert arrays with spaces.
       alertMessageTextTemp = new byte[64];
       Arrays.fill(alertMessageTextTemp, (byte) 0x20);
   }
 

/// -----------| This section manages Notification data coming from the receiver |------  
   
      
   /**
   * Displays the receiver's incoming alert messages to the tablet screen.<br>
   * This function tracks the incoming alert text data segments, because Android delivers each alert text segment in two chunks,<br>
   * The first chunk with a 9 byte header<br>
   * The second chunk without a header,<br>
   * *and* alert text may be repeated multiple times in the RDS transmission,<br>
   * REVISIONS:<br>
   * For NipperOne firmware v1.18 and later, data[] is a fixed-length array of 64 bytes.<br>
   * TODO:<br>
   * On repeats of the same message, add byte verification to display the least-garbled message.
   * <pre>
   * data[0..1] Message Length(excluding these two bytes) Least significant byte first. 
   *            Status Message length is typically 0x36 bytes.
   * data[2..3]  Group   Application 01 01
   * data[4] Function    NPR-EAS 20
   * data[5] Return  Notification = ED  Notification ED is important data for immediate use. No request needed.
   * data[6] Message Number  01..FF  Only odd numbers from receiver
   * data[7] Receiver Mode: 
   *         1 = Text (Emergency) Message
   * data[8]  ODA Text segment number of text string (same as alerttextSegment), range: 1 to 63 decimal. If 0, then invalid.
   * data[9..n] ASCII Text ***NOTE*** AS OF NIPPER ONE FIRMWARE 1.20, NO LINEFEED AND OTHER FORMATTING CHARS ARE PASSED THROUGH THIS API.
   * </pre>
   * @param data A byte array containing the alert message, and possibly also containing the 9 byte header.
   * @param HasNoHeader Flag indicating if the message text has a header, if not, is a continuation of the alert message. 
   */
   public MessageImpl updateReceiverAlertMessage(byte[] data, Boolean HasNoHeader, MessageImpl myMsg ){
       
       
       // This flag disables this function (the updateReceiverEASData() function will then handle the alert messages).
       if (disableupdateReceiverAlertMessage) return myMsg ;
       
       
       // Ignore everything when there isn't valid text being sent. 
       if  (data[8] == 0) {
           //Log.d("updateReceiverAlertMessage",String.format("alertTextSegment = 0  data:%s", new String(data)));
           return myMsg ;
        }
       try {
           if (HasNoHeader) {
               // There is no 9 byte header, so we must check to see if we are expecting this data 
               // to be the second chunk of the text segment.
               if (expectingMoreAlertText) {
                   expectingMoreAlertText = false;
                    // No header; data is just ASCII text with two NULL Terminators. 
                    // Add the remaining bytes to currentTextNotification to complete the text segment
                    for (int n = 0; n < data.length; n++) {
                        if ( data[n] != 0 ) {
                            currentTextNotification[initialMessageLen] = data[n];
                            initialMessageLen++;
                        } else break;
                    }
                    switch (messageRepeatCount) {
                        case 1:
                            rdsvalidation = new rdsValidation(currentTextNotification);
                            //Log.d("Got to case #1, URAM", rdsvalidation.getMessageString());
                            break;
                        case 2:
                            rdsvalidation.rdsSubmitMessageRepeat(currentTextNotification);
                            //Log.d("Got to case #2, URAM", rdsvalidation.getMessageString());
                            break;
                        case 3:
                            rdsvalidation.rdsSubmitMessageRepeat(currentTextNotification);

                            Log.d("updateReceiverAlertMsg", String.format("Segment %d Confidence Score: %.1f%%",
                                    updateReceiverAlertMessage_Segment,
                                    rdsvalidation.getValidityScore()));
                            Log.d("Got to case #3, URAM", rdsvalidation.getMessageString());
                            myMsg.appendMessageString(rdsvalidation.getMessageString());
                            System.out.println("Current Message URAM case3: " + myMsg.getMsgString());
                            break;
                        default:
                           //String tmpMsg = String.format("[%d](%d)%s", updateReceiverAlertMessage_Segment, alerttextSegment, new String(currentTextNotification));
                           //appendTextAndScroll(tmpMsg);
                            myMsg.appendMessageString(rdsvalidation.getMessageString());
                            break;
                   }

               }  else Log.d("updateReceiverAlertMsg",String.format("Received unexpected AlertText:%s",new String(data)));
           
           } else {
               // We have a 9 byte header, indicating this is the first chunk of the text segment.
               updateReceiverAlertMessage_Segment = data[8];
               // Is this a repeated segment or a new segment?
               if (updateReceiverAlertMessage_Segment != updateReceiverAlertMessage_SegmentPrevious) {
                   // It *may* be a new segment, but we are suspicious.
                   // Sanity check: Look at the number of times the message has been repeated; we were promised 3 repeats.
                   if ((messageRepeatCount == 2) || (messageRepeatCount == 1) ) {
                       // If we find only 2 (or less likely, 1) repeats AND the Segment number has already changed, we need to print ASAP!
                       myMsg.appendMessageString(rdsvalidation.getMessageString());
                       Log.e("updateReceiverAlertMsg",String.format("Message Repeat count was: %d for Segment %d Confidence Score: %.1f%%",
                               messageRepeatCount,
                               updateReceiverAlertMessage_SegmentPrevious, 
                               rdsvalidation.getValidityScore()));
                   }
                   messageRepeatCount = 1;
                   updateReceiverAlertMessage_SegmentPrevious = updateReceiverAlertMessage_Segment;
               } else {
                   // Repeated message segment
                   messageRepeatCount++;
               }
               // This code is common to both new and repeated segments.
               initialMessageLen = data.length - 9;
               Arrays.fill(currentTextNotification, (byte) 0x20);
               System.arraycopy(data, 9, currentTextNotification, 0, initialMessageLen);
               expectingMoreAlertText = true;
               
/*                 Log.d("updateReceiverAlertMessage",
                    String.format("HEADER alerttextCount:%d alerttextSegment:%d reportedtextSegment:%d initialMessageLen:%d\ndata:%s", 
                            alerttextCount,
                            alerttextSegment,
                            data[8],
                            initialMessageLen, 
                            new String(currentTextNotification)));
*/                            
           }
               
      } catch (Exception ex) {
          Log.e("updateReceiverAlertMsg","ERROR");
          ex.printStackTrace(); //ex.getMessage());
          //mMessage.append("ERROR:" + ex.getMessage()+ "\n");
      }
       Log.d("##Short Message so far", myMsg.ShortMsgtoString());
       msg = myMsg.getMsgString();
       return myMsg;
  
   } // END updateReceiverAlertMessage()
   
   /**
    * Process the parsed EAS Notification data from the ODA 9A Group, 
    * looking for the CAP message codes other short code properties.<br>
    * REFERENCE: NPR Labs' "Modernized Emergency Alerting ODA" document
    * <pre>
    * data[0..1] Message Length (excluding these two bytes) Least significant byte first.
    * data[2..3] Group Application 0x01 0x01
    * data[4] Function NPR-EAS 0x20
    * data[5] Return Notification 0xED. Notification 0xED is important data for immediate use. No request needed.
    * data[6] Message number 0x01..0xFF Only odd numbers
    * data[7] Receiver Mode: 0x06 = R(B)DS data from EAS group (typically 9A)
    * data[8] Block B - 5 Least Significant Bits only (mask 0x1F). Returned format hh (hex).
    *         It corresponds to the Address Code of the ODA.
    * data[9..10] Block C, MSB first: format hhhh (hex)
    * data[11..12]Block D, MSB first: format hhhh (hex)
    * EXAMPLE:
    * Block B       Block C         Block D
    * data[8]       data[9,10]      data[11,12]
    * data[13]      data[14,15]     data[16,17]
    * data[18]      data[19,20]     data[21,22]
    * data[23]      data[24,25]     data[26,27]
    * data[28]      data[29,30]     data[31,32]
    * data[33]      data[34,35]     data[36,37]
    * data[38]      data[39,40]     data[41,42]
    * data[43]      data[44,45]     data[46,47]
    * data[48]      data[49,50]     data[51,52]
    * data[53]      data[54,55]     data[56,57]
    * ..
    * ..  (repeat Block B, C, D sequence)
    * ..
    * data[58] Block B - 5 Least Significant Bits only (mask 0x1F). Returned format hh (hex).
    *              It corresponds to the Address Code of the ODA.
    * data[59..60] Block C, MSB first: format hhhh (hex)
    * data[61..62] Block D, MSB first: format hhhh (hex)
    *              Maximum 11 groups in one message.
    * data[63] Terminating byte 0x00
    * </pre>
    * @param data A byte array containing RDS EAS Notification data from the NPR Labs FM RDS Receiver.
    */
   public MessageImpl updateReceiverEASData(byte[] data, MessageImpl myMsg) {
       int m = data.length - 1;
       int msbC, lsbC, msbD, lsbD;
       
       // Get every returned ODA Address and parse for EAS Alert Message Shortcodes.
       // Addresses are every 5th byte starting at byte 8.
       for (int n = 8; n < m; n = n + 5) {
           //DEBUG, examining the bytes and their characters
           //Log.d(String.format("AlertTextSegment:%d  Address:%d", alerttextSegment,data[n]),
           //        String.format("%02X %02X %02X %02X %s",msbC,lsbC,msbD,lsbD,msbC)); 

           // Addresses 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 comprise a 64 character
           //            0   4   8  12  16  20  24  28  32  36  40  44  48  52  56  60
           //           byte index = (addr - 16) * 4
           // text segment when alert text is transmitted within the ODA.
           // Address start of an Alert text segment
           
           // Convert the addresses's four associated C and D Block bytes into unsigned int. 
           msbC = (int)data[n+1] & 0xFF;
           lsbC = (int)data[n+2] & 0xFF;
           msbD = (int)data[n+3] & 0xFF;
           lsbD = (int)data[n+4] & 0xFF;

           switch (data[n]){
               
               case 0: //region ODA Address 0: Message Type, Message ID, Message Digest
                   // Message Type: Block C MSB, mask 0x70, shift right 4
                   // Message ID: Block C MSB, mask 0x0F plus Block C LSB 
                   int msgType = (msbC & 0x70) >>> 4;
                   msgID = ((msbC & 0x0F) << 8) + lsbC;
                   msgDigest = (msbD << 8) + lsbD;
                   Log.d("MESSAGE ID-DIGEST",String.format("Message Type: %d Message ID: %d  Message Digest: %d", msgType, msgID,msgDigest));
                   
                   myMsg.setType(typeMessage.get(msgType));
//####                   myMsg.setEndMsg(false);
                   break;
                   //endregion
                   
               case 1:  //region ODA Address 1: Event Duration, Message Origination UTC time
                   // Event Duration DAYS Block C MSB, mask 0x78, shift right 3
                   // Event Duration HOURS Block C MSB, mask 0x07, shift left 2 + Block C LSB, mask 0xC0, shift right 6
                   // Event Duration Quarters: Block C LSB, mask 0x30, shift right 4
                   // Message Origination Ordinal Day: Block C LSB, mask 0x0F, shift left 5 + Block D MSB, mask 0xF8, shift right 3
                   // Message Origination Hour: Block D MSB, mask 0x07, shift left 2 + Block D LSB, mask 0xC0, shift right 2
                   // Message Origination Minute: Block D LSB, mask 0x3F 
                   int evtDurDays = (msbC & 0x78) >>> 3;
                   int evtDurHours = ((msbC & 0x07) << 2) + ((lsbC & 0xC0) >>> 6);
                   // Event duration is number of quarter hours (0-3), so multiply by 15 to get "minutes"
                   int evtDurMin = ((lsbC & 0x30) >> 4) * 15; 
                   
                   int msgOrgDay = ((lsbC & 0x0F) << 5) + ((msbD & 0xF8) >>> 3);
                   int msgOrgHour = ((msbD & 0x07) << 2) + ((lsbD & 0xC0) >>> 2);
                   int msgOrgMin = (lsbD & 0x3F);
                   /*
                       // Debug logging for sanity.
                       Log.d("Event Duration,DAYS:",String.format("%d",evtDurDays));
                       Log.d("Event Duration,HOURS:",String.format("%d",evtDurHours));
                       Log.d("Event Duration,QUARTERS:",String.format("%d",evtDurMin));
                       Log.d("Message Origination, Ordinal Day:",String.format("%d",msgOrgDay));
                       Log.d("Message Origination, Hour:",String.format("%d",msgOrgHour));
                       Log.d("Message Origination, Minute:",String.format("%d",msgOrgMin));
                  */
                   myMsg.setMsgOrgTime(msgOrgDay, msgOrgHour, msgOrgMin);
                   myMsg.setMsgDuration(evtDurDays, evtDurHours, evtDurMin);
                   break;
                   //endregion 
                   
               case 2: //region ODA Address 2: Alarm bit, Short Codes, Alert Text Count and Segment number
                   // Alarm bit: Block C MSB, mask 0x80 (not used and removed from specification)
                   // Certainty: Block C MSB, mask 0x38, shift bits right 3
                   // Severity: Block C MSB, mask 0x07
                   // Urgency: Block C LSB, mask 0xE0, shift bits right 5
                   // Category: Block C LSB, mask 0x1F
                   // Response: Block D MSB, mask 0xF0, shift bits right 4
                   // Text Count: Block D MSB, mask 0x0F, shift left 2 + Block D LSB, mask 0xC0, shift left 6
                   // Text Segment: Block D LSB mask 0x3F
                   int shortcodeCertainty = (msbC & 0x38) >>> 3;
                   int shortcodeSeverity = msbC & 0x07;
                   int shortcodeUrgency = (lsbC & 0xE0) >>> 5;
                   int shortcodeCategory = lsbC & 0x1F;
                   int shortcodeResponse = (msbD & 0xF0) >>> 4;
                   // Total number of text segments
                   alerttextCount = ((msbD & 0x0F) << 2) + ((lsbD & 0xC0) >>> 6);
                   // Current text segment being transmitted.
                   alerttextSegment = lsbD & 0x3F;
                  
                   
//                       NipperOneAndroid.mEASCodesCertainty.setText(codeCertainty.get(shortcodeCertainty));
//                       NipperOneAndroid.mEASCodesSeverity.setText(codeSeverity.get(shortcodeSeverity));
//                       NipperOneAndroid.mEASCodesUrgency.setText(codeUrgency.get(shortcodeUrgency));
//                       NipperOneAndroid.mEASCodesCategory.setText(codeCategory.get(shortcodeCategory));
//                       NipperOneAndroid.mEASCodesResponse.setText("Take Action: " + codeResponse.get(shortcodeResponse));
                   
                   //Log.d("ALERT TEXT COUNT:",String.format("%d", alerttextCount));
                   //Log.d("ALERT TEXT CURRENT SEGMENT:",String.format("%d", alerttextSegment));
                   
                   // Mechanism to determine when the message text is starting.
                   // We look for the receiver to sent two successive alerttextSegment == 0 notifications.
                   // The alert text starts on the 2nd notification.
                   if (alerttextSegment == 0) {
                       if (prepareAlertTextSegment)  {                   
                           prepareAlertTextSegment = false;                           
                           if (alertMessageText != null) Log.d("FULL ALERT MESSAGE",alertMessageText.toString());
                           String tmpAlertCodes = "Certainty: " + codeCertainty.get(shortcodeCertainty) +
                                                "\n Severity: " + codeSeverity.get(shortcodeSeverity) +
                                                "\n  Urgency: " + codeUrgency.get(shortcodeUrgency) +
                                                "\n Category: " + codeCategory.get(shortcodeCategory) +
                                                "\nThe Recommended Action you should take: " + codeResponse.get(shortcodeResponse) + "\n\n";
                           myMsg.setMsgCertainty(codeCertainty.get((shortcodeCertainty)));
                           myMsg.setMsgSeverity(codeSeverity.get(shortcodeSeverity));
                           myMsg.setMsgUrgency(codeUrgency.get(shortcodeUrgency));
                           myMsg.setMsgCategory(codeCategory.get(shortcodeCategory));
                           myMsg.setMsgAction(codeResponse.get(shortcodeResponse));
                                            
                       } else prepareAlertTextSegment = true;
                   } 
                   break;
               //endregion
                   
               case 3: //region ODA Address 3:  Originator and 3 char Event Code
                   
                   // Originator code: Block C MSB, mask 0x70, shift right 4
                   // Event Code 1st char: Block C LSB
                   // Event Code 2nd char: Block D MSB
                   // Event Code 3rd char: Block D LSB
                   int originatorCode = (msbC & 0x70) >>> 4;
                   myMsg.setMsgOriginator(codeOrg.get(originatorCode));
                   
                   StringBuilder evtCode = new StringBuilder();
                   evtCode.append((char)lsbC).append((char)msbD).append((char)lsbD);
                   Log.d("EventCode",evtCode.toString());
                   myMsg.setEvent(codeEvent.get(evtCode.toString()));
                   break;
               //endregion
               
               // Address 4 = Geo Code ID
                   
               // Address 5 = Geo Code X
                   
               // Address 6 = Geo Code Y
                   
               // Address 7 = Alternate Frequency Switching
                                  
               case 8: //region ODA Address 8: First 4 chars of the 8 char Alert Originating Agency
                   break;
              //endregion
                   
               case 9: //region ODA Address 9: Last 4 chars of the 8 char Alert Originating Agency
                   break;
               //endregion
                   
               // Addresses 10, 11, 12, 13, 14, 15 are unassigned in this ODA
               
               default: 
                   if (!disableupdateReceiverAlertMessage) break;
                   // Alert Text Segment (address 16 to 31). These are the sixteen 64-character text segments
                   // that convey the alert message.
                   if (data[n] > 15) {
        /*               Log.d(String.format("AlertTextSegment:%d  Address:%d", alerttextSegment,data[n]),
                           String.format("%s", new String(alertMessageTextTemp)));
                           //String.format("%02X %02X %02X %02X %s",msbC,lsbC,msbD,lsbD,msbC)); 
        */
                   
                       // This *may* mark a new segment, but we are suspicious.
                       // Sanity check: Look at the number of times the message has been repeated; we were promised 3 repeats.
                       if (data[n] == 16){
                           if (alerttextSegmentPrevious != alerttextSegment) {
                               if ((messageRepeatCount == 2) || (messageRepeatCount == 1) ) {
                                   // If we find only 2 repeats (or less likely, just 1 repeat) AND the Segment number has already changed, we need to print ASAP!
                                   myMsg.appendMessageString(rdsvalidation.getMessageString());
                                   //myMsg.setEndMsg(true);
                                   Arrays.fill(alertMessageTextTemp, (byte) 0x20);
                                   Log.e("updateReceivereasdATA",String.format("Message Repeat count was: %d for Segment %d Confidence Score: %.1f%%",
                                   messageRepeatCount,
                                   alerttextSegmentPrevious, 
                                   rdsvalidation.getValidityScore()));
                               }
                               messageRepeatCount = 1;
                               alerttextSegmentPrevious = alerttextSegment;
                           } else {
                               // This is a repeated message segment
                               messageRepeatCount++;
                           }                           
                       } 
               
                       // Copy the 4 bytes following data[n], filter them for characters 0-127, 
                       // replace instances of 0x0D with 0x0A, replace 0x00 with a printable char.
                       // Wait until we've completed the loop through data[] before writing to the screen.
                       // Point to the start of the data
                       int p = n + 1;
                       int index = (data[n] - 16) * 4;
                       for (int z=0; z < 4; z++){
                           //if (data[p] == 0x0D) data[p] = 0x0A;
                           //if (data[p] == 0x00) data[p] = '!'; //0x20;
                           alertMessageTextTemp[index] = (byte) (data[p] & 0x7F);                           
                           index++;
                           p++;
                       }
                       // If this is the last segment address, prepare for either processing or printing to the screen.
                       // Message repeats are sent to the rdsvalidation class for error-reduction.
                       if (data[n] == 31) {
                           switch (messageRepeatCount) {
                               case 1:
                                   rdsvalidation = new rdsValidation(alertMessageTextTemp);
                                   break;
                               case 2:
                                   rdsvalidation.rdsSubmitMessageRepeat(alertMessageTextTemp);
                                   break;
                               case 3:
                                   rdsvalidation.rdsSubmitMessageRepeat(alertMessageTextTemp);
                                   myMsg.appendMessageString("@");
                                   //System.out.println("##### current message in Receiver: " + myMsg.getMsgString());
                                   //myMsg.setEndMsg(true);
                                   //Arrays.fill(alertMessageTextTemp, (byte) 0x20);
                                   Log.d("updateReceivereasdata",String.format("Segment %d Confidence Score: %.1f%%",
                                           alerttextSegment, 
                                           rdsvalidation.getValidityScore())); 
                                   break;
                               default:
                                   break;                   
                           }
                       }
                   } // end if data[n]>15
                   break;
           } // end switch
       }// end for loop
       return myMsg;
   } // END updateReceiverEASData()
      
    
/// -----| This section sends requests to the receiver for data |--------------  
   
   /**
    * Sends the API command to request the receiver's firmware version.
    * The command is 0x04, mode 0x08.
    * 
    * @param sDriver The USB Serial Driver interfacing the receiver
    */
   public void requestReceiverVersion(UsbSerialDriver sDriver){
       if (sDriver != null)
        try {
            sDriver.write(new byte[]
                   {0x06,0x00, // Message Length  
                    0x01,0x01, // Group ID: System
                    0x20,      // Function ID: NPR-EAS
                    0x04,      // Command ID: Get 
                    0x02,      // Message number (even)
                    0x08},     // Mode: Get firmware version number
                    100);      // Timeout delay for writing to receiver (ms)
        } catch (IOException e) {
           Log.e("Fail requesting Rcv Vsn", e.getMessage());
        }
    }
   
   /**
    * Sends the API command to request the receiver's configuration block.
    * The command is 0x04, mode 0x08, configuration block 0.
    * 
    * @param sDriver The USB Serial Driver interfacing the receiver
    */
   public void requestReceiverConfiguration(UsbSerialDriver sDriver){
       // Clear validity because we're getting a new configuration from the receiver
       configValue[19] = 0;
       // If possible, request a new configuration to be returned.
       // Command 4, Mode 0, Block 0
       if (sDriver != null)
        try {
            sDriver.write(new byte[]
                   {0x07,0x00, // Message Length
                    0x01,0x01, // Group ID: System
                    0x20,      // Function ID: NPR-EAS
                    0x04,      // Command ID: Get
                    0x02,      // Message number (even)
                    0x00,      // Mode: Get configuration block
                    0x00},     // Configuration Block 0
                    100);      // Timeout delay for writing to receiver (ms)
        } catch (IOException e) {
            Log.e("Fail request Rcv Config", e.getMessage());
        } 
           
    }

   
/// -----| This section manages incoming 'requested' data from the receiver |--  
   
   /**
    * Writes the received Receiver Firmware Version number to versionNipperOneReceiver.
    * @param mydata Byte Array returned by the receiver
 * @return 
    */
   public String receiveReceiverVersion(byte[] mydata){
       Log.d("Receive Receiver Vsn", Byte.toString(mydata[8]) + "." + Byte.toString(mydata[9]));
        return (Byte.toString(mydata[8]) + "." + Byte.toString(mydata[9]));
    }

   /** 

    * Writes the received Configuration byte array into public Int array configValue[].
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
    * 11 - Tuning Grid [1 - 200kHz; 0 - 100 kHz]
    * 12 - Volume [0..63]
    * 13 - FIPS portion code [0..9]
    * 14 - FIPS state code [0..99] 
    * 15 - FIPS county code [0..999] 
    * 16 - Version [Major]
    * 17 - Version [Minor]
    * 18 - Beacon wait time[ms] 
    * 19 - Validity for other functions to use: 0 = Request in process, don't use. 1 = Valid configuration array. 
    * 
    * @param mydata Byte Array returned by the receiver
    */
   public void receiveReceiverConfiguration(byte[] mydata, Context ParentContext){
       try {
           // Byte [8] is the start of the block
           int n = 8;
           int m = 0; 
    
           // Config Block number
           configValue[m++] = mydata[n++] & 0xFF;
           // Size of the configuration block, in bytes
           configValue[m++]=  mydata[n++] & 0xFF;
           
           // Scan intervals,RF Timeout,USB Timeout,Beacon Timeout, Alert & Alarm Timeouts,Snooze timeout
           for (n = 10; n < 25; n = n + 2){
               configValue[m++] = ((mydata[n] & 0xFF) << 8) | (mydata[n + 1] & 0xFF);
           }
           // AllowRemote,TuningGrid,Volume, FIPS portion code
           for (n = 26; n < 30 ; n++){
               configValue[m++] =  mydata[n] & 0xFF;
           }
           // FIPS state and county code
           for (n = 30; n < 33; n = n + 2){
               configValue[m++] = ((mydata[n] & 0xFF) << 8) | (mydata[n + 1] & 0xFF);
           }
           // Version, major
           configValue[m++] = mydata[n++] & 0xFF;
           // Version, minor
           configValue[m++]=  mydata[n++] & 0xFF;
           
           // Beacon wait time
           configValue[m++] = ((mydata[n] & 0xFF) << 8) | (mydata[n + 1] & 0xFF);
    
           // Indicate the public Int array configValue[] is valid
           configValue[19] = 1;
           
           // Immediately write the array to the parentActivity pref file, "NipperOneAlerter.xml" 
           SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(ParentContext); //parentActivity.getPreferences(Context.MODE_PRIVATE);
           SharedPreferences.Editor prefEditor = prefs.edit();
           prefEditor.putInt("key_ConfigBlock", configValue[0]);
           prefEditor.putInt("key_ScanInterval_2Hz", configValue[2]);
           prefEditor.putInt("key_ScanInterval_05Hz", configValue[3]);
           prefEditor.putInt("key_RFtimeOut", configValue[4]);
           prefEditor.putInt("key_USBtimeOut", configValue[5]);
           prefEditor.putInt("key_BeaconTimeOut", configValue[6]);
           prefEditor.putInt("key_AlertTimeOut", configValue[7]);
           prefEditor.putInt("key_AlarmTimeOut", configValue[8]);
           prefEditor.putInt("key_SnoozeTimeOut", configValue[9]);
           prefEditor.putInt("key_AllowRemote", configValue[10]);
           prefEditor.putInt("key_TuningGrid", configValue[11]);
           prefEditor.putInt("key_Volume", configValue[12]);
           prefEditor.putInt("key_FIPSPortionCode", configValue[13]);
           prefEditor.putInt("key_FIPSStateCode", configValue[14]);
           prefEditor.putInt("key_FIPSCountyCode", configValue[15]);
           prefEditor.putInt("key_VersionMajor", configValue[16]);
           prefEditor.putInt("key_VersionMinor", configValue[17]);
           prefEditor.putInt("key_BeaconWaitTime", configValue[18]);
           prefEditor.putInt("key_ValidReceiverConfig", configValue[19]);
           
           String fips = String.format("%d%02d%03d", configValue[13], configValue[14], configValue[15]); //String.valueOf(API.configValue[13]) + String.valueOf(API.configValue[14]) + String.valueOf(API.configValue[15]);
           prefEditor.putString("key_FIPS", fips);
           prefEditor.apply();
           
           Log.d("FIPS from config file:",prefs.getString("key_FIPS", "<Nothing>"));
           
           
           // DEBUG dump the contents of both arrays so we can make sure the receiver has changed its configuration.
           /* 
           
           for (n=0; n < 20; n++) {
               Log.d("configValue[]",String.format("%d  %d   0x%X", n, configValue[n], configValue[n]));               
           }
           for (n=0; n < 38; n++) {
               int poop = mydata[n] & 0xFF;
               Log.d("mydata[]",String.format("%d  %d   0x%X", n, poop,poop)); 
           }
           
           */
               
           //Log.d("-----| Configuration block |-----",String.format("Message Size: %d, Configuration Block Size: %d bytes (%d bytes).",mydata[0],configValue[1],mydata[9]));
           
       } catch (Exception ex) {
           Log.e("receiveReceiverConfig", ex.getMessage());
           configValue[19] = 0;
       } 
 
    } // END receiveReceiverConfiguration()
      
   
//    /**
//     * Receives 64 byte blocks of Stored Messages, marshals them and prepares them for eventual display.
//     * @param mydata Byte Array returned by the NipperOne receiver
//     */
//   public void receiveStoredMessages(byte[] mydata){
//        try {
//            if (outputStream == null) {
//                outputStream = new ByteArrayOutputStream();
//                Log.d("New StoredMessageStream Header",com.hoho.android.usbserial.util.HexDump.toHexString(mydata));
//            }
//            outputStream.write(mydata);
//        } catch (IOException e) {
//            Log.e("receiveStoredMessages",e.getMessage());
//        }
//    }
//    
//   /**
//    * The dump of stored messages is completed, now process and display them
//    * @TODO  Finish this.
//    */
//   public void receiveStoredMessagesCompleted() {
//        rawStoredMessages = outputStream.toByteArray();
//        try {
//            outputStream.close();
//            outputStream = null;
//            String poop = com.hoho.android.usbserial.util.HexDump.dumpHexString(rawStoredMessages);
//            //appendTextAndScroll(com.hoho.android.usbserial.util.HexDump.dumpHexString(rawStoredMessages));    //.toHexString(mydata));//
//            Log.d("receiveStoredMessagesCompleted", poop);
//            Log.d("receiveStoredMessagesCompleted: rawStoredMessages.length = ", Integer.toString(rawStoredMessages.length));
//        } catch (IOException e) {
//            Log.e("receiveStoredMessagesProcess",e.getMessage());
//        }
//        
//        // Sanity Checks for validation (based on Joop's Delphi code for EAS.exe)
//        
//        // Compare the number of elements in the array with the number of bytes in the API header (add 2 to count the header's size bytes).
//        // If no match, we don't have all the bytes and the array is suspect.
//        int nTmp = ((rawStoredMessages[1] << 8) + rawStoredMessages[0]) + 2;       
//        if (nTmp != rawStoredMessages.length) {
//            Log.e("rawStoredMessages", "Validity size check of StoredMessages did not pass.");
//            rawStoredMessages = null;       
//        } else {         
//            Log.i("rawStoredMessages", "Validity size check of StoredMessages passed.");
//            // Create a map of where each message starts.
//            // Validity test is search for 0x80 byte, then check the following byte for message's number, range 0 to 6 inclusive (see API specification)
//            // Initialize the StoredMessageMap
//            StoredMessageMap = new int[]{0,0,0,0,0,0,0};
//            for (int n = 0; n < rawStoredMessages.length; n++) {
//                int bTmp = 0;
//                if (rawStoredMessages[n] == -128) {
//                    nTmp = rawStoredMessages[n + 1];
//                    if ((nTmp > -1) && (nTmp < 6)) {
//                        // Store the array index marking validity byte of a valid stored message
//                        StoredMessageMap[bTmp] = n;
//                        bTmp++;
//                        Log.d("----FOUND 0x80---",String.format("Found at rawStoredMessages[%d] for message number %d", n,rawStoredMessages[n+1]));
//                    }
//                }
//            }
//            // Mapping complete. StoredMessageMap[] points to validity bytes of (up to 7) messages in rawStoredMessages[].
//            // @TODO Complete the mapping and prepare the display.
//            for (int n = 0; n < 7; n++){
//                if (StoredMessageMap[n] > 0) {
//                    // @TODO process each valid stored message into string array StoredMessages[]
//                }
//            }
//            
//            
//        }
//    }
 
/// -----| This section writes configuration data to the receiver |------------
   
   /**
    * Write a configuration to the NipperOne receiver  FOR TESTING
    * <pre>
    * data[0..1] Message Length (excluding these two bytes) Least significant byte first.
    * data[2..3] Group Application 0x01 0x01
    * data[4] Function NPR-EAS 0x20
    * data[5] Command Set 0x03
    * data[6] Message number 0x02..0xFE Only even numbers
    * data[7] 0x00 Receiver Mode: 00 = Write configuration block
    * data[8] Number of configuration block, currently only block 0 is supported.
    * data[9] Size of configuration block
    * data[10..11] Blink period [ms]: fast mode: default 500 (MSB first)
    * data[12..13] Blink period [ms]: slow mode: default 2000 (MSB first)
    * data[14..15] RF time out [sec]: default 20 (MSB first)
    * data[16..17] USB time out [sec]: default 10 (MSB first)
    * data[18..19] Beacon time out [sec]: default 5 (MSB first)
    * data[20..21] Alert time out [sec]: default 30 (MSB first)
    * data[22..23] Alarm time out [sec]: default 30 (MSB first)
    * data[24..25] User SNOOZE time out [sec]: default 180 (MSB first)
    * data[26] 0..1 Non-target messages: 0=Deny; 1=Allow; default 0
    * data[27] 0..1 Tuning grid: 0=100 kHz; 1=200 kHz; default 1
    * data[28] 0..0x3F Volume: [0..64]; default 40
    * data[29] FIPS portion code; default 0 [0...9]
    * data[30..31] FIPS state code; default 0, [0...99] (MSB first)
    * data[32..33] FIPS county code; default 0 [0...999] (MSB first)
    * data[34..35]Beacon wait time [ms]: default 4000 (MSB first)
    * </pre>
    * @param sDriver
    */
   public void writeReceiverConfiguration(UsbSerialDriver sDriver) {
        try {
            if (sDriver != null) {
                sDriver.write(new byte[]
                    {  34,0x00, // Message Length is 34 bytes
                     0x01,0x01, // group application
                     0x20,      // NPR-EAS
                     0x03,      // Command Set 3
                     0x06,      // Message number 2...0xFE only even numbers
                     0x00,      // receiver mode 00 (write config block)
                     0x00,      // write config block 0
                     26,        // size of configuration block; 26 bytes after this one.
                     0x03,0x18, // 1,000
                     0x0F,0x60, // 4,000
                     0x00,0x14, // 20
                     0x00,0x0A, // 10
                     0x00,0x05, // 05
                     0x00,0x0A, // 10 Alert
                     0x00,0x0A, // 10 Alarm
                     0x00,0x0A, // 10 Snooze
                     0x00,      // deny
                     0x00,      // 100Khz
                     0x28,      // 40 volume
                     0x00,      // portion
                     0x00,0x27, // Ohio: 39
                     0x00,0x09, // Athens County: 009
                     0x0F,0x60  // 4,000
                     },100);
                    
                    //Thread.sleep(10);
                    Log.d("Write to Receiver","Wrote to receiver");
            } else {
                Log.d("FAIL: Write to Receiver","mSerialIoManager was null.");
            }
            //Thread.sleep(250);
        } catch (Exception e) {
            Log.e("Error: config 2 Rcver",e.getMessage());
        }
    }
    
    /**
    * Write a FIPS code to the receiver's non-volatile memory.<br>
    * The byte format for writing this command to the receiver is the following:<br>
    * <pre>
    * 0x0B,0x00, // Length of this message (11 bytes)
    * 0x01,0x01, // group application
    * 0x20,      // NPR-EAS
    * 0x03,      // Command Set 3
    * 0x04,      // Message number (even)
    * 0x05,      // Mode: Set Geo Location
    * 0x00,      // FIPS portion code
    * 0x00,0x27, // Ohio: 39
    * 0x00,0x09, // Athens County: 009
    * </pre>
    * @param sDriver The USB Serial Driver interfacing the receiver 
    * @param sFIPS A 6 character string of the FIPS code to write to the receiver 
    */
   public void writeReceiverConfigurationFIPS(UsbSerialDriver sDriver, String sFIPS) {
        try {
            if (sDriver != null){
                // 009233
                byte portion = Byte.valueOf(sFIPS.substring(0,1));
                byte statecode = Byte.valueOf(sFIPS.substring(1,3));
                int countycode = Integer.valueOf(sFIPS.substring(3,6));
                byte countycode_msb = (byte) ((countycode >> 8) & 0xFF);
                byte countycode_lsb = (byte) (countycode & 0xFF);
                   sDriver.write(new byte[] 
                    {0x0B,0x00,
                     0x01,0x01, 
                     0x20,    
                     0x03,    
                     0x04,      
                     0x05,     
                     portion,      
                     0x00,statecode, 
                     countycode_msb,countycode_lsb}, 100);
                     Log.d("Write to receiver","Wrote FIPS to receiver");
            } else {
                Log.d("FAIL:Write2Recver: FIPS","mSerialIoManager was null");
                
            }
            //Thread.sleep(250);
        } catch (Exception e) {
            Log.e("ErrorWriteFIPS2Receiver", e.getMessage());
        }
        
    }
    
   /**
    * Write the default configuration to the receiver.<br>
    * The default is defined in the Catena Radio Design API Documentation.<br>
    * REVISION:20140516 Default Channel wait time is 6000 (see below)<br>
    * Joop Beunders says the desired Channel wait time must be doubled to get the desired value (A Feature? Not!). 
    * @param sDriver The USB Serial Driver interfacing the receiver 
    */
   public void writeReceiverConfigurationDefault(UsbSerialDriver sDriver) {
        try {
            if (sDriver != null) {
                sDriver.write(new byte[]
                    {  34,0x00,       // Message Length is 34 bytes
                     0x01,0x01,       // group application
                     0x20,            // NPR-EAS
                     0x03,            // Command Set 3
                     0x02,            // Message number 2...0xFE only even numbers
                     0x00,            // receiver mode 00 (write config block)
                     0x00,            // write config block 0
                     26,              // size of configuration block; 26 bytes after this one.
                     0x01,(byte)(0xF4 & 0xFF), // Blink period [ms]: fast mode: default 500
                     0x07,(byte)(0xD0 & 0xFF), // Blink period [ms]: slow mode: default 2000
                     0x00,0x19,       // RF time out [sec]: default 25
                     0x00,0x0F,       // USB time out [sec]: default 15
                     0x00,0x19,       // Beacon time out [sec]: default 25
                     0x00,0x1E,       // Alert time out [sec]: default 30
                     0x00,0x1E,       // Alarm time out [sec]: default 30
                     0x00,(byte)(0xB4 & 0xFF), // User SNOOZE time out [sec]: default 180
                     0x00,            // Non-target messages: 0-Deny; 1-Allow; default 0
                     0x01,            // Tuning grid: 0-100 kHz; 1-200 kHz; default 1
                     0x28,            // Volume: 0..64; default 40
                     0x00,            // FIPS portion code; default 0
                     0x00,0x00,       // FIPS state code; default 0  
                     0x00,0x00,       // FIPS county code; default 0 
                     (byte)(0x2E & 0xFF),(byte)(0xE0 & 0xFF)  // Channel wait time [ms]: default 6000  should be 0x1770, BUG: must be doubled. 
                     }, 100);
                    Thread.sleep(10);
            Log.d("Write to Receiver","Wrote DEFAULT to receiver");
            requestReceiverConfiguration(sDriver);
            } else {
                Log.d("FAIL 2 Write 2 Receiver","mSerialIoManager was null");
            }
            
        } catch (Exception e) {
            Log.e("Error writing 2Receiver",e.getMessage());
        }
        
    }
   
   public void resetAlertStructure() {
       expectingMoreAlertText = false;
       alerttextSegmentPrevious = 0;
       initialMessageLen = 0;
       updateReceiverAlertMessage_SegmentPrevious = 0;
       messageRepeatCount = 0;
       Arrays.fill(currentTextNotification, (byte) 0x20);
       Arrays.fill(alertMessageTextTemp, (byte) 0x20);
    }

   public boolean get_expectingMoreAlertText(){
       return expectingMoreAlertText;
   }
   
   /**
    * Given a five byte array nnnnn, displays nnn.n or nn.n to mStationFreq
    * 10570 is displayed as 105.7  09130 is displayed as 91.xx
    * @param freq Byte array containing the frequency.
    */
   public String displayFrequency(byte[] freq){
       final byte[] bytefreq = new byte[5]; // output: { '1','0','5','.','7' };
       bytefreq[0] =  (freq[0] != '0') ? freq[0] :  0x20 ;
       bytefreq[1] = freq[1];
       bytefreq[2] = freq[2];
       bytefreq[3] = '.';
       bytefreq[4] = freq[3];
       return new String(bytefreq);
    }
    
   /**
    * Converts the array value to a signal strength level and writes a
    * number of "signal level bars" in mSignalLevel.
    * 
    * @param siglvl A 3 byte ASCII character array in the range of " 00" to "100"
    */
   public String displaySignalLevel(byte[] siglvl){
       // ASCII zero is 48, so subtract to get the real value. We ignore the hundreds digit.
       int signalStrength = (((siglvl[1] - 48) * 10 + (siglvl[2] - 48)));
       StringBuilder sSigStrength = new StringBuilder(signalStrength);
       sSigStrength.append(signalStrength + " ");
       if (signalStrength > 0) {
           // Convert to a percentage, rounding if necessary
           signalStrength = (int)Math.round(signalStrength / 10.0);
           // Append as many bars as necessary to the string
           
           for ( int n = 0 ; n < signalStrength ; n++ ) {
               // @TODO Figure out how to express ASCII 221 (the "bar"). 
               sSigStrength.append("▌");
               //sSigStrength.append(""); 
           }
           //Log.d("SIGNALSTRENGTH: ", Integer.toString(signalStrength));
           return sSigStrength.toString();
           
       } else {
           return "";
       }
    }
    public String getMsgString(){
        return msg;
    }
   
} // END API Class

//==========================| End of API.java |===================================================
