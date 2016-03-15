package com.cortxt.app.MMC.ActivitiesOld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.UsageAdapter;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.Utils.Stats;
import com.cortxt.com.mmcextension.datamonitor.beans.CPUStatBean;
import com.cortxt.com.mmcextension.datamonitor.beans.DataStatsBean;
import com.cortxt.com.mmcextension.datamonitor.beans.MemoryStatBean;
import com.cortxt.com.mmcextension.datamonitor.database.DataMonitorDBReader;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


public class AppUsageStats extends Activity{
	
	private ListView mDataStats;
	
	private UsageAdapter mDataStatsAdapter;
	
	private Stats mNowStats;
	public static final String TAG = AppUsageStats.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view  = inflater.inflate(R.layout.app_usage, null, false);
		
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
        MMCActivity.customizeTitleBar(this, view, R.string.app_usage, R.string.app_usage);
		
		mDataStats = (ListView) findViewById(R.id.datastats);
		mDataStats.setTag(null);
		mDataStatsAdapter = new UsageAdapter(this);
		
		

//		mGetDataStats.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				
//				mNowStats = Stats.DATASTATS;
//				if (statsHandler.post(statsUpdateRunnable)) {
//					//mGetDataStats.setEnabled(false);
//				}
//			}
//		});

		mNowStats = Stats.DATASTATS;
	}
	
	class MemoryStatsComparator implements Comparator<MemoryStatBean> {

		@Override
		public int compare(MemoryStatBean lhs, MemoryStatBean rhs) {
			return Double.parseDouble(lhs.getMemoryUsage().trim()) > Double
					.parseDouble(rhs.getMemoryUsage()) ? -1 : Double
					.parseDouble(lhs.getMemoryUsage().trim()) < Double
					.parseDouble(rhs.getMemoryUsage().trim()) ? 1 : lhs
					.getAppName().compareToIgnoreCase(rhs.getAppName());
		}

	}
	
	private Handler statsHandler;

	private void createStatsHandler() {
		statsHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case 2:
					if (Build.VERSION.SDK_INT < 8) {
						
					}
					mDataStats.setVisibility(View.VISIBLE);
					if (msg.obj == null) {
					} else {
						ArrayList<DataStatsBean> data_stats = (ArrayList<DataStatsBean>) msg.obj;
						if (data_stats != null && data_stats.size() > 0) {
							mDataStatsAdapter.setDataUsage(data_stats);
							if (mDataStats.getTag() != null) {
								mDataStatsAdapter.notifyDataSetChanged();
							} else {
								mDataStats.setTag(data_stats);
								mDataStats.setAdapter(mDataStatsAdapter);
							}
						}
					}
					
					//statsHandler.postDelayed(statsUpdateRunnable, 10000);
					break;
				default:
					break;
				}
			};
		};
	}

	@Override
	protected void onPause() {
//		statsHandler.removeMessages(0);
//		statsHandler.removeMessages(1);
//		statsHandler.removeMessages(2);
//		statsHandler.removeCallbacks(statsUpdateRunnable);
		statsHandler = null;
		super.onPause();
	};

	@Override
	protected void onResume() {
		createStatsHandler();
		DataMonitorDBReader dmReader = new DataMonitorDBReader();
		ArrayList<DataStatsBean> data_stats = dmReader.getDataStatistics(getApplicationContext(), null);
		Message msg = statsHandler.obtainMessage();
		msg.what = 2;
		msg.obj = data_stats;
		statsHandler.sendMessage(msg);
//		mGetCpuStats.setEnabled(true);
//		mGetMemStats.setEnabled(true);
//		mGetMemStats.setEnabled(true);
//		statsHandler.post(statsUpdateRunnable);
		super.onResume();
	}
	
	public void backActionClicked(View v) {
		this.finish();
	}


