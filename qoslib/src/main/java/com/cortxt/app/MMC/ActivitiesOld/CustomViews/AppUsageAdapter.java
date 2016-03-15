package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.cortxt.app.MMC.R;
import com.cortxt.com.mmcextension.datamonitor.beans.CPUStatBean;
import com.cortxt.com.mmcextension.datamonitor.beans.DataStatsBean;
import com.cortxt.com.mmcextension.datamonitor.beans.MemoryStatBean;
import com.cortxt.com.mmcextension.datamonitor.beans.RunningAppsBean;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * Adapter to display the list of statistics of different types
 */
public class AppUsageAdapter extends BaseAdapter {

	private ArrayList<DataStatsBean> mDataStats;
	
	private Context mContext;

	public AppUsageAdapter(Context context) {
		mContext = context;
	}

	public void setCPUUsage(ArrayList<CPUStatBean> listOfData) {
		mDataStats = null;
		
	}

	public void setMemoryUsage(ArrayList<MemoryStatBean> listOfData) {
		listOfData = null;
		mDataStats = null;
		
	}

	public void setDataUsage(ArrayList<DataStatsBean> listOfData) {
		mDataStats = listOfData;
		
	}



	public void setRunningAppsDurationData(ArrayList<RunningAppsBean> listOfData) {
		mDataStats = null;
		
		//mRunningAppsDurationList = listOfData;
		
	}


	@Override
	public int getCount() {
		if (mDataStats != null) {
			return mDataStats.size();
		}  else
			return 0;
//		if (mCpuStats != null) {
//			return mCpuStats.size();
//		} else if (mMemoryStats != null) {
//			return mMemoryStats.size();
//		} else if (mConnectionFailuresList != null) {
//			return mConnectionFailuresList.size();
//		} else if (mSMSStatsList != null) {
//			return mSMSStatsList.size();
//		} else {
//			return mCrashStatsList.size();
//		}
	}

