package edu.csulb.android.bluetoothmessenger;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;

import static edu.csulb.android.bluetoothmessenger.FrontActivity.bluetoothAdapter;

public class AudioActivity extends AppCompatActivity {

    private Button  btnSend;
    private ToggleButton btnRecord,btnPlay;
    private TextView recordName;
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_STORE_AUDIO_PERMISSION = 200;

    private static String mFileName = null;

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    File file;
    private String outputFile = null;

    // requesting permission to RECORD_AUDIO
    private boolean permissiontoRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);


        // Record to the external cache directory for visibility
          mFileName = getExternalCacheDir().getAbsolutePath()+ "/myRecording.mp3";
//          mFileName =   Environment.getExternalStorageDirectory().getAbsolutePath()+ "/myRecording.mp3";

//          mFileName += "/myRecording.mp3";

//        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_STORE_AUDIO_PERMISSION);

        btnRecord = (ToggleButton) findViewById(R.id.button_record);

//        btnRecord.setOnClickListener(new View.OnClickListener() {
//            boolean mStartRecording = true;
//
//            @Override
//            public void onClick(View view) {
//                onRecord(mStartRecording);
////                if (mStartRecording) {
////                    btnRecord.setText(getResources().getString(R.string.stop_recording));
////                } else {
////                    btnRecord.setText(getResources().getString(R.string.start_recording));
////                }
//                mStartRecording = !mStartRecording;
//            }
//        });

        btnPlay = (ToggleButton) findViewById(R.id.button_play);
//        btnPlay.setOnClickListener(new View.OnClickListener() {
//            boolean mStartPlaying = true;
//
//            @Override
//            public void onClick(View view) {
//                onPlay(mStartPlaying);
////                if (mStartPlaying) {
////                    btnPlay.setText(getResources().getString(R.string.stop));
////                } else {
////                    btnPlay.setText(getResources().getString(R.string.play));
////                }
//                mStartPlaying = !mStartPlaying;
//            }
//        });
    }

    public void Record(View view) {

        ToggleButton toggleButton = (ToggleButton) view;

        if (toggleButton.isChecked()) {

//            boolean mStartRecording = true;
//            onRecord(mStartRecording);
            startRecording();
//                if (mStartRecording) {
//                    btnRecord.setText(getResources().getString(R.string.stop_recording));
//                } else {
//                    btnRecord.setText(getResources().getString(R.string.start_recording));
//                }
//            mStartRecording = !mStartRecording;
         }
         else
        {
            stopRecording();
        }
    }

    public void Play(View view) {

        ToggleButton toggleButton = (ToggleButton) view;

        if (toggleButton.isChecked()) {

//            boolean mStartPlaying = true;
//            onPlay(mStartPlaying);
            startPlaying();
//                if (mStartRecording) {
//                    btnRecord.setText(getResources().getString(R.string.stop_recording));
//                } else {
//                    btnRecord.setText(getResources().getString(R.string.start_recording));
//                }
//            mStartPlaying = !mStartPlaying;


        }
        else
        {
            stopPlaying();
        }
    }


//        @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case REQUEST_RECORD_AUDIO_PERMISSION:
//                permissiontoRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
//                break;
//        }
//        if (!permissiontoRecordAccepted) finish();
//    }

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
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mRecorder.start();
    }

    /*
    * Stop the recorder and release it
    * */
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        TextView fileNameTextView = (TextView) findViewById(R.id.record_file_name);
        fileNameTextView.setText(mFileName);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.audio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_audio_client:
                Intent intent = new Intent(this, ConnectedActivity.class);
                this.startActivity(intent);
                break;
            case R.id.action_image_client:
                // another startActivity, this is for item with id "menu_item2"
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;

    }

    public void sendVoice(View view) {
        try {
//            mRecorder.stop();
//            mRecorder.release();
//            mRecorder = null;

//            text1.setText("Recording Point: Stop recording");

            Toast.makeText(getApplicationContext(), "Send recording...", Toast.LENGTH_SHORT).show();

            if (bluetoothAdapter.isEnabled()) {

                Log.d("btAdapter", "btAdapter" + bluetoothAdapter);
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("/myRecording.mp3");
                sharingIntent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
                file = new File(mFileName);
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                startActivity(sharingIntent);
                Log.d("/myRecording.mp3", "SharingIntent");

            }
            else {
//                text1.setText("Bluetooth not activated");
            }

        }
        catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        }
        catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        }
    }



}
