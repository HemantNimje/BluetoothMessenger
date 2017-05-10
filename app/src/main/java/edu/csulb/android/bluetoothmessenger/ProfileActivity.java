package edu.csulb.android.bluetoothmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import static android.provider.Settings.ACTION_SETTINGS;

public class ProfileActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    }


    public void OpenSetting(View view) {
        Intent settingIntent = new Intent(ACTION_SETTINGS);
        startActivity(settingIntent);

    }
}
