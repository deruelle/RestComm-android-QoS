package com.cortxt.app.MMC.Sampling.Building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.MMCMapActivity;
import com.cortxt.app.MMC.Activities.MyCoverage.CoverageOverlay;
import com.cortxt.app.MMC.Activities.MyCoverage.MMCMapView;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventsOverlay;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.Sampling.Transit.TransitSamplingMapMath;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.com.mmcextension.datamonitor.database.DatabaseHandler;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

//Need ItemizedOverlay<OverlayItem> for the drawSafe() -- OSM only
public class MappingOverlay extends ItemizedOverlay<OverlayItem> implements MMCMapView.OnZoomLevelChangeListener, MMCMapView.OnChangeListener {
	
	private static final int ZOOM_LEVEL_THRESHOLD = 10;
	private static final long RELOAD_DELAY = 500;
	private Context context;
	private List<GeoPoint> polygonPoints = new ArrayList<GeoPoint>();
	private Map<Long,List<GeoPoint>> buildingPolygons = new HashMap<Long,List<GeoPoint>>();
	private MMCMapView mMapView;
	//Stores all the samples the user 'created'
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	//Used for the marker(crosshair) drag
//	private ArrayList<OverlayItem> tempOverlays = new ArrayList<OverlayItem>();
	private double maxLat = 0;
	private double minLong = 0;
//	private int xDragImageOffset = 0;
// 	  private int yDragImageOffset = 0;
//    private int xDragTouchOffset = 0;
//    private int yDragTouchOffset = 0;
//    private Drawable crosshair;
//    private Drawable crosshairDrag;
    private ImageView crosshairImage;
//    private OverlayItem dragging = null;
    private boolean delete = false;
    private AsyncTask<Void, Void, String> polygonAsyncTask = null;
    private AsyncTask<Void, Void, String> buildingsAsyncTask = null;
    private Projection projection = null;
    private ProgressBar mBusyIndicator;
    private int tapInterval = 0;
    private GeoPoint tapGeoPoint = null;
    private ManualMapping manualMapping = null;
    GeoPoint previousSample = null;
    private GeoPoint mapCenterOSMFix = null;
    List<List<GeoPoint>> buildings = new ArrayList<List<GeoPoint>>();
    public long osm_id = 0;
    private CoverageOverlay mCoverageOverlay = null;
    //public Map<Integer, Integer> signalArray = new TreeMap<Integer, Integer>();
    //public Map<Integer, Integer> lastSignalArray;
    public SparseIntArray signalArray = new SparseIntArray(), lastSignalArray;
    
    // To handle panning (sort of tiling) to load in new buildings
    private Handler mHandler = new Handler();
    private BuildingsTimerTask buildingsTimerTask;
    private Timer buildingsTimer = new Timer();
	public long lastLayout = 0;
	protected boolean mMapChanged = false;
    private GeoPoint lastKnownMapCenter;
    private int zoomLevelChange;
    private int previousZoomLevel;
	private Rect haveCoverageForArea;
	private int lastKnownLatitudeSpanE6, topopsLat;
	private int lastKnownLongitudeSpanE6, topopsLng;
    
	public MappingOverlay(Context context, MapView mapView, ImageView imageView, ProgressBar mBusyIndicator, CoverageOverlay mCoverageOverlay) {
		super(context.getResources().getDrawable(R.drawable.crosshair_mapping));
		
		mMapView = (MMCMapView) mapView;			
		this.context = context;
		this.crosshairImage = imageView;		
//		this.crosshairDrag = context.getResources().getDrawable(R.drawable.crosshair_mapping_dragging);
//		this.crosshair = context.getResources().getDrawable(R.drawable.crosshair_mapping);
//		setImageBounds();
//		int width = drawable.getIntrinsicWidth();
//		int height = drawable.getIntrinsicHeight();
	    addOverlays();
	    projection = mMapView.getProjection();
	    this.mBusyIndicator = mBusyIndicator;
	    this.mCoverageOverlay = mCoverageOverlay;
	    haveCoverageForArea = new Rect();
	    mMapChanged = false;
	    getManualPolygons();
	    
	    populate();
	    
	    try {
			manualMapping = (ManualMapping) context;
		} catch(Exception e) {
			e.printStackTrace();
		}
	    
	    initDrawing ();
	}
		
//	public void setImageBounds() {
//		int width = crosshair.getIntrinsicWidth();
//		int height = crosshair.getIntrinsicHeight();
//		this.crosshair.setBounds(-width/2, -height/2, width/2, height/2);
//		this.crosshairDrag.setBounds(-width/2, -height/2, width/2, height/2);
//		this.xDragImageOffset = crosshairImage.getDrawable().getIntrinsicWidth();
//	    this.yDragImageOffset = crosshairImage.getDrawable().getIntrinsicHeight();
//		this.xDragImageOffset = crosshairImage.getDrawable().getIntrinsicWidth()/2 -4;
//	    this.yDragImageOffset = crosshairImage.getDrawable().getIntrinsicHeight()/8 -4;
//	}
	
	public static Drawable boundCenter(Drawable d) {
		d.setBounds(d.getIntrinsicWidth() /- 2, d.getIntrinsicHeight() / -2,
			d.getIntrinsicWidth() / 2, d.getIntrinsicHeight() / 2);
		return d;
	}
	
	float screenDensityScale;
	Paint paintWhiteLine, smppaint, shadowpaint;
	private void initDrawing ()
	{
		screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		paintWhiteLine = new Paint();
		paintWhiteLine.setColor(Color.WHITE);
		paintWhiteLine.setAntiAlias(true);
		paintWhiteLine.setStyle(Paint.Style.STROKE);
		paintWhiteLine.setStrokeJoin(Paint.Join.ROUND);
		paintWhiteLine.setStrokeCap(Paint.Cap.ROUND);
		paintWhiteLine.setStrokeWidth(1 * screenDensityScale);
		
		smppaint = new Paint();
		smppaint.setAntiAlias(true);
		smppaint.setStyle(Paint.Style.STROKE);
		smppaint.setStrokeJoin(Paint.Join.ROUND);
		smppaint.setStrokeCap(Paint.Cap.ROUND);
		smppaint.setStrokeWidth(1 * screenDensityScale);
		
		shadowpaint = new Paint();
		shadowpaint.setAntiAlias(true);
		shadowpaint.setColor(Color.GRAY);
		shadowpaint.setStyle(Paint.Style.STROKE);
		shadowpaint.setStrokeJoin(Paint.Join.ROUND);
		shadowpaint.setStrokeCap(Paint.Cap.ROUND);
		shadowpaint.setStrokeWidth(1.5f * screenDensityScale);
		shadowpaint.setAlpha(96);
		
	}
	public void setPolygonPoints(List<GeoPoint> newpolygonPoints){
		polygonPoints = newpolygonPoints;
	}
	
