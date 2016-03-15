package com.cortxt.app.MMC.UtilsOld;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.text.format.Time;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.com.mmcextension.utils.TimeDataPoint;
import com.cortxt.com.mmcextension.utils.TimeSeries;
import com.cortxt.app.MMC.ContentProviderOld.ProviderOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesEnumOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.LocalStorageReporter.LocalStorageReporter;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MMCLogger;

public class LiveBuffer {

	/**
	 * Currently, the chart can only hold one time series. This one is for the 
	 * signal strength.
	 * There should be public access to this variable because the owner should be able
	 * to manually tinker with the chart data.
	 */
	public TimeSeries<Float> signalTimeSeries;
	/**
	 * This series holds the various event types  that need to be shown using
	 * event markers on the chart.
	 * There should be public access to this variable because the owner should be able
	 * to manually tinker with the chart marker data.
	 */
	public TimeSeries<EventType> eventTimeSeries;
	public TimeSeries<Integer> cellTimeSeries;
	public TimeSeries<Double> locationTimeSeries;
	/**
	 * This is the minimum value that the signalTrendChart can allow.
	 */
	public static final float SIGNAL_TREND_CHART_MIN_VALUE = -120.0f;
	/**
	 * This is the maximum value that the signalTrendChart can allow.
	 */
	public static final float SIGNAL_TREND_CHART_MAX_VALUE = -40.0f;
	
	Time time = new Time(Time.getCurrentTimezone());		//This is a general time variable that will be constantly "setToNow"
	//and then copied to a long variable

	private Activity parent;
	private View view;
	
	
	/*
	 * End helper methods
	 * ===============================================================
	 * Start public methods
	 */
	public LiveBuffer (Activity _activity, View _view)
	{
		setParent(_activity);
		view = _view;
		//signalTimeSeries = new TimeSeries<Float>(_activity.getString(R.string.Chart_SignalStrength), SIGNAL_TREND_CHART_MIN_VALUE, SIGNAL_TREND_CHART_MAX_VALUE);
		//eventTimeSeries = new TimeSeries<EventType>(_activity.getString(R.string.Chart_Events), null, null);	//event time series doesn't have a minimum and a maximum value
		
	}
	/**
	 * This is another public method that can be used by the container view (containing
	 * the Chart view) to add data points to the signal strength trend chart. The timestamp
	 * is encapsulated into the TimeDataPoint parameter.
	 * @param data
	 */
	public void addDataPoint(TimeDataPoint<Float> datapoint){
		this.signalTimeSeries.addDataPoint(datapoint);
		view.invalidate();
	}

	/**
	 * This is the public method that can be used by the container view (containing
	 * the Chart view) to add data points to the signal strength trend chart. The 
	 * timestamp for the datapoint is automatically set to the current time.
	 */
	public void addDataPoint(float data){
		time.setToNow();
		this.signalTimeSeries.addDataPoint(new TimeDataPoint<Float>(data, 0f, time.toMillis(true)));
		view.invalidate();
	}
	
	public void addLocationPoint(Location loc){
		if (this.locationTimeSeries == null || loc == null || loc.getAccuracy() > 40) return;
		this.locationTimeSeries.addDataPoint(new TimeDataPoint<Double>(loc.getLatitude(), loc.getLongitude(), time.toMillis(true)));
		view.invalidate();
	}
	
	/**
	 * This is the public method that can be used by the container view (containing
	 * the Chart view) to add event markers to the chart. The timestamp for the datapoint
	 * is automatically set to the current time.
	 * @param event
	 */
	public void addEvent(EventType event){
		time.setToNow();
		this.eventTimeSeries.addDataPoint(new TimeDataPoint<EventType>(event, (EventType)null, time.toMillis(true)));
		view.invalidate();
	}
	
