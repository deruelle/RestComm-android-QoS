package com.cortxt.app.MMC.Reporters.WebReporter;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MMCLogger;

/**
 * This class represents the registation of a device api call to the server
 * @author brad scheurman
 *
 */
public class RegistrationRequest  {


    private static final String TAG = RegistrationRequest.class.getSimpleName();
    private static final String END_POINT = "/api/devices/register";

    public static HttpURLConnection POSTConnection(String host, MMCDevice device, String email, boolean share) throws Exception
    {
        URL url = new URL(host + END_POINT);
        String message = toJSON(device, email, share);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(message.getBytes().length);

        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

        MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "authorizeDevice", url.toString());

        //open
        conn.connect();

        //setup send
        OutputStream os = new BufferedOutputStream(conn.getOutputStream());
        os.write(message.getBytes());
        //clean up
        os.flush();
        return conn;

//        EntityTemplate entityTemplate = new EntityTemplate(
//                new ContentProducer(){
//                    public void writeTo(OutputStream outstream) throws IOException {
//                        Writer writer = new OutputStreamWriter(outstream, "UTF-8");
//                        writer.write(toJSON(host, MMCDevice device, String email, boolean share));
//                        writer.flush();
//                    }
//                }
//        );
//        setEntity(entityTemplate);
    }

    /**
     * @return String the json representation of the body of the request.
     */
    public static String toJSON(MMCDevice device, String email, boolean share) {
        String json = "";
        HashMap<String, String> phoneProperties = device.getProperties();
        if(phoneProperties != null) {
          JSONObject data = new JSONObject(phoneProperties);
          try {
            data.put("email", email);
            data.put("share", share);
            json = data.toString();
          } catch (JSONException e) {
            MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "toJSON", e.getMessage());
          }
        }
        return json;
    }

}
