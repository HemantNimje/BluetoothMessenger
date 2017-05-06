package edu.csulb.android.bluetoothmessenger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Andrew on 5/5/2017.
 */

public class ChatHistory extends Activity {
    TextView chat;
    private final static String TAG = "ChatHistory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_history);
        chat = (TextView) findViewById(R.id.chatTextView);

        Messages db = new Messages(getApplicationContext());
        String macAddress = getIntent().getExtras().getString("MAC-ADDRESS");
        String userName = getIntent().getExtras().getString("DEVICE-NAME");

        List<Message> readMessages = db.retrieveReceivedMessages(macAddress);
        for (Message message : readMessages) {
            Log.d(TAG, message.timeStamp + message.message);
            message.user = userName;
        }

        List<Message> sendMessages = db.retrieveSentMessages(macAddress);
        for (Message message : sendMessages) {
            Log.d(TAG, message.timeStamp + " " + message.message);
            message.user = "Me";
        }

        List<Message> combinedMessages = Messages.combineMessages(readMessages, sendMessages);

        chat.setText(getChatHistory(combinedMessages));
    }

    String getChatHistory(List<Message> messages) {
        StringBuilder sb = new StringBuilder();

        for (Message message : messages) {
            sb.append(message.user + " (" + message.timeStamp + "): " + message.message + "\n");
        }

        return sb.toString();
    }
}
