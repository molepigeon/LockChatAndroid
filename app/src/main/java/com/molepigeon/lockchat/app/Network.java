package com.molepigeon.lockchat.app;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Network {
    public String getMessages(String recipient, String sender) throws Exception {
        BufferedReader in;
        String data;

        HttpClient client = new DefaultHttpClient();
        URI uri = new URI("http://molepigeon.com:80/lockchat/getAllMessages.php?recipient=" + recipient + "&sender=" + sender);
        HttpGet request = new HttpGet();
        request.setURI(uri);
        HttpResponse response = client.execute(request);
        response.getStatusLine().getStatusCode();

        in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder sb = new StringBuilder("");
        String l;
        String nl = System.getProperty("line.separator");
        while ((l = in.readLine()) != null) {
            l = l + nl;
            sb.append(l);
        }
        in.close();
        data = sb.toString();
        data = URLDecoder.decode(data, "UTF-8");
        return data;
    }

    public String getNewMessages(String recipient, String sender) throws Exception {
        BufferedReader in;
        String data;

        HttpClient client = new DefaultHttpClient();
        URI uri = new URI("http://molepigeon.com:80/lockchat/getMessages.php?recipient=" + recipient + "&sender=" + sender);
        HttpGet request = new HttpGet();
        request.setURI(uri);
        HttpResponse response = client.execute(request);
        response.getStatusLine().getStatusCode();

        in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder sb = new StringBuilder("");
        String l;
        String nl = System.getProperty("line.separator");
        while ((l = in.readLine()) != null) {
            l = l + nl;
            sb.append(l);
        }
        in.close();
        data = sb.toString();
        data = URLDecoder.decode(data, "UTF-8");
        return data;
    }

    public String sendMessage(String recipient, String sender, String message) {
        String output = "Error - Check console";
        try {
            sender = URLEncoder.encode(sender, "UTF-8");
            message = URLEncoder.encode(message, "UTF-8");

            HttpClient client = new DefaultHttpClient();
            URI uri = new URI("http://molepigeon.com:80/lockchat/sendMessage.php?recipient=" + recipient + "&sender=" + sender + "&message=" + message);
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse response = client.execute(request);
            response.getStatusLine().getStatusCode();
            output = message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public String getUser(String deviceID) {
        BufferedReader in = null;
        String data;

        try {
            HttpClient client = new DefaultHttpClient();
            URI uri = new URI("http://molepigeon.com:80/lockchat/getUser.php?deviceID=" + deviceID);
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse response = client.execute(request);
            response.getStatusLine().getStatusCode();

            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder sb = new StringBuilder("");
            String l;
            String nl = System.getProperty("line.separator");
            while ((l = in.readLine()) != null) {
                l = l + nl;
                sb.append(l);
            }
            in.close();
            data = sb.toString();
            data = URLDecoder.decode(data, "UTF-8");
            data = data.replaceAll("(\\n|\\r)", "");
            if (data.contentEquals("")) {
                return "Unnamed User";
            }
            return data;
        } catch (Exception e) {
            if (in != null) {
                try {
                    in.close();
                    return null;
                } catch (Exception f) {
                    f.printStackTrace();
                }
            }
            return "Something went wrong...";
        }
    }

    public String addUser(String deviceID, String name) {
        String output = "Error - Check console";
        try {
            name = URLEncoder.encode(name, "UTF-8");
            System.out.println(name);
            HttpClient client = new DefaultHttpClient();
            URI uri = new URI("http://molepigeon.com:80/lockchat/addUser.php?deviceID=" + deviceID + "&name=" + name);
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse response = client.execute(request);
            response.getStatusLine().getStatusCode();
            output = name;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
