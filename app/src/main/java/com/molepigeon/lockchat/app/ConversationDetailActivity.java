package com.molepigeon.lockchat.app;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.util.ArrayList;

public class ConversationDetailActivity extends ListActivity {

    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_detail);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        String item_id = getIntent().getStringExtra(ConversationListActivity.PEOPLE_MESSAGE);
        setTitle(item_id);

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);
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

    public void sendMessage (View view){

        EditText editText = (EditText)findViewById(R.id.editText);
        String messageText = editText.getText().toString();

        listItems.add(messageText);
        adapter.notifyDataSetChanged();
    }
}
