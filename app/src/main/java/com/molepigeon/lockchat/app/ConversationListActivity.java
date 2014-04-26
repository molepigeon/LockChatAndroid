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
    public static ArrayList<String> people = new ArrayList<String>();
    public static ArrayList<String> IDs = new ArrayList<String>();
    private static boolean firstRun = true;
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
            text = "Debug User - Loopback";
            people.add(text);
            adapter.notifyDataSetChanged();
            //End debug user

            //TODO register device
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
        IDs.add(nfcMessage);
        new UserGetter().execute();
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
}
