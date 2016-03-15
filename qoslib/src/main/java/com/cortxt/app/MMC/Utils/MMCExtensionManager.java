package com.cortxt.app.MMC.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.os.Handler;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.util.Pair;
import android.view.View;

import com.cortxt.app.MMC.ContentProviderOld.ProviderOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.ServicesOld.Events.EventCoupleOld;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.MMCPhoneStateListenerOld;
import com.cortxt.app.MMC.UtilsOld.DeviceInfoOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.com.mmcextension.IEvent;
import com.cortxt.com.mmcextension.IExtensionCallbacks;
import com.cortxt.com.mmcextension.UsageLimits;
import com.securepreferences.SecurePreferences;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by bscheurman on 16-02-08.
 */
public class MMCExtensionManager implements IExtensionCallbacks {
    MMCService mContext;

    public MMCExtensionManager(MMCService context)
    {
        mContext = context;
    }
    public Context getContext ()
    {
        return mContext;
    }
    public int getLastServiceState()
    {
        if (mContext != null && mContext.getPhoneStateListener() != null)
            return mContext.getPhoneStateListener().getLastServiceState();
        return 0;
    }

    public Location getLastLocation()
    {
        return mContext.getLastLocation();
    }

    public boolean isOnline() {
        return mContext.isOnline();
    }
    public boolean isWifiConnected ()
    {
        return mContext.bWifiConnected;
    }

    public boolean isOffHook (){ return mContext.getPhoneStateListener().isOffHook();}
    public int ActiveConnection ()
    {
        return mContext.ActiveConnection();
    }
    public void logToFile (String level, String className, String methodName, String message)
    {
        MMCLogger.Level lev = MMCLogger.Level.valueOf(level);
        if (lev == null)
            lev = MMCLogger.Level.DEBUG;
        MMCLogger.logToFile(lev, className, methodName, message);
    }
    public void logToFile (String level, String className, String methodName, String message, Exception e)
    {
        MMCLogger.Level lev = MMCLogger.Level.valueOf(level);
        if (lev == null)
            lev = MMCLogger.Level.ERROR;
        MMCLogger.logToFile(lev, className, methodName, message, e);
    }

    public void startRadioLog (boolean bStart, String reason, int eventType)
    {
        mContext.startRadioLog(bStart, reason, null);
    }

    public void setAlarmManager ()
    {
        mContext.setAlarmManager();
    }

    public void manageDataMonitor (int setting, Integer appscan_seconds)
    {
        mContext.manageDataMonitor(setting, appscan_seconds);
    }

    public void updateVideoTestHistory (int bufferProgress, int playProgress, int stalls, int bytes)
    {
        mContext.getConnectionHistory().updateVideoTestHistory(bufferProgress, playProgress, stalls, bytes);
    }

    public void updateSpeedTestHistory (int latency, int latencyProgress, int downloadSpeed, int downloadProgress, int uploadSpeed, int uploadProgress, int counter)
    {
        mContext.getConnectionHistory().updateSpeedTestHistory(latency, latencyProgress, downloadSpeed, downloadProgress, uploadSpeed, uploadProgress, counter);
    }

    public void updateSMSTestHistory (long smsSendTime, long deliveryTime, long responseTime, long responseArrivalTime)
    {
        mContext.getConnectionHistory().updateSMSTestHistory(smsSendTime, deliveryTime, responseTime, responseArrivalTime);
    }

    public void updateThroughputHistory (int rxbytes, int txbytes)
    {
        mContext.getConnectionHistory().updateThroughputHistory(rxbytes, txbytes);
    }

    public void updateEventField (int evtID, String field, String value)
    {
        mContext.getReportManager().updateEventField(evtID, field, value);
    }

    public void updateTravelPreference ()
    {
        mContext.updateTravelPreference();
    }
    public void queueActiveTest(int evType, int trigger) {
        EventType eventType = EventType.get(evType);
        mContext.getEventManager().queueActiveTest(eventType, trigger);
    }
    public void setActiveTestComplete(int testType) {mContext.getEventManager().setActiveTestComplete(testType);}

    public void localReportEvent (IEvent itestEvent)
    {
        EventOld event = (EventOld)itestEvent;
        mContext.getEventManager().localReportEvent(event);
    }

    public void temporarilyStageEvent(IEvent ievent, IEvent icompEvent, Location loc)
    {
        EventOld event = null, compEvent = null;
        if (ievent != null)
            event = (EventOld)ievent;
        if (icompEvent != null)
            compEvent = (EventOld)icompEvent;
        mContext.getEventManager().temporarilyStageEvent(event, compEvent, loc);
    }

    public void unstageEvent(IEvent ievent)
    {
        EventOld event = null;
        if (ievent != null)
            event = (EventOld)ievent;
        mContext.getEventManager().unstageEvent(event);
    }

