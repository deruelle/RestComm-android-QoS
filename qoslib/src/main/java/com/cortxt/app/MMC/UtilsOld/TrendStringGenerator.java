package com.cortxt.app.MMC.UtilsOld;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;

import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.Utils.MMCLogger;;


/**
 * The Server requires a list of coverage samples associated with each event
 * This provides the changes in cells, locations and various signal measurements
 * throughout a time span beginning about 30 seconds before until a minute or so after the event
 * We call it the events time on the 'stage' or 'pre-stage' for the samples leading up to the event
 * 
 * <h2>An Explanation of the Trend Strings</h2>
 * The sample list is built as a list of Cell changes and each cell contains a list of signal/location items
 * Each item in the list has to be different than the previous item (different signal or location)
 * 
 * Different Android phones have different types of signal measurements available
 * In order to accommodate the differences, there is a header which gives the names of the columns for each sample
 * For example: time,cov,lat,lng,acc,sig,sats,eci0,snr,rscp,lteRssi,lteRsrp
 * The first 6 items are standard and have their own fields defines
 * (time = seconds from event time, cov = network tier, lat = latitude,lng = longitude,acc=gps accuracy,sig = main signal dBm)
 * The remaining items are kept in a 'layer3' string for each sample (comma delimited)
 * After the samples are built as a list of list of sample objects, 
 * the last step is to convert to the format to be sent to the server.
 * For the current server, it is slightly flattened to a list of csv strings, 2 strings for each cell change
 * 
 */
public class TrendStringGenerator {
	private static final int LOWER_16BITS_MASK		= 0xffff;
	private static final int UPPER_16BITS_MASK		= ~0xffff;
	
	private String signalTemplate = "";
	private String stats = "";
	private int signalFieldsMask = 0;
	private EventOld trendevent = null;

	public static final String TAG = TrendStringGenerator.class.getSimpleName();

