package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.MyCoverage.CoverageOverlay;
import com.cortxt.app.MMC.Activities.MyCoverage.MMCMapView;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventsOverlay;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class TransitSamplingOverlay extends ItemizedOverlay<OverlayItem> implements MMCMapView.OnZoomLevelChangeListener, MMCMapView.OnChangeListener {

	private Context context;
	private MapView mMapView;
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private List<GeoPoint> polygon = new ArrayList<GeoPoint>();
	private List<GeoPoint> stations = new ArrayList<GeoPoint>();
	private List<GeoPoint> intersects = new ArrayList<GeoPoint>();
	private List<GeoPoint> gpsLocations = new ArrayList<GeoPoint>();
	private CoverageOverlay mCoverageOverlay = null;
	private ProgressBar mBusyIndicator;
	Paint smppaint = new Paint();	
	
	public TransitSamplingOverlay(Context context, MapView mapView, ProgressBar mBusyIndicator, CoverageOverlay mCoverageOverlay) {
		super(context.getResources().getDrawable(R.drawable.crosshair_mapping));
		
		mMapView = (MapView) mapView;			
		this.context = context;
		if (!mMapView.getOverlays().contains(this))
			mMapView.getOverlays().add(this);
		this.mCoverageOverlay = mCoverageOverlay;
		this.mBusyIndicator = mBusyIndicator;
		initDrawing ();
		populate();
	}
	
	@Override
	public void onZoomLevelChange() {
		if(mCoverageOverlay != null)
			mCoverageOverlay.onZoomLevelChange();;
	}
	@Override
	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
		if(shadow)
			return;
		Projection projection = mMapView.getProjection();
		
		if(polygon.size() > 0)
			drawPolyline(canvas, projection);
		if(stations.size() > 0)
			drawStations(canvas, projection);
		//if(intersects.size() > 0)
		//	drawIntersections(canvas, projection);
		if(mOverlays.size() > 0)
			drawSample(canvas, projection);
		if(gpsLocations.size() > 0)
			drawGpsLocationss(canvas, projection);
		
	}
	
	private void initDrawing ()
	{
		smppaint.setAntiAlias(true);
		smppaint.setStyle(Paint.Style.FILL);
		smppaint.setStrokeJoin(Paint.Join.ROUND);
		smppaint.setStrokeCap(Paint.Cap.ROUND);
	}
	public void drawSample(Canvas canvas, Projection projection) {
		
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
//		paint.setAlpha(0);
		
		int size = mOverlays.size();
		for(int index = size - 1; index >= 0; index--)  { 
			OverlayItem overlayItem = mOverlays.get(index); 
			String title = overlayItem.getTitle();
			if(title.equals("Sample")) {
				int color = Integer.valueOf(overlayItem.getSnippet());	
				smppaint.setColor(color);
				
				Point point = new Point();
				GeoPoint geo = mOverlays.get(index).getPoint();
				projection.toPixels(geo ,point);
			 		
				double lat = geo.getLatitudeE6()/1000000.0;
//				int meters = 15;
				
				int radius = (int) (10 * screenDensityScale);
//				int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
				canvas.drawCircle((float)point.x, (float)point.y, radius, smppaint);
			 				
//				Point point = projection.toPixels(overlayItem.getPoint(), null);
//				//Snippet is used to track the drawable id
//				int id = Integer.valueOf(overlayItem.getSnippet());	
//				Drawable drawable = context.getResources().getDrawable(id);
//				int width = drawable.getIntrinsicWidth();
//				int height = drawable.getIntrinsicHeight();
//				drawable.setBounds(-width/2, -height/2, width/2, height/2);
////				overlayItem.setMarkerHotspot(HotspotPlace.CENTER);
//				overlayItem.setMarker(drawable);		
//				
//				BitmapFactory.Options opts = new BitmapFactory.Options();
//				opts.inDither = false;                    
//				opts.inPurgeable = true;                 
//				opts.inInputShareable = true;             
//				opts.inTempStorage = new byte[32 * 1024];		
//				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id, opts); 
//				canvas.drawBitmap(bitmap, point.x - bitmap.getWidth()/2, point.y - bitmap.getHeight()/2, null);
			}
		} 
	}	
	
	public void drawPolyline(Canvas canvas, Projection projection) {
		List<GeoPoint> geoPoints = getPolygon();	
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
		if (geoPoints.size() < 1) 
	        return;
	    
	    Paint polyPaint = new Paint();
	    polyPaint.setColor(Color.rgb(51, 153, 204));
	    polyPaint.setAntiAlias(true);
	    polyPaint.setStyle(Paint.Style.STROKE);
	    polyPaint.setStrokeJoin(Paint.Join.ROUND);
	    polyPaint.setStrokeCap(Paint.Cap.ROUND);
	    polyPaint.setStrokeWidth(4 * screenDensityScale);	
	    
	    
	    Path polyPath = new Path();
//	    polyPath.onDrawCycleStart(canvas);
	    polyPath.setFillType(Path.FillType.EVEN_ODD);
	    Point point = projection.toPixels(geoPoints.get(0), null);
	    
	    polyPath.moveTo((float) point.x, (float) point.y);
	    int i, len;
	    len = geoPoints.size();
	    for (i = 0; i < len; i++) {
	    	point = projection.toPixels(geoPoints.get(i), null);
	    	polyPath.lineTo((float) point.x, (float) point.y);
	    	canvas.drawPath(polyPath, polyPaint);
	    	polyPath.moveTo((float) point.x, (float) point.y);
	    }
	    polyPath.lineTo((float) point.x, (float) point.y);
	    polyPath.close();
	    canvas.drawPath(polyPath, polyPaint); 
	}
	
	public void drawStations(Canvas canvas, Projection projection) {
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
		Paint paint = new Paint();
		paint.setColor(Color.CYAN);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(10 * screenDensityScale);	    
	    
	 	for (int i = 0; i < stations.size(); i++) {
//	 		Point point = new Point();
//	 		GeoPoint geo = stations.get(i);
//	 		projection.toPixels(geo ,point);
////	 		Point point = projection.toPixels(stations.get(i), null);
//		
////	 		int lat = stations.get(i).getLatitudeE6();
//	 		double lat = stations.get(i).getLatitudeE6()/1000000.0;
//			int meters = 1;
//			int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
//			canvas.drawCircle((float)point.x, (float)point.y, radius, paint);
			
			
			Point point = projection.toPixels(stations.get(i), null);

//			int id = R.drawable.train_station_icon_ts;
			int id = R.drawable.station_icon_ts;
			Drawable drawable = context.getResources().getDrawable(id);
			int width = drawable.getIntrinsicWidth();
			int height = drawable.getIntrinsicHeight();
			drawable.setBounds(-width/2, -height/2, width/2, height/2);	
			
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inDither = false;                    
			opts.inPurgeable = true;                 
			opts.inInputShareable = true;             
			opts.inTempStorage = new byte[32 * 1024];		
			Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id, opts); 
			canvas.drawBitmap(bitmap, point.x - bitmap.getWidth()/2, point.y - bitmap.getHeight(), null);
	 	}
	}	
	
	public void drawGpsLocationss(Canvas canvas, Projection projection) {
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(10 * screenDensityScale);	    
	    
	 	for (int i = 0; i < gpsLocations.size(); i++) {
	 		
	 		Point point = new Point();
	 		GeoPoint geo = gpsLocations.get(i);
	 		projection.toPixels(geo ,point);

	 		double lat = gpsLocations.get(i).getLatitudeE6()/1000000.0;
			int meters = 1;
			int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
			canvas.drawCircle((float)point.x, (float)point.y, radius, paint);
	 	}
	}	
	
	public void drawIntersections(Canvas canvas, Projection projection) {
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
		Paint paint = new Paint();
		paint.setColor(Color.rgb(115, 14, 174));
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(10 * screenDensityScale);	    
	    
	 	for (int i = 0; i < intersects.size(); i++) {
	 		
	 		Point point = new Point();
	 		GeoPoint geo = intersects.get(i);
	 		projection.toPixels(geo ,point);
//	 		Point point = projection.toPixels(intersects.get(i), null);
		
//	 		int lat = stations.get(i).getLatitudeE6();
	 		double lat = intersects.get(i).getLatitudeE6()/1000000.0;
			int meters = 1;
			int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
			canvas.drawCircle((float)point.x, (float)point.y, radius, paint);
	 	}
	}	
	
	public OverlayItem addSample(GeoPoint geoPoint, Integer signalStrength) {	
		
		if(signalStrength == null) {
			Toast.makeText(context, "No signal strength", Toast.LENGTH_SHORT).show();
			return null;
		}	
		int icon = signalStrengthIcon(signalStrength);
		OverlayItem overlay = new OverlayItem(geoPoint, "Sample", String.valueOf(icon));
		mOverlays.add(overlay);
		populate();
		mMapView.invalidate();
		return overlay;
	}
	
	public OverlayItem moveSample(OverlayItem sample, GeoPoint geoPoint, Integer signalStrength) {	
		mOverlays.remove(sample);
		int icon = signalStrengthIcon(signalStrength);
		OverlayItem overlay = new OverlayItem(geoPoint, "Sample", String.valueOf(icon));
		mOverlays.add(overlay);
		//populate();
		//mMapView.invalidate();
		return overlay;
	}
	
	public void refreshSamples ()
	{
		populate();
		mMapView.invalidate();
	}
	
	public void makeSamplesInvalid() {	
		int icon = R.drawable.grey_cross_marker;
		
		for(int i = 0; i < mOverlays.size(); i++) {
			OverlayItem overlay = mOverlays.get(i);
			overlay = new OverlayItem(overlay.getPoint(), "Sample", String.valueOf(icon));
			mOverlays.remove(i);
			mOverlays.add(overlay);
		}
		populate();
		mMapView.invalidate();
	}
	
	public int signalStrengthIcon(Integer signalStrength) {		
		
//		if(signalStrength <= -121 || signalStrength == 0)
//			return R.drawable.grey_cross_marker;
//		
//		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//		
//		int percent = -1;
//		if(telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE) { //2g
//			percent = Math.abs(getPercentageFromDbm(signalStrength, -50));
//		}
//		else
//			percent = Math.abs(getPercentageFromDbm(signalStrength, -70));
//		
//		if(percent >= 1 && percent <= 19) 
//			return R.drawable.red_cross_marker;
//		if(percent >= 20 && percent <= 39) 
//			return R.drawable.orange_cross_marker;
//		if(percent >= 40 && percent <= 59) 
//			return R.drawable.yellow_cross_marker;
//		if(percent >= 60 && percent <= 79) 
//			return R.drawable.green_cross_marker;		
//		if(percent >= 80) 
//			return R.drawable.darkgreen_cross_marker;
//					
//		return R.drawable.grey_cross_marker;	
		
//		d16 = 16 is a bright color
//		12 for a darker shade + outline 
		return EventsOverlay.signalColor(signalStrength, 16);
	}
	
	public static int getPercentageFromDbm(int dbm, int MAX_SIG){
//		int MAX_SIG = -70;
		int MIN_SIG = -120;
		
		if(dbm == -1){
			//if dbm is -1, then the categorization in the processNewMMCSignal method of network types is incorrect
			return 0;
		}
		
		return 100 * (dbm - MIN_SIG) / (MAX_SIG - MIN_SIG);
	}

	@Override
	public boolean onSnapToItem(int arg0, int arg1, Point arg2, MapView arg3) {
		return false;
	}

	@Override
	protected OverlayItem createItem(int arg0) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	public void deleteOverlays() {
		if(mOverlays.size() > 0)
			mOverlays.removeAll(mOverlays);
	}
	
	public void setPolygon(List<GeoPoint> newPolygon) {
		polygon = newPolygon;
	}
	
	public void addStation(GeoPoint station) {
		stations.add(station);
	}
	
	public void addIntersect(GeoPoint intersect) {
		intersects.add(intersect);
	}
	
	public void addGpsLocation(GeoPoint gpsLocation) {
		gpsLocations.add(gpsLocation);
	}
	
	public GeoPoint getPolygonPoint(int index) {
		return polygon.get(index);
	}
	
	public List<GeoPoint> getPolygon() {
		return polygon;
	}

	@Override
	public void onChange() {
		if(mCoverageOverlay != null)
			mCoverageOverlay.onChange();
	}
}
