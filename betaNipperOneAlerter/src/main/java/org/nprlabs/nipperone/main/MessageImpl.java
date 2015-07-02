package org.nprlabs.nipperone.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import org.nprlabs.nipperone.framework.Message;
import org.nprlabs.nipperone.framework.NipperConstants;
import org.prss.nprlabs.nipperonealerter.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


/**
 * 
 * @author Katherine Brudos, NPR Labs intern
 * @version summer 2015
 *
 */
public class MessageImpl implements Message {

    private String TAG = "NipperOneAlerter";
    
    private String Message = "";
    private String Certainty = "Unknown"; 
    private String Severity = "Unknown";
    private String Urgency = "Unknown";
    private String Category = "Unknown";
    private String Action = "Unknown";
    private String evtDurTime = "Unknown";
    private String msgOrgTime = "Unknown";
    private String Event = "Unknown";
    
    private int msgID = 0;
    private int msgDigest = 0;
    private int id = 0;
    
    /**
     * This is the Message Originator aka who 'sent' or deemed it necessary to send the message
     * ex: NWS, NPR, the President, etc. 
     */
    String MsgOrginator = "Unknown";

    private String msgType = "";
    
    private boolean endOfMsg = false;



    //constructor
    public MessageImpl() {
        // TODO Auto-generated constructor stub
    }
    public MessageImpl(String eas_event, String eas_action){
        this.Event = eas_event;
        this.Action = eas_action;
    }
        
    //constructor
    public MessageImpl(int id, String eas_event, String eas_action) {
        this.id = id;
        this.Event = eas_event;
        this.Action = eas_action;
    }

    @Override
    public void Message() {
        // TODO Auto-generated method stub
        
    }



    @Override
    public void Message(String Message, String Certainty, String Severity, String Urgency,
            String Category, String Action, String Event) {
          
        this.Message = Message;
        this.Certainty = Certainty;
        this.Severity = Severity;
        this.Urgency = Urgency;
        this.Category = Category;
        this.Action = Action;
        this.Event = Event;
        
    }


    @Override
    public String getMsgString() {return this.Message;}

    @Override
    public String getMsgCertainty() {return this.Certainty;}

    @Override
    public String getMsgSeverity() {return this.Severity;}

    @Override
    public String getMsgUrgency() {return this.Urgency;}

    @Override
    public String getMsgCategory() {return this.Category;}

    @Override
    public String getMsgAction() {return this.Action;}

    @Override
    public String getMsgOrgTime() {return this.msgOrgTime;}

    @Override
    public String getMsgDuration() {return this.evtDurTime;}
   
    public String getEventString(){return this.Event;}
    
    public String getMsgOriginator(){return this.MsgOrginator;}
    
    public String getMsgType(){return this.msgType;}
    
    
    public void setMsgString(String MessageString) {
        this.Message = MessageString;        
    }

    @Override
    public void setMsgCertainty(String MessageCertainty) {
        this.Certainty = MessageCertainty;        
    }

    @Override
    public void setMsgSeverity(String MessageSeverity) {
        this.Severity = MessageSeverity;        
    }

    @Override
    public void setMsgUrgency(String MessageUrgency) {
        this.Urgency = MessageUrgency;        
    }

    @Override
    public void setMsgCategory(String MessageCatagory) {
        this.Category = MessageCatagory;        
    }

    @Override
    public void setMsgAction(String RequiredAction) {
        this.Action = RequiredAction;
    }


    /**
     * to be used by the receiver class, pass in the individual elements from the data
     * to be formatted by this method and saved as a string
     * @param msgOrgDay
     * @param msgOrgHour
     * @param msgOrgMin
     */
    @Override
    public void setMsgOrgTime(int msgOrgDay, int msgOrgHour, int msgOrgMin) {
        String sDays = "day";
        //if (evtDurDays != 1) sDays = "days";
        msgOrgTime = String.format("%d %s, %d hr, %d min", msgOrgDay,sDays,msgOrgHour,msgOrgMin);        
  
    }

    /**
     * to be used by the databaseHelper class.
     * @param orgTime
     */
    public void setMsgOrgTime(String orgTime){this.msgOrgTime = orgTime;}

    /**
     * to be used by the receiver class
     * @param evtDurDays
     * @param evtDurHours
     * @param evtDurMin
     */
    @Override
    public void setMsgDuration(int evtDurDays, int evtDurHours, int evtDurMin) {
        String sDays = "day";
        if (evtDurDays != 1) sDays = "days";
        evtDurTime = String.format("%d %s, %d hr, %d min", evtDurDays,sDays,evtDurHours,evtDurMin);        
    }

    /**
     * to be used by the databaseHelper class.
     * @param duration
     */
    public void setMsgDuration(String duration){this.evtDurTime = duration;}
    
    public void setEvent(String Event){
        this.Event = Event;
    }
    
    public void setMsgOriginator(String Originator){this.MsgOrginator = Originator;}
    
    public void setType(String type){this.msgType  = type;}
    
    /**
     * A short version of the Alert Message with only the 
     * @return the Current Message formatted for mMessage aka TextView display
     */
    public String ShortMsgtoString(){
        String msg = String.format("ID: %d THIS IS A MESSAGE. The Alert: %s MessageString is: %s", this.id, this.Event,this.Message);
        return msg;
    }
    
    public void appendMessageString(String Message){

        this.Message += Message.replace("\n", " ");
//        this.Message += Message;
    }
    
    
    /**
     * getter and setter for the endOfMsg boolean variable.
     * This variable is to be set in the receiver class(set in the msg variable and to be returned to the 
     * main class to be used there. 
     * @return
     */
    public boolean getEndMsg(){return endOfMsg;}
    public void setEndMsg(boolean bool){this.endOfMsg = bool;}
    
    /**
     * resets all of the variables in the MessageImpl class. 
     * For use when 
     */
    public void resetMessageString(){
        this.Message = "";
    }

    /**the get and set ID methods are for use by the databaseHelper class. They are to get and set
     * the id of the messages to keep track of them.
     */
     public int getId(){
        return this.id;
    }

    /**
     * This method is used by the database helper class. If you use this method to set an id
     * please know that it WILL be over written by the dataBase helper class.
     * @param id
     */
    public void setId(int id){
        this.id = id;
    }
}