	/**
	 * This method uses the various cursors provided to generate list of cells and samples to send to the server
	 * In the main loop, it weaves between 3 cursors which provide lists of cells, signals and locations
	 * The cursors are sorted by timestamp but each has their own random schedule
	 * For each iteration, it 
	 * When data related to a particular event is being dealt with, there is a risk of overlapping. That is, if a pair
	 * of events are very close together, then their data can overlap. The server doesn't like these overlaps, which is why
	 * we use a system of "staging" to overcome this problem.
	 * 
	 * In the data provided to this method, only signalDataCursor is really supposed to be staged (which is why we will
	 * use signalDataCursor as the reference cursor in this method). The staging of cellData and locationData is useful
	 * but not necessary (it can lead to a narrowed down query using eventId=? rather than using timestamps).
	 * @param cellDataCursor
	 * @param locationDataCursor
	 * @param signalDataCursor
	 * @param coverageDataCursor
	 * @param phoneType
	 * @return
	 */
	public CoverageSamplesSend generateSamples (Cursor cellDataCursor, Cursor locationDataCursor, Cursor signalDataCursor, MMCService context, EventOld event){
		int i;
		if (cellDataCursor.getCount() == 0 && locationDataCursor.getCount() == 0 && signalDataCursor.getCount() == 0)
			return null;

		trendevent = event;
		long currentQuantizedTimestamp = 0;
		long previousQuantizedTimestamp = 0;
		long currentTimestamp = event.getEventTimestamp() - event.getEventType().getPreEventStageTime();	//get the starting timestamp of the trend string
		int prevsig = 0;
		// Gather the 3 data cursors and move them just prior to the pre-stage time to get initial values for the first sample
		CursorType.CELL.setCursor(cellDataCursor);
		CursorType.LOCATION.setCursor(locationDataCursor);
		CursorType.SIGNAL.setCursor(signalDataCursor);
		moveCursorsToTheBeginning(currentTimestamp);
		
		// Get the initial signal values
		Integer[] currentSignalStrengths = new Integer[EventOld.SIG_COVERAGE+1];
		Integer[] signalStrengthIndexes = new Integer[EventOld.SIG_COVERAGE+1];
		for (i=EventOld.SIG_SIGNAL; i<=EventOld.SIG_COVERAGE; i++)
			signalStrengthIndexes[i] = signalDataCursor.getColumnIndexOrThrow(TablesOld.SignalStrengths.getName(i));
		
		getSignalStrengthValues (signalDataCursor, signalStrengthIndexes, currentSignalStrengths);
			
		signalTemplate = getSignalStrengthTemplate(currentSignalStrengths, signalStrengthIndexes, signalDataCursor);
		// Add signal template as header to the trend, it specifies the number and type of extra columns of layer 3 signal
		// for example: "time,cov,lat,lng,acc,sig,eci0,snr,rscp,lteRssi,lteRsrp"
		// following that, every signal portion of the sample (following the basestation) will follow the format:
		// ",-72,15,-30,51.08445,-114.02512,26,FDEBD098,410000,-80,6,-78,-94,-101," according to the fields listed in the header template
		String header = "time,cov,lat,lng,acc,sig,sat" + signalTemplate;
		
		CellSamples cellSamples = new CellSamples(cellDataCursor), prevcellSamples = null;
		CoverageSamples covSamples = new CoverageSamples();
		covSamples.setHeader(header);
		covSamples.setStartTime(event.getEventTimestamp());
		covSamples.cells = new ArrayList<CellSamples>();

		// Prepare to get location values as it 
		double currentLatitude = 0.0;
		double currentLongitude = 0.0;
		double currentAccuracy =  0.0;
		int currentSatellites = 0;
		// Get the initial signal values
		int latitudeIndex = locationDataCursor.getColumnIndexOrThrow(TablesOld.Locations.LATITUDE);
		int longitudeIndex = locationDataCursor.getColumnIndexOrThrow(TablesOld.Locations.LONGITUDE);
		int gpsAccuracyIndex = locationDataCursor.getColumnIndexOrThrow(TablesOld.Locations.ACCURACY);
		int gpsSatellitesIndex = locationDataCursor.getColumnIndexOrThrow(TablesOld.Locations.SATELLITES);
		
		boolean bRepeatSample = false;
		
		boolean hasBaseStationBeenWritten = false;		//this is a flag to specify whether a particular base station has been mentioned
		//in the trend string in the last pass. When the base station changes (when the 
		//cellDataCursor is moved), this is made false.
		
		EvtSample sample = new EvtSample ();
		sample.lat = (int)(currentLatitude * 100000);
		sample.lng = (int)(currentLongitude * 100000); 
		sample.acc = (int)currentAccuracy;
		sample.sats = (int)currentSatellites;
		fillSampleSignal (sample, currentSignalStrengths, event);
		currentQuantizedTimestamp = (long) (currentTimestamp/2000) * 2000;
		sample.sec = (int)((currentQuantizedTimestamp - (long)(event.getEventTimestamp()/2000)*2000))/1000; //  / 1000); // iNextSeconds;
		
		// If any cursor is prior to the pre-stage time, move it to next, and it will be just inside the staging timespan
		if (!cellDataCursor.isAfterLast() && cellDataCursor.getLong(1) < currentTimestamp)
			cellDataCursor.moveToNext();
		if (!signalDataCursor.isAfterLast() && signalDataCursor.getLong(1) < currentTimestamp)
			signalDataCursor.moveToNext();
		if (!locationDataCursor.isAfterLast() && locationDataCursor.getLong(1) < currentTimestamp)
			locationDataCursor.moveToNext();

		CursorType oldestCursor = getNextCursor(currentTimestamp);	//this is the cursor with the smallest timestamp out of the 4 cursors available
		if (oldestCursor == null)
			return null;
		
		// if first sample time is later than the prestage time (-30 sec), repeat the sample for -30 sec and the actual timestamp
		if (oldestCursor.getTimestamp() > currentTimestamp)
			bRepeatSample = true;
		else
			bRepeatSample = false;
		/*
		 * The oldestCursor cannot be null because if it was then it would be guaranteed that 
		 * signalDataCursor has reached the end row. And if that had been so, then the loop would not have
		 * continued its execution.
		 */
		try {
			do {			
				
				previousQuantizedTimestamp = currentQuantizedTimestamp;
				currentQuantizedTimestamp = (long) (currentTimestamp/2000) * 2000;
				int seconds = (int)((currentQuantizedTimestamp - (long)(event.getEventTimestamp()/2000)*2000))/1000; //  / 1000); // iNextSeconds;
				
				// Update anything we can in the current sample from the current DB record
				// multiple db records may update each sample (location, cell, signal etc)
				if (sample.sec == seconds)
				{
					fillSampleSignal (sample, currentSignalStrengths, event);
					sample.lat = (int)(Math.round(currentLatitude * 100000));
					sample.lng = (int)(Math.round(currentLongitude * 100000));
					sample.acc = (int)currentAccuracy;
					sample.sats = (int)currentSatellites;
				}
				boolean bCellChanged = false;
				if (!hasBaseStationBeenWritten && (prevcellSamples == null || !prevcellSamples.equals(cellSamples))){
					covSamples.cells.add(cellSamples);
					hasBaseStationBeenWritten = true;
					bCellChanged = true;
					prevcellSamples = cellSamples;
				}
				// add to the sample list if timestamp changed by 2 seconds
				if (currentQuantizedTimestamp != previousQuantizedTimestamp || (bCellChanged && covSamples.cells.size() > 1)) {//  || Math.abs(sample.sig - prevsig) > 10) {
					previousQuantizedTimestamp = currentQuantizedTimestamp;
					
					cellSamples.addSample (sample);
					sample = new EvtSample(sample);
					sample.sec = seconds; 
					fillSampleSignal (sample, currentSignalStrengths, event);
					sample.lat = (int)(Math.round(currentLatitude * 100000));
					sample.lng = (int)(Math.round(currentLongitude * 100000));
					sample.acc = (int)currentAccuracy;
					sample.sats = (int)currentSatellites;
					prevsig = sample.sig;
					
					// Stop filling in samples when its beyond the staging time, to avoid much overlap on the next event
					//if (currentTimestamp > event.getStageTimestamp() + 2000 && sample.lat != 0f && event.getStageTimestamp() > event.getEventTimestamp() && event.getEventType() != EventType.MAN_TRACKING) 
					//	break;
					if (currentTimestamp > event.getStageTimestamp() + 60000 && event.getStageTimestamp() > event.getEventTimestamp() && event.getEventType() != EventType.MAN_TRACKING 
							&& event.getEventType() != EventType.MAN_TRANSIT && event.getEventType() != EventType.MAN_PLOTTING)
						break; 
						
				}
				if (oldestCursor != null && !bRepeatSample)
				{
					//now update the local variables
					switch(oldestCursor){
					case CELL:
						if (!CursorType.CELL.getCursor().isAfterLast())
						{
							CellSamples cellSamplesNew = new CellSamples(cellDataCursor);
							// make sure cell is actually different than previous cell sample
							if (prevcellSamples == null || 
									cellSamplesNew.high != prevcellSamples.high || cellSamplesNew.mid != prevcellSamples.mid || cellSamplesNew.low != prevcellSamples.low || cellSamplesNew.code != prevcellSamples.code || cellSamplesNew.chan != prevcellSamples.chan)
							{
								cellSamples = cellSamplesNew;
								//make sure that this updates cell is added into the trend string
								hasBaseStationBeenWritten = false;
							}
						}
						break;
					
					case LOCATION:
						if (!CursorType.LOCATION.getCursor().isAfterLast()){
							currentLatitude = oldestCursor.getCursor().getDouble(latitudeIndex);
							currentLongitude = oldestCursor.getCursor().getDouble(longitudeIndex);
							currentAccuracy = oldestCursor.getCursor().getDouble(gpsAccuracyIndex);
							currentSatellites = oldestCursor.getCursor().getInt(gpsSatellitesIndex);
						}
						break;
					case SIGNAL:
						if (!CursorType.SIGNAL.getCursor().isAfterLast())
							getSignalStrengthValues (oldestCursor.getCursor(), signalStrengthIndexes, currentSignalStrengths);
						break;
					}
					//update the timestamps and repeat count
					if (oldestCursor.getTimestamp() > currentTimestamp)
						currentTimestamp = oldestCursor.getTimestamp();
					//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "generateSamples", currentTimestamp + "," + oldestCursor.getTimestamp() + "," + currentLatitude + "," + currentLongitude );
					
				}
					 
				if (oldestCursor == null)
					break;
				
				//now move the cursor to the next position
				// The only time the cursor is going to be repeated is for 30 seconds and 0 seconds before event if they are actually the sample values
				if (!bRepeatSample)
				{
					oldestCursor.getCursor().moveToNext();
					oldestCursor = getNextCursor(currentTimestamp);
				}
				else
					currentTimestamp = oldestCursor.getTimestamp();
				
				bRepeatSample = false;
				
			} while(true); 
			if (sample != null && cellSamples != null)
			{
				sample.sec = (int)((currentTimestamp - event.getEventTimestamp()) / 1000); // iNextSeconds;
				cellSamples.addSample (sample);
			}
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generatePrimaryTrendStringForGSM", "Exception", e);
		}
		if (covSamples.cells.size() == 0)
			return null;
		
		return new CoverageSamplesSend(covSamples);
		
	}

