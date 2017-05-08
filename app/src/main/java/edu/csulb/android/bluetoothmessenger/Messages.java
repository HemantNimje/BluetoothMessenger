package edu.csulb.android.bluetoothmessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Every device has two databases.  One that records your sent messages.
// And another that records the messages sent to you.
// A unique user id and time uniquely identifies a specific message.

// USAGE in main files:
// Messages db = new Messages(getApplicationContext());
// Message received from another user:
// db.insertReceivedMessage(time, id, message);
// Message you sent:
// db.insertSentMessage(time, id, message);

// Retrieving a chat history for a given user would return an ArrayList<Message>
// You can retrieve all messages you sent to some user, or all messages a user
// has sent you.
// where each Message contains a time and message.


class ChatMessage {
    String timeStamp;
    String message;
    String user;

    ChatMessage(String time, String message) {
        timeStamp = time;
        this.message = message;
    }
}

class ChatMessages extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BlueToothMessenger";
    private static final int DATABASE_VERSION = 1;
    private static final String RECEIVED_MESSAGES_TABLE = "ReceivedMessages";
    private static final String SENT_MESSAGES_TABLE = "SentMessages";
    private static final String USER_NAMES_TABLE = "UserNames";

    //Columns
    private static final String TIME_STAMP = "Time";
    private static final String USER_ID = "User";
    private static final String MESSAGE = "Message";
    private static final String USER_NAME = "UserName";

    private static final String TAG = "Messages";


    ChatMessages(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String MESSAGES_COLUMNS = " (" + TIME_STAMP + " TEXT,"
                + USER_ID + " TEXT," + MESSAGE + " TEXT,"
                + "PRIMARY KEY (" + TIME_STAMP + ", "
                + USER_ID + ") )";

        final String CREATE_RECEIVED_MESSAGES_TABLE = "CREATE TABLE "
                + RECEIVED_MESSAGES_TABLE + MESSAGES_COLUMNS;

        final String CREATE_SENT_MESSAGES_TABLE = "CREATE TABLE "
                + SENT_MESSAGES_TABLE + MESSAGES_COLUMNS;

        final String USERS_COLUMNS = " (" + USER_NAME + " TEXT,"
                + USER_ID + " TEXT, PRIMARY KEY (" + USER_ID + ") )";

        final String CREATE_USER_NAMES_TABLE = "CREATE TABLE " +
                USER_NAMES_TABLE + USERS_COLUMNS;

        Log.i(TAG, CREATE_RECEIVED_MESSAGES_TABLE);
        Log.i(TAG, CREATE_SENT_MESSAGES_TABLE);
        Log.d(TAG, CREATE_USER_NAMES_TABLE);

        db.execSQL(CREATE_RECEIVED_MESSAGES_TABLE);
        db.execSQL(CREATE_SENT_MESSAGES_TABLE);
        db.execSQL(CREATE_USER_NAMES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECEIVED_MESSAGES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SENT_MESSAGES_TABLE);
        onCreate(db);
    }

    private void insertMessage(String timeStamp, String userId, String message, String tableType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put(TIME_STAMP, timeStamp);
        insertValues.put(USER_ID, userId);
        insertValues.put(MESSAGE, message);
        db.insert(tableType, null, insertValues);
        db.close();
    }

    void insertUserName(String macAddress, String userName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put(USER_NAME, userName);
        insertValues.put(USER_ID, macAddress);
        db.insert(USER_NAMES_TABLE, null, insertValues);
        db.close();
    }

    void insertSentMessage(String timeStamp, String userId, String message) {
        insertMessage(timeStamp, userId, message, SENT_MESSAGES_TABLE);
    }

    void insertReceivedMessage(String timeStamp, String userId, String message) {
        insertMessage(timeStamp, userId, message, RECEIVED_MESSAGES_TABLE);
    }

    boolean isUserInDb(String macAddress) {
        String query = "SELECT " + USER_ID + " FROM " + USER_NAMES_TABLE
                + " WHERE " + USER_ID + " = '" + macAddress + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.moveToFirst()) {
                if (cursor.getString(0).equals(macAddress)) {
                    return true;
                }
        }

        cursor.close();
        db.close();
        return false;
    }

    private ArrayList<ChatMessage> retrieveMessages(String userId, String tableType) {
        ArrayList<ChatMessage> userMessages = new ArrayList<>();
        String select = "SELECT Time, Message FROM " + tableType +
                " WHERE " + USER_ID + " = '" + userId + "'";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(select, null);

        if(cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                String message = cursor.getString(1);
                userMessages.add(new ChatMessage(date, message));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return userMessages;
    }

    ArrayList<ChatMessage> retrieveReceivedMessages(String userId) {
        return retrieveMessages(userId, RECEIVED_MESSAGES_TABLE);
    }

    ArrayList<ChatMessage> retrieveSentMessages(String userId) {
        return retrieveMessages(userId, SENT_MESSAGES_TABLE);
    }

    // used for testing purposes, remove later.
    void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + RECEIVED_MESSAGES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SENT_MESSAGES_TABLE);
        onCreate(db);
        db.close();
    }

    // Sorts and combines messages you have sent and received
    static List<ChatMessage> combineMessages(List<ChatMessage> readMessages,
                                             List<ChatMessage> sentMessages) {
        List<ChatMessage> combined = new ArrayList<>();

        for (ChatMessage message : readMessages) {
            combined.add(message);
        }

        for (ChatMessage message : sentMessages) {
            combined.add(message);
        }

        Collections.sort(combined, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage message1, ChatMessage message2) {
                return message1.timeStamp.compareTo(message2.timeStamp);
            }
        });

        return combined;
    }

    // Returns user names and mac addresses
    List<String> getPreviousChatNames() {
        List<String> userNames = new ArrayList<>();
        String select = "SELECT " + USER_ID + ", " + USER_NAME + " FROM " + USER_NAMES_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(select, null);

        if (cursor.moveToFirst()) {
            do {
                String macAddress = cursor.getString(0);
                String userName = cursor.getString(1);

                userNames.add(userName + "\n" + macAddress);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return userNames;
    }
}
