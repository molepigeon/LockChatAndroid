package com.molepigeon.lockchat.app;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Companion class for the Conversation Detail screen.
 * <p/>
 * Includes Internet functionality.
 *
 * @author Michael Hough
 */
public class ConversationDetailActivity extends ListActivity {

    /**
     * Handler used for scheduling delayed tasks
     */
    final Handler handler = new Handler();

    /**
     * Stores the messages that have been sent in this conversation
     */
    ArrayList<String> listItems = new ArrayList<String>();

    /**
     * Monitors the array
     */
    ArrayAdapter<String> adapter;

    /**
     * Device ID
     */
    String item_id;

    /**
     * Contains a message to be sent
     */
    String message = "";

    /**
     * The other party's display name
     */
    String name = "";
    /**
     * Stores the recipient's encryption key in Base64
     */
    String recipientsKey = "";
    /**
     * Cipher used for encryption of outgoing messages
     */
    Cipher ecipher;
    /**
     * Runnable to schedule fetching new messages
     * <p/>
     * New messages are fetched every 5000ms, starting from the moment of object creation.
     */
    final Runnable handlerTask = new Runnable() {
        @Override
        public void run() {
            new NewMessageFetcher().execute("");
            handler.postDelayed(handlerTask, 5000);
        }
    };
    /**
     * Cipher used for decryption of incoming messages
     */
    Cipher dcipher;