	private static CursorType getNextCursor(long currentTimestamp) {
		CursorType returnCursor = null;
		for(CursorType cursorType : CursorType.values()){
			if (cursorType.getCursor().isAfterLast() || cursorType.getTimestamp() < currentTimestamp)
				continue;

			if (returnCursor == null || returnCursor.getTimestamp() > cursorType.getTimestamp()){
				returnCursor = cursorType;
			}
		}
		return returnCursor;
	}

	/**
	 * This method calls the moveToFirst method for all the cursors in the {@link CursorType} enum.
	 * This method assumes that the cursors in the {@link CursorType} enum have been set.
	 */
	private static void moveCursorsToTheBeginning(long preStageTime){
		for(CursorType cursorType : CursorType.values()){
			if (cursorType.getCursor() != null)
			{
				Cursor cursor = cursorType.getCursor();
				if (cursorType == CursorType.CELL || cursorType == CursorType.SIGNAL)
				{
					cursor.moveToLast();
					int timestampIndex  = cursor.getColumnIndex(TablesOld.TIMESTAMP_COLUMN_NAME);
					// find the first known Location after this event began
					while(!cursor.isBeforeFirst() && cursor.getLong(timestampIndex) > preStageTime)
						cursor.moveToPrevious();
					if (cursor.isBeforeFirst())
						cursor.moveToFirst();
				}
				else
				{
					cursor.moveToFirst();
					int timestampIndex  = cursor.getColumnIndex(TablesOld.TIMESTAMP_COLUMN_NAME);
					// find the first known Location after this event began
					while(!cursor.isAfterLast() && cursor.getLong(timestampIndex) < preStageTime)
						cursor.moveToNext();
	
					if (cursor.isAfterLast())
						cursor.moveToLast();
					
				}
			}
		}
	}
	/*
	 * Fill a sample with values from an Integer array of samples returned from the signal table
	 */
	private void fillSampleSignal (EvtSample sample, Integer[] signals, EventOld event)
	{
		if (signals[EventOld.SIG_SIGNAL] == null)
			sample.sig = -255;
		else
			sample.sig = signals[EventOld.SIG_SIGNAL];
		if (signals[EventOld.SIG_SIGNAL] == null)
			sample.cov = event.getTierFromFlags();
		else
			sample.cov = signals[EventOld.SIG_COVERAGE];
		String layer = "";
		int i;
		for (i=EventOld.SIG_SIGNAL+1; i<EventOld.SIG_COVERAGE; i++)
		{
			if ((signalFieldsMask & (1<<i)) > 0)
			{
				layer += signals[i] + ",";
				if (signals[i] != null && signals[i] != 99 && signals[i] != -1 && signals[i] != -120 && signals[i] != -160 && signals[i] != 0 && signals[i] < 10000 && event != null)
					event.setHasSignal (i);
				
			}
		}
		sample.layer3 = layer.trim();
	}
	
	
	private void getSignalStrengthValues (Cursor signalCursor, Integer[] signalStrengthIndexes, Integer[] currentSignalStrengths)
	{
		int i;
		if (signalCursor.isAfterLast())
			return;
		for (i=EventOld.SIG_SIGNAL; i<=EventOld.SIG_COVERAGE; i++)
		{
			if (signalStrengthIndexes[i] != -1)
			{
				currentSignalStrengths[i] = signalCursor.getInt(signalStrengthIndexes[i]);
				
				if (currentSignalStrengths[i] == null || currentSignalStrengths[i] == 255 || currentSignalStrengths[i] == 0 || currentSignalStrengths[i] == 99 || currentSignalStrengths[i] >= 32767 || currentSignalStrengths[i] <= -32767)
				{
					if (i == EventOld.SIG_SIGNAL)
						currentSignalStrengths[i] = -255;
					else if (i == EventOld.SIG_LTE)
						currentSignalStrengths[i] = null;
					else if (i < EventOld.SIG_COVERAGE)
						currentSignalStrengths[i] = -1;
				}
				
			}
			// If -1 was read from private lteRssi variable, change to -255 (unknown signal) to prevent dropping from header
			else if (i == EventOld.SIG_LTE )
				currentSignalStrengths[i] = -255; 
		}
	}
	/**
	 * This method makes sure that null values are cleansed to the INVALID value of
	 * signal strength on the server.
	 * This method assumes that the cursors in {@link CursorType} have been initialized.
	 * @param signalStrength
	 * @param signnalStrengthIndex
	 */
	private String getSignalStrengthTemplate(Integer[] signalStrengths, Integer[] signalStrengthIndex, Cursor signalCursor){
		
		int initialPos = signalCursor.getPosition();
		// Check the signal values at the end as well as the beginning, to try to catch everything that has valid values
		Integer[] signalStrengthsLast = new Integer[EventOld.SIG_COVERAGE+1];
		int i, count = 0;
		signalFieldsMask = 0;
		String validFields = ",";
		while(!signalCursor.isAfterLast())
		{
			getSignalStrengthValues (signalCursor, signalStrengthIndex, signalStrengthsLast);
			// This will be a good place to look at what signal fields have values, to create a template
			for (i=EventOld.SIG_SIGNAL+1; i<EventOld.SIG_COVERAGE; i++)
			{
				// Add signal type to temple if not null, (don't add -1 value either unless for LTE)
				if (signalStrengthsLast[i] != null && (signalStrengthsLast[i] != -1 || i == EventOld.SIG_LTE) && (signalFieldsMask & (1<<i)) != (1<<i))
				{	
					
					count ++;
					signalFieldsMask |= (1<<i);
					//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getSignalStrengthTemplate", validFields);
				}
			}
			signalCursor.moveToNext();
		}
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getSignalStrengthTemplate",  "signalFieldsMask " + signalFieldsMask);
		
		for (i=EventOld.SIG_SIGNAL+1; i<EventOld.SIG_COVERAGE; i++)
		{
			if ((signalFieldsMask & (1<<i)) > 0)
				validFields += TablesOld.SignalStrengths.getName(i) + ",";
		}

//		signalCursor.moveToLast();
//		if (!signalCursor.isAfterLast() && !signalCursor.isFirst())
//		{
//			signalCursor.moveToPrevious();
//			if (!signalCursor.isBeforeFirst())
//				getSignalStrengthValues (signalCursor, signalStrengthIndex, signalStrengthsLast);
//		}
		signalCursor.moveToPosition(initialPos);
		
		// This will be a good place to look at what signal fields have values, to create a template
//		int i, count = 0;
//		String validFields = ",";
//		for (i=EventOld.SIG_SIGNAL+1; i<EventOld.SIG_COVERAGE; i++)
//		{
//			if ((signalStrengths[i] != null && signalStrengths[i] != -1) || (signalFieldsMask & (1<<i)) > 0)
//			{	
//				validFields += TablesOld.SignalStrengths.getName(i) + ",";
//				count ++;
//				signalFieldsMask |= (1<<i);
//			}
//			// Add signal type to temple if not null, (don't add -1 value either unless for LTE)
//			else if (signalStrengthsLast[i] != null && (signalStrengthsLast[i] != -1 || i == EventOld.SIG_LTE))
//			{	
//				validFields += TablesOld.SignalStrengths.getName(i) + ",";
//				count ++;
//				signalFieldsMask |= (1<<i);
//			}
//		}
		if (validFields.length() > 0) // remove final comma
			validFields = validFields.substring(0, validFields.length()-1);
		return validFields;
		
	}
	