	public void addPolygonPoint(GeoPoint geoPoint) {
		if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE)
			previousSample = geoPoint;
		polygonPoints.add(geoPoint);
	}
	
	public List<GeoPoint> getPolygonPoints(){
		return polygonPoints;
	}
	
	public String getPolygonString ()
	{
		String poly = "", pnt = "";
		for (int i=0; i<polygonPoints.size(); i++)
		{
			GeoPoint gpoint = polygonPoints.get(i);
			poly += "[" + (float)gpoint.getLongitudeE6()/1000000 + "," + (float)gpoint.getLatitudeE6()/1000000 + "]";
			if (i<polygonPoints.size()-1)
				poly += ",";
		}
		return poly;
	}
	
	public GeoPoint getPolygonPoint(int index) {
		if(polygonPoints.size() > 0) {
			if(index <= polygonPoints.size()-1) {
				return polygonPoints.get(index);
			}
			else 
				return null;
		}
		else 
			return null;
	}
	
	public String polygonToString() {
		String points =  "";
		if(polygonPoints.size() == 0 || polygonPoints == null)
			return null;
		
		for(int i = 0; i < polygonPoints.size(); i++) {
			GeoPoint geoPoint = polygonPoints.get(i);
			points += String.valueOf(geoPoint.getLatitudeE6() + "," + geoPoint.getLongitudeE6());
			if(i < polygonPoints.size() -1) {
				points += ",";
			}
		}
		return points;
	}
	
	public List<String> readPolygons() {
		List<String> points = new ArrayList<String>();
		SQLiteDatabase sqlDB = null;
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getReadableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_MANUAL_POLYGON, null, null, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
	                    String point = cursor.getString(cursor.getColumnIndex("points"));
	                    points.add(point);
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(sqlDB != null)
				sqlDB.close(); 
		}
		return points;
	}
	
	public void getManualPolygons() {
		List<String> points = readPolygons();
	
		if(points == null)
			return;
		
		for(int i = 0; i < points.size(); i++) {
			//Building is string of lats and longs
			String stringOfLatLongs = points.get(i);
			//Lat longs individual
			String[] singleLatLongs = stringOfLatLongs.split(",");
			List<GeoPoint> building = new ArrayList<GeoPoint>();
			for(int k = 0; k < singleLatLongs.length; k = k + 2) {
				//Pair up lat/longs into GeoPoints
				GeoPoint geoPoint = new GeoPoint(Integer.valueOf(singleLatLongs[k]), Integer.valueOf(singleLatLongs[k+1]));
				building.add(geoPoint);
			}
			//All the geopoints from the original string make a building
			buildings.add(building);
		}
	}
	
	/* Use if the user creates a new manual polygon */
	public void addBuilding(List<GeoPoint> building) {
		buildings.add(building);
	}
	
	public void removeBuilding(List<GeoPoint> building) {
		buildings.remove(building);
	}
	
	public void setTapAnimation(GeoPoint newGeoPoint, int newInterval) {
		tapInterval = newInterval;
		tapGeoPoint = newGeoPoint;
		mMapView.invalidate();
	}
	
	public void removeTapAnimation() {
		tapInterval = 0;
		tapGeoPoint = null;
		mMapView.invalidate();
	}
	
//	public void addmOverlays(OverlayItem overlay) {	
//		
//		if((validatePoint(overlay.getPoint()) == false && getPolygonPoints() != null) 
//				&& mMapView.getMappingType() == MmcConstants.INDOOR_SAMPLING) { 		
//			return;
//		}
//		
//		OverlayItem newOverlay = null;
//		if(overlay.getTitle().equals("Anchor")) {
//			int drawableId = R.drawable.anchor_marker;
//			//Create new overlay so we can store the drawable id in the "snippet", this is used in drawSafeSample
//			newOverlay = new OverlayItem("Anchor", String.valueOf(drawableId), overlay.getPoint());	
//		}
//		if(overlay.getTitle().equals("Sample")) {
//			int icon = signalStrengthIcon();
//			if(icon > -1) {
//				newOverlay = new OverlayItem("Sample", String.valueOf(icon), overlay.getPoint());	
//			}
//		}	
//		mOverlays.add(newOverlay);
//		populate();
//		mMapView.invalidate();
//	}
	
	public void addmOverlays(GeoPoint geoPoint, String title, int signal) {	
		
		if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING && polygonPoints.size() > 0) { 		
			if(!validatePoint(geoPoint, polygonPoints))
				return; //outside polygon
//		if((validatePoint(geoPoint) == false && getPolygonPoints() != null) 
//				&& mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING) { 		
//			return;
		}
		
		OverlayItem newOverlay = null;
		if(title.equals("Anchor")) {
			int drawableId = R.drawable.anchor_marker;
			//Create new overlay so we can store the drawable id in the "snippet", this is used in drawSafeSample
			newOverlay = new OverlayItem(geoPoint,  "Anchor", String.valueOf(drawableId));	
		}
		if(title.equals("Sample")) {
			int icon = signalStrengthIcon(signal);
			if(icon == -1) {
				Toast.makeText(context, "No signal strength", Toast.LENGTH_SHORT).show();
				return;
			}
			newOverlay = new OverlayItem(geoPoint, "Sample", String.valueOf(icon));	
		}
		
		previousSample = newOverlay.getPoint();

		mOverlays.add(newOverlay);
		populate();
		mMapView.invalidate();
	}
	
	public OverlayItem getmOverlay(int index) {
		return mOverlays.get(index);		
	}
	
//	public OverlayItem getTempOverlayItem(int index) {		
//		return tempOverlays.get(index);
//	}
	
	@Override
	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
		if(shadow)
			return;
//		System.out.println("drawSafe");		
//		if(projection == null)
//		Projection	projection = null;// = mMapView.getProjection();
		
		int type = mMapView.getMappingType();

		if(buildings != null && (type == MmcConstants.MANUAL_POLYGON_CREATE || type == MmcConstants.MANUAL_SEARCHING)) {
			drawManualPolygons(canvas, projection);
		}
		
 		if(mOverlays.size() > 0) {
			drawSafeSample(canvas, projection);
			drawDistanceToNextSample(canvas, projection);
		}
		
		if((polygonPoints.size() > 0 && maxLat != 0 && minLong != 0) 
				|| type == MmcConstants.MANUAL_POLYGON_CREATE || type == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS
				|| type == MmcConstants.MANUAL_POLYGON_DONE) {
			drawPolygon(canvas, projection, polygonPoints, Color.GREEN);	
		}
		
		if (buildingPolygons.size() > 0 && type == MmcConstants.MANUAL_SEARCHING) // && maxLat != 0 && minLong != 0))
		{
			Iterator<Entry<Long, List<GeoPoint>>> it = buildingPolygons.entrySet().iterator();
		    while (it.hasNext()) {
		        drawPolygon(canvas, projection, it.next().getValue(), Color.BLUE);	
		    }
		}
		if(tapInterval > 0 && tapGeoPoint != null) 
			drawCircle(canvas);
		
//		testMapCenter(canvas, projection);
	}	
	
	public void testMapCenter(Canvas canvas, Projection projection) {
		Paint circlePaint;
		circlePaint = new Paint();
		circlePaint.setStrokeJoin(Paint.Join.ROUND);
		circlePaint.setColor(Color.BLUE);
		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		circlePaint.setStrokeCap(Paint.Cap.ROUND);
		
		Point point = projection.toPixels(mMapView.getMapCenter(), null);
		int lat = mMapView.getMapCenter().getLatitudeE6()/1000000;
		int meters = 1;
		int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
		canvas.drawCircle((float)point.x, (float)point.y, radius, circlePaint);
	}
	
	public void drawPolygon(Canvas canvas, Projection projection, List<GeoPoint> geoPoints, int color) {
		//List<GeoPoint> geoPoints = getPolygonPoints();	
		float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
		
		if (geoPoints.size() < 1) 
	        return;
		
		Paint circlePaint;
		circlePaint = new Paint();
		circlePaint.setStrokeJoin(Paint.Join.ROUND);
		circlePaint.setColor(Color.BLUE);
		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		circlePaint.setStrokeCap(Paint.Cap.ROUND);
		
	    Paint polyPaint = new Paint();
	    polyPaint.setColor(color);
	    polyPaint.setAntiAlias(true);
	    polyPaint.setStyle(Paint.Style.STROKE);
	    polyPaint.setStrokeJoin(Paint.Join.ROUND);
	    polyPaint.setStrokeCap(Paint.Cap.ROUND);
	    polyPaint.setStrokeWidth(1 * screenDensityScale);
	    
		if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR ||
    			mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING) {
			polyPaint.setColor(Color.rgb(51, 153, 204));
			polyPaint.setStrokeWidth(3 * screenDensityScale);
		}
	    
