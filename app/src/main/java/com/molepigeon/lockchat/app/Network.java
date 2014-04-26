package com.molepigeon.lockchat.app;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Created by molepigeon on 26/04/2014.
 */
public class Network {
    public String getMessages(String recipient, String sender) throws Exception {
        BufferedReader in = null;
        String data = null;

        try {
            HttpClient client = new DefaultHttpClient();
            URI uri = new URI("http://molepigeon.com:80/lockchat/getMessages.php?recipient=" + recipient + "&sender=" + sender);
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse response = client.execute(request);
            response.getStatusLine().getStatusCode();

            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String l = "";
            String nl = System.getProperty("line.separator");
            while ((l = in.readLine()) != null) {
                sb.append(l + nl);
            }
            in.close();
            data = sb.toString();
            return data;
        } finally {
            if (in != null) {
                try {
                    in.close();
                    return data;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String sendMessage(String recipient, String sender, String message) {
        try {
            HttpClient client = new DefaultHttpClient();
            URI uri = new URI("http://molepigeon.com:80/lockchat/sendMessage.php?recipient=" + recipient + "&sender=" + sender + "&message=" + message);
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse response = client.execute(request);
            response.getStatusLine().getStatusCode();
            return message;
        } catch (Exception e) {
            return "Something went wrong...";
        }
    }
}
