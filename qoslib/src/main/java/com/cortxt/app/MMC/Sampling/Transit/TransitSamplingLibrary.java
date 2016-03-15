package com.cortxt.app.MMC.Sampling.Transit;

import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;


public class TransitSamplingLibrary extends MMCTrackedActivityOld {

//	private ListView libraryListView;
//	private TransitDatabaseReadWriteNew transitDB;
//	public static final String TAG = TransitSamplingLibrary.class.getSimpleName();
//	private boolean dataSaved = false;
//	private boolean[] checkedCities;
//	
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		
//		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View view = inflater.inflate(R.layout.transit_sampling_library, null, false);
//		ScalingUtility.getInstance(this).scaleView(view);
//		setContentView(view);
//		
//		libraryListView = (ListView) view.findViewById(R.id.libraryListView);
//		
//		transitDB = new	TransitDatabaseReadWriteNew(this);
//		
//		getAreaInformation();
//	}
//	
//	public void onExit(View view) {
//		//
//		
//		Intent intent = new Intent(TransitSamplingLibrary.this, TransitSamplingMain.class);
//		startActivity(intent);
//		this.finish();
//	}
//	
//	/*** This function will save areas if they are out of date or new to the database ***/
//	public void saveAreas(String areas) {
//		JSONObject json = null;
//		JSONArray jsonArray;	
//		String areaName = null;
//		int areaId = 0;
//		List<String> citiesToDisplay = new ArrayList<String>();
//		
//		if(areas == null)
//			return;
//		
//		try {
//			json = new JSONObject(areas);	
//			String success = json.getString("success");
//			
//			if(success.equals("false")) {
//				//Server request failed, but see if there are areas in the database 
//				lookForAreasInDB();
//				return;
//			}
//			
//			jsonArray = json.getJSONArray("values");
//			int total = json.getInt("count");
//
//			for(int i = 0; i < total; i++) {
//				json = jsonArray.getJSONObject(i);
//				
//				areaName = json.getString("area_name");
//				areaId = json.getInt("area_id");
//				
//				boolean newArea = transitDB.doesAreaExist(String.valueOf(areaId));
//				
//				//if area doesnt have data yet, or area is out of date clean up data
//				if(!transitDB.doesAreaAlreadyHaveData(String.valueOf(areaId)) || 
//						transitDB.isAreaCurrent(String.valueOf(areaId)) == false || newArea) {
//					transitDB.cleanStops(String.valueOf(areaId));
//					transitDB.cleanRoutes(String.valueOf(areaId));
//					transitDB.cleanShapes(String.valueOf(areaId));
//					transitDB.cleanArea(String.valueOf(areaId));
//					transitDB.cleanCities(String.valueOf(areaId));
//					
//					newArea = true;
//					transitDB.saveAreaToDB(areaName, String.valueOf(areaId));
//				}
//			
//				String cities = json.getString("cities");			
//				JSONArray citiesArray = new JSONArray(cities); 
//		//			citiesToDisplay = new String[citiesArray.length()];
//					
//				for(int k = 0; k < citiesArray.length(); k++) {
//					JSONObject cityJson =  citiesArray.getJSONObject(k);
//					String name = cityJson.getString("city_name");
//						if(newArea) {
//							String id = String.valueOf(cityJson.getInt("city_id"));
//							transitDB.saveCityToDB(name, id, String.valueOf(areaId));
//						}
//					
//	//				citiesToDisplay[k] = name;
//					citiesToDisplay.add(areaName);
//				}
//			}
//			
//			String[] temp = (String[]) citiesToDisplay.toArray(new  String[citiesToDisplay.size()]);
//			final ListViewAdapter adapter = new ListViewAdapter(this, R.layout.custom_spinner, temp);
//			libraryListView.setAdapter(adapter); 
//			
//			whichAreasHaveTransitInfo(temp);
//		            
//			libraryListView.setOnItemClickListener(new OnItemClickListener() {
//				@Override
//				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//					adapter.setSelected(position);
//					adapter.notifyDataSetChanged();
//				}
//			}); 
//		}
//		catch (JSONException e) {
//			e.printStackTrace();
//			return;
//		}
//	}
//	
//	public void lookForAreasInDB() {
//		String[] citiesToDisplay = transitDB.getAllAreas();
//		checkedCities = new boolean[citiesToDisplay.length];
//		//This is coming from the DB so we already have the information for each area transit system
//		Arrays.fill(checkedCities, true);
//		
//		final ListViewAdapter adapter = new ListViewAdapter(this, R.layout.custom_spinner, citiesToDisplay);
//		libraryListView.setAdapter(adapter); 
//	            
//		libraryListView.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				adapter.setSelected(position);
//				adapter.notifyDataSetChanged();
//			}
//		}); 
//	}
//	
//	public void whichAreasHaveTransitInfo(String[] cities) {
//		checkedCities = new boolean[cities.length];
//		for(int i = 0; i < cities.length; i++) {
//			String cityName = cities[i];
//        	String cityId = transitDB.getCityId(cityName);
//        	String areaId = transitDB.getAreaIdFromCity(cityName, cityId);
//        	boolean checked = transitDB.getDownload(areaId);	
//        	checkedCities[i] = checked;
//        }
//	}
//	
//	public void showDownloadAlert() {
//		AlertDialog.Builder alert = new AlertDialog.Builder(this);
//		alert.setTitle(getString(R.string.transitsampling_download_title));
//		alert.setMessage(getString(R.string.transitsampling_download_msg));
//		alert.setCancelable(false);
//		
//		alert.setPositiveButton(getString(R.string.transitsampling_download_proceed), new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int whichButton) {	
//				dialog.dismiss();
//			}
//		});	
//		
//		alert.setNegativeButton(getString(R.string.transitsampling_download_wait), new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int whichButton) {
//				dialog.dismiss();
//				TransitSamplingLibrary.this.finish();
//			}
//		});	
//	
//		alert.show();
//	}
//	
//	public void getAreaInformation() {
//		new AsyncTask<Void, Void, String>() {
//			@Override
//			protected String doInBackground(Void... params) {
////				final String apiKey = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.API_KEY, null);
////				if(apiKey != null) { 
//					String url = getString(R.string.MMC_URL_LIN) + "/api/transit?areas";
//					HttpClient client = new DefaultHttpClient();
//					String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
//		https://dev.mymobilecoverage.com/api/transit/areas?apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc
//					HttpGet get = new HttpGet("https://dev.mymobilecoverage.com/api/transit/areas?apiKey=" + apiKey);
//					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");	//ie: Tue, 22 Nov 2011 20:56:21 GMT
//					Calendar calendar = new GregorianCalendar();
//					String date = simpleDateFormat.format(calendar.getTime());
//					get.setHeader("Content-Type", "application/json; charset=utf-8");
//					get.setHeader("Date:", date);
//					HttpResponse response = null;
//					
//					try {
//						response = client.execute(get);
//					} catch(Exception e) {
//						System.out.println(e);
//					}
//					String responseContents = "";
//					try {
//						responseContents = EntityUtils.toString(response.getEntity());
//					} catch(Exception e) {
//						System.out.println(e);
//					}
//					
//					return responseContents;
////				}
////				else {
////					return null;
////				}
//			}
//				
//			@Override
//			protected void onPostExecute(String responseContents) {
//				saveAreas(responseContents);
//			}			
//		}.execute((Void[])null);
//	}
//	
//	public void getCityInformation(final String cityId, final boolean lastDownload) {
//		new AsyncTask<Void, Void, String>() {
//			@Override
//			protected String doInBackground(Void... params) {	
//				HttpResponse response = null;
//				boolean success = false;
//				try {
//					DefaultHttpClient httpClient = new DefaultHttpClient();
//					String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
//					HttpGet request = new HttpGet("https://dev.mymobilecoverage.com/api/transit?city_id=" + cityId 
//							+ "&apiKey=" + apiKey );//+ "&limit=50");
//					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");	//ie: Tue, 22 Nov 2011 20:56:21 GMT
//					Calendar calendar = new GregorianCalendar();
//					String date = simpleDateFormat.format(calendar.getTime());
//					request.setHeader("Content-Type", "application/json; charset=utf-8");
//					request.setHeader("Date:", date);
//					
//					response = httpClient.execute(request);
//	
//					HttpEntity responseEntity = response.getEntity();
//					InputStream in = responseEntity.getContent();
//	
//					if(readJsonStream(in, cityId)) {
//						success = true;
//					}
//			
//				} catch (Exception e) {
//					if(!dataSaved) {
//						//If this fails remove areas or next time isAreaCurrent() will return true and for 30 days 
//						//there will only be area info (no city, routes etc..) 
//						MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getCityInformation", "Processing or download failed");
//						transitDB.cleanArea(String.valueOf(cityId));
//						transitDB.cleanCities(String.valueOf(cityId));
//					}
//					else {
//						//reset
//						dataSaved = false;
//					}
//				}
//				
//				return String.valueOf(success);
//			}
//			
//			@Override
//			protected void onPostExecute(String success) {
//				if(lastDownload == true) {
////					Intent intent = new Intent(MMCIntentHandlerOld.ACTION_TRANSIT_DL_DONE);
////					sendBroadcast(intent);
////					TransitSamplingLibrary.this.finish();
//				}
//			}			
//		}.execute((Void[])null);
//	} 
//	
//	public double[] toDoubleArray(List<Double> list) {
//		double[] ret = new double[list.size()];       
//		Iterator<Double> iter = list.iterator();
//		for (int i=0; iter.hasNext(); i++) {       
//		    ret[i] = iter.next();                
//		}                                        
//		return ret;
//	}
//	
//	public int[] toIntegerArray(List<Integer> list) {
//		int[] ret = new int[list.size()];       
//		Iterator<Integer> iter = list.iterator();
//		for (int i=0; iter.hasNext(); i++) {       
//		    ret[i] = iter.next();                
//		}                                        
//		return ret;
//	}
//	
//	public boolean readJsonStream(InputStream in, String areaId) throws IOException {
//		JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
//		jsonReader.setLenient(true);
//		jsonReader.beginObject();
//		Gson gson = new Gson();
//		boolean success = false;
//
//		List<Routes> routes = new ArrayList<Routes>();
//		List<Shape> shapes = new ArrayList<Shape>();
//		
//		while (jsonReader.hasNext()) {
//			String name = "";
//			try {
//				name = jsonReader.nextName();
//				System.out.println("name: " + name);
//			} catch(Exception e) {
//				jsonReader.skipValue();
//			}
//			
//			if (name.equals("success")) {
//				success = jsonReader.nextBoolean();
//				if(success == false) {
//					jsonReader.endObject();
//					jsonReader.close();
//					return false;
//				}
//			}
//			else if(name.equals("values")) {
//				jsonReader.beginObject();
//			}
//			else if(name.equals("routes_array")) {
//				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Starting to process routes_array");
//				jsonReader.beginArray();
//				int i = 0;
//				while (jsonReader.hasNext()) {
//					Routes route = gson.fromJson(jsonReader, Routes.class);
//					routes.add(route);
//					System.out.println("route process = " + ++i + " route name " + route.getRouteName());
////					List<Stop> stops = route.getStops();
//					
//				}
//				jsonReader.endArray();
//			}
//			else if(name.equals("shapes")){
//				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Starting to process shapes");
//				try {
//					jsonReader.beginObject();
//					String shapeObjName = "";
//				
//					while((shapeObjName = jsonReader.nextName()) != null) {
//						System.out.println("new shape obj: " + shapeObjName);
//						jsonReader.beginObject();
//						
//						Shape shape = new Shape();
//						
//						while((shapeObjName = jsonReader.nextName())  != null) {
////							System.out.println("shape name: " + shapeObjName);
//							if(shapeObjName.equals("shape_id")) {
//								String shapeId = jsonReader.nextString();
//								shape.setShapeId(shapeId);
//							}
//							else if(shapeObjName.equals("lat_arr")) {
//								jsonReader.beginArray();
//								List<Double> latitudes = new ArrayList<Double>();
//								while (jsonReader.hasNext()) {
//									latitudes.add(jsonReader.nextDouble());
//								}
//								shape.setLatArr(toDoubleArray(latitudes));
//								jsonReader.endArray();
//							}
//							else if(shapeObjName.equals("lon_arr")) {
//								jsonReader.beginArray();
//								List<Double> longitudes = new ArrayList<Double>();
//								while (jsonReader.hasNext()) {
//									longitudes.add(jsonReader.nextDouble());
//								}
//								shape.setLongArr(toDoubleArray(longitudes));
//								jsonReader.endArray();
//							}
//							else if(shapeObjName.equals("pt_seq_arr")) {
//								jsonReader.beginArray();
//								List<Integer> ptrSeqArr = new ArrayList<Integer>();
//								while (jsonReader.hasNext()) {
//									ptrSeqArr.add(jsonReader.nextInt());
//								}
//								shape.setPtSeqArr(toIntegerArray(ptrSeqArr));
//								jsonReader.endArray();
//								shapes.add(shape);
//								jsonReader.endObject();
//								break;
//							}
//						}
//					}
//					jsonReader.endObject();
//				} catch(Exception e) {
////					e.printStackTrace();
//					System.out.println("Error reading shape json = " + e.getMessage());
//				}
//			}
//		}	
//		
//		dataSaved = true;
//		jsonReader.endObject();
//		jsonReader.close();
//		
//		for(int i = 0; i < routes.size(); i++) {
//			Routes route = routes.get(i);
//			
//			String routeName = route.getRouteName();
//			String id = route.getRouteId();
//			String type = route.getType();
//			String speed = route.getSpeed();
//			String shapeId = route.getShapeId();
//			//If this route does not already exist in DB, getRouteCount() will assign 0
//			int usageCount = transitDB.getRouteCount(id);
//			if(speed == null || speed.equals("null"))
//				speed = "0";
//			transitDB.saveRoutesToDB(routeName, id, String.valueOf(areaId), type, Integer.valueOf(speed), usageCount, shapeId);
//				
//			List<Stop> stops = route.getStops();
//			for(int k = 0; k < stops.size(); k++) {
//				Stop stop = stops.get(k);
//				
//				String stopName = stop.getStopName();
//				String arrivalTime = stop.getArrivalTime();
//				String departureTime = stop.getDepartureTime();
//				String stopId = stop.getStopId();
//				double _stopLat = stop.getStopLat();
//				double _stopLong = stop.getStopLong();
//				
//				int stopLat = (int) (_stopLat*1000000);
//				int stopLong = (int) (_stopLong*1000000);
//				
//				transitDB.saveRouteStopsToDB(stopName, departureTime, arrivalTime, stopId, String.valueOf(areaId), stopLat, stopLong, routeName, id);
//			}
//		}
//		
//		try {
//			for(int m = 0; m < shapes.size(); m++) {
//				Shape shape = shapes.get(m);
//				
//				String shapeId = shape.getShapeId();
//				double[] latitudes = shape.getLatArr();
//				double[] longitudes = shape.getLonArr();
//				int[] sequences = shape.getPtSeqArr();
//				System.out.println("Saving shapeId = " + shapeId);
//				
//				for(int n = 0; n < latitudes.length; n++) {
//					int latitude = (int) (latitudes[n] * 1000000);
//					int longitude = (int) (longitudes[n] * 1000000);
//					int sequence = sequences[n];
//					transitDB.saveShape(latitude, longitude, sequence, shapeId, String.valueOf(areaId));
//				}
//				if(shapes.size() == m+1) {
//					transitDB.setDownload(String.valueOf(areaId), true);
//					return true;
//				}
//			}  
//		} catch(Exception e) {
//			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Error saving shape = " + e.getMessage());
//		}
//		return false;
//	}
//
//	private class ListViewAdapter extends ArrayAdapter<String> {
//		
//		String[] cities;
//		 
//        public ListViewAdapter(Context context, int txtViewResourceId, String[] objects) {
//            super(context, txtViewResourceId, objects);
//            cities = objects;
//        }
//
//        public void setSelected(int selectedItem) {
//        	String cityName = cities[selectedItem];
//        	String cityId = transitDB.getCityId(cityName);
//        	String areaId = transitDB.getAreaIdFromCity(cityName, cityId);
//        	boolean checked = false;
//        	if(checkedCities[selectedItem] == false) {
//        		checked = true;
//        	}
//        	checkedCities[selectedItem] = checked;
//        	transitDB.setDownload(areaId, checked);
//        }
//        
//        @Override
//        public View getView(int position, View view, ViewGroup viewGroup) {
//            LayoutInflater inflater = getLayoutInflater();
//            View listView = inflater.inflate(R.layout.transit_sampling_library_item, viewGroup, false);
//
//            final TextView textView = (TextView) listView.findViewById(R.id.libraryTextView);
//            textView.setText(cities[position]);
//            
//            CheckBox checkBox = (CheckBox) listView.findViewById(R.id.libraryCheckBox);
//            if(checkedCities != null && checkedCities.length > 0) {
//	            boolean checked = checkedCities[position];
//	            checkBox.setChecked(checked);
//            }
//            
//            checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//				@Override
//				public void onCheckedChanged(CompoundButton button, boolean checked) {
//					if(checked == true) {
//						String name = textView.getText().toString();
//						if(name.equals("Vancouver"))
//							name = "GVR";
//						String areaId = transitDB.getAreaId(name);
//						if(areaId == null) //TODO remove
//							areaId = "2";
//						if(areaId != null)
//							getCityInformation(areaId, false);
//					}
//				}
//            });
//
//            return listView;
//        }
//	}
}
