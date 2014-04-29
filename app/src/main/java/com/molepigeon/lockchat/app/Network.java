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

/**
 * Class to hold network queries
 *
 * @author Michael Hough
 */
public class Network {
    /**
     * Fetches all messages for a given recipient and sender from the server.
     * <p/>
     * Messages are returned as XML in the format:
     * <messages>
     * <message>(message text)</message>
     * </messages>
     * <p/>
     * Message texts are encrypted with RSA and stored in Base64 format.
     *
     * @param recipient the recipient (this device)
     * @param sender    the sender (the other party of the conversation)
     * @return every message for this conversation on the server, encrypted and in XML format
     * @throws Exception when something goes wrong with the network transfer
     */
    public String getMessages(String recipient, String sender) throws Exception {
        BufferedReader in;
        String data;

        //Make a HTTP client
        HttpClient client = new DefaultHttpClient();

        //Set up the URI with the relevant GET parameters filled in with the parameters of this method
        URI uri = new URI("http://molepigeon.com:80/lockchat/getAllMessages.php?recipient=" +
                recipient + "&sender=" + sender);

        //Make a new HttpGet object
        HttpGet request = new HttpGet();

        //And set its URI to the URI above
        request.setURI(uri);

        //Execute the request
        HttpResponse response = client.execute(request);

        //This isn't really needed, but it's worth having because why not.
        //It's the HTTP status code returned from the query
        response.getStatusLine().getStatusCode();

        //Get the response as a BufferedReader
        in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        //Make a new StringBuilder starting with an empty string
        StringBuilder sb = new StringBuilder("");
        //And a temporary variable
        String l;
        //Get the line separator, so that the string is returned with them included
        String nl = System.getProperty("line.separator");
        //Fetch each new line
        while ((l = in.readLine()) != null) {
            //And add them to the StringBuilder
            l = l + nl;
            sb.append(l);
        }
        //Close the BufferedReader
        in.close();

        //Return the string
        data = sb.toString();
        return data;
    }

    /**
     * Fetches only messages that haven't been read before from the server
     * <p/>
     * Messages are returned as XML in the format:
     * <messages>
     * <message>(message text)</message>
     * </messages>
     * <p/>
     * Message texts are encrypted with RSA and stored in Base64 format.
     *
     * @param recipient the recipient (this device)
     * @param sender    the sender (the other party of the conversation)
     * @return every message for this conversation on the server, encrypted and in XML format
     * @throws Exception when something goes wrong with the network transfer
     */
    public String getNewMessages(String recipient, String sender) throws Exception {
        BufferedReader in;
        String data;

        //Make a HTTP client
        HttpClient client = new DefaultHttpClient();

        //Set up the URI with the relevant GET parameters filled in with the parameters of this method
        URI uri = new URI("http://molepigeon.com:80/lockchat/getMessages.php?recipient=" +
                recipient + "&sender=" + sender);

        //Make a new HttpGet object
        HttpGet request = new HttpGet();

        //And set its URI to the URI above
        request.setURI(uri);

        //Execute the request
        HttpResponse response = client.execute(request);

        //This isn't really needed, but it's worth having because why not.
        //It's the HTTP status code returned from the query
        response.getStatusLine().getStatusCode();

        //Get the response as a BufferedReader
        in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        //Make a new StringBuilder starting with an empty string
        StringBuilder sb = new StringBuilder("");
        //And a temporary variable
        String l;
        //Get the line separator, so that the string is returned with them included
        String nl = System.getProperty("line.separator");
        //Fetch each new line
        while ((l = in.readLine()) != null) {
            //And add them to the StringBuilder
            l = l + nl;
            sb.append(l);
        }
        //Close the BufferedReader
        in.close();

        //Return the string
        data = sb.toString();
        return data;
    }

