package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static edu.csulb.android.bluetoothmessenger.FrontActivity.bluetoothAdapter;
import static edu.csulb.android.bluetoothmessenger.FrontActivity.bluetoothAdapter;

public class BluetoothManager
{
//	private BluetoothAdapter bluetoothAdapter;
	private Context context;
	private ArrayAdapter<String> list;
	private ArrayList<BluetoothDevice> foundDevices;
	private HashMap<String, Integer> hash;
	private static final int ENABLE_BT_REQUEST_CODE = 1;


	public BluetoothManager(Context context, ArrayAdapter<String> list,BluetoothAdapter bluetoothAdapter_client)
	{
		this.context = context;
//		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//		bluetoothAdapter = bluetoothAdapter_client;
		foundDevices = new ArrayList<BluetoothDevice>();
		hash = new HashMap<String, Integer>();
		this.list = list;
	}

	public boolean checkDevices()
	{

		if (bluetoothAdapter == null)
		{
			// check if the device has Bluetooth
			return false;
		}
		if (!bluetoothAdapter.isEnabled())
		{
			// check if the bluetooth is enabled
			System.out.println("INSERT checkDevices !!!");
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			context.startActivity(enableBluetooth);
			System.out.println("Enable !!!");
		}

		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0)
		{
			for (BluetoothDevice device : pairedDevices)
			{
				foundDevices.add(device);
				list.add("Name: " + device.getName() + "\n" + "Adress: " + device.getAddress() + "\nPaired and not in range");
				hash.put(device.getAddress(), hash.size());
			}
			list.notifyDataSetChanged();
		}

//		discoverDevices();
		// To scan for remote Bluetooth devices
//		bluetoothAdapter.startDiscovery();
//		if (bluetoothAdapter.startDiscovery()) {
//			Toast.makeText(context, "Discovering other bluetooth devices...",
//					Toast.LENGTH_SHORT).show();
//		} else {
//			Toast.makeText(context, "Discovery failed to start.",
//					Toast.LENGTH_SHORT).show();
//		}
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(receiver, filter);
		return true;
	}




//	protected void discoverDevices(){
//		// To scan for remote Bluetooth devices
//
//
//		if (bluetoothAdapter.startDiscovery()) {
//			Toast.makeText(context, "Discovering other bluetooth devices...",
//					Toast.LENGTH_SHORT).show();
//		} else {
//			Toast.makeText(context, "Discovery failed to start.",
//					Toast.LENGTH_SHORT).show();
//		}
//	}



//	public  void visible(){
//		Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//		discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//		context.startActivity(discoverable);
//	}

	private final BroadcastReceiver receiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (!hash.containsKey(device.getAddress()))
				{
					foundDevices.add(device);
					list.add("Name: " + device.getName() + "\n" + "Adress: " + device.getAddress() + "\nNot paired and in range");
					list.notifyDataSetChanged();
					Log.d("BLT", "Name: " + device.getName() + "\n" + "Adress: " + device.getAddress());
				}
				else
				{
					int index = hash.get(device.getAddress());
					String value = list.getItem(index);
					list.remove(value);
					value = ("Name: " + device.getName() + "\n" + "Adress: " + device.getAddress() + "\nPaired and in range");
					list.insert(value, index);
					list.notifyDataSetChanged();
				}
			}
		}

	};


	protected void onPause() {

		context.unregisterReceiver(receiver);
	}

	protected void onResume() {
//		super.onResume();
		// Register the BroadcastReceiver for ACTION_FOUND
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(receiver, filter);
	}

	public void stopDiscovery()
	{
		bluetoothAdapter.cancelDiscovery();
	}

	public void destroy()
	{
		bluetoothAdapter = null;
		foundDevices.clear();
		foundDevices = null;
		list.clear();
		list = null;
		hash.clear();
		hash = null;
	}

	public BluetoothDevice getDevice(int index)
	{
		return foundDevices.get(index);
	}

}
