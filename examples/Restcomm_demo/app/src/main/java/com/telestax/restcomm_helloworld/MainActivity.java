package com.telestax.restcomm_helloworld;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.cortxt.app.MMC.Utils.MMCCommand;
import com.cortxt.app.MMC.Utils.RestCommManager;

import java.util.HashMap;

import org.mobicents.restcomm.android.client.sdk.RCClient;
import org.mobicents.restcomm.android.client.sdk.RCConnection;
import org.mobicents.restcomm.android.client.sdk.RCConnectionListener;
import org.mobicents.restcomm.android.client.sdk.RCDevice;
import org.mobicents.restcomm.android.client.sdk.RCDeviceListener;
import org.mobicents.restcomm.android.client.sdk.RCPresenceEvent;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;

public class MainActivity extends Activity implements RCDeviceListener, RCConnectionListener, OnClickListener {

    private RCDevice device;
    private RCConnection connection, pendingConnection;
    private HashMap<String, Object> params;
    private static final String TAG = "MainActivity";

    private GLSurfaceView videoView;
    private VideoRenderer.Callbacks localRender = null;
    private VideoRenderer.Callbacks remoteRender = null;
    private boolean videoReady = false;
    private EditText editServer, editUser, editPwd, editDial;
    VideoTrack localVideoTrack, remoteVideoTrack;
    VideoRenderer localVideoRenderer, remoteVideoRenderer;

    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 2;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType;