//	    SafePaint fillPaint = new SafePaint();
//	    fillPaint.setAntiAlias(true);
//	    fillPaint.setColor(android.graphics.Color.RED);
//	    fillPaint.setStyle(Paint.Style.STROKE);
//	    fillPaint.setStrokeWidth(1);
//	    fillPaint.setAlpha(60);
	    
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
//	    	canvas.drawPath(polyPath, fillPaint);
	    	polyPath.moveTo((float) point.x, (float) point.y);
	    	
	    	if((mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE ||
	    			mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS ) && i == 0) {
//	    		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.anchor_marker); 
//				canvas.drawBitmap(bitmap, point.x - bitmap.getWidth()/2, point.y - bitmap.getHeight()/2, null);
	    		int lat = getPolygonPoint(0).getLatitudeE6()/1000000;
				int meters = 1;
				int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
				canvas.drawCircle((float)point.x, (float)point.y, radius, circlePaint);
	    	}
	    }
	    polyPath.lineTo((float) point.x, (float) point.y);
	    polyPath.close();
	    canvas.drawPath(polyPath, polyPaint); 
//	    canvas.drawPath(polyPath, fillPaint); 
	}
	
	public void drawManualPolygons(Canvas canvas, Projection projection) {
		
		for(int k = 0; k < buildings.size(); k++) {
			List<GeoPoint> building = buildings.get(k);
			float screenDensityScale = this.context.getResources().getDisplayMetrics().density;	
			
			Paint circlePaint;
			circlePaint = new Paint();
			circlePaint.setStrokeJoin(Paint.Join.ROUND);
			circlePaint.setColor(Color.BLUE);
			circlePaint.setAntiAlias(true);
			circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
			circlePaint.setStrokeCap(Paint.Cap.ROUND);
			
		    Paint polyPaint = new Paint();
		    polyPaint.setColor(Color.BLUE);
		    polyPaint.setAntiAlias(true);
		    polyPaint.setStyle(Paint.Style.STROKE);
		    polyPaint.setStrokeJoin(Paint.Join.ROUND);
		    polyPaint.setStrokeCap(Paint.Cap.ROUND);
		    polyPaint.setStrokeWidth(1 * screenDensityScale);	    
		    
		    Path polyPath = new Path();
		    polyPath.setFillType(Path.FillType.EVEN_ODD);
		    Point point = projection.toPixels(building.get(0), null);
		    
		    polyPath.moveTo((float) point.x, (float) point.y);
		    int i, len;
		    len = building.size();
		    for (i = 0; i < len; i++) {
		    	point = projection.toPixels(building.get(i), null);
		    	polyPath.lineTo((float) point.x, (float) point.y);
		    	canvas.drawPath(polyPath, polyPaint);
		    	polyPath.moveTo((float) point.x, (float) point.y);
		    }
		    polyPath.lineTo((float) point.x, (float) point.y);
		    polyPath.close();
		    canvas.drawPath(polyPath, polyPaint);  
		}
	}
	
	public void drawSafeSample(Canvas canvas, Projection projection) {

		Paint circlePaint;
		circlePaint = new Paint();
		circlePaint.setStrokeJoin(Paint.Join.ROUND);
		circlePaint.setColor(Color.BLUE);
		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		circlePaint.setStrokeCap(Paint.Cap.ROUND);
	
		int size = mOverlays.size();
		
		for(int index = size - 1; index >= 0; index--)  { 
			OverlayItem overlayItem = mOverlays.get(index); 
			String title = overlayItem.getTitle();
			if(title.equals("Sample") || title.equals("Anchor")) {
				Point point = projection.toPixels(overlayItem.getPoint(), null);
				//Snippet is used to track the drawable id
				int id = Integer.valueOf(overlayItem.getSnippet());	
				Drawable drawable = context.getResources().getDrawable(id);
				int width = drawable.getIntrinsicWidth();
				int height = drawable.getIntrinsicHeight();
				drawable.setBounds(-width/2, -height/2, width/2, height/2);
//				overlayItem.setMarkerHotspot(HotspotPlace.CENTER);
				overlayItem.setMarker(drawable);		
				
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inDither = false;                    
				opts.inPurgeable = true;                 
				opts.inInputShareable = true;             
				opts.inTempStorage = new byte[32 * 1024];		
				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id, opts); 
//				Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id); 
				canvas.drawBitmap(bitmap, point.x - bitmap.getWidth()/2, point.y - bitmap.getHeight()/2, null);
//				}
//				else { //if the above fails - for testing
//					int lat = overlayItem.getPoint().getLatitudeE6()/1000000;
//					int meters = 1;
//					int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
//					canvas.drawCircle((double)point.x, (double)point.y, radius, circlePaint);
//				}
			}
		} 
	}	
	
	public void drawCircle(Canvas canvas) {	
		Paint paint = new Paint();		
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setColor(Color.GRAY);
		//paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));
		
		int zoom = mMapView.getZoomLevel();
		int padding = 1;
		if(zoom <= 20)
			padding = 22 - zoom;
		
		Point point = projection.toPixels(tapGeoPoint, null);
		int lat = tapGeoPoint.getLatitudeE6()/1000000;
		int meters = 1;
				
		for(int i  = tapInterval; i >= 1; i--) {
			switch(i) { 
			case 1:
				paint.setColor(Color.rgb(192, 192, 192));
				meters = 1 * padding;
				paint.setStrokeWidth(70);	  
				break;
			case 2:
				paint.setColor(Color.rgb(160, 160, 160));
				meters = 3 * padding;
				paint.setStrokeWidth(50);	
				break;
			case 3:
				paint.setColor(Color.rgb(128, 128, 128));
				meters = 5 * padding;
				break;
			case 4:
				paint.setColor(Color.rgb(96, 96, 96));
				meters = 7 * padding;
				break;
			case 5:
				paint.setColor(Color.rgb(64, 64, 64));
				meters = 9;
				break;
			}
			
			int radius = (int) (projection.metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(lat))));   
			canvas.drawCircle((float)point.x, (float)point.y, radius, paint);
			mMapView.invalidate();
		}		
	}
	
	public void drawDistanceToNextSample(Canvas canvas, Projection projection) {
			 
		if(previousSample != null) {
			Path path = new Path();
			path.setFillType(Path.FillType.EVEN_ODD);
		    Point previousPoint = projection.toPixels(previousSample, null);
		    Point currentPosition = projection.toPixels(mMapView.getMapCenter(), null);
		    path.moveTo((float) previousPoint.x, (float) previousPoint.y);
		    path.lineTo((float) currentPosition.x, (float) currentPosition.y);
		    path.moveTo((float) currentPosition.x, (float) currentPosition.y);
		    path.close();
		    canvas.drawPath(path, paintWhiteLine);
		    
		    double dx = (mMapView.getMapCenter().getLongitudeE6()-previousSample.getLongitudeE6())/1000000.0;
	    	double dy = (mMapView.getMapCenter().getLatitudeE6()-previousSample.getLatitudeE6())/1000000.0;
	    	double distToSample = Math.sqrt(dx*dx+dy*dy);
	    	float second = 0;
	    	
	    	if (signalArray.size() > 1)
	    	{
	    		int radius = (int) (8 * screenDensityScale);
		    	float accumSeconds = (float)signalArray.size(); // accumulated # of seconds of walking
		    	double distance = 0; // percent of the distance along the line (seconds/accumSeconds)
		    	double prevdistance = 0;
		    	for (int i=0; i<signalArray.size(); i++){
			    	// draw some faded circles to show possible samples
		    		int signal = signalArray.valueAt(i);
		    		if (signal != 0)
		    		{
		    			second += 1;
				    	int color = EventsOverlay.signalColor(signalArray.valueAt(i), 16);
				    	smppaint.setColor (color);
				    	Point fillPoint;
				    	if (distToSample > 0.00005)
					    {
					    	distance = second*distToSample/accumSeconds;
					    	if (distance - prevdistance < 0.00002)
					    		continue;
					    	prevdistance = distance;
					    	GeoPoint point = TransitSamplingMapMath.findNextGeoPoint(previousSample, mMapView.getMapCenter(), distance);
					    	fillPoint = projection.toPixels(point, null);
					    }
				    	else
				    	{
				    		fillPoint = projection.toPixels(previousSample, null);
				    		distance = (int) (second * (3 * screenDensityScale));
				    		float xShadow = (float)(fillPoint.x + distance/2+1*screenDensityScale);
					    	float yShadow = (float)(fillPoint.y - distance/2-1*screenDensityScale);
					    	fillPoint.y -= distance;
					    	if (second > 10)
					    		break;
					    	
					    	canvas.drawCircle(xShadow, yShadow, radius, shadowpaint);
				    	}
						canvas.drawCircle((float)fillPoint.x, (float)fillPoint.y, radius, smppaint);
		    		}
			    }
			    
	    	}
		}
		
	}

	public void deleteLastPolygonCorner() {
		if(polygonPoints.size() > 0) {
			polygonPoints.remove(polygonPoints.size() -1);
//			if(0 == polygonPoints.size() && mOverlays.size() > 0) {
				//remove anchor icon
//				mOverlays.remove(0);
//				populate();
//			}
		}
		mMapView.invalidate();
	}
	
	@Override
	public void onZoomLevelChange() {
//		forceZoomLevel(mMapView.getZoomLevel());
		int zoom = mMapView.getZoomLevel();
		//reachedMaxZoomTransitionSmoother(zoom);
		System.out.println(zoom);
		
		if(mCoverageOverlay != null)
			mCoverageOverlay.onZoomLevelChange();
		int change = mMapView.getZoomLevel() - previousZoomLevel;
		zoomLevelChange += change;
//		if (zoom == 22 && change > 0)
//			mMapView.setSatellite(false);
//		else if (zoom == 21 && change < 0)
//			mMapView.setSatellite(true);
		
//		if(manualMapping == null)
//			return;
//		
//		if(zoom <= 19) {
//			manualMapping.disableZoomOut();
//			manualMapping.enableZoomIn();
//		}
//		else
//			manualMapping.enableZoomOut();
//		
//		if(zoom >= 22) {
//			manualMapping.disableZoomIn();
//			manualMapping.enableZoomOut();
//		}
//		else
//			manualMapping.enableZoomIn();
	}
	
	@Override
	public void onChange() { 
		
		projection = mMapView.getProjection();
		
		if(mCoverageOverlay != null)
			mCoverageOverlay.onChange();
		
//		int threshold = 10;
//		int zoom = mMapView.getZoomLevel();
//		if(zoom <= 17 && zoom >= 14)
//			threshold = 100;
//		else if(zoom <= 17 && zoom >= 14)
//			threshold = 300;
//		else if(zoom <= 13 && zoom >= 10)
//			threshold = 1300;
////		else return;
//		
//		if(mapCenterOSMFix == null) {
//			mapCenterOSMFix = (GeoPoint) mMapView.getMapCenter();
//			if(mapCenterOSMFix.getLatitudeE6() == 0 && mapCenterOSMFix.getLongitudeE6() == 0)
//				mapCenterOSMFix = null;
//		}
//		else if(distanceTo((GeoPoint)mMapView.getMapCenter(), mapCenterOSMFix) > threshold) {
//			mMapView.getController().animateTo(mapCenterOSMFix);
//		}
//		else {
//			mapCenterOSMFix = (GeoPoint) mMapView.getMapCenter();
//		}
		
		if(manualMapping == null) {
			return;
		}
		
		if(mMapView.getZoomLevel() > ZOOM_LEVEL_THRESHOLD) {
			lastLayout = System.currentTimeMillis();
			buildingsTimerTask = new BuildingsTimerTask();
			buildingsTimer.schedule(buildingsTimerTask, 600);	
		}
		else if (previousZoomLevel > ZOOM_LEVEL_THRESHOLD)
		{
			previousZoomLevel = mMapView.getZoomLevel();
			mMapChanged = false;
		}
		
		GeoPoint mapCenter = mMapView.getMapCenter();
		int type = mMapView.getMappingType();
		if(type == MmcConstants.MANUAL_SEARCHING || type == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS || type == MmcConstants.MANUAL_ANCHOR) {
			return;
		}
		if(polygonPoints.size() > 0 && type == MmcConstants.MANUAL_POLYGON_CREATE) {
			try {
				GeoPoint lastSample = polygonPoints.get(size() -1);
				String distanceInMeters = String.valueOf(manualMapping.distanceTo(mapCenter, lastSample));
				distanceInMeters = distanceInMeters.substring(0, distanceInMeters.indexOf(".")+ 2);
				manualMapping.setScaleText(distanceInMeters + "m");
			} catch(Exception e) {
//				e.printStackTrace();
			}
		}
		else if(mOverlays.size() > 0) {
			GeoPoint lastSample = mOverlays.get(size() -1).getPoint();
			String distanceInMeters = String.valueOf(manualMapping.distanceTo(mapCenter, lastSample));
			distanceInMeters = distanceInMeters.substring(0, distanceInMeters.indexOf("." ) + 2);
			manualMapping.setScaleText(distanceInMeters + "m");
		}
	}
	
	// Refresh the coverage after a delay
	public void refreshCoverage (int delay_ms)
	{
		haveCoverageForArea = new Rect();
		//load coverage image again
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mCoverageOverlay.forceNewCoverage();
			}
		}, delay_ms); 
		
		//buildingsTimerTask = new BuildingsTimerTask();
		//buildingsTimer.schedule(buildingsTimerTask, delay_ms);
	}
	
	class BuildingsTimerTask extends TimerTask {
		
		
		public BuildingsTimerTask() {
		}
		
		@Override
		public void run() {
			MMCMapActivity mappingContext = null;
			
			try {
				mappingContext = (MMCMapActivity) mMapView.getContext();
			} catch(Exception e) {
//				e.printStackTrace();
			}
				
			if (lastLayout != 0 && lastLayout + 400 <  System.currentTimeMillis())
     		{
				GeoPoint center = (GeoPoint) mMapView.getMapCenter();
//				int latSpanE6 = mMapView.getLatitudeSpan() * 2;
//				int longSpanE6 = mMapView.getLongitudeSpan() * 2;
				
//				int swLatE6 = center.getLatitudeE6() - latSpanE6/2;
//				int swLongE6 = center.getLongitudeE6() - longSpanE6/2;
//				int neLatE6 = center.getLatitudeE6() + latSpanE6/2;
//				int neLongE6 = center.getLongitudeE6() + longSpanE6;
//				
				int centerLatitudeE6, centerLongitudeE6, screenDlatE6, screenDlongE6;
				
				//while the map is being draw, latitude and longitude spans are invalid values, so we need to ensure they are valid before using them
				boolean isMapLatLongSpanValid = (mMapView.getLatitudeSpan() != 0 && mMapView.getLongitudeSpan() != 360000000);
				if(!isMapLatLongSpanValid && lastKnownMapCenter != null) {
					centerLatitudeE6 = lastKnownMapCenter.getLatitudeE6();
					centerLongitudeE6 = lastKnownMapCenter.getLongitudeE6();
					screenDlatE6 = lastKnownLatitudeSpanE6;
					screenDlongE6 = lastKnownLongitudeSpanE6;
				}
				else if (isMapLatLongSpanValid) {
					centerLatitudeE6 = mMapView.getMapCenter().getLatitudeE6();
					centerLongitudeE6 = mMapView.getMapCenter().getLongitudeE6();
					screenDlatE6 = mMapView.getLatitudeSpan();
					screenDlongE6 = mMapView.getLongitudeSpan();
				}
				else
					return;
				
				int screenTopLeftLatE6 = centerLatitudeE6 + screenDlatE6/2;
				int screenTopLeftLongE6 = centerLongitudeE6 - screenDlongE6/2;
				int screenBottomRightLatE6 = centerLatitudeE6 - screenDlatE6/2;
				int screenBottomRightLongE6 = centerLongitudeE6 + screenDlongE6/2;
				
				//rectangle that has the currently visible part of the screen (or last known part of screen, if current values are invalid)
				//boolean screenAreaIsCovered = haveCoverageForArea.contains(screenTopLeftLongE6, screenTopLeftLatE6, screenBottomRightLongE6, screenBottomRightLatE6);
				boolean screenAreaIsCovered = CoverageOverlay.doesRectContain (haveCoverageForArea, screenTopLeftLongE6, screenTopLeftLatE6, screenBottomRightLongE6, screenBottomRightLatE6);
				int zoomLevelChange = mMapView.getZoomLevel() - previousZoomLevel;
				if(!screenAreaIsCovered || Math.abs(zoomLevelChange) >= 2) {
					//start thread to request map coverage
					if(buildingsAsyncTask == null || buildingsAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
		 				// Cover an area twice the size of the screen
						int coveredAreaDlatE6 = screenDlatE6 * 2;
						int coveredAreaDlongE6 = screenDlongE6 * 2;
						
						final double bottomLeftLat = CoverageOverlay.round(((double)centerLatitudeE6 - coveredAreaDlatE6/2.0) / 1000000.0, false);
						final double bottomLeftLong = CoverageOverlay.round(((double)centerLongitudeE6 - coveredAreaDlongE6/2.0) / 1000000.0, false);
						final double topRightLat = CoverageOverlay.round(((double)centerLatitudeE6 + coveredAreaDlatE6/2.0) / 1000000.0, true);
						final double topRightLong = CoverageOverlay.round(((double)centerLongitudeE6 + coveredAreaDlongE6/2.0) / 1000000.0, true);
						haveCoverageForArea.set((int) (bottomLeftLong*1000000),
												(int) (topRightLat*1000000),
												(int) (topRightLong*1000000),
												(int) (bottomLeftLat*1000000));
						requestBuildings (bottomLeftLat, topRightLat, bottomLeftLong, topRightLong);
						
						previousZoomLevel = mMapView.getZoomLevel();
						mMapChanged = false;
						zoomLevelChange = 0;
					}
					else {
						lastKnownMapCenter = (GeoPoint) mMapView.getMapCenter();
						lastKnownLatitudeSpanE6 = mMapView.getLatitudeSpan();
						lastKnownLongitudeSpanE6 = mMapView.getLongitudeSpan();
						mMapChanged = true;
					}
				}
     		}
         }
	}
			
