package org.nprlabs.nipperone.main;

/**
 * Created by kbrudos on 7/16/2015.
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import java.util.Vector;

/**
 * class that handles the communication between the client and the service.
 * contains a message buffer, which "collects" the sent messages
 * so that when messages are sent while the client is in
 * another activity then the messages are not missed/discarded and an error thrown.
 *
 * Please see the following for the original author (quickdraw mcgraw) of this class on Stack overflow
 * http://stackoverflow.com/a/8122789/5111318
 */
public class PauseHandler extends Handler {

    //message queue buffer
    final Vector<Message> messageQueueBuffer = new Vector<Message>();

    //Flag indicating the pause state
    private boolean paused;

    //resume the handler
    final public void resume(){
        paused = false;

        while(messageQueueBuffer.size() > 0 ){
            final Message msg = messageQueueBuffer.elementAt(0);
            messageQueueBuffer.remove(0);
            sendMessage(msg);
        }
    }

    /**
     * pause the handler
     */
    public final void pause(){
        paused = true;
    }


    /**
     * Notification that the message is about to be stored as the activity is paused. If
     * not handled the message will be saved and replayed when the activity resumes.
     * @param message
     * @return
     */
    protected boolean storeMessage (Message message){return false;}

    /**
     * Notification message to be processed. This will either be
     * directly from handleMessage of played back from a saved
     * message when the activity was paused.
     * @param message
     */
    protected void processMessage(Message message){handleMessage(message);}

    @Override
    public final void handleMessage(Message msg){

        if(paused){
            if(storeMessage(msg)){
                Message msgCopy = new Message();msgCopy.copyFrom(msg);
                messageQueueBuffer.add(msgCopy);
            }
        }else{
            processMessage(msg);
        }
    }

}
