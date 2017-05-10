package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.csulb.android.bluetoothmessenger.ChatActivity.DATATYPE_TEXT;


/**
 *  Class for mangaging multiple connections at once.
 *  Before using the class, ensure that bluetooth is turned on
 *  Maintains a list of user names
 */

public class GroupChat {
    List<BluetoothChatService> chatSockets = new ArrayList<>();
    List<UserInfo> userNames;
    HashMap<String, Boolean> deviceConnections;
    String groupId = "";
    private final static String TAG = "GroupChat";

    GroupChat(List<UserInfo> users, Handler handler) {
        userNames = users;
        deviceConnections = new HashMap<>();
        for (UserInfo user : users) {
            BluetoothChatService chatService = new BluetoothChatService(handler);
            deviceConnections.put(user.macAddress, false);
            groupId += user.macAddress + "\n";

            if (chatService.getState() == BluetoothChatService.STATE_NONE) {
                chatService.start();
            }
            chatSockets.add(chatService);
        }
    }

    // Each user will attempt to connect to every other user
    // Find open socket and find user you are not connected to
    public void startConnection() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (int i = 0; i < chatSockets.size(); i++) {
            BluetoothChatService socket = chatSockets.get(i);
            if (socket.getState() == BluetoothChatService.STATE_CONNECTED) {
                continue;
            }

            for (Map.Entry<String, Boolean> connections : deviceConnections.entrySet()) {
                Boolean connected = connections.getValue();
                if (!connected) {
                    String macAddress = connections.getKey();
                    BluetoothDevice device = adapter.getRemoteDevice(macAddress);
                    socket.connect(device);
                    deviceConnections.put(macAddress, true);
                    Log.d(TAG, "Connected to " + macAddress);
                    break;
                }
            }
        }
    }

    // Once you connect to a specific bluetooth device,
    // do not try connecting to it again
    public void setConnectionAsTrue(String macAddress) {
        deviceConnections.put(macAddress, true);
    }

    public void sendTextMessage(byte[] message) {
        for (BluetoothChatService service: chatSockets) {
            service.write(message, DATATYPE_TEXT);
        }
    }

    public boolean areAllDevicesConnected() {
        for (BluetoothChatService userService : chatSockets) {
            if (userService.getState() != BluetoothChatService.STATE_CONNECTED) {
                return false;
            }
        }

        return true;
    }

    public void stop() {
        for (BluetoothChatService userService : chatSockets) {
            userService.stop();
        }
    }

    // Group id is a combination of all the mac addresses in the group chat
    // separated by a newline.
    public String getGroupId() {
        return groupId;
    }
}
