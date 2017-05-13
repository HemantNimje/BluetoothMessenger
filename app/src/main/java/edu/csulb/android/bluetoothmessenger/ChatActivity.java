package edu.csulb.android.bluetoothmessenger;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.DEVICE_ADDRESS;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_READ_AUDIO;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_READ_IMAGE;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_READ_TEXT;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_WRITE_AUDIO;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_WRITE_IMAGE;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.MESSAGE_WRITE_TEXT;
import static edu.csulb.android.bluetoothmessenger.ChatMessages.GROUP_CHAT_USER_TABLE;
import static edu.csulb.android.bluetoothmessenger.ChatMessages.USER_NAMES_TABLE;
import static edu.csulb.android.bluetoothmessenger.ChatMessages.compressBitmap;
import static edu.csulb.android.bluetoothmessenger.MainActivity.mBluetoothAdapter;
import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_AUDIO;
import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_IMAGE;
import static edu.csulb.android.bluetoothmessenger.MessageInstance.DATA_TEXT;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 3;
    private BluetoothChatService mChatService = null;

    private static final int SELECT_IMAGE = 11;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private static String mFileName = null;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissiontoRecordAccepted = false;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private static final String LOG_TAG = "AudioRecordTest";

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;

    private StringBuffer mOutStringBuffer;
    private ListView mConversationView;
    private EditText mEditText;
    private ImageButton mButtonSend;
    private TextView connectionStatus;
    ChatMessageAdapter chatMessageAdapter;
    String fileName = null;
    Bitmap imageBitmap;
    private static final int CAMERA_REQUEST = 1888;

    private ImageView fullscreen;

    private ChatMessages db;
    private GroupChat groupChatManager = null;
    private boolean isGroupChat = false;

    private ArrayList<UserInfo> users;

    private final static String TAG = "ChatActivity";
    private final static int MAX_IMAGE_SIZE = 200000;

    private HashMap<String, String> macToUser = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = new ChatMessages(getApplicationContext());

        ArrayList<UserInfo> usersInfo = (ArrayList<UserInfo>) getIntent()
                .getSerializableExtra("USERS-INFO");

        users = usersInfo;

        if (usersInfo != null && usersInfo.size() > 1) {
            isGroupChat = true;
        }

        init();


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null || groupChatManager == null) {

            setupChat();
        }

        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";

        mConversationView = (ListView) findViewById(R.id.message_history);
        chatMessageAdapter = new ChatMessageAdapter(ChatActivity.this, R.layout.chat_message);
        mConversationView.setAdapter(chatMessageAdapter);
        fullscreen = (ImageView) findViewById(R.id.fullscreen_image);

        mConversationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MessageInstance msg = (MessageInstance) parent.getItemAtPosition(position);
                if (msg.audioFile != null) {
                    mPlayer = MediaPlayer.create(ChatActivity.this, Uri.fromFile(msg.audioFile));
                    mPlayer.start();
                }
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);


        final ImageButton btnRecord = (ImageButton) findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            boolean mStartRecording = true;

            @Override
            public void onClick(View view) {
                onRecord(mStartRecording);

                if (mStartRecording) {
                    btnRecord.setImageResource(R.drawable.ic_stop_black_24dp);
                } else {
                    btnRecord.setImageResource(R.drawable.ic_mic_black_24dp);
                }
                mStartRecording = !mStartRecording;
            }
        });
    }

    public void init() {
        connectionStatus = (TextView) findViewById(R.id.connection_status);
        mEditText = (EditText) findViewById(R.id.edit_text_text_message);
        mButtonSend = (ImageButton) findViewById(R.id.btn_send);

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChatHistory(getIntent());
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void startPreviousChat() {
        if (users == null) {
            return;
        } else if (isGroupChat) {
            groupChatManager.startConnection();
            return;
        }

        for (UserInfo user : users) {
            Log.d(TAG, "Connect to " + user.macAddress);
            connectDevice(user.macAddress);
        }
    }

    private void loadChatHistory(Intent intent) {
        Log.d(TAG, "Loading chat history");

        users = (ArrayList<UserInfo>) intent
                .getSerializableExtra("USERS-INFO");

        if (users == null) {
            Log.d(TAG, "Users is null");
            return;
        } else if (isGroupChat) {
            for (UserInfo user : users) {
                macToUser.put(user.getMacAddress(), user.getName());
            }
            Log.d(TAG, "Group chat");
            if (db.isUserInDb(groupChatManager.getGroupId(), GROUP_CHAT_USER_TABLE)) {
                Log.d(TAG, "Group is in database");
                List<ChatMessage> readMessages = getAllGroupMessages(groupChatManager.getGroupId(),
                        "Received");
                List<ChatMessage> sentMessages = getAllGroupMessages(groupChatManager.getGroupId(),
                        "Sent");

                List<ChatMessage> combinedMessages = ChatMessages.combineMessages(readMessages,
                        sentMessages);
                showChatHistory(combinedMessages);

            } else {
                Log.d(TAG, "Group is not in database");
            }
            return;
        }

        chatMessageAdapter.clear();

        List<ChatMessage> readMessages = getAllMessages(users, "Received");
        List<ChatMessage> sentMessages = getAllMessages(users, "Sent");
        List<ChatMessage> combinedMessages = ChatMessages.combineMessages(readMessages, sentMessages);

        showChatHistory(combinedMessages);
    }

    void showChatHistory(List<ChatMessage> messages) {
        String receivedFrom = null;
        for (ChatMessage message : messages) {
            Log.d(TAG, message.user);
            Log.d(TAG, Integer.toString(message.dataType));
            if (message.user.equals("Me")) {

                if (message.dataType == DATA_IMAGE) {
                    chatMessageAdapter.add(new MessageInstance(true, message.image));
                } else if (message.dataType == DATA_TEXT) {
                    chatMessageAdapter.add(new MessageInstance(true,
                            message.user + ": " + message.message + "\n ("
                                    + message.timeStamp + ")"));
                } else {
                    chatMessageAdapter.add(new MessageInstance(true, message.audioFile));
                }
            } else {
                if (message.dataType == DATA_IMAGE) {
                    chatMessageAdapter.add(new MessageInstance(false, message.image));
                } else if (message.dataType == DATA_TEXT){
                    chatMessageAdapter.add(new MessageInstance(false,
                            message.user + ": " + message.message + "\n ("
                                    + message.timeStamp + ")"));
                } else {
                    chatMessageAdapter.add(new MessageInstance(false, message.audioFile));
                }
            }
            /* Save the user name of the other device here provided that its not group chat */
            if (receivedFrom == null && !isGroupChat) {
                receivedFrom = message.user;
            }
            chatMessageAdapter.notifyDataSetChanged();
        }

        setChatTitle(users.get(0).getName());
    }

    public void setChatTitle(String deviceName){
        /* Set the title of the chat to the user with whom chat is done */
        if (!isGroupChat) {
            setTitle(deviceName);
        } else {
            setTitle("Group Chat");
        }
    }

    List<ChatMessage> getAllMessages(List<UserInfo> usersInfo, String messageType) {
        List<ChatMessage> messages = new ArrayList<>();

        for (UserInfo info : usersInfo) {
            String macAddress = info.macAddress;
            String userName = info.name;
            List<ChatMessage> readMessages;

            if (messageType.equals("Sent")) {
                readMessages = db.retrieveSentMessages(macAddress);
            } else {
                readMessages = db.retrieveReceivedMessages(macAddress);
            }

            for (ChatMessage message : readMessages) {
                if (messageType.equals("Sent")) {
                    message.user = "Me";
                } else {
                    message.user = userName;
                }
                messages.add(message);
            }
        }
        return messages;
    }

    List<ChatMessage> getAllGroupMessages(String groupId, String messageType) {
        List<ChatMessage> messages;

        if (messageType.equals("Sent")) {
            messages = db.retrieveSentGroupMessages(groupId);
        } else {
            messages = db.retrieveReceivedGroupMessages(groupId);
        }

        for (ChatMessage message : messages) {
            Log.d(TAG, message.user + ":" + message.message);
            if (messageType.equals("Sent")) {
                message.user = "Me";
            } else {
                message.user = macToUser.get(message.user);
            }
        }
        return messages;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy called");
        super.onDestroy();

        if (mChatService != null) {
            mChatService.stop();
        }
        if (groupChatManager != null) {
            groupChatManager.stop();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back pressed");

        if (groupChatManager != null) {
            groupChatManager.stop();
        }

        if (mChatService != null) {
            mChatService.stop();
        }
        users = null;
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_make_discoverable:
                ensureDiscoverable();
                return true;

            case R.id.menu_search_devices:
                Intent bluetoothIntent = new Intent(getApplicationContext(),
                        DeviceListActivity.class);
                startActivityForResult(bluetoothIntent, REQUEST_CONNECT_DEVICE);
                break;

            case R.id.device_connect_disconnect:
                Log.d(TAG, "Trying to direct connect");
                if (users == null) {
                    Toast.makeText(this, "Direct connection not possible",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                startPreviousChat();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void PhotoMessage(View view) {
        permissionCheck();
    }


    public void CameraPhoto(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    public void permissionCheck() {


        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Add your explanation for the user here.
                Toast.makeText(this, "You have declined the permissions. " +
                        "Please allow them first to proceed.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            requestImageFromGallery();
        }
    }

    public void requestImageFromGallery() {
        Intent attachImageIntent = new Intent();
        attachImageIntent.setType("image/*");
        attachImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(attachImageIntent, "Select Picture"),
                SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();

                    // Initialize the BluetoothChatService to perform bluetooth connection
                    mChatService = new BluetoothChatService(mHandler);
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case SELECT_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                    data.getData());

                            // If you can't compress the image, then do not try sending.
                            byte[] imageSend;
                            try {
                                imageSend = compressBitmap(bitmap, true);
                            } catch (NullPointerException e) {
                                Log.d(TAG, "Image cannot be compressed");
                                Toast.makeText(getApplicationContext(), "Image can not be found" +
                                                " or is too large to be sent",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Calendar calendar = Calendar.getInstance();
                            String timeSent = sdf.format(calendar.getTime());

                            if (imageSend.length > MAX_IMAGE_SIZE) {
                                Toast.makeText(getApplicationContext(), "Image is too large",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (isGroupChat) {
                                groupChatManager.sendMessage(imageSend, DATA_IMAGE);
                            } else {
                                mChatService.write(imageSend, DATA_IMAGE, timeSent);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            case CAMERA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                        byte[] cameraSend;
                        try {
                            cameraSend = compressBitmap(bitmap, true);
                        } catch (Exception e) {
                            Log.d(TAG, "Could not find the image");
                            Toast.makeText(getApplicationContext(), "Image could not be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (cameraSend.length > MAX_IMAGE_SIZE) {
                            Toast.makeText(getApplicationContext(), "Image is too large to be sent",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Calendar calendar = Calendar.getInstance();
                        String timeSent = sdf.format(calendar.getTime());

                        if (isGroupChat) {
                            groupChatManager.sendMessage(cameraSend, DATA_IMAGE);
                        } else {
                            mChatService.write(cameraSend, DATA_IMAGE, timeSent);
                        }
                    }
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String macAddress = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    connectDevice(macAddress);
                }
                break;

        }
    }

    private void connectDevice(String macAddress) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
        mConnectedDeviceAddress = macAddress;
        mChatService.connect(device);
    }

    static final SimpleDateFormat sdf = new SimpleDateFormat("y-MM-dd HH:mm:ss");

    String prevSendTime = null;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            connectionStatus.setText(getResources().getString(R.string.connected));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            connectionStatus.setText(getResources().getString(R.string.disconnected));
                            break;
                    }
                    break;


                case MESSAGE_WRITE_TEXT:
                    MessageInstance textWriteInstance = (MessageInstance) msg.obj;
                    byte[] writeBuf = (byte[]) textWriteInstance.getData();
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Calendar calendar = Calendar.getInstance();
                    String txtWriteTime = sdf.format(calendar.getTime());

                    // This is stored in milliseconds for time checking
                    String time = textWriteInstance.getTime();

                    // If there is a group chat, you will not send multiple times
                    // sometimes back to back messages have the same time
                    // maybe use milliseconds to break ties
                    if (prevSendTime == null) {
                        prevSendTime = time;
                    } else if (prevSendTime.equals(time)) {
                        Log.d(TAG, "Time equal, msg not repeated");
                        break;
                    }
                    prevSendTime = time;

                    if (isGroupChat) {
                        db.insertSentGroupMessage(txtWriteTime, groupChatManager.getGroupId(),
                                writeMessage, DATA_TEXT, groupChatManager.getGroupId());
                    } else {
                        db.insertSentMessage(txtWriteTime, mConnectedDeviceAddress, writeMessage,
                                DATA_TEXT);
                    }

                    String writeDisplayMessage = "Me: " + writeMessage + "\n"
                            + "(" + txtWriteTime + ")";

                    chatMessageAdapter.add(new MessageInstance(true, writeDisplayMessage));
                    chatMessageAdapter.notifyDataSetChanged();
                    break;

                case MESSAGE_WRITE_AUDIO:
                    MessageInstance audioWriteInstance = (MessageInstance) msg.obj;
                    String connectedMacAddress = audioWriteInstance.getMacAddress();
                    // Used for DB storage
                    Calendar AudioCalendar = Calendar.getInstance();
                    String AudioWriteTime = sdf.format(AudioCalendar.getTime());

                    time = audioWriteInstance.getTime();

                    if (prevSendTime == null) {
                        prevSendTime = time;
                    } else if (prevSendTime.equals(time)) {
                        Log.d(TAG, "Time equal, msg not repeated");
                        break;
                    }
                    prevSendTime = time;

                    Log.d(TAG, "Audio should be stored at: " + fileName);
                    File f = new File(fileName);
                    chatMessageAdapter.add(new MessageInstance(true, f));
                    chatMessageAdapter.notifyDataSetChanged();

                    if (isGroupChat) {
                        db.insertSentGroupMessage(AudioWriteTime, groupChatManager.getGroupId(), f.toString(),
                                DATA_AUDIO, groupChatManager.getGroupId());
                    } else {
                        db.insertSentMessage(AudioWriteTime, connectedMacAddress, f.toString(), DATA_AUDIO);
                    }

                    break;

                case MESSAGE_WRITE_IMAGE:
                    Log.d(TAG, "Writing image");
                    MessageInstance imageWriteInstance = (MessageInstance) msg.obj;
                    String userMacAddress = imageWriteInstance.getMacAddress();

                    Calendar ImageCalendar = Calendar.getInstance();
                    String imageWriteTime = sdf.format(ImageCalendar.getTime());

                    time = imageWriteInstance.getTime();

                    if (prevSendTime == null) {
                        prevSendTime = time;
                    } else if (prevSendTime.equals(time)) {
                        Log.d(TAG, "Time equal, msg not repeated");
                        break;
                    }
                    prevSendTime = time;

                    imageBitmap = (Bitmap) imageWriteInstance.getData();
                    byte[] writeDecodedStringArray = compressBitmap(imageBitmap, false);

                    if (isGroupChat) {
                        db.insertSentGroupMessage(imageWriteTime, groupChatManager.getGroupId(),
                                writeDecodedStringArray, DATA_IMAGE, groupChatManager.getGroupId());
                    } else {
                        db.insertSentMessage(imageWriteTime, userMacAddress,
                                writeDecodedStringArray, DATA_IMAGE);
                        Log.d(TAG, "Inserted write image into DB");
                    }

                    if (imageBitmap != null) {
                        chatMessageAdapter.add(new MessageInstance(true, imageBitmap));
                        chatMessageAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Fatal: Image bitmap is null");
                    }
                    break;

                case MESSAGE_READ_IMAGE:
                    MessageInstance msgImgData = (MessageInstance) msg.obj;
                    userMacAddress = msgImgData.getMacAddress();
                    Calendar calTest = Calendar.getInstance();
                    String readImageTime = sdf.format(calTest.getTime());

                    if (msgImgData.getDataType() == DATA_IMAGE) {
                        imageBitmap = (Bitmap) msgImgData.getData();

                        // Compress and store in database
                        byte[] decodedStringArray = compressBitmap(imageBitmap, false);

                        if (isGroupChat) {
                            db.insertReceivedGroupMessage(readImageTime, userMacAddress,
                                    decodedStringArray, DATA_IMAGE, groupChatManager.getGroupId());
                        } else {
                            db.insertReceivedMessage(readImageTime, userMacAddress, decodedStringArray,
                                    DATA_IMAGE);
                        }
                        Log.d(TAG, "Image stored in db");

                        if (imageBitmap != null) {
                            chatMessageAdapter.add(new MessageInstance(false, imageBitmap));
                            chatMessageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    }

                    Log.d(TAG, "Image was read from " + msgImgData.getUserName() + ": "
                        + msgImgData.getMacAddress());

                    break;

                case MESSAGE_READ_TEXT:
                    MessageInstance msgTextData = (MessageInstance) msg.obj;
                    byte[] readBuf = (byte[]) msgTextData.getData();

                    Calendar cal = Calendar.getInstance();
                    String readTime = sdf.format(cal.getTime());

                    String message = new String(readBuf);
                    connectedMacAddress = msgTextData.getMacAddress();
                    // modify this for group chat
                    if (isGroupChat) {
                        db.insertReceivedGroupMessage(readTime, connectedMacAddress, message,
                                DATA_TEXT, groupChatManager.getGroupId());
                    } else {
                        db.insertReceivedMessage(readTime, mConnectedDeviceAddress,
                                message, DATA_TEXT);
                    }

                    String displayMessage = msgTextData.getUserName() + ": " + message + "\n"
                            + "(" + readTime + ")";

                    chatMessageAdapter.add(new MessageInstance(false, displayMessage));
                    chatMessageAdapter.notifyDataSetChanged();

                    Log.d(TAG, "Text was read from " + msgTextData.getUserName() + ": "
                            + msgTextData.getMacAddress());
                    break;

                case MESSAGE_READ_AUDIO:
                    MessageInstance msgAudioData = (MessageInstance) msg.obj;
                    connectedMacAddress = msgAudioData.getMacAddress();
                    Calendar readAudioCal = Calendar.getInstance();
                    readTime = sdf.format(readAudioCal.getTime());
                    String filename = getFilename();
                    FileOutputStream fos;

                    try {
                        if (filename != null) {
                            byte[] buff = (byte[]) msgAudioData.getData();
                            Log.d(TAG, "TIME: " + readTime);
                            fos = new FileOutputStream(filename);
                            fos.write(buff);
                            fos.flush();
                            fos.close();
                            chatMessageAdapter.add(new MessageInstance(false, new File(filename)));
                            chatMessageAdapter.notifyDataSetChanged();

                            if (isGroupChat) {
                                db.insertReceivedGroupMessage(readTime, connectedMacAddress, filename,
                                        DATA_AUDIO, groupChatManager.getGroupId());
                            } else {
                                db.insertReceivedMessage(readTime, connectedMacAddress, filename,
                                        DATA_AUDIO);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(ChatActivity.this, "Could not save the file",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Could not save the file", e);
                    }

                    Log.d(TAG, "Audio was saved from " + msgAudioData.getUserName() + ": "
                        + msgAudioData.getMacAddress());
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                    String deviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }

                    setChatTitle(mConnectedDeviceName);
                    // Check if user is in DB, if so, retrieve chat history
                    // else clear screen

                    Log.d(TAG, "Before check if in db");

                    if (isGroupChat) {
                        groupChatManager.setConnectionAsTrue(deviceAddress);

                        if (groupChatManager.isConnectedToAll()) {
                            Log.d(TAG, "Connected to all in group");
                            db.insertGroupName(groupChatManager.getGroupId(),
                                    groupChatManager.getGroupUserNames());
                        }
                        break;
                    }

                    if (db.isUserInDb(mConnectedDeviceAddress, USER_NAMES_TABLE)) {
                        Log.d(TAG, "User in db");
                        Intent intent = new Intent();
                        ArrayList<UserInfo> users = new ArrayList<>();
                        users.add(new UserInfo(mConnectedDeviceName, mConnectedDeviceAddress));
                        intent.putExtra("USERS-INFO", users);
                        loadChatHistory(intent);
                    } else {
                        Log.d(TAG, "User not in db");
                        try {
                            chatMessageAdapter.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // insert users name and mac address to the database
                    db.insertUserName(mConnectedDeviceAddress, mConnectedDeviceName);

                    break;
                case MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                Toast.LENGTH_SHORT).show();

                        String toastMsg = msg.getData().getString(TOAST);

                        // Insure that we always reset the connection
                        if (toastMsg.equals("Unable to connect device") ||
                                toastMsg.equals("Device connection was lost")) {
                            Log.d(TAG, "Lost connection: " + toastMsg);
                            onBackPressed();
                        }
                    }
                    break;
            }
        }
    };

    private void setupChat() {
        // Initialize the compose field with a listener for the return key
        mEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                String message = mEditText.getText().toString();
                if (isGroupChat) {
                    sendGroupMessage(message);
                } else {
                    sendMessage(message);
                }
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        if (isGroupChat) {
            Log.d(TAG, "setting up group chat");
            groupChatManager = new GroupChat(users, mHandler);
            db.insertGroupName(groupChatManager.getGroupId(), groupChatManager.getGroupUserNames());
        } else {
            Log.d(TAG, "setting up single chat");
            mChatService = new BluetoothChatService(mHandler);
        }
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            System.out.println("Message Length = " + message.length());

            Calendar calendar = Calendar.getInstance();
            String timeSent = sdf.format(calendar.getTime());
            mChatService.write(message.getBytes(), DATA_TEXT, timeSent);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mEditText.setText(mOutStringBuffer);
        }
    }

    private void sendGroupMessage(String message) {
        if (!groupChatManager.areAllDevicesConnected()) {
            Toast.makeText(getApplicationContext(), R.string.not_all_connected,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            groupChatManager.sendMessage(message.getBytes(), DATA_TEXT);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();

                if (isGroupChat) {
                    sendGroupMessage(message);
                } else {
                    sendMessage(message);
                }

            }
            return true;
        }
    };

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Do the external storage related work here.

//                    requestImageFromGallery();
                } else {
                    // Permission is denied.
                    ActivityCompat.requestPermissions(this, new String[]
                                    {Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
//                    Toast.makeText(this, "Can't Proceed. You rejected the permission.",
//                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissiontoRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    private String getFilename() {
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if (!appFolder.exists()) {
            if (!appFolder.mkdirs()) {
                Toast.makeText(this, "Could not create App folder. Any activity requiring storage is suspended",
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return appFolder.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp3";
    }

    /* Handle record start and stop */
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    /*
    * Create a new recorder
    * Set its properties as its source, output format, output file, audio encoder
    * */
    private void startRecording() {
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        fileName = getFilename();
        Log.d("Start Recording File :", fileName);
        mRecorder.setOutputFile(fileName);
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Recording failed", e);
        }
    }

    /*
    * Stop the recorder and release it
    * */
    private void stopRecording() {
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
        if (null != mRecorder) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            if (mChatService != null || groupChatManager != null) {
                try {
                    File f = new File(fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buff = new byte[(int) f.length()];
                    fis.read(buff);
                    Calendar calendar = Calendar.getInstance();
                    String timeSent = sdf.format(calendar.getTime());
                    if (isGroupChat) {
                        groupChatManager.sendMessage(buff, DATA_AUDIO);
                    } else {
                        mChatService.write(buff, DATA_AUDIO, timeSent);
                    }
                    fis.close();
                } catch (Exception e) {
                    Log.e(TAG, "Could not open stream to save data", e);
                }
            }
        }
    }
}
