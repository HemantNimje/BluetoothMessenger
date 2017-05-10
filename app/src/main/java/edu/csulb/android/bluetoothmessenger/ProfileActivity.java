package edu.csulb.android.bluetoothmessenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static android.provider.Settings.ACTION_SETTINGS;

public class ProfileActivity extends AppCompatActivity {

    public static String User_name;
    public static String mac_add;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }


    public void OpenSetting(View view) {

        Intent settingIntent = new Intent(ACTION_SETTINGS);
        startActivity(settingIntent);

    }

    public void SaveInfo(View view) {

        EditText name = (EditText) findViewById(R.id.profile_name);
        EditText macAddress = (EditText) findViewById(R.id.profile_mac_address);

        User_name = name.getText().toString();
        mac_add = macAddress.getText().toString();

        if(User_name.length()==0)
        {
            Toast.makeText(this, "Name can not be blank", Toast.LENGTH_SHORT).show();
            name.setError("Name can not be blank");
            return;
        }

        if(mac_add.length()==0)
        {
            Toast.makeText(this, "Mac address can not be blank", Toast.LENGTH_SHORT).show();
            macAddress.setError("Mac Address can not be blank");
            return;
        }

        finish();
    }

    public void CancelProfile(View view) {

        finish();
    }


    }
