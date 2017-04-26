package edu.csulb.android.bluetoothmessenger;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;


public class FrontActivity extends AppCompatActivity {

	public static  BluetoothAdapter bluetoothAdapter;
	private Context context;
	private ToggleButton bluetooth_toggleButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_front);
		context=this;
		bluetooth_toggleButton = (ToggleButton) findViewById(R.id.btnBluetooth);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


            if (bluetoothAdapter.isEnabled()) {
                // check if the bluetooth is enabled
                bluetooth_toggleButton.setChecked(true);

            }


	}

	public void enableBluetoothActivity(View view) {

		ToggleButton bluetooth_toggleButton = (ToggleButton) view;
		if (bluetooth_toggleButton.isChecked()) { // to turn on bluetooth

			if (!bluetoothAdapter.isEnabled()) {
				// check if the bluetooth is enabled

				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				context.startActivity(enableBluetooth);

                Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
                startActivity(intent);

			}


		}
		else { // Turn off bluetooth


            bluetoothAdapter.disable();
			Toast.makeText(getApplicationContext(), "Your device bluetooth is now disabled.",
					Toast.LENGTH_SHORT).show();
		}
	}

//    public void enableWiFiActivity(View view) {
//
//        ToggleButton bluetooth_toggleButton = (ToggleButton) view;
//        if (bluetooth_toggleButton.isChecked()) { // to turn on WiFi
//
//
//                Intent intent = new Intent(getApplicationContext(), WiFiServiceDiscoveryActivity.class);
//                startActivity(intent);
//
//
//
//        }
//        else { // Turn off WiFi Direct
//
//            Toast.makeText(getApplicationContext(), "WiFi Direct is now disabled.",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

	public void getBluetoothActivity(View view) {
		Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
		startActivity(intent);
	}

    public void image_activity(View view)
    {
        Intent intent = new Intent(this, PhotoActivity.class);
        this.startActivity(intent);
    }
    public void audio_activity(View view)
    {
        Intent intent = new Intent(this, AudioActivity.class);
        this.startActivity(intent);
    }
}