package com.molepigeon.lockchat.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.molepigeon.lockchat.app.dummy.DummyContent;

import java.util.ArrayList;

/**
 * A fragment representing a single Conversation detail screen.
 * This fragment is either contained in a {@link ConversationListActivity}
 * in two-pane mode (on tablets) or a {@link ConversationDetailActivity}
 * on handsets.
 */
public class ConversationDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ConversationDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            /*
      The dummy content this fragment is presenting.
     */
            DummyContent.DummyItem mItem = DummyContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
            getActivity().setTitle(mItem.content);

            adapter=new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1,
                    listItems);
            ListView listView = (ListView) getActivity().findViewById(android.R.id.list);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversation_detail, container, false);
    }

}
