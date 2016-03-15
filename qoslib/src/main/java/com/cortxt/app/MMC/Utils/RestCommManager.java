package com.cortxt.app.MMC.Utils;

import android.content.Intent;
import android.net.TrafficStats;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.ServicesOld.Events.EventCoupleOld;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bscheurman on 15-09-24.
 */
public class RestCommManager {

    private PhoneStateListener phoneState;
    private MMCService owner;
    private long disconnectTime = 0, offhookTime = 0;
    private String txtIncomingNumber = "";
    private Timer callTimer;
    private long rxBytes0 = 0, rxBytes = 0, rxRemainder = 0;
    private long txBytes0 = 0, txBytes = 0, txRemainder = 0;
    public static final String TAG = RestCommManager.class.getSimpleName();
    public RestCommManager (PhoneStateListener _phoneState, MMCService mmc)
    {
        phoneState = _phoneState;
        owner = mmc;
    }
    public void handleIntent (MMCService owner, Intent intent)
    {
        Bundle intentExtras = intent.getExtras();
        String state = intentExtras.getString("STATE");
        String cause = intentExtras.getString("ERRORTEXT");
        String request = intentExtras.getString("REQUEST");
        boolean bIncoming = intentExtras.getBoolean("INCOMING");
        boolean bVideo = intentExtras.getBoolean("VIDEO");
        if (state == null)
            state = "unknown";

        EventCoupleOld eventCouple = callStateChanged(state, cause, bIncoming);
    }

    public EventCoupleOld callStateChanged(String state, String cause, boolean bIncoming) {

        try
        {
            EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.SIP_CONNECT, EventType.SIP_DISCONNECT);
            Intent intent;

            MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onSIPCallStateChanged", state);

            if (state.equals("disconnected") || state.equals("disconnect error") || state.equals("cancelled")
                    || state.equals("declined") || state.equals("connect failed")) {

                if (bOffHook == false && !state.equals("connect failed")) {
                    MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "not off hook");
                    return null;
                }
                disconnectTime = System.currentTimeMillis();
                bOffHook = false;
                intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_DISCONNECT);
                owner.sendBroadcast(intent);
                if (disconnectLatch != null)
                    disconnectLatch.countDown();
                if (callTimer != null)
                {
                    callTimer.cancel ();
                    callTimer = null;
                }

                MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "Disconnect all", "call connected=" + callConnected + " event=" + targetEventCouple);

                if(callConnected || callDialing || callRinging) {


                }
                if (state.equals("disconnect error")) {
                    MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call changed to IDLE with low signal while during call (CALL DROPPED)");
                    targetEventCouple.setStopEventType(EventType.SIP_DROP);
                    //owner.getEventManager().cancelCouple (targetEventCouple);
                    owner.getEventManager().stopPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_DROP);

                    targetEventCouple.getStopEvent().setCause (cause);
                    targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
                    targetEventCouple.getStopEvent().setEventIndex(5); // something has to hold the confidence rating. This field will be sent to server as 'eventIndex'

                    //owner.getEventManager().updateEventDBField(targetEventCouple.getStopEvent().getUri(), TablesOld.Events.TIMESTAMP, Long.toString(disconnectTime));
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), LocalStorageReporter.Events.KEY_TIER, Integer.toString(rating));
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));
                    int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
                    if (allowConfirm > 0)
                        owner.getPhoneStateListener().popupDropped (EventType.SIP_DROP, 5, targetEventCouple.getStopEvent().getLocalID());

                } else if (state.equals("cancelled") || state.equals("declined") || state.equals("disconnected")) {
                    EventType evtType = EventType.SIP_DISCONNECT;
                    if (callConnected)
                        owner.getEventManager().stopPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_DISCONNECT);
                    else
                    {
                        evtType = EventType.SIP_UNANSWERED;
                        targetEventCouple.setStopEventType(EventType.SIP_UNANSWERED);
                        owner.getEventManager().stopPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_UNANSWERED);
                    }

                    targetEventCouple.getStopEvent().setEventIndex(0);
                    targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), LocalStorageReporter.Events.KEY_TIER, Integer.toString(0));
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));

                } else if (state.equals("connect failed")) {
                    if (targetEventCouple == null)
                    {
                        EventOld event = owner.getEventManager().startPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_CALLFAIL);
                        targetEventCouple = owner.getEventManager().getEventCouple(EventType.SIP_CONNECT, EventType.SIP_CALLFAIL);
                    }
                    owner.getEventManager().stopPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_CALLFAIL);
                    targetEventCouple.getStopEvent().setCause(cause);
                    targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
                    targetEventCouple.getStopEvent().setEventIndex(5); // rating
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), LocalStorageReporter.Events.KEY_TIER, Integer.toString(0));
                    //ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));

                    MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call changed to IDLE while call was dialing/ringing (CALL FAILED)");

                    int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
                    if (allowConfirm > 0)
                        owner.getPhoneStateListener().popupDropped(EventType.SIP_CALLFAIL, 5, targetEventCouple.getStopEvent().getLocalID());
                }
                callRinging = callDialing = callConnected = false;

            }
            else if (state.equals("dialing") || state.equals("ringing")) {
                if (bOffHook)
                    return null;

                phoneOffHook (bIncoming);
                //intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_CONNECT);
                //owner.sendBroadcast(intent);
                if (connectLatch != null)
                    connectLatch.countDown();
                onConnect (state);
            }
