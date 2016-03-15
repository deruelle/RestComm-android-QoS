package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.List;

public class AccelerometerHistory {
	
	private List<AccelerometerSample> accelerometer_history = new ArrayList<AccelerometerSample>();
	public static final String TAG = AccelerometerHistory.class.getSimpleName();
	
	//each new train station cleanup history list by removing old items
	public void clearAccelerometerHistory()	{
		for(AccelerometerSample sample : accelerometer_history) {			
			if (sample.timestamp + 180*60000 < System.currentTimeMillis())	{
				accelerometer_history.remove(sample);				
			}
			else {
				break;	
			}
		}
	}
	
	public String updateAccelerometerHistory(float e, float n, float r) {			
		try	{
			AccelerometerSample sample = new AccelerometerSample(e, n, r);
			accelerometer_history.add(sample);
			return this.toString();			
		}
		catch (Exception x)	{
			return null;
		}		
	}
	
	public String toString(long startTime, long stopTime) {		
		String txt = "";
		int size = getSize();
		for(int i = 0; i < size; i++) {
			AccelerometerSample sample = accelerometer_history.get(i);
			if (sample.timestamp >= startTime-10000 && sample.timestamp <= stopTime)
			{
				txt += (sample.timestamp - startTime)/1000;
				txt += "," + (int)(sample.x);
				txt += "," + (int)(sample.y);
				txt += "," + (int)(sample.z);
				txt += "\r\n ";
			}
		}
		return txt;
	}

	public int getSize() {
		return accelerometer_history.size();
	}
	
	public class AccelerometerSample {
		
		private long timestamp = 0;
		private float x = 0;
		private float y = 0;
		private float z= 0;
		
		public AccelerometerSample(float x, float y, float z) {
			this.timestamp = System.currentTimeMillis();
			this.x = x;
			this.y = y;
			this.z = z;
		}		
	}
}
