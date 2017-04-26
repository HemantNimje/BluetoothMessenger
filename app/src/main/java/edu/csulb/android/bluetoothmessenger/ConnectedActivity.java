package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class ConnectedActivity extends AppCompatActivity
{
	private int deviceIndex;
	private BluetoothDevice deviceToConnect;
	private AsyncClientComponent client;
	private EditText chatText;
	private EditText inputText;
	private ConnectionManager mManager;

	private UILink updater = new UILink()
	{
		@Override
		public void useData(String... args)
		{
			chatText.append(args[0] + "\n");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connected);
		chatText = (EditText) findViewById(R.id.clientEditText);
		inputText = (EditText) findViewById(R.id.clientInput);
		Bundle extras = this.getIntent().getExtras();
		deviceIndex = extras.getInt("index");
		deviceToConnect = ClientActivity.getDevice(deviceIndex);
		client = new AsyncClientComponent(deviceToConnect, updater);
		client.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.connected, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {
			case R.id.action_audio_client:
				Intent intent = new Intent(this, AudioActivity.class);
				this.startActivity(intent);
				break;
			case R.id.action_image_client:
				// another startActivity, this is for item with id "menu_item2"
				Intent image = new Intent(this, PhotoActivity.class);
				this.startActivity(image);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}

		return true;

	}

//	public void onDestroy()
//	{
//		client.closeSockets();
//		client.cancel(true);
//		super.onDestroy();
//	}

	public void SendClick(View view)
	{
		try
		{
			String text = inputText.getText().toString();
			chatText.append(MyDeviceData.name + ": " + text + "\n");
			client.write(MyDeviceData.name + ": " + text + "\n");
			inputText.setText("");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
//	public void write(String data)
//	{
//		mManager.write(data);
//	}
}