//	public float distanceTo(GeoPoint StartP, GeoPoint EndP) {
//		
//		if(EndP == null) 
//			return 0;
//		
//	    int Radius = 6371;//radius of earth in Km         
//	    double lat1 = StartP.getLatitudeE6()/1E6;
//	    double lat2 = EndP.getLatitudeE6()/1E6;
//	    double lon1 = StartP.getLongitudeE6()/1E6;
//	    double lon2 = EndP.getLongitudeE6()/1E6;
//	    double dLat = Math.toRadians(lat2-lat1);
//	    double dLon = Math.toRadians(lon2-lon1);
//	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
//	    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
//	    Math.sin(dLon/2) * Math.sin(dLon/2);
//	    double c = 2 * Math.asin(Math.sqrt(a));
//	    double valueResult= Radius*c;
//	    double km = valueResult/1;
//	    DecimalFormat newFormat = new DecimalFormat("####");
//	    float kmInDec =  Float.valueOf(newFormat.format(km));
////	    float meter = kmInDec%1000;
////	    System.out.println("kms: " + kmInDec);
//	    return kmInDec;
////	    return meter;
//	 }
	
	public void forceZoomLevel(int level) { 
		//zoom level 19-21: need to be zoomed in enough to make accurate taps
		GeoPoint geoPoint = mMapView.getMapCenter();
		if(level < 19) {			
			Toast.makeText(context, context.getString(R.string.manualmapping_indoor_zoom_accuracy), Toast.LENGTH_SHORT).show(); 
			level = 19;
		}
		level = level > 22 ? 22 : level; //this should never hit bc we limit zoom to 22
		mMapView.getController().setZoom(level);
		mMapView.getController().setCenter(geoPoint);
	}
	
	//Map jumps back out too far when zoom tries zoom level > 22
	public void reachedMaxZoomTransitionSmoother(int level) { 
		GeoPoint geoPoint = mMapView.getMapCenter();
		if(level > 23) {			
			mMapView.getController().setZoom(23);
			mMapView.getController().setCenter(geoPoint);
		}
	}
	
	//Checks to see if the given x and y are close enough to an item resulting in snapping the current action (e.g. zoom) to the item
	@Override
	public boolean onSnapToItem(int x, int y, Point point, MapView map) {
		return false;
	}
	
