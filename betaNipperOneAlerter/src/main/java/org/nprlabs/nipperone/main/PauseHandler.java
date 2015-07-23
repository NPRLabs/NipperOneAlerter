package org.nprlabs.nipperone.main;

/**
 * Created by kbrudos on 7/16/2015.
 */

import android.app.Activity;
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
public abstract class PauseHandler extends Handler {

    //message queue buffer
    final Vector<Message> messageQueueBuffer = new Vector<Message>();

    //Flag indicating the pause state
    private boolean paused;

    private Activity activity;

    //resume the handler
    public final synchronized void resume(Activity activity){
        paused = false;
        this.activity = activity;

        while(messageQueueBuffer.size() > 0 ){
            final Message msg = messageQueueBuffer.elementAt(0);
            messageQueueBuffer.remove(0);
            sendMessage(msg);
        }
    }

    /**
     * pause the handler
     */
    public final synchronized void pause(){
        paused = true;
        activity = null;
    }


    /**
     * Notification message to be processed. This will either be
     * directly from handleMessage of played back from a saved
     * message when the activity was paused.
     * @param message
     */
    abstract protected void processMessage(Message message);

    @Override
    public final synchronized void handleMessage(Message msg){

        if(paused){
            final Message msgCopy = new Message();
            msgCopy.copyFrom(msg);
            messageQueueBuffer.add(msgCopy);
        }else{
            processMessage(msg);
        }
    }

}
