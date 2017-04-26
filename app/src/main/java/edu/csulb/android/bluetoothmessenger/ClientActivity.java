package edu.csulb.android.bluetoothmessenger;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static edu.csulb.android.bluetoothmessenger.FrontActivity.bluetoothAdapter;

public class ClientActivity extends AppCompatActivity {

//	private final static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private Context context;
	private static BluetoothManager mngr;
	private static ArrayList<BluetoothDevice> foundDevices;
//	private  BluetoothSocket bluetoothSocket;
	private  BluetoothDevice bluetoothDevice;
	private AsyncServerComponent server;
	public  static BluetoothSocket bluetoothSocket;


	private ArrayAdapter<String> adapter;

//	public static  BluetoothAdapter bluetoothAdapter;
	private BluetoothAdapter bluetoothAdapter1;
	private ToggleButton toggleButton;
	private ListView listview;

	private static final int ENABLE_BT_REQUEST_CODE = 1;
	private static final int DISCOVERABLE_BT_REQUEST_CODE = 2;
	private static final int DISCOVERABLE_DURATION = 300;

	private  UILink mUpdater;
	private ConnectionManager mManager;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		context=this;


		toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

		listview = (ListView) findViewById(R.id.devicesList);
		adapter = new ArrayAdapter
				(this,android.R.layout.simple_list_item_1);
		listview.setAdapter(adapter);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (!bluetoothAdapter.isEnabled())
		{
			// check if the bluetooth is enabled

			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			context.startActivity(enableBluetooth);

		}

		MyDeviceData.adress = bluetoothAdapter.getAddress();
		MyDeviceData.name = bluetoothAdapter.getName();

		// ListView Item Click Listener
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id) {
				// ListView Clicked item value
//				String  itemValue = (String) listview.getItemAtPosition(position);
//
//				String MAC = itemValue.substring(itemValue.length() - 17);
//
//				BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(MAC);

				// Initiate a connection request in a separate thread
//				ConnectingThread t = new ConnectingThread(bluetoothDevice);
//				t.start();

//				Intent next = new Intent(ClientActivity.this, IntermediateActivity.class);
				Intent next = new Intent(ClientActivity.this, ConnectedActivity.class);
				next.putExtra("index", position);
				mngr.stopDiscovery();
				context.startActivity(next);

			}
		});

	}