	@Override
	public Object getItem(int position) {
		if (mDataStats != null) {
			return mDataStats.get(position);
		} else {
			return null;
		}	
		
//		 else if (mConnectionFailuresList != null) {
//			return mConnectionFailuresList.get(position);
//		} else if (mSMSStatsList != null) {
//			return mSMSStatsList.get(position);
//		} else {
//			return mCrashStatsList.get(position);
//		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = (View) inflater.inflate(R.layout.listitem, null);
			holder = new ViewHolder();
			holder.appName = (TextView) convertView.findViewById(R.id.name);
			holder.appPackageName = (TextView) convertView
					.findViewById(R.id.packagename);
			holder.usage = (TextView) convertView.findViewById(R.id.usageinfo);
			holder.usage1 = (TextView) convertView
					.findViewById(R.id.usageinfo1);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			holder.dataStatLayout = (LinearLayout) convertView
					.findViewById(R.id.data_stat_layout);
			holder.wifiSent = (TextView) convertView
					.findViewById(R.id.wifi_sent);
			holder.wifiReceived = (TextView) convertView
					.findViewById(R.id.wifi_received);
			holder.cellularSent = (TextView) convertView
					.findViewById(R.id.cellular_sent);
			holder.cellularReceived = (TextView) convertView
					.findViewById(R.id.cellular_received);
			holder.wifiPercentage = (TextView) convertView
					.findViewById(R.id.wifi_percent);
			holder.cellularPercentage = (TextView) convertView
					.findViewById(R.id.cellular_percent);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		if (mDataStats != null) {
			holder.usage.setVisibility(View.GONE);
			holder.usage1.setVisibility(View.GONE);
			holder.dataStatLayout.setVisibility(View.VISIBLE);
			holder.icon.setVisibility(View.VISIBLE);
			holder.appName.setText("App Name: "
					+ mDataStats.get(position).getAppName());
			holder.appPackageName.setText("Package Name: "
					+ mDataStats.get(position).getAppPkgName());
			double wifiSent = mDataStats.get(position).getWifiSent();
			double wifiReceived = mDataStats.get(position).getWifiReceived();
			double cellularSent = mDataStats.get(position).getCellularSent();
			double cellularReceived = mDataStats.get(position)
					.getCellularReceived();
			double wifiPercent = (((wifiSent + wifiReceived) / (wifiReceived
					+ wifiSent + cellularReceived + cellularSent)) * 100);
			double cellularPercent = (((cellularReceived + cellularSent) / (wifiReceived
					+ wifiSent + cellularReceived + cellularSent)) * 100);
			holder.wifiSent.setText(String.format(Locale.getDefault(),
					"%.2f KB", wifiSent));
			holder.wifiReceived.setText(String.format(Locale.getDefault(),
					"%.2f KB", wifiReceived));
			holder.wifiPercentage.setText(String.format(Locale.getDefault(),
					"%.2f", wifiPercent) + " %");
			holder.cellularSent.setText(String.format(Locale.getDefault(),
					"%.2f KB", cellularSent));
			holder.cellularReceived.setText(String.format(Locale.getDefault(),
					"%.2f KB", cellularReceived));
			holder.cellularPercentage.setText(String.format(
					Locale.getDefault(), "%.2f", cellularPercent) + " %");
			if (mDataStats.get(position).getIcon() == null) {
				holder.icon.setImageResource(R.drawable.ic_mmclauncher);
			} else {
				holder.icon
						.setImageDrawable(mDataStats.get(position).getIcon());
			}
		} 
//		else if (mRunningAppsDurationList != null) {
//			holder.usage.setVisibility(View.VISIBLE);
//			holder.usage1.setVisibility(View.VISIBLE);
//			holder.dataStatLayout.setVisibility(View.GONE);
//			holder.icon.setVisibility(View.GONE);
//			holder.appName.setText("App Name: "
//					+ mRunningAppsDurationList.get(position).getAppName());
//			holder.appPackageName.setText("Package Name: "
//					+ mRunningAppsDurationList.get(position).getAppPkgName());
//			long currentTotalTime = mRunningAppsDurationList.get(position)
//					.getCurrentTime()
//					- mRunningAppsDurationList.get(position).getStartedTime();
//			long dbTotalTime = mRunningAppsDurationList.get(position)
//					.getTotalTime();
//			long grandTotalTime = currentTotalTime + dbTotalTime;
//			int seconds = (int) (currentTotalTime / 1000) % 60;
//			int minutes = (int) ((currentTotalTime / (1000 * 60)) % 60);
//			int hours = (int) ((currentTotalTime / (1000 * 60 * 60)) % 24);
//			String time = "";
//			if (hours > 0) {
//				if (hours == 1) {
//					time += hours + " Hour";
//				} else {
//					time += hours + " Hours";
//				}
//			}
//			if (minutes > 0) {
//				if (minutes == 1) {
//					time += minutes + " Min";
//				} else {
//					time += minutes + " Mins";
//				}
//			}
//			if (seconds > 0) {
//				if (seconds == 1) {
//					time += seconds + " Sec";
//				} else {
//					time += seconds + " Secs";
//				}
//			}
//			if (time.length() <= 0)
//				holder.usage.setText("Current Running Time:" + 0+" Sec");
//			else
//				holder.usage.setText("Current Running Time:" + time);
//			seconds = (int) (grandTotalTime / 1000) % 60;
//			minutes = (int) ((grandTotalTime / (1000 * 60)) % 60);
//			hours = (int) ((grandTotalTime / (1000 * 60 * 60)) % 24);
//			time = "";
//			if (hours > 0) {
//				if (hours == 1) {
//					time += hours + " Hour";
//				} else {
//					time += hours + " Hours";
//				}
//			}
//			if (minutes > 0) {
//				if (minutes == 1) {
//					time += minutes + " Min";
//				} else {
//					time += minutes + " Mins";
//				}
//			}
//			if (seconds > 0) {
//				if (seconds == 1) {
//					time += seconds + " Sec";
//				} else {
//					time += seconds + " Secs";
//				}
//			}
//			if (time.length() <= 0)
//				holder.usage1.setText("Total Running Time:" + 0+" Sec");
//			else
//				holder.usage1.setText("Total Running Time:" + time);
//			
//		} 
//		else if (mConnectionFailuresList != null) {
//			holder.usage.setVisibility(View.VISIBLE);
//			holder.usage1.setVisibility(View.GONE);
//			holder.dataStatLayout.setVisibility(View.GONE);
//			holder.icon.setVisibility(View.VISIBLE);
//			int connectionType = mConnectionFailuresList.get(position)
//					.getConnectionType();
//			if (connectionType == 0) {
//				holder.appName.setText("Mobile Network ");
//				holder.appPackageName.setVisibility(View.GONE);
//				holder.usage.setText("Time: "
//						+ mConnectionFailuresList.get(position)
//								.getConnectionFailureTime());
//				holder.icon.setImageResource(R.drawable.mobilenetwork);
//			} else if (connectionType == 1) {
//				holder.appName.setText("WiFi Network ");
//				holder.appPackageName.setVisibility(View.GONE);
//				holder.usage.setText("Time: "
//						+ mConnectionFailuresList.get(position)
//								.getConnectionFailureTime());
//				holder.icon.setImageResource(R.drawable.wifi);
//			}
//		} else if (mSMSStatsList != null) {
//			holder.usage.setVisibility(View.VISIBLE);
//			holder.usage1.setVisibility(View.GONE);
//			holder.dataStatLayout.setVisibility(View.GONE);
//			holder.icon.setVisibility(View.VISIBLE);
//			int smsType = mSMSStatsList.get(position).getSMSType();
//			String smsAddress = mSMSStatsList.get(position).getSMSAddress();
//			String smsDate = mSMSStatsList.get(position).getSMSDate();
//
//			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
//					Locale.getDefault());
//			Date date = new Date(Long.parseLong(smsDate));
//
//			if (smsType == 1) {
//				// SMS Received
//				holder.appName.setText("SMS received from " + smsAddress);
//				holder.icon.setImageResource(R.drawable.smsreceived);
//			} else if (smsType == 2) {
//				// SMS SENT
//				holder.appName.setText("SMS sent to " + smsAddress);
//				holder.icon.setImageResource(R.drawable.smssent);
//			} else if (smsType == 5) {
//				// SMS Failed
//				holder.appName.setText("SMS failed to " + smsAddress);
//				holder.icon.setImageResource(R.drawable.smsfailed);
//			}
//			holder.appPackageName.setVisibility(View.GONE);
//			holder.usage.setText("Time: " + dateFormat.format(date).toString());
//		} else if (mThroughputStatsList != null) {
//			holder.usage.setVisibility(View.VISIBLE);
//			holder.usage1.setVisibility(View.GONE);
//			holder.dataStatLayout.setVisibility(View.GONE);
//			holder.icon.setVisibility(View.VISIBLE);
//			holder.appName.setText("App Name: "
//					+ mThroughputStatsList.get(position).getAppName());
//			holder.appPackageName.setText("Package Name: "
//					+ mThroughputStatsList.get(position).getAppPkgName());
//			double throughput = (mThroughputStatsList.get(position)
//					.getThroughput()) / (1024 * 8);
//			holder.usage.setText(String.format(Locale.getDefault(),
//					"Throughput: %.2f KB/Sec", throughput));
//			if (mThroughputStatsList.get(position).getIcon() == null) {
//				holder.icon.setImageResource(R.drawable.ic_launcher);
//			} else {
//				holder.icon.setImageDrawable(mThroughputStatsList.get(position)
//						.getIcon());
//			}
//		}  else {
//			holder.usage.setVisibility(View.VISIBLE);
//			holder.dataStatLayout.setVisibility(View.GONE);
//			holder.icon.setVisibility(View.GONE);
//			holder.appName.setText("App Name: "
//					+ mCrashStatsList.get(position).getAppName());
//			holder.appPackageName.setText("Package Name: "
//					+ mCrashStatsList.get(position).getAppPackage());
//			String lastCrashDate = mCrashStatsList.get(position).getLastCrashDate().trim();
//			int totalCrashCount = mCrashStatsList.get(position).getCrashCount();
//			String appCrashDetails = "Last Crash Time: " + lastCrashDate + "\nCrash count: " + totalCrashCount;
//			holder.usage.setText(appCrashDetails);
//		}
		return convertView;
	}

	class ViewHolder {
		TextView appName;
		TextView appPackageName;
		TextView usage;
		TextView usage1;
		ImageView icon;
		LinearLayout dataStatLayout;
		TextView wifiSent;
		TextView wifiReceived;
		TextView cellularSent;
		TextView cellularReceived;
		TextView wifiPercentage;
		TextView cellularPercentage;
	}

}
