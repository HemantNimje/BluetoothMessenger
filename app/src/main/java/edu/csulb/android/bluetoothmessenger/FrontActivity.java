package edu.csulb.android.bluetoothmessenger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

public class FrontActivity extends AppCompatActivity {
    private BluetoothManager bltManager;
    private Context context;
    private BluetoothAdapter bltAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front);

        // Enable the up button on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = this;
        bltAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bltAdapter.isEnabled()) {
            // check if the bluetooth is enabled
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBluetooth);
        }
        MyDeviceData.adress = bltAdapter.getAddress();
        MyDeviceData.name = bltAdapter.getName();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.front, menu);
        return true;
    }

    public void clientClick(View v) {
        Intent clt = new Intent(FrontActivity.this, ClientActivity.class);
        startActivity(clt);
    }

    public void serverClick(View v) {
        Intent srv = new Intent(FrontActivity.this, ServerActivity.class);
        startActivity(srv);
    }

}
