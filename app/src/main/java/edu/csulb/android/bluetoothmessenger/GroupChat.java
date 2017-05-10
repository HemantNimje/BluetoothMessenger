package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static edu.csulb.android.bluetoothmessenger.ChatActivity.DATATYPE_TEXT;


/**
 *  Class for mangaging multiple connections at once.
 *  Before using the class, ensure that bluetooth is turned on
 *  Maintains a list of user names
 */

public class GroupChat {
    List<BluetoothChatService> connectedUsers = new ArrayList<>();
    List<UserInfo> userNames;
    private final static String TAG = "GroupChat";

    GroupChat(List<UserInfo> users, Handler handler) {
        userNames = users;

        for (UserInfo user : users) {
            BluetoothChatService chatService = new BluetoothChatService(handler);
            if (chatService.getState() == BluetoothChatService.STATE_NONE) {
                chatService.start();
            }
            connectedUsers.add(chatService);
        }
    }

    // Each user will attempt to connect to every other user
    public void startConnection() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (int i = 0; i < userNames.size(); i++) {
            BluetoothDevice device = adapter.getRemoteDevice(userNames.get(i).macAddress);
            Log.d(TAG, device.getAddress());
            BluetoothChatService userConnection = connectedUsers.get(i);
            if (userConnection.getState() != BluetoothChatService.STATE_CONNECTED) {
                connectedUsers.get(i).connect(device);
            } else {
                Log.d(TAG, "Already connected");
            }
        }
    }

    public void sendTextMessage(byte[] message) {
        for (BluetoothChatService service: connectedUsers) {
            service.write(message, DATATYPE_TEXT);
        }
    }

    public boolean areAllDevicesConnected() {
        for (BluetoothChatService userService : connectedUsers) {
            if (userService.getState() != BluetoothChatService.STATE_CONNECTED) {
                return false;
            }
        }

        return true;
    }

    public void stop() {
        for (BluetoothChatService userService : connectedUsers) {
            userService.stop();
        }
    }

    // Group id is a combination of all the mac addresses in the group chat
    public String getGroupId() {
        return "";
    }
}
