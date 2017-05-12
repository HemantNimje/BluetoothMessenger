package edu.csulb.android.bluetoothmessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_AUDIO;
import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_IMAGE;
import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_TEXT;

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
    Bitmap image;
    File audioFile;
    int dataType;

    ChatMessage(String time, String message) {
        timeStamp = time;
        this.message = message;
        dataType = DATA_TEXT;
    }

    ChatMessage(String time, Bitmap image) {
        timeStamp = time;
        this.image = image;
        dataType = DATA_IMAGE;
    }

    ChatMessage(String time, File audioFile) {
        timeStamp = time;
        this.audioFile = audioFile;
        dataType = DATA_AUDIO;
    }
}

class ChatMessages extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BlueToothMessenger";
    private static final int DATABASE_VERSION = 1;
    private static final String RECEIVED_MESSAGES_TABLE = "ReceivedMessages";
    private static final String SENT_MESSAGES_TABLE = "SentMessages";
    static final String USER_NAMES_TABLE = "UserNames";
    static final String GROUP_CHAT_USER_TABLE = "GroupChatUserNames";

    //Columns
    private static final String TIME_STAMP = "Time";
    private static final String USER_ID = "User";
    private static final String MESSAGE = "Message";
    private static final String USER_NAME = "UserName";
    private static final String GROUP_ID = "GroupId";
    private static final String IMAGE = "Image";
    private static final String AUDIO = "Audio";

    private static final String TAG = "Messages";

    ChatMessages(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String MESSAGES_COLUMNS = " ("
                + TIME_STAMP + " TEXT,"
                + USER_ID + " TEXT,"
                + MESSAGE + " TEXT,"
                + IMAGE + " BLOB,"
                + AUDIO + " TEXT,"
                + GROUP_ID + " TEXT,"
                + "PRIMARY KEY ("
                + TIME_STAMP + ", "
                + USER_ID + ") )";

        final String CREATE_RECEIVED_MESSAGES_TABLE = "CREATE TABLE "
                + RECEIVED_MESSAGES_TABLE + MESSAGES_COLUMNS;

        final String CREATE_SENT_MESSAGES_TABLE = "CREATE TABLE "
                + SENT_MESSAGES_TABLE + MESSAGES_COLUMNS;

        final String USERS_COLUMNS = " (" + USER_NAME + " TEXT,"
                + USER_ID + " TEXT, PRIMARY KEY (" + USER_ID + ") )";

        final String CREATE_USER_NAMES_TABLE = "CREATE TABLE " +
                USER_NAMES_TABLE + USERS_COLUMNS;

        final String CREATE_GROUP_CHAT_USER_TABLE = "CREATE TABLE " +
                GROUP_CHAT_USER_TABLE + USERS_COLUMNS;

        Log.d(TAG, CREATE_RECEIVED_MESSAGES_TABLE);
        Log.d(TAG, CREATE_SENT_MESSAGES_TABLE);
        Log.d(TAG, CREATE_USER_NAMES_TABLE);
        Log.d(TAG, CREATE_GROUP_CHAT_USER_TABLE);

        db.execSQL(CREATE_RECEIVED_MESSAGES_TABLE);
        db.execSQL(CREATE_SENT_MESSAGES_TABLE);
        db.execSQL(CREATE_USER_NAMES_TABLE);
        db.execSQL(CREATE_GROUP_CHAT_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RECEIVED_MESSAGES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SENT_MESSAGES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + USER_NAMES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + GROUP_CHAT_USER_TABLE);
        onCreate(db);
    }

    private void insertMessage(String timeStamp, String userId, Object message, String tableType,
                               int dataType, String groupId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put(TIME_STAMP, timeStamp);
        insertValues.put(USER_ID, userId);

        if (dataType == DATA_TEXT) {
            insertValues.put(MESSAGE, (String) message);
        }
        else if (dataType == DATA_IMAGE) {
            insertValues.put(IMAGE, (byte []) message);
        }
        else if (dataType == DATA_AUDIO) {
            insertValues.put(AUDIO, message.toString());
        }

        if (groupId != null) {
            insertValues.put(GROUP_ID, groupId);
        }

        try {
            db.insert(tableType, null, insertValues);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Error inserting messages");
        }
        db.close();
    }

    void insertUserName(String macAddress, String userName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put(USER_NAME, userName);
        insertValues.put(USER_ID, macAddress);
        try {
            db.insert(USER_NAMES_TABLE, null, insertValues);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Error inserting data");
        }
        db.close();
    }

    void insertGroupName(String GroupId, String GroupUserNames) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put(USER_NAME, GroupUserNames);
        insertValues.put(USER_ID, GroupId);
        Log.d(TAG, GroupUserNames);
        Log.d(TAG, GroupId);
        try {
            db.insert(GROUP_CHAT_USER_TABLE, null, insertValues);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Error inserting data into GroupTable");
        }
        db.close();
    }

    void insertSentMessage(String timeStamp, String userId, Object message, int dataType) {
        insertMessage(timeStamp, userId, message, SENT_MESSAGES_TABLE, dataType, null);
    }

    void insertReceivedMessage(String timeStamp, String userId, Object message, int dataType) {
        insertMessage(timeStamp, userId, message, RECEIVED_MESSAGES_TABLE, dataType, null);
    }

    void insertSentGroupMessage(String timeStamp, String userId, Object message, int dataType,
                                String groupId) {
        insertMessage(timeStamp, userId, message, SENT_MESSAGES_TABLE, dataType, groupId);
        Log.d(TAG, "Inserting sent group message");
    }

    void insertReceivedGroupMessage(String timeStamp, String userId, Object message, int dataType,
                                    String groupId) {
        insertMessage(timeStamp, userId, message, RECEIVED_MESSAGES_TABLE, dataType, groupId);
        Log.d(TAG, "Inserting received group message");
    }

    boolean isUserInDb(String macAddress, String table) {
        String query = "SELECT " + USER_ID + " FROM " + table
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

    private ArrayList<ChatMessage> retrieveMessages(String userId, String tableType,
                                                    boolean isGroupReceived) {
        ArrayList<ChatMessage> userMessages = new ArrayList<>();
        String select = "SELECT Time, Message, Image, Audio FROM " + tableType +
                " WHERE " + USER_ID + " = '" + userId + "' AND " + GROUP_ID
                + " IS NULL";

        if (isGroupReceived) {
            select = "SELECT " + TIME_STAMP + ", " + MESSAGE + ", " + IMAGE
                    + ", " + AUDIO + ", " + USER_ID + " FROM " + tableType +
                    " WHERE " + GROUP_ID + " = '" + userId + "'";
        }

        Log.d(TAG, select);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(select, null);
        Log.d(TAG, "retrieving messages");
        if(cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndex(TIME_STAMP));
                String message = cursor.getString(cursor.getColumnIndex(MESSAGE));
                byte[] image = cursor.getBlob(cursor.getColumnIndex(IMAGE));
                String audio = cursor.getString(cursor.getColumnIndex(AUDIO));
                String user =  null;

                if(isGroupReceived) {
                    user = cursor.getString(cursor.getColumnIndex(USER_ID));
                }

                ChatMessage cm = null;
                if (image != null) {
                    Log.d(TAG, "Message is null, adding image");
                    Bitmap bpImage = BitmapFactory.decodeByteArray(image, 0, image.length);
                    cm = new ChatMessage(date, bpImage);
                } else if (message != null){
                    Log.d(TAG, "Adding Message");
                    cm = new ChatMessage(date, message);
                } else {
                    Log.d(TAG, "Adding audio file");
                    cm = new ChatMessage(date, new File(audio));
                }

                if (isGroupReceived) {
                    cm.user = user;
                }
                userMessages.add(cm);

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return userMessages;
    }

    ArrayList<ChatMessage> retrieveReceivedMessages(String userId) {
        Log.d(TAG, "retrieving received messages");
        return retrieveMessages(userId, RECEIVED_MESSAGES_TABLE, false);
    }

    ArrayList<ChatMessage> retrieveSentMessages(String userId) {
        Log.d(TAG, "retrieving sent messages");
        return retrieveMessages(userId, SENT_MESSAGES_TABLE, false);
    }

    ArrayList<ChatMessage> retrieveSentGroupMessages(String groupId) {
        return retrieveMessages(groupId, SENT_MESSAGES_TABLE, true);
    }

    ArrayList<ChatMessage> retrieveReceivedGroupMessages(String groupId) {
        Log.d(TAG, "retrieving received group messages");
        return retrieveMessages(groupId, RECEIVED_MESSAGES_TABLE, true);
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
    List<String> getPreviousChatNames(String tableName) {
        List<String> userNames = new ArrayList<>();
        String select = "SELECT " + USER_ID + ", " + USER_NAME + " FROM " + tableName;
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

    static byte[] compressBitmap(Bitmap image, boolean isBeforeSocketSend) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        image.compress(Bitmap.CompressFormat.JPEG, 50, bos);
        String encodedImage = Base64.encodeToString(bos.toByteArray(),
                Base64.DEFAULT);

        byte[] compressed = isBeforeSocketSend ? encodedImage.getBytes()
                : Base64.decode(encodedImage, Base64.DEFAULT);

        return compressed;
    }

    static List<String> orderGroupChat(List<String> groupChat) {
        ArrayList<String> correctGroupings = new ArrayList<>();
        for (String group : groupChat) {
            String[] usersSplitUp = group.split("[\\r?\\n]+");
            String matchUserToAddress = "";

            // First half of the list maps to user names and the second
            // half maps to mac addresses
            for(int i = 0, j = usersSplitUp.length/2; j < usersSplitUp.length; i++, j++ ) {
                if (j == usersSplitUp.length - 1) {
                    matchUserToAddress += usersSplitUp[i] + "\n" + usersSplitUp[j];
                } else {
                    matchUserToAddress += usersSplitUp[i] + "\n" + usersSplitUp[j] + "\n";
                }
                Log.d(TAG, matchUserToAddress);
            }
            correctGroupings.add(matchUserToAddress);
        }
        return correctGroupings;
    }
}