    public IEvent getStartEvent (int startEventType, int stopEventType, boolean bStart) {
        IEvent event = null;
        EventType start = EventType.get(startEventType);
        EventType stop = EventType.get(stopEventType);
        EventCoupleOld eventCouple = mContext.getEventManager().getEventCouple(start, stop);
        if (eventCouple != null)
        {
            if (bStart)
                event = eventCouple.getStartEvent();
            else
                event = eventCouple.getStopEvent();
        }
        return event;
    }
    public long getLastCellSeen (CellLocation cell)
    {
        return mContext.getCellHistory().getLastCellSeen(cell);
    }
    public boolean isCallConnected ()
    {
        return mContext.getPhoneStateListener().isCallConnected();
    }
    public boolean isTracking ()
    {
        return mContext.getTrackingManager().isTracking();
    }
    public boolean isEventRunning(int iEventType)
    {
        return mContext.getEventManager().isEventRunning(iEventType);
    }
    public boolean isDebuggable ()
    {
        return MMCLogger.isDebuggable();
    }

    public boolean isServiceModeEnabled ()
    {
        return MMCSystemUtil.isServiceModeEnabled();
    }


    public int getNetworkType ()
    {
        return mContext.getPhoneStateListener().getNetworkType();
    }
    public int getPhoneType ()
    {
        return mContext.getPhoneStateListener().getPhoneType();
    }

    public HashMap<String, Integer> getHandsetCaps ()
    {
        return mContext.getHandsetCaps();
    }

    public IEvent triggerSingletonEvent ( int iEventType)
    {
        final EventType eventType = EventType.get(iEventType);
        return mContext.getEventManager().triggerSingletonEvent(eventType);
    }

    public double getDistanceTravelled (int seconds)
    {
        ProviderOld provider = mContext.getDBProvider();//.acquireContentProviderClient(TablesOld.AUTHORITY).getLocalContentProvider();
        return provider.getDistanceTravelled(seconds);
    }

    public Handler getHandler ()
    {
        return mContext.handler;
    }

    public String fixMNC(String mnc )
    {
        return MMCDevice.fixMNC(mnc);
    }
    public int getBattery ()
    {
        return com.cortxt.app.MMC.UtilsOld.DeviceInfoOld.battery;
    }
    public boolean isBatteryCharging ()
    {
        return DeviceInfoOld.batteryCharging;
    }
    public void setUpdatePeriod (int milliseconds)
    {
        mContext.UPDATE_PERIOD = milliseconds;
    }
    public UsageLimits getUsageLimits () { return mContext.getUsageLimits();}
    public int getNetworkGeneration(int networkType)
    {
        return MMCPhoneStateListenerOld.getNetworkGeneration(networkType);
    }
    public boolean waitForConnect ()
    {
        return mContext.getPhoneStateListener().waitForConnect();
    }

    // Secure Preference access
    public void putSecurePrefBoolean (String name, boolean value)
    {
        SecurePreferences prefs = MMCService.getSecurePreferences (mContext);
        prefs.edit().putBoolean(name, value).commit();
    }
    public boolean getSecurePrefBoolean (String name, boolean defValue)
    {
        SecurePreferences prefs = MMCService.getSecurePreferences(mContext);
        return prefs.getBoolean(name, defValue);
    }

    public String getString (String res)
    {
        String str = "";
        int resid = mContext.getResources().getIdentifier(res, "string", mContext.getPackageName());
        if (resid > 0)
            str = mContext.getString (resid);
        return str;
    }

    public String getApiKey ()
    {
        return MMCService.getApiKey(mContext);
    }
    public String getIPAddress () { return MMCDevice.getIPAddress();}

    public View inflateView (String res)
    {
        View view = null;
        int resid = mContext.getResources().getIdentifier(res, "layout", mContext.getPackageName());
        if (resid > 0)
            view = View.inflate(mContext, resid, null);
        return view;
    }

    public View findView (Dialog dialog, String res)
    {
        View view = null;
        int resid = mContext.getResources().getIdentifier(res, "id", mContext.getPackageName());
        if (resid > 0)
            view = dialog.findViewById(resid);
        return view;
    }

    public String getHttpURLResponse(String url, boolean bVerify) throws Exception
    {
        return WebReporter.getHttpURLResponse(url, bVerify);
    }

    public String URLEncodedFormat (LinkedList<Pair> params)
    {
        return WebReporter.URLEncodedFormat(params);
    }

    public Cursor fetchSignals (int timespan, long startTime)
    {
        Cursor cursor = mContext.getDBProvider().query(
                UriMatchOld.SIGNAL_STRENGTHS.getContentUri(),
                new String[]{ TablesOld.TIMESTAMP_COLUMN_NAME, TablesOld.SignalStrengths.SIGNAL, TablesOld.SignalStrengths.LTE_RSRP, TablesOld.SignalStrengths.ECN0, TablesOld.SignalStrengths.ECI0, TablesOld.SignalStrengths.SIGNAL2G, TablesOld.SignalStrengths.COVERAGE },
                TablesOld.TIMESTAMP_COLUMN_NAME + ">?",
                new String[]{ Long.toString(startTime - timespan) },
                TablesOld.TIMESTAMP_COLUMN_NAME + " DESC"
        );
        return cursor;
    }


}
