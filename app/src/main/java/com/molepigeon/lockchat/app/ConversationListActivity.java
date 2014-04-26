package com.molepigeon.lockchat.app;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.nfc.NdefRecord.createMime;

public class ConversationListActivity extends Activity
        implements CreateNdefMessageCallback {

    public static final String PEOPLE_MESSAGE = "people_message";
    public static final String ID_MESSAGE = "id_message";
    public static String myName = "";
    public static ArrayList<String> people = new ArrayList<String>();
    public static ArrayList<String> IDs = new ArrayList<String>();
    private static boolean firstRun = true;
    private static String lastNFC = "";
    NfcAdapter mNfcAdapter;
    private String nfcMessage = "";
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println(Secure.getString(getContentResolver(), Secure.ANDROID_ID));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, people);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(view.getContext(), ConversationDetailActivity.class);
                String messageText = people.get(position);
                intent.putExtra(PEOPLE_MESSAGE, messageText);
                messageText = IDs.get(position);
                intent.putExtra(ID_MESSAGE, messageText);
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(view.getContext(), "Conversation with " + people.get(position) + " deleted.", Toast.LENGTH_SHORT).show();
                people.remove(position);
                IDs.remove(position);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mNfcAdapter.setNdefPushMessageCallback(this, this);

        if (firstRun) {
            Toast.makeText(this, "Beam with another device with LockChat to get started!", Toast.LENGTH_LONG).show();

            //DEBUG user - Delete this when finished
            String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID));
            IDs.add(text);
            text = "Loopback";
            people.add(text);
            adapter.notifyDataSetChanged();
            //End debug user

            new FindMyName().execute("");

        }
        firstRun = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conversation_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_person) {
            //openSettings();
            Intent intent = new Intent(this, Register.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID));
        return new NdefMessage(
                new NdefRecord[]{createMime(
                        "application/vnd.com.molepigeon.lockchat.app", text.getBytes())
                        , NdefRecord.createApplicationRecord("com.molepigeon.lockchat.app")
                }
        );
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        //Toast.makeText(this, new String(msg.getRecords()[0].getPayload()), Toast.LENGTH_SHORT).show();
        nfcMessage = new String(msg.getRecords()[0].getPayload());
        if (!lastNFC.contentEquals(nfcMessage)) {
            IDs.add(nfcMessage);
            new UserGetter().execute();
            lastNFC = nfcMessage;
        }
    }

    private class UserGetter extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getUser(nfcMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        protected void onPostExecute(String result) {
            try {
                people.add(result);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class FindMyName extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getUser(Secure.getString(getContentResolver(), Secure.ANDROID_ID));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        protected void onPostExecute(String result) {
            try {
                myName = result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