	public void addCellID(int bsHigh, int bsMid, int bsLow){
		time.setToNow();
		this.cellTimeSeries.addDataPoint(new TimeDataPoint<Integer>(bsHigh, bsMid, bsLow& 0xFFFF, time.toMillis(true)));
		view.invalidate();
	}
	public void addCellID(int bsHigh, int bsLow){
		time.setToNow();
		this.cellTimeSeries.addDataPoint(new TimeDataPoint<Integer>(bsHigh, bsLow& 0xFFFF, time.toMillis(true)));
		view.invalidate();
	}

	/**
	 * This is the public method that can be used by the container view (containing
	 * the Chart view) to add event markers to the chart. The timestamp for the datapoint
	 * is encapsulated within the TimeDataPOint parameter.
	 * @param event
	 */
	public void addEvent (TimeDataPoint<EventType> event){
		this.eventTimeSeries.addDataPoint(event);
		view.invalidate();
	}
	
	/*
	 * table for changes and updates both the percentometer and the chart.
	 */
	public void updateActivityFromDB(long timespan){
		boolean bLocations = true;
        ReportManager reportManager = ReportManager.getInstance(parent.getApplicationContext());
		MMCDevice device = reportManager.getDevice();
		String phoneType = device.getPhoneType();
		ProviderOld dbProvider = reportManager.getDBProvider();
		Cursor signalStrengthCursor = null;
		Cursor eventCursor = null;
		try
		{
			signalStrengthCursor = dbProvider.query(
					UriMatchOld.SIGNAL_STRENGTHS.getContentUri(),
					new String[]{ TablesOld.TIMESTAMP_COLUMN_NAME, TablesOld.SignalStrengths.SIGNAL, TablesOld.SignalStrengths.LTE_RSRP },
					TablesOld.TIMESTAMP_COLUMN_NAME + ">?",
					new String[]{ Long.toString(System.currentTimeMillis() - timespan) },
					TablesOld.TIMESTAMP_COLUMN_NAME + " DESC"
				);
			
			signalStrengthCursor.moveToFirst();
			
			eventCursor =  ReportManager.getInstance(parent.getApplicationContext()).getRecentEvents(timespan);
//			eventCursor = getParent().managedQuery(
//				UriMatchOld.EVENTS.getContentUri(),
//				new String[]{ TablesOld.TIMESTAMP_COLUMN_NAME, TablesOld.Events.EVENT_TYPE },
//				TablesOld.TIMESTAMP_COLUMN_NAME + ">?",
//				new String[]{ Long.toString(System.currentTimeMillis() - timespan) },
//				TablesOld.TIMESTAMP_COLUMN_NAME + " DESC"
//			);
			eventCursor.moveToFirst();
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, "LiveBuffer", "updateActivityFromDB", "error with query", e);
		}
		Cursor cellCursor = null;
		try
		{
			Uri baseStationTable = 	(UriMatchOld.BASE_STATIONS.getContentUri()
					
					/*phoneType.equals("cdma")
					? UriMatchOld.BASE_STATIONS_CDMA.getContentUri()
					: UriMatchOld.BASE_STATIONS_GSM.getContentUri()*/
				);
			//Uri limitBSUri = baseStationTable.buildUpon().appendQueryParameter("limit", "1").build();
			
			//Cursor cell_cursor = managedQuery(
			 cellCursor = dbProvider.query(
					baseStationTable,
				null,
				TablesOld.TIMESTAMP_COLUMN_NAME + ">?",
				new String[]{ Long.toString(System.currentTimeMillis() - timespan) },
				TablesOld.TIMESTAMP_COLUMN_NAME + " DESC"
			);
			
			cellCursor.moveToFirst();
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, "LiveBuffer", "updateActivityFromDB", "error with basestations query", e);
		}
		
		Cursor locCursor = null;
		if (bLocations)
		{
			try
			{
				locCursor = dbProvider.query(
						UriMatchOld.LOCATIONS.getContentUri(),
						new String[]{ TablesOld.TIMESTAMP_COLUMN_NAME, TablesOld.Locations.LATITUDE, TablesOld.Locations.LONGITUDE },
						TablesOld.TIMESTAMP_COLUMN_NAME + ">? And " + TablesOld.Locations.ACCURACY + "<50",
						new String[]{ Long.toString(System.currentTimeMillis() - timespan) },
						TablesOld.TIMESTAMP_COLUMN_NAME + " DESC"
					);
				locCursor.moveToFirst();
			}
			catch (Exception e)
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "LiveBuffer", "updateActivityFromDB", "error with locations query", e);
			}
		}
		//use the cursor to update the percentometer
		//updatePercentometerFromCursor(signalStrengthCursor);
		
		//use the cursors to update the chart
		updateChartFromCursors(signalStrengthCursor, eventCursor, cellCursor, locCursor, phoneType);
	}
	/**
	 * This method updates the signal strength trend chart using the two cursors provided.
	 * 
	 * @param signalStrengthCursor A cursor that resulted from a query on either the {@link TablesEnumOld#SIGNAL_STRENGTHS_CDMA} 
	 * or the {@link TablesEnumOld#SIGNAL_STRENGTHS_GSM} table.
	 * It is assumed that this cursor includes either the {@value TablesOld.SignalStrengths.CDMAVersion#RSSI} 
	 * or the {@value TablesOld.SignalStrengths.GSMVersion#SIGNAL_STRENGTH} column along with the 
	 * {@value TablesOld#TIMESTAMP_COLUMN_NAME} column.
	 * @param eventCursor A cursor that resulted from a query on the {@link TablesEnumOld#EVENTS} table. It is assumed that this
	 * cursor includes both {@link TablesOld#TIMESTAMP_COLUMN_NAME} and {@link TablesOld.Events#EVENT_TYPE} columns.
	 */
	private void updateChartFromCursors(Cursor signalStrengthCursor, Cursor eventCursor, Cursor cellCursor, Cursor locCursor, String phoneType){
		
		//create the time series datastructures
		signalTimeSeries = new TimeSeries<Float>(
			getParent().getString(R.string.Chart_SignalStrength), 
			SIGNAL_TREND_CHART_MIN_VALUE,
			SIGNAL_TREND_CHART_MAX_VALUE
		);
		eventTimeSeries = new TimeSeries<EventType>(getParent().getString(R.string.Chart_Events), null, null);
		
		//create the time series datastructures
		cellTimeSeries = new TimeSeries<Integer>(
			getParent().getString(R.string.Chart_CellIDs), 
			-1, 
			(1<<32)
		);
		
		if (locCursor != null)
			locationTimeSeries = new TimeSeries<Double>(
					getParent().getString(R.string.Chart_Locations), null, null);
		//get the indexes
		int locLatIndex = 0, locLngIndex = 0, locTimestampIndex = 0;
		
		if (cellCursor != null)
		{
			int cellTimestampIndex = cellCursor.getColumnIndex(TablesOld.TIMESTAMP_COLUMN_NAME);
			int cellHighIndex = 0;
			int cellMidIndex = 0;
			int cellLowIndex = 0;
			
			cellLowIndex = cellCursor.getColumnIndex(TablesOld.BaseStations.BS_LOW);
			cellMidIndex = cellCursor.getColumnIndex(TablesOld.BaseStations.BS_MID);
			cellHighIndex = cellCursor.getColumnIndex(TablesOld.BaseStations.BS_HIGH);
			
//			if (phoneType.equals("cdma")){
//				//set the cellId using the bid
//				cellLowIndex = cellCursor.getColumnIndex(TablesOld.BaseStations.CDMAVersion.BID);
//				cellHighIndex = cellCursor.getColumnIndex(TablesOld.BaseStations.CDMAVersion.SID);
//				
//			} else {
//				//set the cellId using
//				cellLowIndex= cellCursor.getColumnIndex(TablesOld.BaseStations.GSMVersion.CELL_ID);
//				cellHighIndex= cellCursor.getColumnIndex(TablesOld.BaseStations.GSMVersion.LAC);
//			}
			
			//update the cell handoffs on the chart
			while (!cellCursor.isAfterLast()){
				cellTimeSeries.addDataPoint(
					new TimeDataPoint<Integer>(
							cellCursor.getInt(cellHighIndex),cellCursor.getInt(cellMidIndex), cellCursor.getInt(cellLowIndex) & 0xFFFF,
						cellCursor.getLong(cellTimestampIndex)
					)
				);
				
				cellCursor.moveToNext();
			}
			cellCursor.close();
			cellCursor = null;
		}
		
		if (signalStrengthCursor != null)
		{
			int signalIndex = signalStrengthCursor.getColumnIndex(TablesOld.SignalStrengths.SIGNAL);
			int ltersrpIndex = signalStrengthCursor.getColumnIndex(TablesOld.SignalStrengths.LTE_RSRP);
			int signalStrengthTimestampIndex = signalStrengthCursor.getColumnIndex(TablesOld.TIMESTAMP_COLUMN_NAME);
			
			//update the signal strengths on the chart
			while (!signalStrengthCursor.isAfterLast()){
				/*
				 * unknown signal strengths are stored as null, and signalStrengthCursor.getFloat(signalIndex)
				 * returns 0.0 when the stored value is null. We wont draw the unknown signal
				 */
				float signalStrength = signalStrengthCursor.getFloat(signalIndex);
				float ltersrpStrength = signalStrengthCursor.getFloat(ltersrpIndex);
				//if (signalStrength != 0f || ltersrpStrength != 0f)  
				{
					//signalStrength = (signalStrength != 0.0f) ? signalStrength : Chart.SIGNAL_TREND_CHART_MIN_VALUE;
					if (signalStrength < SIGNAL_TREND_CHART_MIN_VALUE)
						signalStrength = SIGNAL_TREND_CHART_MIN_VALUE;
					if (signalStrength > -40 && signalStrength != 0) signalStrength = SIGNAL_TREND_CHART_MIN_VALUE;
					
					signalTimeSeries.addDataPoint(new TimeDataPoint<Float>(
						signalStrength,
						ltersrpStrength,
						signalStrengthCursor.getLong(signalStrengthTimestampIndex)
					));
				}
				signalStrengthCursor.moveToNext();
			}
			signalStrengthCursor.close();
			signalStrengthCursor = null;
		}
		if (eventCursor != null)
		{
			int eventTypeIndex = eventCursor.getColumnIndex(LocalStorageReporter.Events.KEY_TYPE);
			int eventTimestampIndex = eventCursor.getColumnIndex(LocalStorageReporter.KEY_TIMESTAMP);
			
			//update the events on the chart
			while (!eventCursor.isAfterLast()){
				eventTimeSeries.addDataPoint(
					new TimeDataPoint<EventType>(
						EventType.get(eventCursor.getInt(eventTypeIndex)), (EventType)null,
						eventCursor.getLong(eventTimestampIndex)
					)
				);
				
				eventCursor.moveToNext();
			}
			eventCursor.close();
			eventCursor = null;
		}
		
		if (locCursor != null)
		{
			locLatIndex = locCursor.getColumnIndex(TablesOld.Locations.LATITUDE);
			locLngIndex = locCursor.getColumnIndex(TablesOld.Locations.LONGITUDE);
			locTimestampIndex = locCursor.getColumnIndex(TablesOld.TIMESTAMP_COLUMN_NAME);
			//update the cell handoffs on the chart
			while (!locCursor.isAfterLast()){
				locationTimeSeries.addDataPoint(
					new TimeDataPoint<Double>(
							locCursor.getDouble(locLatIndex),locCursor.getDouble(locLngIndex),
						locCursor.getLong(locTimestampIndex)
					)
				);
				
				locCursor.moveToNext();
			}
			locCursor.close();
			locCursor = null;
		}
		
	}
	public Activity getParent() {
		return parent;
	}
	public void setParent(Activity parent) {
		this.parent = parent;
	}
	
}