//	@Override
//	protected void onResume() {
//		super.onResume();
//		// Register the BroadcastReceiver for ACTION_FOUND
//
//		mngr.onResume();
//	}
//	@Override
//	protected void onPause() {
//		super.onPause();
//		mngr.onPause();
//	}

	public void Search(View view) {

		//adapter.clear();

		ToggleButton toggleButton = (ToggleButton) view;
//		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			// Device does not support Bluetooth
			Log.i(TAG, "Bluetooth not supported");

			Toast.makeText(getApplicationContext(), "Oop! Your device does not support Bluetooth",
					Toast.LENGTH_SHORT).show();
			toggleButton.setChecked(false);
		} else {

			if (toggleButton.isChecked()) { // to turn on bluetooth
//				if (!bluetoothAdapter.isEnabled()) {
				// To scan for remote Bluetooth devices
//				if (!bluetoothAdapter.isEnabled())
//				{
//					// check if the bluetooth is enabled
//					Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//					context.startActivity(enableBluetooth);
//				}

//				manager();

				mngr = new BluetoothManager(this.context, adapter,bluetoothAdapter);
				boolean flag = mngr.checkDevices();

				System.out.println("flag: "+flag);

				// Start a thread to create a  server socket to listen
				// for connection request
//				call_listening_thread();
//Nisarg code
				if(flag) {
					bluetoothAdapter.enable();
					System.out.println("bluon");
					makeDiscoverable();
					discoverDevices();
					ListeningThread t = new ListeningThread();

						t.start();
				}


//				ListeningThread();

			} else { // Turn off bluetooth

				bluetoothAdapter.disable();
				adapter.clear();
				Toast.makeText(getApplicationContext(), "Your device is now disabled.",
						Toast.LENGTH_SHORT).show();
			}

		}
	}



	protected void discoverDevices(){
		// To scan for remote Bluetooth devices
		int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
		ActivityCompat.requestPermissions(this,
				new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
				MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

		if (bluetoothAdapter.startDiscovery()) {
			Toast.makeText(getApplicationContext(), "Discovering other bluetooth devices...",
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Discovery failed to start.",
					Toast.LENGTH_SHORT).show();
		}
	}

	protected void makeDiscoverable(){
		// Make local device discoverable

		Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		context.startActivity(discoverable);
	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.front, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_audio_client) {
			return true;
		}
		if (id == R.id.action_image_client) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private UILink asdf = new UILink()
	{
		@Override
		public void useData(String... args)
		{
			Log.d("BLT", "de aci " + args[0]);
		}
	};


	private class ListeningThread extends Thread {
		private  BluetoothServerSocket bluetoothServerSocket;

		public ListeningThread() {
			BluetoothServerSocket temp = null;
			mUpdater = asdf;


			if (bluetoothAdapter == null)
			{
				// check if the device has Bluetooth
				return ;
			}
			if (!bluetoothAdapter.isEnabled())
			{
				// check if the bluetooth is enabled
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				context.startActivity(enableBluetooth);
			}

			try {
				temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BLT", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			} catch (IOException e) {
					e.printStackTrace();
				}
				bluetoothServerSocket = temp;
			}



		public void run() {
//			BluetoothSocket bluetoothSocket;
			// This will block while listening until a BluetoothSocket is returned
			// or an exception occurs
			while (true) {
				try {
					bluetoothSocket = bluetoothServerSocket.accept();
				} catch (IOException e) {
					break;
				}
				// If a connection is accepted
				if (bluetoothSocket != null) {

					try
					{
						bluetoothServerSocket.close();
						Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
//						intent.putExtra("Server_socket",bluetoothSocket.toString());
//						intent.putExtra("Updater",mUpdater.toString());
						startActivity(intent);
//						mManager = new ConnectionManager(bluetoothSocket , mUpdater);
//						mManager.execute();
						break;
					}
					catch (IOException e)
					{
						break;
					}

				}

				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

		}


	}



		public void ListeningThread() {

		server = new AsyncServerComponent(this, asdf);
		server.execute();

	}

//	private class ConnectingThread extends Thread {
//
//
//		public ConnectingThread(BluetoothDevice device) {
//
//			BluetoothSocket temp = null;
//			bluetoothDevice = device;
//
//			// Get a BluetoothSocket to connect with the given BluetoothDevice
//			try {
//				temp = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			bluetoothSocket = temp;
//		}
//
//		public void run() {
//			// Cancel discovery as it will slow down the connection
//			bluetoothAdapter.cancelDiscovery();
//
//			try {
//				// This will block until it succeeds in connecting to the device
//				// through the bluetoothSocket or throws an exception
//				bluetoothSocket.connect();
//			} catch (IOException connectException) {
//				connectException.printStackTrace();
//				try {
//					bluetoothSocket.close();
//				} catch (IOException closeException) {
//					closeException.printStackTrace();
//				}
//			}
//		}

//		protected Void doInBackground(Void... params)
//		{
//			try
//			{
//				bluetoothSocket.connect();
//			}
//			catch (Exception connectEr)
//			{
//				try
//				{
//					Log.d("BLT", connectEr.getMessage());
//					this.publishProgress("Connection to " + bluetoothSocket.getRemoteDevice().getName() + " has failed!");
//					bluetoothSocket.close();
//					return null;
//				}
//				catch (IOException closeEr)
//				{
//					Log.d("BLT", closeEr.getMessage());
//					this.publishProgress("Connection to " + bluetoothSocket.getRemoteDevice().getName() + " has failed!");
//					return null;
//				}
//			}
//			mngr = new ConnectionManager(bluetoothSocket, mUpdater);
//			mngr.execute();
//			this.publishProgress("Connection established to " + bluetoothSocket.getRemoteDevice().getName());
//
//			return null;
//		}
//
//			// Code to manage the connection in a separate thread
//            /*
//               manageBluetoothConnection(bluetoothSocket);
//            */
//		}


		// Cancel an open connection and terminate the thread
//		public void cancel() {
//			try {
//				bluetoothSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	public static BluetoothDevice getDevice(int index)
	{
		return mngr.getDevice(index);

	}
//	public void onDestroy()
//	{
//		mngr.stopDiscovery();
//		mngr.destroy();
//		mngr = null;
//		super.onDestroy();
//	}
}
