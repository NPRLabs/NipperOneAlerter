package org.nprlabs.nipperone.main;

import android.util.Log;

/**
 * rdsValidation<br>
 * The purpose of the rdsValidation class is to provide elementary RDS error correction
 * in alerting messages, when a 64-character segment is received 3 times.
 * It is an implementation based on a error correction algorhythim by Jump2Go's Seth Stroh.
 * @author rrarey
 */
public final class rdsValidation {
    private byte[] dataTemp;
    private byte[] dataActive;
    private byte[] dataValidity;

    /**
     * This constructor is used when we have the first message is ready for processing.
     * @param data A byte array of the received 64-character text segment.
     */
    public rdsValidation(byte[] data) {
        rdsSubmitMessageInitial(data);
    }
    
    /**
     * This constructor is used when we need to instantiate the class <b>before</b> the first message is ready for processing.
     */
    public rdsValidation(){
        // Nothing to do here.
    }
    /**
     * Called first when a new 64-character segment is received; use this when the class
     * has been instantiated using the rdsValidation() class constructor, or when you need to
     * reinitialize the class variables.
     * @param data A byte array of the received 64-character text segment.
     */
    public void rdsSubmitMessageInitial(byte[] data){
        dataTemp = new byte[data.length];
        dataActive = new byte[data.length];
        dataValidity = new byte[data.length];
        System.arraycopy(data, 0, dataTemp, 0, data.length);
        System.arraycopy(data, 0, dataActive, 0, data.length);
        java.util.Arrays.fill(dataValidity, (byte) 1);
    }
    
    /**
     * Called on successive repeats of a 64-character text segment.
     * @param data A byte array of the received 64-character text segment.
     */
    public void rdsSubmitMessageRepeat(byte[] data){
        // Sanity Check:
        if (data.length > dataActive.length ) return;
        for (int n = 0; n < data.length; n++) {
            if (data[n] == dataActive[n]){
                dataTemp[n]=data[n];
                dataValidity[n]++;
            } else if (data[n] == dataTemp[n]) {
                dataActive[n]=data[n];
                dataValidity[n] = 2;
            } else {
                dataTemp[n]=data[n];
            }
        }
    }

    /**
     * Returns the 'corrected' 64-character text segment in a byte array.
     * Intended to be called after the segment repeats are finished. 
     * @return A byte array of the message.
     */
    public byte[] getMessage(){
        return dataActive;
    }
    
    /**
     * Returns the 'corrected' 64-character text segment as a String.
     * Intended to be called after the segment repeats are finished. 
     * @return A string of the message.
     */
    public String getMessageString(){
       String msg = new String(dataActive);
        Log.d("DATA ACTIVE STRING", msg);
        return msg;
    }
    /**
     * Returns the Validity value for each character return in a getMessage call in a byte array,
     * where each element indicates a confidence level of correctness corresponding to each text 
     * segment character: 3 = "most confidence" and 1 = "least confidence"
     * @return A byte array of the Validity values
     */
    public byte[] getValidity(){
        return dataValidity;
    }
    
    /**
     * Returns the Validity value for each character return in a getMessage call as a String,
     * where each char in the returned string indicates a confidence level of correctness corresponding to each text 
     * segment character: 3 = "most confidence" and 1 = "least confidence"
     * Useful for debugging the algorithm and experimenting with its effectiveness.
     * @return The Validity values, converted to a string. 
     */
    public String getValidityString(){
        StringBuilder sb = new StringBuilder(128);
        for (int n=0; n < dataValidity.length; n++){
            sb.append(Byte.toString(dataValidity[n]));   
        }
        return sb.toString();
    }
    
    /**
     * Calculates a confidence score of the entire text segment, expressed
     * as a percentage; 100.000000 is a perfect reception.
     * Experiments with 3 completely different text segments (not related to each other)
     * shows it is likely that there will be letters and spaces common to
     * each different segment resulting in a confidence level of around 39%. 
     * @return Confidence that the received segment text is correct, expressed as a percentage.
     */
    public float getValidityScore(){
        float sumValidity = 0f;
        // If every char in the segment was received perfectly
        float maxValidityScore = (float)(dataValidity.length * 3);
        // If every char in the segment was different on each repeat
        // float minValidityScore = dataValidity.length * 1;
        for (int n=0; n < dataValidity.length; n++) {
            sumValidity+=dataValidity[n];
        }
        return (sumValidity/maxValidityScore)* 100;
    }
} // END rdsValidation Class


/*
From:   Seth Stroh <seth.stroh@gmail.com>
Sent:   Monday, March 17, 2014 20:20
To: Rich Rarey
Cc: Allen Hartle - Jump2Go
Subject:    Re: Algorithm for validating the repeated alert text

The solution I have used for radiotext works something like this:

Starting from two empty character arrays (temp and active) and a "character validity" array 
initialized to zero...

When a new character is received where its position in the "temp" and "active" array are both 
empty, write that character to both "temp" and "active" array position and set the "validity" to 1.
(Basically assume the character to be valid.)

For repeats in that character position, there are three cases:
1. If the new character is equal to that in the "active" array, write that character to the temp array 
and increment the validity.

2. If the character is equal to that in the "temp" array but not equal to the "active" array, write 
that character to the "active" array and set validity to "2"  (same character received twice)

3. If the character is not equal to the "active" array AND is not equal to the "temp" array, write 
the character to the temp array.  Do not change the character in the active array or the validity 
count.

After receiving data three times, the best case is a validity of 3.  (Character in the "active" array 
was identical all three times.)  Next best is a validity of 2.  (Character in the "active" array was 
identical twice with one mismatch.)  A validity of 1 means the character was different all three 
times and as such there is no way to know if the "active" or "temp" array is more correct.  Finally 
a validity of 0 would mean a character was never received in that position.

If you are being passed the character data and the associated 10-bit block error correcting code, 
you could enhance this algorithm by looking at the error correcting code.  (Calculate the 10 bit 
CRC for a data block and see if it exactly matches the CRC received for that data block.  If so, 
the resulting characters can be given a higher validity then if there is a CRC mismatch for that 
block.)

I have an implementation for the 10-bit CRC calculation.  If you have access to that data from 
the receiver, let me know and I will dig up that code.


For radiotext, the a/b flag is used to signal a transition to the next text segment.  In the ODA, we 
are signaling this using the "segment" value transmitted in address 2 (alert identity even.)

- seth



  

On Mon, Mar 17, 2014 at 8:11 AM, Rich Rarey <rrarey@npr.org> wrote:
Seth, 
Do you have a  recommended algorithm for validating the alert text segment's text when each 
segment is repeated more than once (in our case, 3 times) ? 
 
I am not sure how to discern garbled words in a message as it is repeated. 
 
Thanks! 
 
   2 
R

*/
