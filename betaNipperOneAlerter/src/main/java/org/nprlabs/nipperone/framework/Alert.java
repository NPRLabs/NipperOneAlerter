package org.nprlabs.nipperone.framework;

public interface Alert {


    void Message();
    void Message(String Message, String Certainty, String Severity, String Urgency, String Category, String Action, String Time);
     
	String getMsgString();
	
	String getMsgCertainty();
	
	String getMsgSeverity();
	
	String getMsgUrgency();
	
	String getMsgCategory();
	
	String getMsgAction();
	
	String getMsgOrgTime();
	String getMsgDuration();
	
	
    public void setMsgString(String MessageString);
    public void setMsgCertainty(String MessageCertainty);
    public void setMsgSeverity(String MessageSeverity);
    public void setMsgUrgency(String MessageUrgency);
    public void setMsgCategory(String MessageCatagory);
    public void setMsgAction(String RequiredAction);
    public void setMsgOrgTime(int msgOrgDay, int msgOrghour, int msgOrgMin);
    public void setMsgDuration(int evtDurDays, int evtDurHours, int evtDurMin);
    //public void setMessageEvent(String 
}
