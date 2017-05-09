package edu.csulb.android.bluetoothmessenger;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;
import static edu.csulb.android.bluetoothmessenger.BluetoothChatService.DEVICE_ADDRESS;
import static edu.csulb.android.bluetoothmessenger.MainActivity.mBluetoothAdapter;

public class ChatActivity extends AppCompatActivity {


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 3;
    private BluetoothChatService mChatService = null;

    private static final int SELECT_IMAGE = 11;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 100;
    private String selectedImagePath;
    private ImageView selectedImage;

    private static String mFileName = null;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissiontoRecordAccepted = false;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private static final String LOG_TAG = "AudioRecordTest";

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DATATYPE_IMAGE = "image";
    public static final String DATATYPE_TEXT = "text";
    public static final String DATATYPE_FILE = "file";

    private ArrayAdapter<String> mConversationArrayAdapter;

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

    private final static String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();

        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        } else if (mChatService == null) {

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
                if(msg.audioFile != null) {
                    mPlayer = MediaPlayer.create(ChatActivity.this, Uri.fromFile(msg.audioFile));
                    mPlayer.start();
                }
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);

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
        if (mChatService == null) {
            Log.d(TAG, "Setting up chat");
            setupChat();
        }
        db = new ChatMessages(getApplicationContext());
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

       //startPreviousChat();
    }

    private void startPreviousChat() {
        ArrayList<UserInfo> usersInfo = (ArrayList<UserInfo>) getIntent()
                .getSerializableExtra("USERS-INFO");

        if (usersInfo == null) {
            return;
        }


        for (UserInfo user : usersInfo) {
            connectDevice(user.macAddress);
        }
    }

    private void loadChatHistory(Intent intent) {
        Log.d(TAG, "Loading chat history");

        ArrayList<UserInfo> usersInfo = (ArrayList<UserInfo>) intent
                .getSerializableExtra("USERS-INFO");
        if (usersInfo == null) {
            return;
        }

        chatMessageAdapter.clear();

        List<ChatMessage> readMessages = getAllMessages(usersInfo, "Received");
        List<ChatMessage> sentMessages = getAllMessages(usersInfo, "Sent");
        List<ChatMessage> combinedMessages = ChatMessages.combineMessages(readMessages, sentMessages);

        String chatHistory = getChatHistory(combinedMessages);
        Log.d(TAG, "chatHistory: " + chatHistory);
        chatMessageAdapter.add(new MessageInstance(true,new String(chatHistory)));;
    }

    String getChatHistory(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();

        for (ChatMessage message : messages) {
            sb.append(message.user + " (" + message.timeStamp + "): " + message.message + "\n");
        }

        return sb.toString();
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
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "back pressed");

        if (mChatService != null) {
            mChatService.stop();
        }
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
                Intent bluetoothIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(bluetoothIntent, REQUEST_CONNECT_DEVICE);
                break;
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
                Toast.makeText(this, "You have declined the permissions. Please allow them first to proceed.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            // Request image from the gallery app
            requestImageFromGallery();
        }
    }

    public void requestImageFromGallery() {
        Intent attachImageIntent = new Intent();
        attachImageIntent.setType("image/*");
        attachImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(attachImageIntent, "Select Picture"), SELECT_IMAGE);
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
            if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
                        String encodedImage = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                        Log.d(TAG, "Base64 encoded string: " + encodedImage);
