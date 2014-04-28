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
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static android.nfc.NdefRecord.createMime;

public class ConversationListActivity extends Activity
        implements CreateNdefMessageCallback {

    public static final String PEOPLE_MESSAGE = "people_message";
    public static final String ID_MESSAGE = "id_message";
    public static final String KEY_MESSAGE = "key_message";
    public static String myName = "";
    public static ArrayList<String> people = new ArrayList<String>();
    public static ArrayList<String> IDs = new ArrayList<String>();
    public static ArrayList<String> publicKeys = new ArrayList<String>();
    public static KeyPair thisKey;
    private static boolean firstRun = true;
    private static String lastNFC = "";
    NfcAdapter mNfcAdapter;
    byte[] encryptedRSA;
    byte[] encryptedAES;
    private String nfcMessage = "";
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                messageText = publicKeys.get(position);
                intent.putExtra(KEY_MESSAGE, messageText);
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

            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                thisKey = keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Test base64 encoding
            String blah = "blah";
            String blahEncoded = Base64.encodeToString(blah.getBytes(), Base64.DEFAULT);
            System.out.println(new String(Base64.decode(blahEncoded.getBytes(), Base64.DEFAULT)));

            //DEBUG user - Delete this when finished
            String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID));
            IDs.add(text);
            text = "Loopback";
            people.add(text);
            publicKeys.add(Base64.encodeToString(thisKey.getPublic().getEncoded(), Base64.URL_SAFE));
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
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = (Secure.getString(getContentResolver(), Secure.ANDROID_ID) + " " + Base64.encodeToString(thisKey.getPublic().getEncoded(), Base64.URL_SAFE));
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                new getKey().execute("");
//            }
//        }, 5000);
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
        String payload = new String(msg.getRecords()[0].getPayload());
        String[] splitPayload = payload.split(" ");
        nfcMessage = splitPayload[0];
        if (!lastNFC.contentEquals(nfcMessage)) {
            IDs.add(nfcMessage);
            new UserGetter().execute();
            publicKeys.add(splitPayload[1]);
            lastNFC = nfcMessage;


            //Encrypt our RSA key with AES and the AES key with the received RSA key
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey key = keyGenerator.generateKey();
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, key);
                encryptedRSA = cipher.doFinal(thisKey.getPublic().getEncoded());
                cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(splitPayload[1].getBytes(), Base64.URL_SAFE))));
                encryptedAES = cipher.doFinal(key.getEncoded());
                //new sendKey().execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private class sendKey extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                String sessionKey = Base64.encodeToString(encryptedAES, Base64.URL_SAFE);
                String payload = Base64.encodeToString(encryptedRSA, Base64.URL_SAFE);
                returned = network.sendKey(nfcMessage, sessionKey, payload);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }
    }

    private class getKey extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getKey(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        protected void onPostExecute(String result) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = factory.newDocumentBuilder();
                InputSource inStream = new InputSource();
                inStream.setCharacterStream(new StringReader(result));
                Document doc = db.parse(inStream);
                Node n = doc.getFirstChild();
                NodeList nl = n.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        nl.item(i).getTextContent();
                        //TODO parse incoming key
                    }
                }
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