//            else if (state.equals("ringing")) {
//                if (bOffHook)
//                    return null;
//                phoneOffHook (TelephonyManager.CALL_STATE_RINGING);
//                if (intentExtras.containsKey("FROM")) {
//                    txtIncomingNumber = intentExtras.getString("FROM");
//                }
//                onConnect (state);
//            }
            else if (state.equals("connecting")) {
                onConnect (state);
            }
            else if (state.equals("connected")) {
                onConnect (state);
            }

            return targetEventCouple;
        }
        catch (Exception e)
        {
            MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "Exception", e);
        }
        return null;
    }

    CountDownLatch connectLatch, disconnectLatch;

    public boolean waitForConnect ()
    {
        connectLatch = new CountDownLatch(1);
        try {
            boolean res = connectLatch.await (30, TimeUnit.SECONDS);
            boolean b = res;
            return res;
        } catch (InterruptedException e) {
            //if (connectLatch.getCount() <= 0)
            //	return true;
            return false;
        }
    }
    public boolean waitForDisconnect ()
    {
        disconnectLatch = new CountDownLatch(1);
        try {
            return disconnectLatch.await (50,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //if (connectLatch.getCount() <= 0)
            //	return true;
            return false;
        }
    }

    public void phoneOffHook (boolean bIncoming)
    {
        EventOld event = null;
        event = owner.getEventManager().startPhoneEvent(EventType.SIP_CONNECT, EventType.SIP_DISCONNECT);
        if (event != null)
        {
            owner.startRadioLog (true, "call", EventType.SIP_CONNECT); // "monitoring signal strength");
            if (bIncoming)
            {
                event.setFlag(EventOld.CALL_INCOMING, true);
                owner.getPhoneStateListener().setCallRinging(true);
            }
            else
            {
                owner.getPhoneStateListener().setCallDialing(true); // in case it is an outgoing call (not sure), dialing time will start now
                owner.getPhoneStateListener().setCallRinging (false); // in case it is an outgoing call (not sure), dialing time will start now
            }
        }
        bOffHook = true;
        offhookTime = System.currentTimeMillis();
        startSIPCallTimer ();

        lastCallDropped = false;

        Intent intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_CONNECT);
        owner.sendBroadcast(intent);

    }

    public boolean bOffHook = false, callConnected = false, lastCallDropped = false;
    public boolean callRinging = false, callDialing = false;
    public long timeConnected = 0, timeRinging = 0, timeDialed = 0;

    public void onConnect ( String _state)
    {

        //start a phone connected event
        MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onConnect", _state);

        if(_state.equalsIgnoreCase("connected"))
        {
            if (bOffHook == true)
            {
                if (callConnected == false) {
                    callConnected = true;
                    timeConnected = System.currentTimeMillis();
                    lastCallDropped = false;
                    callDialing = false;
                    //start a phone connected event
                    EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.SIP_CONNECT, EventType.SIP_DISCONNECT);
                    if (targetEventCouple != null && targetEventCouple.getStartEvent() != null) {
                        targetEventCouple.getStartEvent().setEventTimestamp(System.currentTimeMillis());
                        //eventManager.updateEventDBField(targetEventCouple.getStartEvent().getUri(), TablesOld.Events.TIMESTAMP, Long.toString(System.currentTimeMillis()));
                        //start a phone connected event
                        long connectDuration = 0;
                        // The duration on the connected Call event will represent the time it took the call to begin ringing
                        if (callRinging = true && timeRinging > timeDialed && timeDialed > 0 && timeDialed > timeRinging - 100000)
                            connectDuration = timeRinging - timeDialed;
                        connectDuration = timeConnected - timeDialed;
                        targetEventCouple.getStartEvent().setConnectTime((int) connectDuration);
                    }
                }
                else
                {
                    MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "onConnect", "call active but already connected");
                }
            }
            else
            {
                callDialing = false;
                MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "onConnect", "call active but not offhook");
            }
            callRinging = false;
        }

        if (bOffHook == true && callRinging == false && callConnected == false &&
                (_state.equalsIgnoreCase("dialing") || _state.equalsIgnoreCase("connecting")))
        {

            if (_state.equalsIgnoreCase("dialing") && callDialing == false)
            {
                callDialing = true;
                timeDialed = System.currentTimeMillis();
            }
            if (_state.equalsIgnoreCase("connecting") && callRinging == false)
            {
                callRinging = true;
                timeRinging = System.currentTimeMillis();
            }
        }

    }

    // Start a timer during phone call for purposes:
    // to collect samples of tx and rx data transfers
    // and to timeout the call if it goes unanswered for 1 minute, or stays connected more than 10 minutes
    private void startSIPCallTimer ()
    {
        callTimer = new Timer ();
        TimerTask callTimerTask = new SIPCallTimerTask();
        callTimer.scheduleAtFixedRate(callTimerTask, 1000, 2000);
    }

    private void cancelEvent ()
    {

    }
    class SIPCallTimerTask extends TimerTask {

        @Override
        public void run() {
            if (callConnected == false && System.currentTimeMillis() > offhookTime + 60000 )
                callStateChanged("cancelled", "timeout", false);
            if (callConnected == true && System.currentTimeMillis() > timeConnected + 600000 )
                callStateChanged("cancelled", "timeout", false);

            rxBytes = TrafficStats.getTotalRxBytes();
            txBytes = TrafficStats.getTotalTxBytes();

            int state = 0;
            if (callConnected == true)
                state = 2;
            else if (callRinging)
                state = 1;


            if (rxBytes0 > 0) {
                int rx = (int)((rxBytes + rxRemainder- rxBytes0) / 1000);
                rxRemainder = (rxBytes - rxBytes0) - (rx*1000);

                int tx = (int)((txBytes + txRemainder - txBytes0) / 1000);
                txRemainder = (txBytes - txBytes0) - (tx*1000);
                owner.getConnectionHistory().updateSIPCallHistory(rx, tx, state);
            }

            rxBytes0 = rxBytes;
            txBytes0 = txBytes;
        }
    }
}
