package com.cortxt.app.MMC.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

import com.cortxt.app.MMC.Activities.Settings;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bscheurman on 15-12-08.
 */
public class MMCSystemUtil {

    private static String privapp = "priv-app";
    public static final String TAG = MMCSystemUtil.class.getSimpleName();
    public static void promptUpdateSvcMode (final Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        String strTitle = activity.getString(R.string.system_install_request_title);
        String strText = activity.getString(R.string.system_install_request_message);
        String appname = activity.getString(R.string.app_label);
        if (!appname.equals("MyMobileCoverage")) {
            strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
            strText = strText.replaceAll("MyMobileCoverage", appname);
        }
        builder.setTitle(strTitle);
        builder.setMessage(strText);
        builder.setInverseBackgroundForced(true);

        builder.setPositiveButton(R.string.GenericText_Yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.DEBUG, "Settings", "onCreated", "Attempt to install mmcsys app");
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", true).commit();

                // Allow root access always unless otherwise changed in preferences
                updateSystemService(activity);
            }
        });

        builder.setNegativeButton(R.string.GenericText_No, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE_CHANGED", true).commit();
                // Don't ask again for root access
                //PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, true).commit();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void installSystemService (final Activity activity, final boolean reboot)
    {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                try {
                    String[] CMDLINE_GRANTPERMS = {"su", "-c", null};

                    // Copy asset apk to /sdcard
                    copyAsset(activity);
                    String sdcard = getStoragePath ();
                    if (Build.VERSION.SDK_INT < 19)
                        privapp = "app";

                    //CMDLINE_GRANTPERMS[2] = String.format("mount -o rw,remount /system && mv /sdcard/mmcsys.apk /system/priv-app && chmod 644 /system/priv-app/mmcsys.apk");
                    CMDLINE_GRANTPERMS[2] = String.format("mount -o rw,remount /system && cp " + sdcard + "/mmcsys.apk /system/" + privapp + " && chmod 644 /system/" + privapp + "/mmcsys.apk");
                    java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));//.getInputStream()));
                    String line2, line = bufferedReader.readLine();
                    if (line != null)  // skip first line (header)
                    {
                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "installSystemService", "error: "+ line);
                        line2 = bufferedReader.readLine();
                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "installSystemService", "error2: "+ line2);
                    }

                    int res = p.waitFor();
                    PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", true).commit();


                    if (reboot == true) {
                        CMDLINE_GRANTPERMS[2] = String.format("reboot");
                        p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                    } else
                    {
                        handler.post(new Runnable() {
                            public void run() {
                                promptDebugMode(activity);
                            }
                        });
                    }

                } catch (final Exception e) {
                    MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "installSystemService", "Exception", e);


                    handler.post(new Runnable() {
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setCancelable(true);
                            if (activity instanceof Settings)
                                ((Settings)activity).svcmodeActive.setChecked(false);
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", false).commit();
                            String strTitle = "Service Mode failed"; // getString(R.string.system_install_request_title);
                            String strText = "There was an error attempting to enable service mode: " + e.getLocalizedMessage(); // getString(R.string.system_install_request_message);
                            builder.setTitle(strTitle);
                            builder.setMessage(strText);
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                }

            }
        }).start();
    }

    public static void updateSystemService (final Activity activity)
    {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                try {
                    String[] CMDLINE_GRANTPERMS = {"su", "-c", null};

                    // Copy asset apk to /sdcard
                    copyAsset(activity);
                    String sdcard = getStoragePath();
                    if (Build.VERSION.SDK_INT < 19)
                        privapp = "app";

                    //CMDLINE_GRANTPERMS[2] = String.format("mount -o rw,remount /system && mv /sdcard/mmcsys.apk /system/priv-app && chmod 644 /system/priv-app/mmcsys.apk");
                    CMDLINE_GRANTPERMS[2] = String.format("mount -o rw,remount /system && rm /system/" + privapp + "/mmcsys.apk && cp " + sdcard + "/mmcsys.apk /system/" + privapp + " && rm " + sdcard + "/mmcsys.apk && chmod 644 /system/" + privapp + "/mmcsys.apk");
                    java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));//.getInputStream()));
                    String line2, line = bufferedReader.readLine();
                    if (line != null)  // skip first line (header)
                    {
                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "updateSystemService", "error: "+ line);
                        line2 = bufferedReader.readLine();
                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "updateSystemService", "error2: "+ line2);
                    }

                    int res = p.waitFor();

                    CMDLINE_GRANTPERMS[2] = String.format("reboot");
                    p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);

                } catch (final Exception e) {
                    MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "updateSystemService", "Exception", e);

                    handler.post(new Runnable() {
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setCancelable(true);
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", false).commit();
                            String strTitle = "MMC system update failed"; // getString(R.string.system_install_request_title);
                            String strText = "There was an error attempting to update MMC system module: " + e.getLocalizedMessage(); // getString(R.string.system_install_request_message);
                            builder.setTitle(strTitle);
                            builder.setMessage(strText);
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                }

            }
        }).start();
    }

    public static void promptDebugMode (final Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        String strTitle = "Raw Radio Access needs debug access";
        String strText = "In the following screen(s), please select 'Debug Level Enabled = HIGH'. This will reboot your phone.";
        builder.setTitle(strTitle);
        builder.setMessage(strText);
        builder.setInverseBackgroundForced(true);

        builder.setPositiveButton(R.string.GenericText_Yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.DEBUG, "Settings", "onCreated", "Enabling debug level = MED");
                // Allow root access always unless otherwise changed in preferences
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, true).commit();

                try {
                    String[] CMDLINE_GRANTPERMS = {"su", "-c", null};
                    CMDLINE_GRANTPERMS[2] = String.format("am broadcast -a android.provider.Telephony.SECRET_CODE -d android_secret_code://9900");
                    java.lang.Process p2 = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                } catch (Exception e) {
                }
                dialMenu(activity);
            }
        });

        builder.setNegativeButton(R.string.GenericText_No, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Disable root access always unless otherwise changed in preferences
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, false).commit();
                MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "promptDebugMode", "No button");
                dialog.dismiss();
                if (activity instanceof Settings)
                    ((Settings) activity).svcmodeActive.setChecked(false);
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", false).commit();
                //PreferenceManager.getDefaultSharedPreferences(Settings.this).edit().putBoolean("KEY_SETTINGS_SVCMODE_PHONECALLS", false).commit();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void dialMenu (final Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        String strTitle = "If menu did not appear..";
        String strText = "You will need to manually dial *#9900# on the phone. And then enable DEBUG LEVEL HIGH";
        builder.setTitle(strTitle);
        builder.setMessage(strText);
        builder.setInverseBackgroundForced(true);

        builder.setPositiveButton(R.string.GenericText_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                callIntent.setData(Uri.parse("tel:"));
                activity.startActivity(callIntent);

                try {

                    Thread.sleep(4000);
                    Toast toast2 = Toast.makeText(activity, "Dial *#9900# then set DEBUG MODE = HIGH. You may need to try twice.", Toast.LENGTH_LONG);
                    toast2.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0);
                    toast2.show();
                } catch (Exception e) {
                }
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

        try {

            Thread.sleep(2000);
            Toast toast2 = Toast.makeText(activity, "please set Debug Level Enabled = HIGH. ", Toast.LENGTH_LONG);
            //toast2.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0);
            toast2.show();
        } catch (Exception e) {
        }
    }
    public static void downloadApk (Activity activity)
    {
        try {
            //HttpGet vqRequest = new HttpGet("http://ivr.mymobilecoverage.com/vq_config.json");
            String version = activity.getString(R.string.svcmode_version);

            URL request = new URL ("http://d1l72qawknwf5q.cloudfront.net/sysfile/samsung/" + version + "/mmcsys.apk");
            HttpURLConnection connection = (HttpURLConnection) request.openConnection();
            connection.connect ();
            InputStream stream = connection.getInputStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            FileOutputStream fos = new FileOutputStream(getStoragePath () + "/mmcsys.apk");
            while ( (n = stream.read(byteChunk)) > 0 ) {
                fos.write(byteChunk, 0, n);
            }

            fos.flush();
            fos.close();
            stream.close();
        }
        catch (Exception e)
        {
            MMCLogger.logToFile(MMCLogger.Level.ERROR, "MMCSystemUtil", "downloadApk ", "exception ", e);

        }
    }

    public static void promptInstallSystem (final Activity activity, boolean skipPrompt)
    {
        boolean svcEnabled = MMCSystemUtil.isServiceModeEnabled();

        boolean needDebugMode = false;
        try {

            if (Build.VERSION.SDK_INT < 20) {
                // check whether debug=high is enabled on the device
                java.lang.Process p = Runtime.getRuntime().exec("getprop ro.boot.debug_level");
                int res = p.waitFor();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = bufferedReader.readLine();
                if (!line.equals("0x4948") && !line.equals("0x494d"))
                    needDebugMode = true;
            }
        }
        catch (Exception e)
        {}

        if (needDebugMode && svcEnabled) {
            MMCSystemUtil.promptDebugMode(activity);
            return;
        }
        else if (svcEnabled == true)
            return;
        final boolean reboot = !needDebugMode;
        // get user to enable debug mode, then it will reboot
        if (skipPrompt)
        {
            MMCSystemUtil.installSystemService(activity, reboot);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        String strTitle = "Install system service"; // getString(R.string.system_install_request_title);
        String strText = "Allow MyMobileCoverage to install a small system service for Raw Radio Access?"; // getString(R.string.system_install_request_message);
        String appname = activity.getString(R.string.app_label);
        if (!appname.equals("MyMobileCoverage")) {
            strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
            strText = strText.replaceAll("MyMobileCoverage", appname);
        }
        builder.setTitle(strTitle);
        builder.setMessage(strText);
        builder.setInverseBackgroundForced(true);

        builder.setPositiveButton(R.string.GenericText_Yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.DEBUG, "Settings", "onCreated", "Attempt to install mmcsys app");
                // Allow root access always unless otherwise changed in preferences
                MMCSystemUtil.installSystemService(activity, reboot);
            }
        });

        builder.setNegativeButton(R.string.GenericText_No, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "promptInstallSystem", "No button");
                if (activity instanceof Settings)
                    ((Settings) activity).svcmodeActive.setChecked(false);
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("KEY_SETTINGS_SVCMODE", false).commit();
                dialog.dismiss();
                // Don't ask again for root access
                //PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, true).commit();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void promptRemoveSystem (final Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        String appname = activity.getString(R.string.app_label);
        String strTitle = "Remove system service?"; // getString(R.string.system_install_request_title);
        String strText = "Do you want to remove the " + appname + " system module?"; // getString(R.string.system_install_request_message);

        if (!appname.equals("MyMobileCoverage")) {
            strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
            strText = strText.replaceAll("MyMobileCoverage", appname);
        }
        builder.setTitle(strTitle);
        builder.setMessage(strText);
        builder.setInverseBackgroundForced(true);

        builder.setPositiveButton(R.string.GenericText_Yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.DEBUG, "Settings", "onCreated", "Attempt to install mmcsys app");
                // Allow root access always unless otherwise changed in preferences
                removeSystemService(activity);
            }
        });

        builder.setNegativeButton(R.string.GenericText_No, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "promptRemoveSystem", "No button");
                dialog.dismiss();
                // Don't ask again for root access
                //PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, true).commit();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private static String getStoragePath () {
        return "/sdcard";
        // return Environment.getExternalStorageDirectory().toString();
    }


    private static void removeSystemService (Activity activity)
    {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String[] CMDLINE_GRANTPERMS = {"su", "-c", null};

                    if (Build.VERSION.SDK_INT < 19)
                        privapp = "app";
                    CMDLINE_GRANTPERMS[2] = String.format("mount -o rw,remount /system && rm /system/" + privapp + "/mmcsys.apk");
                    java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));//.getInputStream()));
                    int res = p.waitFor();
                    CMDLINE_GRANTPERMS[2] = String.format("reboot");
                    p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
                } catch (final Exception e) {
                    MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "removeSystemService", "Exception", e);
                }
            }
        }).start();
    }

    private static void copyAsset(Activity activity) {
        AssetManager assetManager = activity.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open("apk/mmcsys.apk");
            File outFile = new File("/sdcard", "mmcsys.apk");
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(Exception e) {
            downloadApk(activity);
            MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "copyAssets", "Failed to copy asset file:", e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }

    }
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }


    // Check the version of the Service Mode module and offer to upgrade it if a new one is bundled
    private static Activity checkActivity = null;
    public static void checkSvcModeVersion ( Activity activity) {

        // Detect if service mode is installed
        try {
            if (Build.VERSION.SDK_INT < 19)
                privapp = "app";
            File f = new File("/system/" + privapp + "/mmcsys.apk");
            if (!f.exists()) {
                checkForRootedLogcat(activity);
                return;
            }

            boolean bSvcEnabled = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("KEY_SETTINGS_SVCMODE", false);
            boolean bSvcEnabledChanged = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("KEY_SETTINGS_SVCMODE_CHANGED", false);
            if (!bSvcEnabled && bSvcEnabledChanged)
                return;
        }
        catch (Exception e)
        {
            return;
        }
        checkActivity = activity;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.cortxt.app.rilreader", "com.cortxt.app.rilreader.RadioLog.RadioLogService"));
        intent.putExtra("cmd", "getversion");
        intent.putExtra("appname", activity.getPackageName());
        activity.startService(intent);  // Get it to report its version and then service is done
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            // @Override
            public void run() {
                // no answer from service mode, then maybe we should upgrade it
                if (checkActivity != null) {
                    //MMCSystemUtil.promptUpdateSvcMode(checkActivity);
                    checkActivity = null;
                }
            }
        }, 3000);
    }

    // Response: Check the version of the Service Mode module and offer to upgrade it if a new one is bundled
    public static void onSvcModeVersion ( int version) {
        if(checkActivity != null && checkActivity.isFinishing()) //|| checkActivity.isDestroyed())
            checkActivity = null;
        if (checkActivity == null)
            return;
        String currVersion = checkActivity.getString(R.string.svcmode_version);
        try
        {
            int iversion = Integer.valueOf(currVersion, 10);
            if (iversion > version) {
                // Show alert from the Activity
                MMCSystemUtil.promptUpdateSvcMode(checkActivity);
            }
            else
            {
                PreferenceManager.getDefaultSharedPreferences(checkActivity).edit().putBoolean("KEY_SETTINGS_SVCMODE", true).commit();
            }
            checkActivity = null;
        }
        catch (Exception e)
        {
        }
    }
    public static boolean isSystem (Context context)
    {
        final String pname = context.getPackageName();
        int permissionForSystemStuff = context.getPackageManager().checkPermission(android.Manifest.permission.MODIFY_PHONE_STATE, pname); // 0 means allowed
        if (permissionForSystemStuff == 0)
            return true;
        else
            return false;
    }

    public static boolean isServiceModeAllowed (Context context)
    {
        // Does Server allow service mode?
        int useSvcMode = PreferenceManager.getDefaultSharedPreferences(context).getInt(PreferenceKeys.Miscellaneous.USE_SVCMODE, 0);
        if (useSvcMode != 1)
            return false;
        String phonetype = ReportManager.getInstance(context).getDevice().getManufacturer();
        // Is phone a Samsung with Jellybean or higher?
        if (!phonetype.toLowerCase().equals("samsung") || Build.VERSION.SDK_INT < 17)
            return false;

        // Is phone rooted
        if(MMCDevice.isDeviceRooted(context) == false)
            return false;

        return true;
    }


    public static void checkForRootedLogcat (final Activity activity) {
        if (MMCService.getPlatform() != 3) {  // rooted androids, not BB10
            final String pname = activity.getPackageName();
            boolean dontAskForRoot = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, false);
            boolean dontAskForSystem = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_SYSTEM_ACCESS, false);
            boolean dontAskForNonRoot = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_NON_ROOT, false);
            boolean permissionForRoot = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, false);
            int askForRoot = activity.getResources().getInteger(R.integer.ASKFOR_SUPERUSER);
            int permissionForReadLogs = activity.getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS, pname); // 0 means allowed
            int permissionForPrecise = activity.getPackageManager().checkPermission("android.permission.READ_PRECISE_PHONE_STATE", pname); // 0 means allowed

            final boolean isSvcAllowed = isServiceModeAllowed(activity);
            if (isSvcAllowed)
                dontAskForRoot = dontAskForSystem;
            boolean isSvcEnabled = isServiceModeEnabled();
            if (dontAskForSystem == false && permissionForReadLogs == 0 && isSvcAllowed == true && isSvcEnabled == false)
                permissionForReadLogs = 1;  // even if we have permission for logs, pretend we dont if eligable for system mode
            if (android.os.Build.VERSION.SDK_INT >= 16 && isSvcEnabled == false && permissionForReadLogs != 0 && MMCDevice.isDeviceRooted(activity) == true) {

                if (dontAskForRoot == false && askForRoot != 0) {
                    MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCreated", "we do not have the READ_LOGS permission!");

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setCancelable(false);

                    String strTitle = activity.getString(R.string.dashboard_root_request_title);
                    String strText = activity.getString(R.string.dashboard_root_request_message);
                    if (isSvcAllowed && !isSystem(activity))
                        strText = activity.getString(R.string.dashboard_root_request_system_message);
                    String appname = activity.getString(R.string.app_label);
                    if (!appname.equals("MyMobileCoverage")) {
                        strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
                        strText = strText.replaceAll("MyMobileCoverage", appname);
                    }
                    builder.setTitle(strTitle);
                    builder.setMessage(strText);
                    builder.setInverseBackgroundForced(true);

                    builder.setPositiveButton(R.string.GenericText_Yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "checkForRootedLogcat", "Super user allowed");
                            dialog.dismiss();
                            // Allow root access always unless otherwise changed in preferences
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, true).commit();
                            if (isSvcAllowed == true && !isSystem(activity)) {
                                MMCSystemUtil.promptInstallSystem(activity, true);
                            } else {
                                getRootPrivilege(activity, pname);
                            }
                        }
                    });

                    builder.setNegativeButton(R.string.GenericText_No, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Disable root access always unless otherwise changed in preferences
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, false).commit();
                            dialog.dismiss();
                            // Don't ask again for root access
                            if (isSvcAllowed && !isSystem(activity))
                                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_SYSTEM_ACCESS, true).commit();
                            else
                                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, true).commit();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();

                } else if (permissionForReadLogs != 0 && permissionForRoot == true) {
                    //getRootPrivilege(pname);
                } else {
                    MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCreated", "we do not have user permission to READ_LOGS!");
                }
            } else if (permissionForReadLogs == 0) {
                SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(activity);
                preferenceSettings.edit().putBoolean(PreferenceKeys.Miscellaneous.READ_LOG_PERMISSION, true).commit();
            } else if (android.os.Build.VERSION.SDK_INT >= 16 && MMCDevice.isDeviceRooted(activity) == false && isSvcEnabled == false && permissionForReadLogs != 0 &&
                    dontAskForNonRoot == false && dontAskForRoot == false && dontAskForSystem == false) {
                // IF non-Rooted phone has never been asked about rooting, tell them about it
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(false);

                String strTitle = activity.getString(R.string.dashboard_nonroot_request_title);
                String strText = activity.getString(R.string.dashboard_nonroot_request_message);
                if (isSvcAllowed)
                    strText = activity.getString(R.string.dashboard_nonroot_request_system_message);
                String appname = activity.getString(R.string.app_label);
                if (!appname.equals("MyMobileCoverage")) {
                    strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
                    strText = strText.replaceAll("MyMobileCoverage", appname);
                }
                builder.setTitle(strTitle);
                builder.setMessage(strText);
                builder.setInverseBackgroundForced(true);

                builder.setNeutralButton(R.string.GenericText_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_NON_ROOT, true).commit();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    public static boolean isServiceModeEnabled ()
    {
        try {
            if (Build.VERSION.SDK_INT < 19)
                privapp = "app";
            File f = new File("/system/" + privapp + "/mmcsys.apk");
            if (f.exists())
                return true;
            MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "isServiceModeEnabled", "not found: " + "/system/" + privapp + "/mmcsys.apk");
            return false;
        }
        catch (Exception e)
        {
            MMCLogger.logToFile(MMCLogger.Level.ERROR, "Settings", "isServiceModeEnabled", "Exception", e);
        }
        return false;
    }

    public static void getRootPrivilege(Activity activity, String pname) {

        String[] CMDLINE_GRANTPERMS = { "su", "-c", null };

        try {
            // format the command line parameter
            CMDLINE_GRANTPERMS[2] = String.format("pm grant %s android.permission.READ_LOGS", pname);
            java.lang.Process p = Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
            // *** might create a zombie process everything time ^^^^ this line
            // ("Runtime.getRuntime().exec()") is executed
            int res = p.waitFor();

            if (res != 0)
                throw new Exception("failed to become root");

            SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(activity);
            preferenceSettings.edit().putBoolean(PreferenceKeys.Miscellaneous.READ_LOG_PERMISSION, true).commit();

            Intent restartSelf = new Intent(activity.getApplicationContext(),
                    activity.getClass());
            restartSelf.setPackage(activity.getPackageName());
            PendingIntent restartServicePI = PendingIntent.getActivity(
                    activity.getApplicationContext(), 1, restartSelf,
                    PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager)activity.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +1000, restartServicePI);

            // Don't ask again for root access
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, true).commit();
            System.exit(0);


        } catch (Exception e) {
            MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "exec(): ", e.toString());
            MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getRootPrivilege", "Failed to obtain READ_LOGS permission");
        }
    }

    public static void startRilReader (Context context, boolean bStart, boolean useLogcat)
    {
        if (bStart) {

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(getRawRadioPackageName (context), "com.cortxt.app.rilreader.RadioLog.RadioLogService"));
            //intent.setClassName("com.cortxt.app.rilreader", "com.cortxt.app.rilreader.RadioLog.RadioLogService");
            intent.putExtra("cmd", "start");
            intent.putExtra("appname", context.getPackageName());
            intent.putExtra("svcmode", true);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean value = pref.getBoolean("KEY_SETTINGS_SVCMODE_BASIC", true);
            intent.putExtra("basic", value);
            value = pref.getBoolean("KEY_SETTINGS_SVCMODE_NEIGHBORS", false);
            intent.putExtra("neighbors", value);
            value = pref.getBoolean("KEY_SETTINGS_SVCMODE_MM", false);
            intent.putExtra("mm", value);
            if (Build.VERSION.SDK_INT < 20)
                intent.putExtra("needlogcat", true);
            if (useLogcat)
                intent.putExtra("wantlogcat", true);

            context.startService(intent);
        }
        else
        {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(getRawRadioPackageName(context), "com.cortxt.app.rilreader.RadioLog.RadioLogService"));
            intent.putExtra ("cmd", "stop");
            intent.putExtra("svcmode", true);

            context.startService(intent);
        }
    }
    // logcat/svcmode module could be internal or seperate system app, depending if this app already has system permissions
    private static String getRawRadioPackageName (Context context)
    {
        boolean svcmodeActive = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("KEY_SETTINGS_SVCMODE", false);

        if (svcmodeActive == true && !MMCSystemUtil.isSystem(context))
            return "com.cortxt.app.rilreader";
        else
            return context.getPackageName();
    }
}
