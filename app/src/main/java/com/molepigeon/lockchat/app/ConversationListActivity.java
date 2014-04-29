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
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;

import static android.nfc.NdefRecord.createMime;

/**
 * Companion class for the Conversation List screen.
 * <p/>
 * Includes NFC and Internet functionality.
 *
 * @author Michael Hough
 */
public class ConversationListActivity extends Activity implements CreateNdefMessageCallback {

    /**
     * String reference used to identify the people ArrayList in intent extra information
     */
    public static final String PEOPLE_MESSAGE = "people_message";

    /**
     * String reference used to identify the IDs ArrayList in intent extra information
     */
    public static final String ID_MESSAGE = "id_message";

    /**
     * String reference used to identify the publicKeys ArrayList in intent extra information
     */
    public static final String KEY_MESSAGE = "key_message";

    /**
     * String containing the device's display name.
     */
    public static String myName = "";

    /**
     * List of contact names.
     */
    public static ArrayList<String> people = new ArrayList<String>();

    /**
     * List of device IDs.
     */
    public static ArrayList<String> IDs = new ArrayList<String>();

    /**
     * List of public keys.
     */
    public static ArrayList<String> publicKeys = new ArrayList<String>();

    /**
     * RSA key pair for this device.
     */
    public static KeyPair thisKey;

    /**
     * Boolean value stating whether this is the first time the activity has been created.
     */
    private static boolean firstRun = true;

    /**
     * String containing the payload of the last NFC transfer sent to this device
     */
    private static String lastNFC = "";

    /**
     * NFC adapter
     */
    NfcAdapter mNfcAdapter;

    /**
     * String containing an the device ID of a device that just beamed to this device
     */
    private String nfcMessage = "";

    /**
     * Array Adapter for monitoring the names array
     */
    private ArrayAdapter<String> adapter;

