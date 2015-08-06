package org.nprlabs.nipperone.framework;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.nprlabs.nipperone.main.AlertImpl;

import java.util.ArrayList;
import java.util.List;


/**
 * This is the Helper class for the SQLite database.
 */
public class DatabaseHandler extends SQLiteOpenHelper{

    private String TAG = "Database Handler";


    /**
     * This method needs to be called to get the existing database or create a new one.
     * Therefore this needs to be called first.
     */
    private static DatabaseHandler dbInstance;

    //All static variables
    //Database version
    private static final int DATABASE_VERSION = 1;
   
    //Database name
    private static final String DATABASE_NAME = "MessagesManager";
    
    //Messages table name
    private static final String TABLE_MESSAGES = "messages";
    
    //Messages Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_EAS_EVENT = "event";
    private static final String KEY_EAS_ACTION = "action";
    private static final String KEY_EAS_CERTAINTY = "certainty";
    private static final String KEY_EAS_SEVERITY = "severity";
    private static final String KEY_EAS_URGENCY = "urgency";
    private static final String KEY_EAS_CATEGORY = "category";
    private static final String KEY_EVT_DURATION = "evtDuration";
    private static final String KEY_ORIGIN_TIME = "originTime";
    private static final String KEY_MSG_ORIGINATOR = "msgOriginator";
    private static final String KEY_MSG_TYPE = "msgType";
    private static final String KEY_MESSAGE = "message";


    /**
     * This method is the classes "constructor". This is to be the only way to get the one and only
     * instance of the database. Makes sure that all variables reference the same database.
     *
     * @param context The applications context
     * @return The current/existing instance of the database. If it doesn't exist one is created which
     * is then always returned
     */
    public static DatabaseHandler getInstance(Context context){

        if(dbInstance == null){
            dbInstance = new DatabaseHandler(context.getApplicationContext());
        }
        return dbInstance;
    }

    private DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MSG_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_EAS_EVENT + " TEXT,"
                + KEY_EAS_ACTION + " TEXT," + KEY_EAS_CERTAINTY + " TEXT,"
                + KEY_EAS_SEVERITY + " TEXT," + KEY_EAS_URGENCY + " TEXT,"
                + KEY_EAS_CATEGORY+ " TEXT," + KEY_EVT_DURATION + " TEXT,"
                + KEY_ORIGIN_TIME + " TEXT,"+ KEY_MSG_ORIGINATOR + " TEXT,"
                + KEY_MSG_TYPE + " TEXT,"+ KEY_MESSAGE + " TEXT"+")";
        