    /**
     * Sends a message to the server
     *
     * Messages should be encrypted with RSA and in Base64 format (NOT BINARY TEXT)
     *
     * @param recipient the recipient of the message
     * @param sender the sender of the message (this device)
     * @param message the content of the message
     * @return the message that was sent, or "Error - Check console" if something went wrong
     */
    public String sendMessage(String recipient, String sender, String message) {
        //Set up the string with default behaviour
        String output = "Error - Check console";

        //If something goes wrong in this bit, drop out of the lot
        try {
            //Make sure the sender's device ID and the message are URL safe
            sender = URLEncoder.encode(sender, "UTF-8");
            message = URLEncoder.encode(message, "UTF-8");

            //Make a HTTP client
            HttpClient client = new DefaultHttpClient();

            //Set up the URI with the relevant GET parameters filled in with the parameters of this method
            URI uri = new URI("http://molepigeon.com:80/lockchat/sendMessage.php?recipient=" + recipient + "&sender=" + sender + "&message=" + message);

            //Make a new HttpGet object
            HttpGet request = new HttpGet();

            //And set its URI to the URI above
            request.setURI(uri);

            //Execute the request
            HttpResponse response = client.execute(request);

            //This isn't really needed, but it's worth having because why not.
            //It's the HTTP status code returned from the query
            response.getStatusLine().getStatusCode();

            //Set the output to be the message that was sent
            output = message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /**
     * Fetches the display name for a given device ID from the server
     *
     * Display names are returned as plain text.
     *
     * @param deviceID the device ID to query
     * @return the user's display name
     */
    public String getUser(String deviceID) {
        BufferedReader in = null;
        String data;

        //If something goes wrong, drop right out
        try {
            //Make a HTTP client
            HttpClient client = new DefaultHttpClient();

            //Set up the URI with the relevant GET parameters filled in with the parameters of this method
            URI uri = new URI("http://molepigeon.com:80/lockchat/getUser.php?deviceID=" + deviceID);

            //Make a new HttpGet object
            HttpGet request = new HttpGet();

            //And set its URI to the URI above
            request.setURI(uri);

            //Execute the request
            HttpResponse response = client.execute(request);

            //This isn't really needed, but it's worth having because why not.
            //It's the HTTP status code returned from the query
            response.getStatusLine().getStatusCode();

            //Get the response as a BufferedReader
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            //Make a new StringBuilder starting with an empty string
            StringBuilder sb = new StringBuilder("");
            //And a temporary variable
            String l;
            //Get the line separator, so that the string is returned with them included
            String nl = System.getProperty("line.separator");
            //Fetch each new line
            while ((l = in.readLine()) != null) {
                //And add them to the StringBuilder
                l = l + nl;
                sb.append(l);
            }
            //Close the BufferedReader
            in.close();

            //Get the string from the StringBuilder
            data = sb.toString();

            //Get rid of the URL encoded text, so the username doesn't display with +'s instead
            //of spaces
            data = URLDecoder.decode(data, "UTF-8");

            //Remove newlines
            data = data.replaceAll("(\\n|\\r)", "");

            //If the string is empty, the user doesn't have a name set yet.
            if (data.contentEquals("")) {
                return "Unnamed User";
            }

            //Return the user's name
            return data;
        } catch (Exception e) {
            if (in != null) {
                //Close the BufferedReader
                try {
                    in.close();
                    return "Something went wrong...";
                } catch (Exception f) {
                    f.printStackTrace();
                }
            }
            return "Something went wrong...";
        }
    }

    /**
     * Add or change the user's display name
     *
     * @param deviceID the user's device ID
     * @param name the display name to set
     * @return the display name that was set
     */
    public String addUser(String deviceID, String name) {
        //Set up the string with default behaviour
        String output = "Error - Check console";

        //If something goes wrong in this bit, drop out of the lot
        try {
            //Make sure the new name is URL safe
            name = URLEncoder.encode(name, "UTF-8");

            //Make a HTTP client
            HttpClient client = new DefaultHttpClient();

            //Set up the URI with the relevant GET parameters filled in with the parameters of this method
            URI uri = new URI("http://molepigeon.com:80/lockchat/addUser.php?deviceID=" +
                    deviceID + "&name=" + name);

            //Make a new HttpGet object
            HttpGet request = new HttpGet();

            //And set its URI to the URI above
            request.setURI(uri);

            //Execute the request
            HttpResponse response = client.execute(request);

            //This isn't really needed, but it's worth having because it silences unused warnings.
            //It's the HTTP status code returned from the query
            response.getStatusLine().getStatusCode();

            //Set the output to be the user's name
            output = name;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Return either the name or the error message
        return output;
    }
}
