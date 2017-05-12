package edu.csulb.android.bluetoothmessenger;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupChatDeviceListActivity extends Activity {

    private MyCustomAdapter mNewDeviceArrayAdapter = null;
    private BluetoothAdapter mBtAdapter;
    int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    ArrayList<Device> deviceList = new ArrayList<>();
    private ListView deviceListView;
    private LinearLayout layout;
    private static final String TAG = "GroupChatList";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.group_chat_bluetooth_device_list);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        layout = (LinearLayout) findViewById(R.id.group_new_devices_layout);

        mNewDeviceArrayAdapter = new MyCustomAdapter(this, R.layout.custom_row, deviceList);
        deviceListView = (ListView) findViewById(R.id.group_new_devices);

        /* Added from the DeviceListActivity */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(new Device(device.getName(), device.getAddress(), false));
            }
        }

        discoverDevices();

        deviceListView.setAdapter(mNewDeviceArrayAdapter);

        Button connectButton = (Button) findViewById(R.id.group_new_devices_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Device> selectedDevices = new ArrayList<Device>();
                for (int i = 0; i < deviceList.size(); i++) {
                    if (deviceList.get(i).isSelected()) {
                        selectedDevices.add(deviceList.get(i));
                    }
                }

                ArrayList<UserInfo> usersForGroupChat = new ArrayList<>();
                for (int i = 0; i < selectedDevices.size(); i++) {
                    Log.d(TAG, "Checkboxes get: " + selectedDevices.get(i).getName());
                    String name = selectedDevices.get(i).getName();
                    String macAddress = selectedDevices.get(i).getAddress();
                    usersForGroupChat.add(new UserInfo(name, macAddress));
                }

                Intent startGroupChat = new Intent(getApplicationContext(), ChatActivity.class);
                startGroupChat.putExtra("USERS-INFO", usersForGroupChat);
                startActivity(startGroupChat);
                finish();
            }
        });

    }

    /*
    * Start device discovery using bluetooth adapter
    * */
    private void discoverDevices() {
        //Toast.makeText(this, "discoverDevices()", Toast.LENGTH_SHORT).show();

        setTitle("Scanning for devices");

        // If we are already discovering bluetooth devices stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);


        // Request device discovery from bluetooth adapter
        mBtAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    deviceList.add(new Device(device.getName(), device.getAddress(), false));
                    mNewDeviceArrayAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle(R.string.select_device);
                if (mNewDeviceArrayAdapter.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), R.string.none_found, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private class MyCustomAdapter extends ArrayAdapter<Device> {

        private ArrayList<Device> devices;

        public MyCustomAdapter(Context context, int resource, ArrayList<Device> devices) {
            super(context, resource, devices);
            this.devices = devices;
        }

        private class ViewHolder {
            CheckBox deviceCheckBox;
            TextView deviceName;
            TextView deviceAddress;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            /* View recycling */
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_row, null);

                /* Create a viewholder to hold the different elements of single row */
            holder = new ViewHolder();
            holder.deviceCheckBox = (CheckBox) convertView.findViewById(R.id.device_checkbox);
            holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
            holder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
            convertView.setTag(holder);

            final ViewHolder finalHolder = holder;

            /* Handle the click of the checkbox */
            holder.deviceCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deviceList.get(position).setSelected(!deviceList.get(position).isSelected());
                    if (!finalHolder.deviceCheckBox.isChecked()){
                        finalHolder.deviceCheckBox.setChecked(true);
                    }else {
                        finalHolder.deviceCheckBox.setChecked(false);
                    }
                    mNewDeviceArrayAdapter.notifyDataSetChanged();
                }
            });

            /* Handle the click of the complete view */
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Toast.makeText(getApplicationContext(), deviceList.get(position).getAddress(), Toast.LENGTH_SHORT).show();
                    deviceList.get(position).setSelected(!deviceList.get(position).isSelected());
                    if (!finalHolder.deviceCheckBox.isChecked()) {
                        finalHolder.deviceCheckBox.setChecked(true);
                    } else {
                        finalHolder.deviceCheckBox.setChecked(false);
                    }
                    mNewDeviceArrayAdapter.notifyDataSetChanged();

                }
            });

            Device device = deviceList.get(position);
            holder.deviceName.setText("(" + device.getName() + ")");
            holder.deviceAddress.setText("(" + device.getAddress() + ")");
            holder.deviceCheckBox.setChecked(device.isSelected());
            holder.deviceCheckBox.setTag(device);

            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
}
