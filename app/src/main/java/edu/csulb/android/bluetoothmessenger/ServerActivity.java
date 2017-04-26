package edu.csulb.android.bluetoothmessenger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import static edu.csulb.android.bluetoothmessenger.ClientActivity.bluetoothSocket;
import static edu.csulb.android.bluetoothmessenger.ClientActivity.bluetoothSocket;

public class ServerActivity extends AppCompatActivity
{
	private AsyncServerComponent server;
	private EditText chatText;
	private EditText inputText;
	private ConnectionManager mManager;
	private  UILink mUpdater;

	private UILink asdf = new UILink()
	{
		@Override
		public void useData(String... args)
		{
			Log.d("BLT", "de aci " + args[0]);
			chatText.append(args[0] + "\n");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		chatText = (EditText) findViewById(R.id.serverEditText);
		inputText = (EditText) findViewById(R.id.serverInput);

//
//		Bundle extras = this.getIntent().getExtras();
//		mUpdater = extras.getInt("Updater");

		mManager = new ConnectionManager(bluetoothSocket , asdf);
		mManager.execute();
//		server = new AsyncServerComponent(this, asdf);
//		server.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.server, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_audio_server:
			//	uninstall();
				return true;

//			case R.id.delete:
//				DatabaseHandler db = new DatabaseHandler(getApplicationContext());
//
//				db.DeleteNote();
//				pic_captions.clear();
//				pic_captions.addAll(db.getAllNotes()); // reload the items from database
//				dataAdapter.notifyDataSetChanged();
//				return true;

		}
		return super.onOptionsItemSelected(item);
	}



//	@Override
//	public void onDestroy()
//	{
//		server.closeSockets();
//		server.cancel(true);
//		super.onDestroy();
//	}

	public void SendClick(View view)
	{
		try
		{
			String text = inputText.getText().toString();
			chatText.append(MyDeviceData.name + ": " + text + "\n");
			write(MyDeviceData.name + ": " + text + "\n");
			inputText.setText("");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void write(String data)
	{
		mManager.write(data);
	}

}