        db.execSQL(CREATE_MSG_TABLE);
        
    }

    
    //this method will "reset"/recreate the tables after upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Drops the older table if it exists, 
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        
        //creates the table again, but it has zero information in it
        onCreate(db);
    }
    
    /**
     * adding a new message
     * @param msg the message to add to the database
     */
    public synchronized void addMessage(AlertImpl msg){
        Log.d(TAG, msg.getEventString() + " was received and added to the database!");

        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(KEY_EAS_EVENT, msg.getEventString());
        values.put(KEY_EAS_ACTION, msg.getMsgAction());
        values.put(KEY_EAS_CERTAINTY, msg.getMsgCertainty());
        values.put(KEY_EAS_SEVERITY, msg.getMsgSeverity());
        values.put(KEY_EAS_URGENCY, msg.getMsgUrgency());
        values.put(KEY_EAS_CATEGORY, msg.getMsgCategory());
        values.put(KEY_EVT_DURATION, msg.getMsgDuration());
        values.put(KEY_ORIGIN_TIME, msg.getMsgOrgTime());
        values.put(KEY_MSG_ORIGINATOR, msg.getMsgOriginator());
        values.put(KEY_MSG_TYPE, msg.getMsgType());
        values.put(KEY_MESSAGE, msg.getMsgString());


        //System.out.println("Values to be added: event: " + msg.getEventString() + "Action: " + msg.getMsgAction());
        
        //inserting row
        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }
    
    /**
     * get a message 
     * @param id
     * @return
     */
    public synchronized AlertImpl getMessage(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_MESSAGES, new String[]{KEY_ID, KEY_EAS_EVENT,
                        KEY_EAS_ACTION, KEY_EAS_CERTAINTY, KEY_EAS_SEVERITY, KEY_EAS_URGENCY, KEY_EAS_CATEGORY, KEY_EVT_DURATION, KEY_ORIGIN_TIME, KEY_MSG_ORIGINATOR, KEY_MSG_TYPE, KEY_MESSAGE},
                KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);
        if(cursor != null)
            cursor.moveToFirst();

        AlertImpl msg = new AlertImpl();

        msg.setId(Integer.parseInt(cursor.getString(0)));
        msg.setEvent(cursor.getString(1));
        msg.setMsgAction(cursor.getString(2));
        msg.setMsgCertainty(cursor.getString(3));
        msg.setMsgSeverity(cursor.getString(4));
        msg.setMsgUrgency(cursor.getString(5));
        msg.setMsgCategory(cursor.getString(6));
        msg.setMsgDuration(cursor.getString(7));
        msg.setMsgOrgTime(cursor.getString(8));
        msg.setMsgOriginator(cursor.getString(9));
        msg.setType(cursor.getString(10));
        msg.setMsgString(cursor.getString(11));

        cursor.close();

        Log.d(TAG, msg.getEventString() + " was retrieved FROM the database for display!");
        return msg;
    }

    /**
     * Returns all of the Messages in the database.
     * @return A list of List<AlertImpl> with the oldest Alert first
     */
    public synchronized List<AlertImpl> getAllMessages(){
        List<AlertImpl> msgList = new ArrayList<AlertImpl>();
        //Select all query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        //looping through all rows and adding to list
        if(cursor.moveToFirst()){
            do{    
                AlertImpl msg = new AlertImpl();
                msg.setId(Integer.parseInt(cursor.getString(0)));
                msg.setEvent(cursor.getString(1));
                msg.setMsgAction(cursor.getString(2));
                msg.setMsgCertainty(cursor.getString(3));
                msg.setMsgSeverity(cursor.getString(4));
                msg.setMsgUrgency(cursor.getString(5));
                msg.setMsgCategory(cursor.getString(6));
                msg.setMsgDuration(cursor.getString(7));
                msg.setMsgOrgTime(cursor.getString(8));
                msg.setMsgOriginator(cursor.getString(9));
                msg.setType(cursor.getString(10));
                msg.setMsgString(cursor.getString(11));

                msgList.add(msg);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return msgList;

    }

    /**
     * Returns all of the Messages in the database, but in reverse order or the most recent
     * entry first
     * @return A list of List<AlertImpl> with the most recent entry first
     */
    public synchronized List<AlertImpl> getAllMessagesReverse(){
        List<AlertImpl> msgList = new ArrayList<AlertImpl>();
        //Select all query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        //looping through all rows and adding to list
        if(cursor.moveToLast()){
            do{
                AlertImpl msg = new AlertImpl();
                msg.setId(Integer.parseInt(cursor.getString(0)));
                msg.setEvent(cursor.getString(1));
                msg.setMsgAction(cursor.getString(2));
                msg.setMsgCertainty(cursor.getString(3));
                msg.setMsgSeverity(cursor.getString(4));
                msg.setMsgUrgency(cursor.getString(5));
                msg.setMsgCategory(cursor.getString(6));
                msg.setMsgDuration(cursor.getString(7));
                msg.setMsgOrgTime(cursor.getString(8));
                msg.setMsgOriginator(cursor.getString(9));
                msg.setType(cursor.getString(10));
                msg.setMsgString(cursor.getString(11));

                msgList.add(msg);
            }while(cursor.moveToPrevious());
        }
        cursor.close();
        return msgList;

    }
    
    public synchronized int getMessageCount(){
        String countQuery = "SELECT * FROM " + TABLE_MESSAGES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Updates a message that is assumed to already be in the database.
     * @param msg
     * @return
     */
    public synchronized int updateMessage(AlertImpl msg){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_EAS_EVENT, msg.getEventString());
        values.put(KEY_EAS_ACTION, msg.getMsgAction());
        values.put(KEY_EAS_CERTAINTY, msg.getMsgCertainty());
        values.put(KEY_EAS_SEVERITY, msg.getMsgSeverity());
        values.put(KEY_EAS_URGENCY, msg.getMsgUrgency());
        values.put(KEY_EAS_CATEGORY, msg.getMsgCategory());
        values.put(KEY_EVT_DURATION, msg.getMsgDuration());
        values.put(KEY_ORIGIN_TIME, msg.getMsgOrgTime());
        values.put(KEY_MSG_ORIGINATOR, msg.getMsgOriginator());
        values.put(KEY_MSG_TYPE, msg.getMsgType());
        values.put(KEY_MESSAGE, msg.getMsgString());

        int returnInt = db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(msg.getId())});

        db.close();

        //updating row
        return returnInt;
    }


    public synchronized void deleteMessage(AlertImpl msg){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " = ?", new String[]{String.valueOf(msg.getId())});
        db.close();
    }

    public synchronized boolean msgExists(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MESSAGES, new String[] { KEY_ID, KEY_EAS_EVENT,
                KEY_EAS_ACTION}, KEY_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);

        boolean msgExists;
        if(cursor != null){msgExists =  true;}
        else{msgExists = false;}

        cursor.close();
        db.close();

        return msgExists;



    }

}
