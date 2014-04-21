package com.molepigeon.lockchat.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;


/**
 * An activity representing a list of Conversations. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ConversationDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * This activity implements the required
 * {@link ConversationListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ConversationListActivity extends FragmentActivity
        implements ConversationListFragment.Callbacks {

    public static final String ARG_ITEM_ID = "item_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);
    }

    /**
     * Callback method from {@link ConversationListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        Intent detailIntent = new Intent(this, ConversationDetailActivity.class);
        detailIntent.putExtra(ConversationListActivity.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
