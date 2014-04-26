package com.molepigeon.lockchat.app;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ConversationDetailActivity extends ListActivity {

    final Handler handler = new Handler();
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    String item_id;
    String message = "";
    String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_detail);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        item_id = getIntent().getStringExtra(ConversationListActivity.ID_MESSAGE);
        name = getIntent().getStringExtra(ConversationListActivity.PEOPLE_MESSAGE);
        setTitle(name);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);

        new MessageFetcher().execute("");
        adapter.notifyDataSetChanged();
        scheduleNewMessageFetch();
    }

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

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(handlerTask);
    }

    final Runnable handlerTask = new Runnable() {
        @Override
        public void run() {
            new NewMessageFetcher().execute("");
            handler.postDelayed(handlerTask, 5000);
        }
    };

    public void sendMessage(View view) {

        EditText editText = (EditText) findViewById(R.id.editText);
        message = editText.getText().toString();
        editText.setText("");
        new MessageSender().execute("");
    }

    private void scheduleNewMessageFetch() {
//        TimerTask aSyncTask;
//        final Handler handler = new Handler();
//        Timer timer = new Timer();
//        aSyncTask = new TimerTask() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        try{
//                           new NewMessageFetcher().execute("");
//                        }catch(Exception e){
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            }
//        };
//
//        timer.schedule(aSyncTask, 5000, 5000);

        handlerTask.run();

    }

    private class MessageFetcher extends AsyncTask<String, Void, String> {
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
                        listItems.add(name + ": " + nl.item(i).getTextContent());
                    }
                }
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class NewMessageFetcher extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.getNewMessages(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), item_id);
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
                        listItems.add(name + ": " + nl.item(i).getTextContent());
                    }
                }
                adapter.notifyDataSetChanged();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class MessageSender extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.sendMessage(item_id, Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        protected void onPostExecute(String result) {
            try {
                result = URLDecoder.decode(result, "UTF-8");
                listItems.add(ConversationListActivity.myName + ": " + result);
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}