    /**
     * Called as part of the activity lifecycle when the activity (or the app, since this is the
     * launcher activity) is first created, or when it is recreated (like when the device rotates)
     * <p/>
     * This method fetches the contact's information from the intent that launched the activity,
     * sets the activity title, sets up the array adapter to link to the ListView, sets up ciphers
     * for both encryption and decryption, fetches message history, and schedules new message checks.
     *
     * @param savedInstanceState Can be used to restore the activity on auto-rotate or back presses.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_detail);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        item_id = getIntent().getStringExtra(ConversationListActivity.ID_MESSAGE);
        name = getIntent().getStringExtra(ConversationListActivity.PEOPLE_MESSAGE);
        recipientsKey = getIntent().getStringExtra(ConversationListActivity.KEY_MESSAGE);

        setTitle(name);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);

        //Set up ciphers
        try {
            //Set up the decryption cipher with this device's private key
            dcipher = Cipher.getInstance("RSA");
            dcipher.init(Cipher.DECRYPT_MODE, ConversationListActivity.thisKey.getPrivate());

            //Set up the encryption cipher with the recipient's public key, which needs to
            //be converted from its Base64 format first.
            ecipher = Cipher.getInstance("RSA");
            ecipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.decode(recipientsKey.getBytes(),
                            Base64.URL_SAFE))
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Fetch the message history
        new MessageFetcher().execute("");

        //Update the ListView
        adapter.notifyDataSetChanged();

        //Schedule new message fetching
        handlerTask.run();
    }

    /**
     * Called when an item is tapped in the action bar to handle that action.
     *
     * Overrides up button functionality to work on legacy devices.
     *
     * @param item the menu item that was clicked
     * @return true if a menu item was clicked.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //Set up the up button
            NavUtils.navigateUpTo(this, new Intent(this, ConversationListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called as part of the activity lifecycle when the activity is paused.
     * <p/>
     * Stop checking for new messages.
     */
    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(handlerTask);
    }

    /**
     * Action handler for the Send button
     * <p/>
     * This function gets the message from the text field and sets up the AsyncTask to send it to
     * the server.
     *
     * @param view the view that triggered the action
     * @see com.molepigeon.lockchat.app.ConversationDetailActivity.MessageSender
     */
    public void sendMessage(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        message = editText.getText().toString();
        editText.setText("");
        new MessageSender().execute("");
    }

    /**
     * Fetches all messages sent by the recipient in this conversation, defined by the device ID in
     * item_id
     *
     * @see com.molepigeon.lockchat.app.ConversationDetailActivity#item_id
     */
    private class MessageFetcher extends AsyncTask<String, Void, String> {
        /**
         * Runs the network operation
         *
         * @param params not used, but required for compliance with the abstract class
         * @return messages in XML format
         * @see com.molepigeon.lockchat.app.Network#getMessages(String, String)
         */
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getMessages(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), item_id);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        /**
         * Parse XML results and display them
         * <p/>
         * XML input must be:
         * <messages>
         * <message>(message text)</message>
         * </messages>
         * <p/>
         * Messages must be encrypted with RSA and then converted to URL safe Base64 format.
         *
         * @param result a list of messages in the correct XML format
         */
        protected void onPostExecute(String result) {
            try {
                byte[] temp;

                //New objects
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = factory.newDocumentBuilder();
                InputSource inStream = new InputSource();

                //Set the stream to read the results string
                inStream.setCharacterStream(new StringReader(result));

                //Use the DocumentBuilder to parse the string as XML.
                Document doc = db.parse(inStream);

                //Get the first child (the <messages> tag)
                Node n = doc.getFirstChild();

                //Get all <message> tags in a list
                NodeList nl = n.getChildNodes();

                //Iterate through the list (NodeList isn't iterable, so we can't use a foreach)
                for (int i = 0; i < nl.getLength(); i++) {
                    //If the item is a valid XML element
                    if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        //Convert the Base64 to a byte array
                        temp = Base64.decode(nl.item(i).getTextContent().getBytes(), Base64.URL_SAFE);
                        //Decrypt the message and add it to the list
                        listItems.add(name + ": " + new String(dcipher.doFinal(temp)));
                    }
                }
                //Update the ListView
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fetches only new messages sent by the recipient in this conversation, defined by the device
     * ID in item_id.
     *
     * @see com.molepigeon.lockchat.app.ConversationDetailActivity#item_id
     */
    private class NewMessageFetcher extends AsyncTask<String, Void, String> {
        /**
         * Runs the network operation
         *
         * @param params not used, but required for compliance with the abstract class
         * @return messages in XML format
         * @see com.molepigeon.lockchat.app.Network#getNewMessages(String, String)
         */
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getNewMessages(Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID), item_id);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        /**
         * Parse XML results and display them
         * <p/>
         * XML input must be:
         * <messages>
         * <message>(message text)</message>
         * </messages>
         * <p/>
         * Messages must be encrypted with RSA and then converted to URL safe Base64 format.
         *
         * @param result a list of messages in the correct XML format
         */
        protected void onPostExecute(String result) {
            try {
                byte[] temp;

                //New objects
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = factory.newDocumentBuilder();
                InputSource inStream = new InputSource();

                //Set the stream to read the results string
                inStream.setCharacterStream(new StringReader(result));

                //Use the DocumentBuilder to parse the string as XML.
                Document doc = db.parse(inStream);

                //Get the first child (the <messages> tag)
                Node n = doc.getFirstChild();

                //Get all <message> tags in a list
                NodeList nl = n.getChildNodes();

                //Iterate through the list (NodeList isn't iterable, so we can't use a foreach)
                for (int i = 0; i < nl.getLength(); i++) {
                    //If the item is a valid XML element
                    if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        //Convert the Base64 to a byte array
                        temp = Base64.decode(nl.item(i).getTextContent().getBytes(), Base64.URL_SAFE);
                        //Decrypt the message and add it to the list
                        listItems.add(name + ": " + new String(dcipher.doFinal(temp)));
                    }
                }
                //Update the ListView
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a message, encrypted with RSA and converted to Base64 format.
     *
     * @see com.molepigeon.lockchat.app.ConversationDetailActivity#message
     */
    private class MessageSender extends AsyncTask<String, Void, String> {
        /**
         * Encrypts the string given in message, converts the bytes to Base64, and sends them.
         *
         * @param params not used, but required for compliance with the abstract class
         * @return the message that was sent (in encrypted and Base64 format)
         */
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                //Encrypt the string and convert it to URL safe Base64.
                String encryptedString = Base64.encodeToString(ecipher.doFinal(message.getBytes()),
                        Base64.URL_SAFE);

                //Send the message.
                returned = network.sendMessage(item_id, Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID), encryptedString);
            } catch (Exception e) {
                //If something went wrong, display "An error occurred" in the conversation
                message = "An error occurred";
                e.printStackTrace();
            }

            return returned;
        }

        /**
         * Displays the sent message in the conversation list next to this device's display name.
         *
         * @see com.molepigeon.lockchat.app.ConversationListActivity#myName
         * @see com.molepigeon.lockchat.app.ConversationDetailActivity#message
         * @param result the message that was sent)
         */
        protected void onPostExecute(String result) {
            try {
                //Add the message to the list
                listItems.add(ConversationListActivity.myName + ": " + message);
                //Update the ListView
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }






}