//	private void executeTop() {
//		java.lang.Process p = null;
//		BufferedReader in = null;
//		try {
//			p = Runtime.getRuntime().exec("top -n 1 -d 1");
//			if (p == null) {
//				MMCLogger.logToFile(MMCLogger.Level.WARNING, TAG, "executeTop", "Requested Program cannot be executed.");
//				Log.e("executeTop", "Requested Program cannot be executed.");
//				return;
//			}
//			in = new BufferedReader(new InputStreamReader(p.getInputStream()));
//			String line = null;
//			int cpuIndex = -1;
//			int nameIndex = -1;
//			int pidIndex = -1;
//			listOfData = new ArrayList<CPUStatBean>();
//			while ((line = in.readLine()) != null) {
//				CPUStatBean appData = new CPUStatBean();
//				if (line.contains("CPU%") && line.contains("Name")) {
//					line = line.trim();
//					String[] headers = line.split("\\s+");
//					for (int i = 0; i < headers.length; i++) {
//						if (headers[i].equals("PID")) {
//							pidIndex = i;
//						} else if (headers[i].equals("CPU%")) {
//							cpuIndex = i;
//						} else if (headers[i].equals("Name")) {
//							nameIndex = i;
//						}
//					}
//					continue;
//				}
//				if (line.contains(".") && pidIndex != -1 && nameIndex != -1
//						&& cpuIndex != -1) {
//					line = line.trim();
//					String[] listOfParams = line.split("\\s+");
//					ApplicationInfo ai;
//					final PackageManager pm = getApplicationContext()
//							.getPackageManager();
//					try {
//						appData.setPid(listOfParams[pidIndex].trim());
//						appData.setCpuUsage(listOfParams[cpuIndex].trim().replace(
//								"%", ""));
//						appData.setPackageName(listOfParams[nameIndex].trim());
//						
//						ai = pm.getApplicationInfo(
//								listOfParams[nameIndex].trim(), 0);
//					} catch (final NameNotFoundException e) {
//						ai = null;
//					} catch (Exception e)
//					{
//						ai = null;
//						appData = null;
//					}
//					if (appData != null)
//					{
//						Drawable icon = (ai != null ? pm.getApplicationIcon(ai)
//								: null);
//						appData.setIcon(icon);
//						if (ai != null) {
//							if (isARunningProcess(ai.processName)
//									&& ai.uid >= 10000) {
//								appData.setAppName((String) pm
//										.getApplicationLabel(ai));
//								listOfData.add(appData);
//							}
//						}
//					}
//				}
//			}
//			MMCLogger.logToFile(MMCLogger.Level.WARNING, TAG, "executeTop", "total number of running processes" + listOfData.size());
//			
//		} catch (IOException e) {
//			MMCLogger.logToFile(MMCLogger.Level.WARNING, TAG, "executeTop", "Requested Program cannot be executed.", e);
//			
//		} finally {
//			try {
//				if (in != null)
//					in.close();
//				destroyProcess(p);
//			} catch (IOException e) {
//				MMCLogger.logToFile(MMCLogger.Level.WARNING, TAG, "executeTop", "error in closing and destroying top process.", e);
//			}
//		}
//	}
//
//	private boolean isARunningProcess(String processName) {
//		if (processName == null)
//			return false;
//		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//		List<RunningAppProcessInfo> processes = manager
//				.getRunningAppProcesses();
//
//		for (RunningAppProcessInfo process : processes) {
//			if (processName.equals(process.processName)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private static void destroyProcess(Process process) {
//		try {
//			if (process != null) {
//				process.exitValue();
//			}
//		} catch (IllegalThreadStateException e) {
//			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "destroyProcess", "Illegal state exception occurred while destroying the process.", e);
//			process.destroy();
//		}
//	}
//
//	private ArrayList<CPUStatBean> listOfData;
//
//	private ArrayList<MemoryStatBean> getMemoryStats() {
//		ArrayList<MemoryStatBean> numberOfApplications = new ArrayList<MemoryStatBean>();
//		if (listOfData != null) {
//			int[] numberOfProcesses = new int[listOfData.size()];
//			for (int i = 0; i < listOfData.size(); i++) {
//				numberOfProcesses[i] = Integer.parseInt(listOfData.get(i)
//						.getPid().trim());
//			}
//			ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//			Debug.MemoryInfo[] memoryInfo = activityManager
//					.getProcessMemoryInfo(numberOfProcesses);
//			double totalRAM = 0;
//			for (int i = 0; i < memoryInfo.length; i++) {
//				if (memoryInfo[i].getTotalPss() > 0) {
//					MemoryStatBean bean = new MemoryStatBean();
//					bean.setAppName(listOfData.get(i).getAppName());
//					bean.setIcon(listOfData.get(i).getIcon());
//					bean.setPackageName(listOfData.get(i).getPackageName());
//					totalRAM = totalRAM
//							+ (memoryInfo[i].getTotalPss() / 1024.0);
//					bean.setMemoryUsage(String.format(Locale.getDefault(),
//							"%.2f", memoryInfo[i].getTotalPss() / 1024.0));
//					bean.setPid(listOfData.get(i).getPid());
//					numberOfApplications.add(bean);
//				}
//			}
//			return numberOfApplications;
//		}
//		return null;
//	}
}