	public class EvtSample
	{
		public int sig = 0, cov = 0, sec = -999;
		public String layer3 = "";
		public int lat = 0, lng = 0;
		private int acc, sats;
		public EvtSample () {}
		public EvtSample (EvtSample sample)
		{
			if (sample != null)
			{
				sig = sample.sig; cov=sample.cov; lat = sample.lat; lng = sample.lng;
				acc = sample.acc; layer3 = sample.layer3;
				sats = sample.sats;
			}
		}
		public String toString () 
		{
			return String.format(Locale.US,"%d,%d,%d,%d,%d,%d,%d,%s ", sec,cov,lat,lng,acc,sig,sats,layer3);
		}
	}
	
	private class CellSamples
	{
		public int high, mid, low, code;
		public int band, chan;
		public String netType;
		ArrayList<EvtSample> samples = new ArrayList<EvtSample>();
		public CellSamples (int _high, int _mid, int _low, int _code, String _netType)
		{
			high = _high;
			mid = _mid;
			low = _low;
			code = _code; // (PSC)
			netType = _netType;
		}
		public CellSamples (Cursor cellCursor)
		{	
			int currentHigh = 0;				
			int currentMid = 0;	
			int currentLow = 0;
			int currentCode = 0;
			int currentBand = 0;
			int currentChan = 0;
			
			if (!cellCursor.isAfterLast())
			{
				int highIndex, midIndex, lowIndex, codeIndex, bandIndex, chanIndex, netTypeIndex;
				
				highIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_HIGH);				
				midIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_MID);
				lowIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_LOW);
				codeIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_CODE);
				bandIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_BAND);
				chanIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.BS_CHAN);
				netTypeIndex = cellCursor.getColumnIndexOrThrow(TablesOld.BaseStations.NET_TYPE);
							
				currentHigh = cellCursor.getInt(highIndex);
				currentHigh = currentHigh == -1 ? 0 : currentHigh;	//server treats zero an invalid
				
				currentMid = cellCursor.getInt(midIndex);
				currentMid = currentMid == -1 ? 0 : currentMid;	//server treats zero as invalid
				
				currentLow = cellCursor.getInt(lowIndex);
				currentLow = currentLow == -1 ? 0 : currentLow;	//server treats zero as invalid
				
				currentCode = cellCursor.getInt(codeIndex);
				currentCode = currentCode == -1 ? 0 : currentCode;	//server treats zero as invalid

				currentBand = cellCursor.getInt(bandIndex);
				currentBand = currentBand == -1 ? 0 : currentBand;	//server treats zero as invalid

				currentChan = cellCursor.getInt(chanIndex);
				currentChan = currentChan == -1 ? 0 : currentChan;	//server treats zero as invalid

				high = currentHigh;
				mid = currentMid;
				low = currentLow;
				code = currentCode;
				band = currentBand;
				chan = currentChan;
				netType = cellCursor.getString(netTypeIndex);

			}
		}
		public void addSample (EvtSample sample)
		{
			if (trendevent.getEventType() == EventType.MAN_PLOTTING && sample.lat == 0)
				return;
			// don't add sample if its the same
			if (samples.size() > 0)
			{
				EvtSample lastSmp = (samples.get(samples.size()-1));
				
				if (lastSmp.lat == sample.lat && lastSmp.lng == sample.lng && lastSmp.acc < 0 && sample.acc < 0)
					return;
				if (lastSmp.lat == sample.lat && lastSmp.lng == sample.lng && lastSmp.sig == sample.sig && lastSmp.acc == sample.acc
							&& lastSmp.cov == sample.cov)
				{
					if (lastSmp.layer3 != null && lastSmp.layer3.length() > 1 && sample.layer3 != null && sample.layer3.length() > 2)
					{
						String[] lparts0 = lastSmp.layer3.split(",");
						String[] lparts1 = sample.layer3.split(",");
						if (lparts0.length > 0 && lparts1.length > 0 && lparts0[0].equals(lparts1[0]))
						{
							if (lparts0.length > 1 && lparts1.length > 1 && lparts0.length == lparts1.length && lparts0[1].equals(lparts1[1]))
								return;
							if (lparts0.length == 1 && lparts1.length == 1)
								return;
						}
						//else if (lparts0.length <= 1)
						//	MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "addSample", "problem: lastSmp.layer3=" + lastSmp.layer3 + "   sample.layer3=" + sample.layer3);
					}
					else
						return;
				}
				
			}
			samples.add(sample);
		}
	}
	
	public class CoverageSamples
	{
		private long startTime = 0l;
		private String aheader;
		public List<CellSamples> cells;
		
		public long getStartTime () {return startTime;}
		public void setStartTime (long value) { startTime = value;}
		public String getHeader () {return aheader;}
		public void setHeader (String value) { aheader = value;}
	}
	
	public class CellSamplesSend
	{
		protected String cell = "";
		public String samples = "";
		public CellSamplesSend (CellSamples cellsamp)
		{
			cell = cellsamp.high + "," + cellsamp.mid + "," + cellsamp.low;
			if (cellsamp.code > 0)
				cell += "," + cellsamp.code;
			else
				cell += ",";
			if (cellsamp.band > 0)
				cell += "," + cellsamp.band;
			else
				cell += ",";
			if (cellsamp.chan > 0)
				cell += "," + cellsamp.chan;
			else
				cell += ",";

			int i;
			for (i=0; i<cellsamp.samples.size(); i++)
				addSample (cellsamp.samples.get(i));
		}
		public void addSample (EvtSample sample)
		{
			samples += sample.toString();
		}
	}
	