    // UI elements
    Button btnDial;
    Button btnAnswer;
    Button btnHangup;
    Button btnApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);

        // initialize UI
        btnDial = (Button)findViewById(R.id.button_dial);
        btnDial.setOnClickListener(this);
        btnAnswer = (Button)findViewById(R.id.button_answer);
        btnAnswer.setOnClickListener(this);
        btnHangup = (Button)findViewById(R.id.button_hangup);
        btnHangup.setOnClickListener(this);
        btnApply = (Button)findViewById(R.id.button_apply);
        btnApply.setOnClickListener(this);

        editServer = (EditText)findViewById(R.id.editServer);
        editUser = (EditText)findViewById(R.id.editUser);
        editPwd = (EditText)findViewById(R.id.editPwd);
        editDial= (EditText)findViewById(R.id.editDial);

        String prefServer = PreferenceManager.getDefaultSharedPreferences(this).getString("PREF_SERVER", null);
        if (prefServer != null)
            editServer.setText (prefServer);
        String prefUser = PreferenceManager.getDefaultSharedPreferences(this).getString("PREF_USER", null);
        if (prefUser != null)
            editUser.setText (prefUser);
        String prefPwd = PreferenceManager.getDefaultSharedPreferences(this).getString("PREF_PWD", null);
        if (prefPwd != null)
            editPwd.setText(prefPwd);
        String prefDial = PreferenceManager.getDefaultSharedPreferences(this).getString("PREF_DIAL", null);
        if (prefDial != null)
            editDial.setText(prefDial);

        RCClient.setLogLevel(Log.VERBOSE);
        RCClient.initialize(getApplicationContext(), new RCClient.RCInitListener() {
            public void onInitialized() {
                Log.i(TAG, "RCClient initialized");
            }

            public void onError(Exception exception) {
                Log.e(TAG, "RCClient initialization error");
            }
        });

        params = new HashMap<String, Object>();
        // update the IP address to your Restcomm instance
        params.put("pref_proxy_domain", "");//editServer.getText().toString() + ":5080");
        //params.put("pref_proxy_ip", editServer.getText().toString());
        //params.put("pref_proxy_port", "5080");
        params.put("pref_sip_user", editUser.getText().toString());
        params.put("pref_sip_password", editPwd.getText().toString());
        device = RCClient.createDevice(params, this);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        // we don't have a separate activity for the calls, so use the same intent both for calls and messages
        device.setPendingIntents(intent, intent);
        device.listen();
        MMCCommand.start (this);

        // Setup video stuff
        scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
        videoView = (GLSurfaceView) findViewById(R.id.glview_call);
        // Create video renderers.
        VideoRendererGui.setView(videoView, new Runnable() {
            @Override
            public void run() {
                videoContextReady();
            }
        });
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
        Log.i(TAG, "%% onDestroy");
        RCClient.shutdown();
        device = null;
    }

    private void videoContextReady()
    {
        videoReady = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // UI Events
    public void onClick(View view) {
        if (view.getId() == R.id.button_dial) {
            if (connection != null) {
                Log.e(TAG, "Error: already connected");
                return;
            }
            String dial = editDial.getText().toString();
            if (dial.indexOf("@") == -1)
                dial = dial + "@" + editServer.getText().toString ();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("PREF_DIAL", dial).commit();

            HashMap<String, Object> connectParams = new HashMap<String, Object>();
            // CHANGEME: update the IP address to your Restcomm instance. Also, you can update the number
            // from '1235' to any Restcomm application you wish to reach
            //connectParams.put("username", "sip:+1235@cloud.restcomm.com");
            connectParams.put("username", "sip:" + dial);
            connectParams.put("video-enabled", true);

            // if you want to add custom SIP headers, please uncomment this
            //HashMap<String, String> sipHeaders = new HashMap<>();
            //sipHeaders.put("X-SIP-Header1", "Value1");
            //connectParams.put("sip-headers", sipHeaders);

            connection = device.connect(connectParams, this);
            if (connection == null) {
                Log.e(TAG, "Error: error connecting");
                return;
            }
            //device.updateParams(params);
        } else if (view.getId() == R.id.button_hangup) {
            if (connection == null) {
                Log.e(TAG, "Error: not connected");
            }
            else {
                connection.disconnect();
                connection = null;
                pendingConnection = null;
            }
        }
        else if (view.getId() == R.id.button_apply) {
            if (connection == null) {
                Log.e(TAG, "Error: not connected");
            } else {
                connection.disconnect();
                connection = null;
                pendingConnection = null;
            }

            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("PREF_SERVER", editServer.getText().toString()).commit();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("PREF_USER", editUser.getText().toString()).commit();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("PREF_PWD", editPwd.getText().toString()).commit();

            // If dial setting changed, register with the QoS server
            if (!editDial.getText().equals(PreferenceManager.getDefaultSharedPreferences(this).getString("PREF_DIAL",null))) {
                MMCCommand.setLogin (this, editDial.getText().toString());
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("PREF_DIAL", editDial.getText().toString()).commit();
            }
            params = new HashMap<String, Object>();
            // CHANGEME: update the IP address to your Restcomm instance
            params.put("pref_proxy_ip", editServer.getText());
            params.put("pref_proxy_port", "5080");
            params.put("pref_sip_user", editUser.getText());
            params.put("pref_sip_password", editPwd.getText());
            device = RCClient.createDevice(params, this);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            // we don't have a separate activity for the calls, so use the same intent both for calls and messages
            device.setPendingIntents(intent, intent);

            device.listen();

        }else if (view.getId() == R.id.button_answer) {
            if (this.pendingConnection == null) {
                Log.e(TAG, "Error: not ringing");
                return;
            }
            HashMap<String, Object> connectParams = new HashMap<String, Object>();
            connectParams.put("username", "sip:dane@184.73.55.15:5080");
            connectParams.put("video-enabled", false);
            this.pendingConnection.accept(connectParams);
            connection = this.pendingConnection;
            if (connection == null) {
                Log.e(TAG, "Error: error connecting");
                //sendRCNoConnectionIntent(connectParams.get("username").toString(), device.getReachability());
                return;
            }
        }
    }


    // RCDevice Listeners
    public void onStartListening(RCDevice device)
    {

    }

    public void onStopListening(RCDevice device)
    {

    }

    public void onStopListening(RCDevice device, int errorCode, String errorText)
    {
        Log.i(TAG, errorText);
    }

    public boolean receivePresenceEvents(RCDevice device)
    {
        return false;
    }

    public void onPresenceChanged(RCDevice device, RCPresenceEvent presenceEvent)
    {

    }

    public void onIncomingConnection(RCDevice device, RCConnection connection)
    {
        Log.i(TAG, "Connection arrived");
        this.pendingConnection = connection;
    }

    public void onIncomingMessage(RCDevice device, String message, HashMap<String, String> parameters)
    {
        final HashMap<String, String> finalParameters = parameters;
        final String finalMessage = message;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String newText = finalParameters.get("username") + ": " + finalMessage + "\n";
                Log.i(TAG, "Message arrived: " + newText);
            }
        });
    }

    public void onConnectivityUpdate(RCDevice device, RCConnectivityStatus connectivityStatus)
    {

    }

    // RCConnection Listeners
    public void onConnecting(RCConnection connection)
    {
        Log.i(TAG, "RCConnection connecting");
    }

    public void onConnected(RCConnection connection)
    {
        Log.i(TAG, "RCConnection connected");
    }

    public void onDisconnected(RCConnection connection)
    {
        Log.i(TAG, "RCConnection disconnected");
        this.connection = null;
        pendingConnection = null;

        // reside local renderer to take up all screen now that the call is over
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        if (localVideoTrack != null) {

            localVideoTrack.removeRenderer(localVideoRenderer);
            localVideoTrack = null;
        }

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeRenderer(remoteVideoRenderer);
            remoteVideoTrack = null;
        }
    }

    public void onDisconnected(RCConnection connection, int errorCode, String errorText) {

        Log.i(TAG, errorText);
        this.connection = null;
        pendingConnection = null;
    }

    public void onCancelled(RCConnection connection) {
        Log.i(TAG, "RCConnection cancelled");
        this.connection = null;
        pendingConnection = null;
    }

    public void onDeclined(RCConnection connection) {
        Log.i(TAG, "RCConnection declined");
        this.connection = null;
        pendingConnection = null;
    }
    public void onReceiveLocalVideo(RCConnection connection, VideoTrack videoTrack) {
        Log.v(TAG, "onReceiveLocalVideo(), VideoTrack: " + videoTrack);
        if (videoTrack != null) {
            //show media on screen
            videoTrack.setEnabled(true);
            localVideoRenderer = new VideoRenderer(localRender);
            videoTrack.addRenderer(localVideoRenderer);
            localVideoTrack = videoTrack;
        }
    }

    public void onReceiveRemoteVideo(RCConnection connection, VideoTrack videoTrack) {
        Log.v(TAG, "onReceiveRemoteVideo(), VideoTrack: " + videoTrack);
        if (videoTrack != null) {
            //show media on screen
            videoTrack.setEnabled(true);
            remoteVideoRenderer = new VideoRenderer(remoteRender);
            videoTrack.addRenderer(remoteVideoRenderer);

            VideoRendererGui.update(remoteRender,
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
            VideoRendererGui.update(localRender,
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);

            remoteVideoTrack = videoTrack;
        }
    }
}