//	@Override
//    public boolean onDoubleTap(MotionEvent e, MapView mapView) {
//        //double tap disabled
//        return true;
//    }

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}
	 
	@Override
	public int size() {
		return mOverlays.size();
	}
	
	public void newFloor() {		
		OverlayItem anchor = null;
		for(int i = 0; i < mOverlays.size(); i++) {
			int icon = Integer.valueOf(mOverlays.get(i).getSnippet());
			if(icon == R.drawable.anchor_marker) 
				anchor = mOverlays.get(i);
		}
		
		mOverlays.clear();		
//		tempOverlays.clear();
		if(anchor != null)
			mOverlays.add(anchor);
		if(mMapView.getMappingType() == MmcConstants.MANUAL_SEARCHING) {
			polygonPoints = new ArrayList<GeoPoint>();
		}
		previousSample = null;
		mMapView.invalidate();
		
		mCoverageOverlay.setFloor(0);
	}
	
	public void newBuilding() {
		mOverlays.clear();		
		polygonPoints = new ArrayList<GeoPoint>();
		previousSample = null;
		mMapView.invalidate();
		mCoverageOverlay.setFloor(-999);
	}
	
	public void showCrosshair(boolean on) {
		if(on) {
			crosshairImage.setVisibility(View.VISIBLE); 
		}
		else
			crosshairImage.setVisibility(View.GONE);
	}
	