//	public static class AccessPointSamples
//	{	
//		protected static String accessPoints = "";
//		public String samples = "";
//		public int type;
//		
//		public AccessPointSamples (AccessPointHistory accessPointsamp)
//		{
//			List<AccessPointSample> list = accessPointsamp.getListBasedOnType(getType());
//			
//			for(AccessPointSample sample : list) {	
//				accessPoints += accessPointsamp.toString(sample);	
//			}
//		}	
//		public static String getAccessPointSamples() {
//			return accessPoints;
//		}
//		
//		public void setType(int _type) {
//			type = _type;
//		}
//		
//		public int getType() {
//			return type;
//		}
//	}
//	
	public class CoverageSamplesSend
	{
		private long startTime = 0l;
		private String aheader;
		public ArrayList<CellSamplesSend> cells = new ArrayList<CellSamplesSend>();
		public CoverageSamplesSend (CoverageSamples cov)
		{
			int i,j;
			for (i=0; i<cov.cells.size(); i++)
				cells.add(new CellSamplesSend(cov.cells.get(i)));
			startTime = cov.startTime;
			aheader = cov.aheader;
		}
		
		public long getStartTime () {return startTime;}
		public void setStartTime (long value) { startTime = value;}
		public String getHeader () {return aheader;}
		public void setHeader (String value) { aheader = value;}
	}
}

enum CursorType {
	CELL,
	SIGNAL,
	LOCATION;
	
	private Cursor cursor = null;
	private int timestampIndex = -1;

	public Cursor getCursor(){
		return cursor;
	}
	public int getTimestampIndex(){
		return this.timestampIndex;
	}
	public long getTimestamp(){
		if (this.cursor != null && !this.cursor.isAfterLast())
			return this.cursor.getLong(timestampIndex);
		else
			return -1;
	}

	public void setCursor(Cursor cursor){
		this.cursor = cursor;
		this.timestampIndex = cursor.getColumnIndexOrThrow(TablesOld.TIMESTAMP_COLUMN_NAME);	//all tables have a timestamp column
	}
}
