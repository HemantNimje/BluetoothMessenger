package edu.csulb.android.bluetoothmessenger;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import static edu.csulb.android.bluetoothmessenger.ChatMessages.GROUP_CHAT_USER_TABLE;
import static edu.csulb.android.bluetoothmessenger.ChatMessages.USER_NAMES_TABLE;
import static edu.csulb.android.bluetoothmessenger.ChatMessages.orderGroupChat;

public class MainActivity extends AppCompatActivity {

    private ListView chatHistoryListView;
    private ArrayList<String> previousChatNames;
    public static BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chatHistoryListView = (ListView) findViewById(R.id.list_chat_history);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                System.out.println(mBluetoothAdapter.enable());
                Intent newChatIntent = new Intent(getApplicationContext(), ChatActivity.class);
                startActivity(newChatIntent);
            }
        });


        // Start chat history and allow user to start connection
        // Currently ChatHistory is a stub to test that messages are in the
        // correct order
        chatHistoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                String users = chatHistoryListView.getItemAtPosition(position).toString();
                ArrayList<UserInfo> usersInfo = (ArrayList<UserInfo>) UserInfo.getUsersInfo(users);
                intent.putExtra("USERS-INFO", usersInfo);
                startActivity(intent);
            }
        });

    }

    // Update chat history when you return
    @Override
    protected void onResume() {
        super.onResume();
        ChatMessages db = new ChatMessages(getApplicationContext());
        previousChatNames = (ArrayList<String>) db.getPreviousChatNames(USER_NAMES_TABLE);

        ArrayList<String> previousGroupChatNames = (ArrayList<String>) db
                .getPreviousChatNames(GROUP_CHAT_USER_TABLE);

        List<String> orderedGroupChats = orderGroupChat(previousGroupChatNames);

        for (String group : orderedGroupChats) {
            previousChatNames.add(group);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, previousChatNames);
        chatHistoryListView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        switch(id) {
            case R.id.action_profile:
                Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(profileIntent);
                break;
            case R.id.action_groupchat:
                Intent groupChatIntent = new Intent(getApplicationContext(),
                        GroupChatDeviceListActivity.class);
                startActivity(groupChatIntent);
                break;
            case R.id.group_chat_make_discoverable:
                ensureDiscoverable();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
}