//	public void addOverlayItem(OverlayItem overlay) {
//		
//		//Coordinates of tapped sample
//		int newLat = Math.abs(overlay.getPoint().getLatitudeE6());
//		int newLong = Math.abs(overlay.getPoint().getLongitudeE6());
//			
//		for(int i = 0; i < tempOverlays.size(); i++) {
//			OverlayItem item = tempOverlays.get(i);
//			GeoPoint temp = item.getPoint();
//			
//			//Coordinates of previous samples + 1 meter allowance
//			int oldLat = Math.abs(temp.getLatitudeE6());
//			int oldLong = Math.abs(temp.getLongitudeE6());			
//			
//			System.out.println("OLD LAT/LONG " + oldLat + " " + oldLong);
//			System.out.println("NEW LAT/LONG " + newLat + " " + newLong);
//					
//			int latDifference = Math.abs(newLat - oldLat);
//			int longDifference = Math.abs(newLong - oldLong);
//					
//			if(latDifference <= 10 && longDifference <= 10) {
//				//tapped on a previous sample
////				deleteSamplePrompt(i);
//				return;	
//			}
//		}		
//		
////		OverlayDetails overlayDetail = new OverlayDetails(overlay, System.currentTimeMillis()/1000);
//		tempOverlays.add(overlay);
////		mOverlayDetails.add(overlayDetail);
//	
////		DecimalFormat df = new DecimalFormat("#.0000");
////		String latLong = df.format(overlay.getPoint().getLatitude()) + ", " + df.format(overlay.getPoint().getLongitude());
////		Toast.makeText(context, latLong, Toast.LENGTH_SHORT).show(); 		
//		
//		//If we have a polygon make sure we sample inside the building
//		//If the user is sampling in a building that we do not yet have a polygon for, we have to trust them :(
//		if((validatePoint(overlay.getPoint()) == false && getPolygonPoints() != null) && mMapView.getMappingType() == MmcConstants.INDOOR_SAMPLING) { 		
//			tempOverlays.remove(overlay);
//		}
//		
//		if(mMapView.getMappingType() == MmcConstants.OUTDOOR_SAMPLING)
//			addOverlays();
//		
//		mMapView.invalidate();	
//	}
	  
	public void addOverlays() {
		if (!mMapView.getOverlays().contains(this))
			mMapView.getOverlays().add(this);
		populate();
	} 
	
	/**** Methods to request and display the polygon ****/
	public String getExtraUrlString(double latitude, double longitude) {
		String apiKey = MMCService.getApiKey(context);
        if(apiKey != null) {
			return "/api/osm/building?longitude=" + longitude + "&latitude=" + latitude + "&precision=10&apiKey=" + apiKey;
		}
		return null;
	}
	
	public String getBuildingsUrlString(double lat0, double lat1, double lng0, double lng1) {
		String apiKey = MMCService.getApiKey(context);
		if(apiKey != null) {
			return "/api/osm/buildings?sw=" +lat0 + "&sw=" + lng0 + "&ne=" + lat1 + "&ne=" + lng1 + "&limit=1000&apiKey=" + apiKey;
		}
		return null;
	}
	
	public AsyncTask.Status returnAsyncTaskStatus() {
		if(polygonAsyncTask != null)
			return polygonAsyncTask.getStatus();
		else
			return null;
	}
		
	public void requestPolygon(final double latitude, final double longitude) {
		final String urlExtra = getExtraUrlString(latitude, longitude);
		isLongPressInManualPolygon(latitude, longitude);
		
//		if (polygonAsyncTask != null) {
//			Toast.makeText(context, context.getString(R.string.manualmapping_still_searching), Toast.LENGTH_SHORT).show();
//			return;
//		}
//		Toast.makeText(context, context.getString(R.string.manualmapping_searching), Toast.LENGTH_SHORT).show();
//		mBusyIndicator.setVisibility(View.VISIBLE);
//		if(urlExtra != null) {	
//			polygonAsyncTask = new AsyncTask<Void, Void, String>() {
//				@Override
//				protected String doInBackground(Void... params) {				
//					String responseContents = "";
//					try { 
//						String url = context.getString(R.string.MMC_URL_LIN) + urlExtra;
//						//Timeouts
//						HttpParams httpParameters = new BasicHttpParams();
//						int timeoutConnection = 10000;
//						HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//						int timeoutSocket = 10000;
//						HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);	
//						
//						DefaultHttpClient client = new DefaultHttpClient(httpParameters);						
//						HttpGet get = new HttpGet(url); 					
//						get.setHeader("Content-Type", "application/json; charset=utf-8");
//						HttpResponse response = null;
//						response = client.execute(get);
//						responseContents = EntityUtils.toString(response.getEntity());	
//						System.out.println(url);
//						System.out.println(responseContents);	
////						
//					} catch(Exception e) {
//						System.out.println(e);
//					}
//					return responseContents;
//				}
//				
//				@Override
//				protected void onPostExecute(String result) {
//					if(validateResponse(result)) {
//						parseJSONString(result);
//					}
//					else {
//						//Look for manually created polygon
//						isLongPressInManualPolygon(latitude, longitude);
//					}
//					polygonAsyncTask = null;
//					mBusyIndicator.setVisibility(View.GONE);
//					removeTapAnimation();
//				}
//			};
//			TaskHelper.execute(polygonAsyncTask); 
//		}
	}
	
	public void requestBuildings(final double lat0, final double lat1, final double lng0, final double lng1) {
		final String urlExtra = getBuildingsUrlString(lat0, lat1, lng0, lng1);
		if (buildingsAsyncTask != null) {
			//Toast.makeText(context, context.getString(R.string.manualmapping_still_searching), Toast.LENGTH_SHORT).show();
			return;
		}
		//Toast.makeText(context, context.getString(R.string.manualmapping_searching), Toast.LENGTH_SHORT).show();
		//mBusyIndicator.setVisibility(View.VISIBLE);
		if(urlExtra != null) {	
			buildingsAsyncTask = new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {				
					String responseContents = "";
					try { 
						String url = context.getString(R.string.MMC_URL_LIN) + urlExtra;
						responseContents = WebReporter.getHttpURLResponse(url, false);

					} catch(Exception e) {
						System.out.println(e);
					}
					return responseContents;
				}
				
				@Override
				protected void onPostExecute(String result) {
					if(validateResponse(result)) {
						parseBuildingsJSON(result);
					}
					
					buildingsAsyncTask = null;
					//mBusyIndicator.setVisibility(View.GONE);
					if(mMapChanged) {
						//load coverage image again
						mHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								onChange();
							}
						}, RELOAD_DELAY);
					}
				}
			};
			TaskHelper.execute(buildingsAsyncTask); 
		}
	}
	
	//Returns true if the tapped point (sample) is outside of the polygon
	public boolean validatePoint(GeoPoint tappedPoint, List<GeoPoint> polyPoints) {
		if(polyPoints == null)
			return false;
		int	size = polyPoints.size();
		int[] polyX = new int[size];
		int[] polyY = new int[size];
    	
    	for(int i = 0; i < size; i++) {
    		polyX[i] = polyPoints.get(i).getLatitudeE6();
    		polyY[i] = polyPoints.get(i).getLongitudeE6();
    	}
    	
    	int x = tappedPoint.getLatitudeE6();
    	int y = tappedPoint.getLongitudeE6();
    	
    	boolean inside = false;
    	int i, j = 0;
    	for (i = 0, j = size - 1; i < size; j = i++) {
    		if (((polyY[i] > y) != (polyY[j] > y))
    				&& (x < (polyX[j] - polyX[i]) * (y - polyY[i]) / (polyY[j] - polyY[i]) + polyX[i]))
    			inside = !inside;
    	}
    	return inside;			
	}
		
	public GeoPoint findMidPolygonPoint() {
			
		long latitude = 0;
		long longitude = 0;
		List<GeoPoint> geoPoints = getPolygonPoints();
		int size = geoPoints.size();
		System.out.println("Geopoints: " + size);
		maxLat = 0;
		minLong =  10000000000000.0;
					
		for(int i = 0; i < size; i++) {
			int temp1 = geoPoints.get(i).getLatitudeE6();
			int temp2 = geoPoints.get(i).getLongitudeE6();
			latitude += temp1;
			longitude += temp2;
			if(maxLat < temp1)
				maxLat = temp1;
			if(minLong > temp2)
				minLong = temp2;
		}
			
		latitude = latitude / size;
		longitude = longitude / size;
		System.out.println("Mid lat/long: " + latitude + " | " + longitude);
			
		return	(new GeoPoint((int)latitude, (int)longitude));
	}
			
	public void drawPolygon() {
		addOverlays();
		mMapView.getController().animateTo(findMidPolygonPoint()); //center to middle of polygon
		mMapView.invalidate();
	}

	public void isLongPressInManualPolygon(double latitude, double longitude) {
		List<GeoPoint> points = new ArrayList<GeoPoint>(); 
		
		if (buildingPolygons.size() > 0)
		{
			Iterator<Entry<Long, List<GeoPoint>>> it = buildingPolygons.entrySet().iterator();
		    while (it.hasNext()) {
		    	Entry<Long, List<GeoPoint>> building = it.next();
		    	boolean found = validatePoint(new GeoPoint((int)(latitude*1000000), (int)(longitude*1000000)), building.getValue());
		    	if (found)
		    	{
		    		osm_id = building.getKey ();
		    		polygonPoints = building.getValue();
		    		drawPolygon();
		    		mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
		    		return;
		    	}
		    }
		}
		
		if(buildings == null || buildings.size() == 0) {
			Toast.makeText(context, context.getString(R.string.manualmapping_polygon_notfound), Toast.LENGTH_SHORT).show(); 
			return;
		}
		
		for(int i = 0; i < buildings.size(); i++) {
			boolean found = validatePoint(new GeoPoint((int)(latitude*1000000), (int)(longitude*1000000)), buildings.get(i));
			if(found) {
				points = buildings.get(i);
				osm_id = 0;
				break;
			}
		}
		
		if(points.size() > 0) {
			polygonPoints = points;
//			mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
			mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_DELETE_OR_SELECT);
			mMapView.getController().animateTo(findMidPolygonPoint());
			mMapView.invalidate();
		}
		else
			Toast.makeText(context, context.getString(R.string.manualmapping_polygon_notfound), Toast.LENGTH_SHORT).show(); 
	}