//                        mChatService = new BluetoothChatService(mHandler);
                        mChatService.write(encodedImage.getBytes(), DATATYPE_IMAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
                break;

            case CAMERA_REQUEST:
                if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
                        String encodedImage = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                        Log.d(TAG, "Base64 encoded string: " + encodedImage);
                        mChatService.write(encodedImage.getBytes(), DATATYPE_IMAGE);
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
        // Attempt to connect to the device
        mChatService.connect(device);
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("y-MM-dd HH:mm:ss");

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //FragmentActivity activity = getActivity();
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
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:

                    if (msg.arg2 == 1) {

                        System.out.println("IIINSIIIDEE MESSAGE");
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        Calendar calendar = Calendar.getInstance();
                        String time = sdf.format(calendar.getTime());

                        chatMessageAdapter.add(new MessageInstance(true, new String(writeMessage)));
                        chatMessageAdapter.notifyDataSetChanged();
                        // Write messages to database
                        // Add mAddress and mConnectedDeviceName to db for future recovery
                        db.insertSentMessage(time, mConnectedDeviceAddress, writeMessage);
                    }
                    else if (msg.arg2 == 2) {
                        System.out.println("IIINSIIIDEE IMAGE");
                        imageBitmap = (Bitmap) msg.obj;
                        if (imageBitmap != null) {
                            chatMessageAdapter.add(new MessageInstance(true, imageBitmap));
                            chatMessageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    } else if (msg.arg2 == 3) {
                        File f = new File(fileName);
                        chatMessageAdapter.add(new MessageInstance(true, f));
                        chatMessageAdapter.notifyDataSetChanged();
                    }
                    break;

                case MESSAGE_READ:
                    if (msg.arg2 == 1) {
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        Calendar cal = Calendar.getInstance();
                        String readTime = sdf.format(cal.getTime());


                        chatMessageAdapter.add(new MessageInstance(false, new String(readBuf)));
                        chatMessageAdapter.notifyDataSetChanged();

                        // Write messages to db
                        db.insertReceivedMessage(readTime, mConnectedDeviceAddress, new String(readBuf));
                    }
                    else if (msg.arg2 == 2) {
                        imageBitmap = (Bitmap) msg.obj;
                        if (imageBitmap != null) {
                            chatMessageAdapter.add(new MessageInstance(false, imageBitmap));
                            chatMessageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    } else if (msg.arg2 == 3) {
                        String filename = getFilename();
                        FileOutputStream fos;
                        try {
                            if (filename != null) {
                                byte[] buff = (byte[]) msg.obj;
                                fos = new FileOutputStream(filename);
                                fos.write(buff);
                                fos.flush();
                                fos.close();
                                chatMessageAdapter.add(new MessageInstance(false, new File(filename)));
                                chatMessageAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ChatActivity.this, "Could not save the file", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Could not save the file", e);
                        }
                    }
                    break;


                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }

                    // Check if user is in DB, if so, retrieve chat history
                    // else clear screen

                    Log.d(TAG, "Before check if in db");
                    if (db.isUserInDb(mConnectedDeviceAddress)) {
                        Log.d(TAG, "User in db");
                        Intent intent = new Intent();
                        ArrayList<UserInfo> users = new ArrayList<>();
                        users.add(new UserInfo(mConnectedDeviceName, mConnectedDeviceAddress));
                        intent.putExtra("USERS-INFO", users);
                        loadChatHistory(intent);
                    } else {
                        Log.d(TAG, "User not in db");
                        try {
                            mConversationArrayAdapter.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // insert users name and mac address to the database
                    try {
                        db.insertUserName(mConnectedDeviceAddress, mConnectedDeviceName);
                    } catch(Exception e) {
                        Log.d(TAG, "Unique");
                    }

                    break;
                case MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                Toast.LENGTH_SHORT).show();

                        if (msg.getData().getString(TOAST).equals("Device connection was lost")) {
                            finish();
                        }
                    }
                    break;
            }
        }
    };

    public String getPath(Uri uri) {
        if (uri == null) {
            Toast.makeText(getApplicationContext(), "Uri is null", Toast.LENGTH_SHORT).show();
            return null;
        }

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        // this is our fallback here
        return uri.getPath();
    }


    private void setupChat() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

//        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                String message = mEditText.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {

            System.out.println("Message Length = "+message.length());
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
//            mChatService.write(send);
            mChatService.write(message.getBytes(),DATATYPE_TEXT);

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
                sendMessage(message);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Do the external storage related work here.

                    requestImageFromGallery();
                } else {
                    // Permission is denied.
                    Toast.makeText(this, "Can't Proceed. You rejected the permission.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissiontoRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    private String getFilename(){
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if(!appFolder.exists()) {
            if(!appFolder.mkdirs()) {
                Toast.makeText(this, "Could not create App folder. Any activity requiring storage is suspended", Toast.LENGTH_LONG).show();
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
        if(null != mRecorder){
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            if(mChatService != null) {
                try {
                    File f = new File(fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buff = new byte[(int)f.length()];
                    fis.read(buff);
                    mChatService.write(buff, DATATYPE_FILE);
                    fis.close();
                } catch(Exception e) {
                    Log.e(TAG, "Could not open stream to save data", e);
                }
            }
        }

    }

    /*
    * Handle record play
    * */
    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    /*
    * Create new MediaPlayer object. Set its resource to file and start playback
    * */
    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    /*
    * Release he player and reset it to null
    * */
    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

}
