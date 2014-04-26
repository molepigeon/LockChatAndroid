package com.molepigeon.lockchat.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class Register extends Activity {

    private String messageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }

    public void registerClicked(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        messageText = editText.getText().toString();
        new UserCreator().execute("");
    }

    private class UserCreator extends AsyncTask<String, Void, String> {
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

        protected void onPostExecute(String result) {
            try {
                Toast.makeText(Register.this, result, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
