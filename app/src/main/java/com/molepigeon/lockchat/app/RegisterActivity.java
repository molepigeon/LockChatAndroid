package com.molepigeon.lockchat.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Companion class for the Registration screen.
 * <p/>
 * Includes Internet functionality.
 *
 * @author Michael Hough
 */
public class RegisterActivity extends Activity {
    /**
     * The content of the text entry field
     */
    private String messageText;

    /**
     * Called as part of the activity lifecycle when the activity (or the app, since this is the
     * launcher activity) is first created, or when it is recreated (like when the device rotates)
     * <p/>
     * This method sets the hint for the text entry field to be the device's current display name.
     *
     * @param savedInstanceState Can be used to restore the activity on auto-rotate or back presses.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setHint(ConversationListActivity.myName);
    }

    /**
     * Action handler for the Accept button
     *
     * This function gets the text from the field and starts the AsyncTask to send it to the server.
     *
     * @see com.molepigeon.lockchat.app.RegisterActivity.UserCreator
     * @param view the view that triggered the action
     */
    public void registerClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        messageText = editText.getText().toString();
        ConversationListActivity.myName = messageText;
        editText.setHint(messageText);
        new UserCreator().execute("");
    }

    /**
     * Sends the user name seen in messageText to the server
     *
     * @see com.molepigeon.lockchat.app.RegisterActivity#messageText
     */
    private class UserCreator extends AsyncTask<String, Void, String> {
        /**
         * Runs the network operation
         *
         * @see com.molepigeon.lockchat.app.Network#addUser(String, String)
         * @param params not used, but required for compliance with the abstract class
         * @return the new display name
         */
        @Override
        protected String doInBackground(String... params) {
            Network network = new Network();
            String returned = null;

            try {
                returned = network.addUser(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID), messageText);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return returned;
        }

        /**
         * Called when doInBackground() finishes.
         *
         * Displays a toast message with the new username.
         *
         * @param result the display name
         */
        protected void onPostExecute(String result) {
            try {
                Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