    /**
     * Called as part of the activity lifecycle when the activity (or the app, since this is the
     * launcher activity) is first created, or when it is recreated (like when the device rotates)
     * <p/>
     * This method sets up the connection between this class and the ListView in the activity's
     * layout, enables NFC, and sets up an RSA key pair.
     *
     * @param savedInstanceState Can be used to restore the activity on auto-rotate or back presses.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        //Set up the array adapter to connect to the people ArrayList
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, people);

        //Find the list view in the layout
        ListView listView = (ListView) findViewById(R.id.listView);

        //Link the array adapter and the list view
        listView.setAdapter(adapter);

        //Set a listener to open the relevant conversation when it is tapped in the list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Create an intent for the ConversationDetailActivity
                Intent intent = new Intent(view.getContext(), ConversationDetailActivity.class);

                //Put the extra information in
                String messageText = people.get(position);
                intent.putExtra(PEOPLE_MESSAGE, messageText);
                messageText = IDs.get(position);
                intent.putExtra(ID_MESSAGE, messageText);
                messageText = publicKeys.get(position);
                intent.putExtra(KEY_MESSAGE, messageText);

                //Start the ConversationDetailActivity
                startActivity(intent);
            }
        });

        //Set a listener to delete the relevant conversation when it is long clicked in the list
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //Make a new toast to inform the user of what's happened.
                Toast.makeText(view.getContext(), "Conversation with " + people.get(position) +
                        " deleted.", Toast.LENGTH_SHORT).show();

                //Delete that conversation's information from all three arrays.
                people.remove(position);
                IDs.remove(position);
                publicKeys.remove(position);

                //Notify the adapter that the data set has changed, so it updates the ListView.
                adapter.notifyDataSetChanged();

                return true;
            }
        });

        //Set up the NFC connection
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            //If NFC isn't available on this device, inform the user
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();

            //And exit the app
            finish();

            return;
        }
        //Set the NFC adapter to listen for NFC messages
        mNfcAdapter.setNdefPushMessageCallback(this, this);

        //This code is to only be run when the app first starts
        if (firstRun) {

            //Generate an RSA key pair
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                thisKey = keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Let the user know how to start a conversation
            Toast.makeText(this, "Beam with another device with LockChat to get started!",
                    Toast.LENGTH_LONG).show();

            //Debug user - Loops messages back to this device
            //Probably best to at least comment this for the demo. Or leave it. I'm a comment,
            //not your boss.
            String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID));
            IDs.add(text);
            text = "Loopback";
            people.add(text);
            publicKeys.add(Base64.encodeToString(thisKey.getPublic().getEncoded(),
                    Base64.URL_SAFE));
            adapter.notifyDataSetChanged();
            //End debug user

            //Fetch the display name for this device from the server
            new FindMyName().execute("");

        }

        // Don't run the first run stuff again.
        firstRun = false;
    }

    /**
     * Called as part of the activity lifecycle when the activity is resumed. This could happen
     * if it has been closed by the user pressing the home button, switching to another app,
     * or if they receive a beam.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    /**
     * Overridden method to handle an NFC intent.
     *
     * @param intent the intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Called when the activity is drawn to draw the action bar menu.
     *
     * @param menu the menu
     * @return a populated menu with information from the relevant menu file
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conversation_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Called when an item is tapped in the action bar to handle that action.
     *
     * The Up functionality provided by tapping the app icon or activity title are
     * handled automatically (assuming the parent activity is set correctly in the Manifest)
     *
     * @param item the menu item that was clicked
     * @return true if a menu item was clicked.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Get the Android ID of the menu item that was clicked
        int id = item.getItemId();

        //Look to see what the menu item was
        if (id == R.id.action_add_person) {
            //Register button was clicked, so create a new intent to start RegisterActivity...
            Intent intent = new Intent(this, RegisterActivity.class);
            //And start it
            startActivity(intent);

            return true;
        }

        //If the item isn't handled above
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the device is brought into range of another NFC enabled device. This method
     * constructs a string payload for the NFC transfer.
     *
     * NB: This is called every time the device is brought into range, whether a beam was sent
     * or not.
     *
     * The created string has the format:
     * (16 character Device ID)(space)(300 (about) character RSA public key)
     *
     * @param event not used
     * @return The NDEF record to be sent via NFC when the device initiates the transfer.
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID) + " " +
                Base64.encodeToString(thisKey.getPublic().getEncoded(), Base64.URL_SAFE));
        return new NdefMessage(
                new NdefRecord[]{createMime(
                        "application/vnd.com.molepigeon.lockchat.app", text.getBytes())
                        , NdefRecord.createApplicationRecord("com.molepigeon.lockchat.app")
                }
        );
    }

    /**
     * Called by onResume() to handle an incoming NFC message
     *
     * @see com.molepigeon.lockchat.app.ConversationListActivity#onResume()
     * @param intent the intent that was created
     */
    void processIntent(Intent intent) {
        //Get the NFC message(s) from the extra information of the intent
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // Fetch the message that was sent
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        String payload = new String(msg.getRecords()[0].getPayload());

        //Split the payload on the space
        String[] splitPayload = payload.split(" ");

        //Get the device ID from the message
        nfcMessage = splitPayload[0];

        //If the device isn't the last one to have beamed (stops duplicate conversations from being
        //created)
        if (!lastNFC.contentEquals(nfcMessage)) {
            //Add its ID to the list
            IDs.add(nfcMessage);
            //Fetch its display name from the server
            new UserGetter().execute();
            //Add the public key to the list
            publicKeys.add(splitPayload[1]);

            //Set this to be the last device that beamed
            lastNFC = nfcMessage;
        }
    }

    /**
     * Fetches the display name of the user given in nfcMessage
     *
     * @see com.molepigeon.lockchat.app.ConversationListActivity#nfcMessage
     */
    private class UserGetter extends AsyncTask<String, Void, String> {
        /**
         * Runs the network operation
         *
         * @see com.molepigeon.lockchat.app.Network#getUser(String)
         * @param params not used, but required for compliance with the abstract class
         * @return the returned display name
         */
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

        /**
         * Called when doInBackground completes on the UI thread to update the user interface
         * with the results from the network query.
         *
         * @param result the display name returned in doInBackground()
         * @see com.molepigeon.lockchat.app.ConversationListActivity.UserGetter#doInBackground(String...)
         */
        protected void onPostExecute(String result) {
            try {
                //Add the person's name to the list
                people.add(result);

                //Notify the adapter that the list of names has changed, to update the list view.
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fetches the display name of this device and stores it in myName
     *
     * @see com.molepigeon.lockchat.app.ConversationListActivity#myName
     */
    private class FindMyName extends AsyncTask<String, Void, String> {
        /**
         * Runs the network operation
         *
         * @see com.molepigeon.lockchat.app.Network#getUser(String)
         * @param params not used, but required for compliance with the abstract class
         * @return this device's display name
         */
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                //run getUser with this device's UUID
                returned = network.getUser(Secure.getString(getContentResolver(),
                        Secure.ANDROID_ID));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        /**
         * Updates myName with the result of the network operation
         *
         * @param result the display name returned in doInBackground()
         * @see com.molepigeon.lockchat.app.ConversationListActivity.FindMyName#doInBackground(String...)
         */
        protected void onPostExecute(String result) {
            try {
                myName = result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