//	public List<GeoPoint> parseJSONString(String jsonString) {
//		JSONObject json = null;
//		JSONArray jsonArray;
//		
//		List<GeoPoint> points = new ArrayList<GeoPoint>(); 
//		try {
//			json = new JSONObject(jsonString);		 			
//			jsonArray = json.getJSONArray("polygon");
//			for(int i = 0; i < jsonArray.length(); i++) {					
//				String results = jsonArray.getString(i);
//				results = results.replace("[", "");
//				results = results.replace("]", "");
//				double longitude = Double.valueOf(results.substring(0,results.indexOf(",")));
//				double latitude = Double.valueOf(results.substring(results.indexOf(",")+1));
//				longitude *= 1E6;
//				latitude *= 1E6;
//				points.add(new GeoPoint((int) latitude, (int) longitude));			
//			}
//			if (json.has("osm_id"))
//				osm_id = json.getLong("osm_id");
//			else
//				osm_id = 0;
//			
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		polygonPoints = points;
//		drawPolygon();
//		mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
//		return points;
//	}
	
	public void parseBuildingsJSON(String jsonString) {
		JSONObject json = null;
		JSONArray jsonBuildings;
		
		try {
			json = new JSONObject(jsonString);		 			
			jsonBuildings = json.getJSONArray("values");
			for(int i = 0; i < jsonBuildings.length(); i++) {	
				List<GeoPoint> points = new ArrayList<GeoPoint>(); 
				JSONObject building = jsonBuildings.getJSONObject(i);
				Long osmid = building.getLong("osm_id");
				JSONObject polygon = building.getJSONObject("polygon");
				JSONArray coords = polygon.getJSONArray("coordinates");
				JSONArray apoints = coords.getJSONArray(0);
				for(int j = 0; j < apoints.length(); j++) {	
					String results = apoints.getString(j);
					results = results.replace("[", "");
					results = results.replace("]", "");
					double longitude = Double.valueOf(results.substring(0,results.indexOf(",")));
					double latitude = Double.valueOf(results.substring(results.indexOf(",")+1));
					longitude *= 1E6;
					latitude *= 1E6;
					points.add(new GeoPoint((int) latitude, (int) longitude));
				}
				buildingPolygons.put(osmid, points);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		addOverlays();
		mMapView.invalidate();
	}

	public boolean validateResponse(String response) {
		JSONObject json = null;
		try {
			json = new JSONObject(response);		 
			String success = json.optString("success");
			if(success.equals("true")) 
				return true;
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**** Methods to change the sample color based on signal strength ****/
	public int signalStrengthIcon(int signalStrength) {		
		//int signalStrength = PreferenceManager.getDefaultSharedPreferences(context).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
		
		if(signalStrength <= -121 || signalStrength == 0)
			return R.drawable.mapping_marker_grey_default;
		
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		int percent = -1;
		if(telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE) { //2g
			percent = Math.abs(getPercentageFromDbm(signalStrength, -50));
		}
		else
			percent = Math.abs(getPercentageFromDbm(signalStrength, -70));
		
		if(percent >= 1 && percent <= 19) 
			return R.drawable.mapping_marker_red;
		if(percent >= 20 && percent <= 39) 
			return R.drawable.mapping_marker_orange;
		if(percent >= 40 && percent <= 59) 
			return R.drawable.mapping_marker_yellow;
		if(percent >= 60 && percent <= 79) 
			return R.drawable.mapping_marker_light_green;		
		if(percent >= 80) 
			return R.drawable.mapping_marker_dark_green;
					
		return R.drawable.mapping_marker_grey_default;		
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
	
//	@Override
//    public boolean onTouchEvent(MotionEvent event, MapView mapView) {			
//		
//		OverlayItem item = null;
//		final int action = event.getAction();
//		final int x = (int) event.getX();
//		final int y = (int) event.getY();
//		boolean result = false;
//				
//		Point t = new Point(0,0);
//		Point p = new Point(0,0);	
//		    	
//		if(mMapView.getMappingType() == MmcConstants.INDOOR_ANCHOR || mMapView.getMappingType() == MmcConstants.OUTDOOR_SAMPLING 
//				||mMapView.getMappingType() == MmcConstants.INDOOR_SAMPLING) {			
//			
//			if (action == MotionEvent.ACTION_DOWN && getSize() != 0) {
//			
//				item = tempOverlays.get(getSize()-1);
//				item.setMarkerHotspot(HotspotPlace.CENTER);
//				item.setMarker(crosshair);
//				
//		    	mapView.getProjection().fromMapPixels(x, y, t);
//		    	mapView.getProjection().toPixels(item.getPoint(), p);
//		    	
//		    	if (hitTest(item, crosshair, t.x - p.x, t.y - p.y)) {
//					result = true;
//		
//					dragging = item;
//					tempOverlays.remove(item);
////					xDragTouchOffset = 0;
////					yDragTouchOffset = 0;
//					xDragTouchOffset = t.x - p.x;
//					yDragTouchOffset = t.y - p.y;
//		            
////					setDragImagePosition(p.x, p.y);
//					setDragImagePosition(x, y);
//					crosshairImage.setImageDrawable(crosshairDrag);
//					 
////					xDragTouchOffset = t.x - p.x;
////					yDragTouchOffset = t.y - p.y;
////					xDragTouchOffset = x - p.x;
////					yDragTouchOffset = y - p.y;	            
//				}			
//			}
//			else if (action == MotionEvent.ACTION_MOVE && dragging != null) {
//				setDragImagePosition(x, y);
//				result = true;
//			}
//			else if (action == MotionEvent.ACTION_UP && dragging != null) {
//				crosshairImage.setImageDrawable(crosshair);
//				GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels(x - xDragTouchOffset, y - yDragTouchOffset);
//				
//				if(mMapView.getMappingType() == MmcConstants.INDOOR_SAMPLING || mMapView.getMappingType() == MmcConstants.OUTDOOR_SAMPLING) {	
//					OverlayItem overlayItem = new OverlayItem("Sample", "", geoPoint); //title snippet point
//					addOverlayItem(overlayItem);				
//				}
//				
//				if(mMapView.getMappingType() == MmcConstants.INDOOR_ANCHOR) {
//					OverlayItem overlayItem = new OverlayItem("Anchor", "", geoPoint);
//					addOverlayItem(overlayItem);
//				}
//				
//				dragging = null;
//		        result = true;
//			}
//		}
//		return(result || super.onTouchEvent(event, mapView));
////	}	
//	
//	@Override
//	protected boolean hitTest(OverlayItem overlayItem, Drawable drawable, int x, int y) {
//	    Rect bounds = drawable.getBounds();
//	    return bounds.contains(Math.abs(x), Math.abs(y));
//	}
//	    
//    private void setDragImagePosition(int x, int y) {
//    	RelativeLayout.LayoutParams relativeLayout = (RelativeLayout.LayoutParams) crosshairImage.getLayoutParams();    	
//    	relativeLayout.setMargins(x - xDragImageOffset - xDragTouchOffset, y - yDragImageOffset - yDragTouchOffset, 0, 0);
//    	crosshairImage.setLayoutParams(relativeLayout);
//    }
    
	/*** Methods for deletion ***/
    public void setDelete() {
    	//switch the state
    	if(delete) {
    		//Delete was turned off
    		delete = false; 
    		crosshairImage.setVisibility(View.VISIBLE);
    		if(mMapView.getMappingType() != MmcConstants.MANUAL_POLYGON_CREATE)
    			removeDeleteIcons();
    	}
    	else {
    		delete = true;
    		crosshairImage.setVisibility(View.GONE);
    	}
    }
    
    public boolean getDelete() {
    	return delete;
    }
    
    public void deleteLastSample() {
    	if(size() == 0)
    		return;
    	int icon = Integer.valueOf(mOverlays.get(size()-1).getSnippet());
    	if(icon == R.drawable.anchor_marker)
    		return;   		 
		mOverlays.remove(mOverlays.get(size()-1));
		populate();        	
		mMapView.invalidate();  
    }
    
    public void deleteOverlays() { 
    	//Loop through all the overlays and if they have the delete icon, remove them
    	for(int i = 0; i < size(); i++) {    		
    		int icon = Integer.valueOf(mOverlays.get(i).getSnippet());
    		boolean hasDeleteIcon = false;
            if (icon == R.drawable.mapping_marker_red_delete) {
                hasDeleteIcon = true;

            } else if (icon == R.drawable.mapping_marker_orange_delete) {
                hasDeleteIcon = true;

            } else if (icon == R.drawable.mapping_marker_yellow_delete) {
                hasDeleteIcon = true;

            } else if (icon == R.drawable.mapping_marker_light_green_delete) {
                hasDeleteIcon = true;

            } else if (icon == R.drawable.mapping_marker_dark_green_delete) {
                hasDeleteIcon = true;

            } else if (icon == R.drawable.mapping_marker_grey_delete_default) {
                hasDeleteIcon = true;

//	    		case R.drawable.anchor_marker_delete:
//	    			hasDeleteIcon = true;
//	    			break;
            }
    		if(hasDeleteIcon) {
    			mOverlays.remove(mOverlays.get(i));
    			i--;
    		}
    	}	
    	populate();        	
    	mMapView.invalidate();
    }
    
    @Override
    public boolean onTap(int index) {
		if(delete) {
			//Retrieve the tapped overlay
			OverlayItem tappedOverlay = mOverlays.get(index);
			String title = tappedOverlay.getTitle();
			int icon = Integer.valueOf(tappedOverlay.getSnippet());
			icon = updateIcon(icon);
			//Create a new overlay with the current point but different image
			OverlayItem deleteOverlay = new OverlayItem(tappedOverlay.getPoint(), title, String.valueOf(icon));
			//Update overlays to be displayed
			mOverlays.remove(tappedOverlay);		
			mOverlays.add(deleteOverlay);
			populate();
	    	mMapView.invalidate();
			return true;
		}   	
    	
    	return false;
    }
    
    public void removeDeleteIcons() {
    	for(int i = 0; i < size(); i++) {    		
    		int icon = Integer.valueOf(mOverlays.get(i).getSnippet());
    		int newIcon = 0;
            if (icon == R.drawable.mapping_marker_grey_delete_default) {
                newIcon = R.drawable.mapping_marker_grey_default;

            } else if (icon == R.drawable.mapping_marker_red_delete) {
                newIcon = R.drawable.mapping_marker_red;

            } else if (icon == R.drawable.mapping_marker_orange_delete) {
                newIcon = R.drawable.mapping_marker_orange;

            } else if (icon == R.drawable.mapping_marker_yellow_delete) {
                newIcon = R.drawable.mapping_marker_yellow;

            } else if (icon == R.drawable.mapping_marker_light_green_delete) {
                newIcon = R.drawable.mapping_marker_light_green;

            } else if (icon == R.drawable.mapping_marker_dark_green_delete) {
                newIcon = R.drawable.mapping_marker_dark_green;

//	    		case R.drawable.anchor_marker_delete:
//	    			newIcon = R.drawable.anchor_marker;
//		    		break;
            }
    		if(newIcon > 0) {
    			//Remove the delete icon and replace it with the normal one
    			OverlayItem item = mOverlays.get(i);
    			mOverlays.remove(item);
    			OverlayItem newItem = new OverlayItem(item.getPoint(), item.getTitle(), String.valueOf(newIcon));
    			mOverlays.add(newItem);
    		}
    	}	
    	populate();        	
    	mMapView.invalidate();
    }
    
    public int updateIcon(int icon) {  
    	//switch the icon when tapped between delete or normal
        if (icon == R.drawable.mapping_marker_grey_delete_default) {
            icon = R.drawable.mapping_marker_grey_default;

        } else if (icon == R.drawable.mapping_marker_red_delete) {
            icon = R.drawable.mapping_marker_red;

        } else if (icon == R.drawable.mapping_marker_orange_delete) {
            icon = R.drawable.mapping_marker_orange;

        } else if (icon == R.drawable.mapping_marker_yellow_delete) {
            icon = R.drawable.mapping_marker_yellow;

        } else if (icon == R.drawable.mapping_marker_light_green_delete) {
            icon = R.drawable.mapping_marker_light_green;

        } else if (icon == R.drawable.mapping_marker_dark_green_delete) {
            icon = R.drawable.mapping_marker_dark_green;

            //Samples from normal to delete
        } else if (icon == R.drawable.mapping_marker_grey_default) {
            icon = R.drawable.mapping_marker_grey_delete_default;

        } else if (icon == R.drawable.mapping_marker_red) {
            icon = R.drawable.mapping_marker_red_delete;

        } else if (icon == R.drawable.mapping_marker_orange) {
            icon = R.drawable.mapping_marker_orange_delete;

        } else if (icon == R.drawable.mapping_marker_yellow) {
            icon = R.drawable.mapping_marker_yellow_delete;

        } else if (icon == R.drawable.mapping_marker_light_green) {
            icon = R.drawable.mapping_marker_light_green_delete;

        } else if (icon == R.drawable.mapping_marker_dark_green) {
            icon = R.drawable.mapping_marker_dark_green_delete;

//		//Anchor normal to delete
//		case R.drawable.anchor_marker:
//			icon = R.drawable.anchor_marker_delete;
//			break;
//		//Anchor delete to normal
//		case R.drawable.anchor_marker_delete:
//			icon = R.drawable.anchor_marker;
//			break;
        }
    	return icon;
    }
}
