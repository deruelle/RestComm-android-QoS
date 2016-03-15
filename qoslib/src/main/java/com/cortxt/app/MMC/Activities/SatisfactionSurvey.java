package com.cortxt.app.MMC.Activities;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.SurveyButton;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.app.MMC.UtilsOld.Carrier;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;

public class SatisfactionSurvey extends MMCActivity {

//	public static final String TAG = SatisfactionSurvey.class.getSimpleName();
	private String[] surveyQuestions = null;
	private int[] surveyAnswers = null;
	private String[] ids = null;
	private String[][] answers;	
	private int surveyId = 0, instanceId = 0;
	private static int index;	
	private LinearLayout questionsLayout;
	private TextView surveyTitle;
	private SeekBar surveySeekBar;
	private TextView surveyQuestion;
	private RadioGroup radioGroup;
	private SurveyButton answerOne;
	private SurveyButton answerTwo;
	private SurveyButton answerThree;
	private SurveyButton answerFour;
	private SurveyButton answerFive;
	private int tempSurveyId = 1;

	// private View root;
	// private Animation animation;

	public SatisfactionSurvey() {}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		View view = inflater.inflate(R.layout.satisfaction_survey, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
//		root = view;
        MMCActivity.customizeTitleBar (this,view,R.string.survey_title, R.string.survey_title);
		
		surveyTitle = (TextView) view.findViewById(R.id.surveyTitle);
		surveySeekBar = (SeekBar) view.findViewById(R.id.surveySeekBar);
		surveyQuestion = (TextView) view.findViewById(R.id.surveyQuestion);
		radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
		answerOne = (SurveyButton) view.findViewById(R.id.answerOne);
		answerTwo = (SurveyButton) view.findViewById(R.id.answerTwo);
		answerThree = (SurveyButton) view.findViewById(R.id.answerThree);
		answerFour = (SurveyButton) view.findViewById(R.id.answerFour);
		answerFive = (SurveyButton) view.findViewById(R.id.answerFive);
		questionsLayout = (LinearLayout) view.findViewById(R.id.questionsLayout);
		// progress = (ImageView) view.findViewById(R.drawable.stripe_bg);
		// animation = AnimationUtils.loadAnimation(this, R.anim.radio_clicked);
		surveySeekBar.setClickable(false);
		surveySeekBar.setFocusable(false);
		surveySeekBar.setEnabled(false);
		// surveySeekBar.setBackgroundColor(Color.rgb(85, 85, 85));

		// if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
		// Typeface myFont = Typeface.createFromAsset(getAssets(),"RobotoCondensed.ttf");
		// surveyTitle.setTypeface(myFont);
		// surveyQuestion.setTypeface(myFont);
		// answerOne.setTypeface(myFont);
		// answerTwo.setTypeface(myFont);
		// answerThree.setTypeface(myFont);
		// answerFour.setTypeface(myFont);
		// answerFive.setTypeface(myFont);
		// }

		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, surveyTitle, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, surveyQuestion, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, answerOne, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, answerTwo, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, answerThree, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, answerFour, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, answerFive, this);

		// FontsUtil.applyFontToTextView(MmcConstants.font_Regular, surveyQuestion, this);
		// FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, surveyTitle, this);

		index = 0;
//		Intent intent = getIntent();
//		if (intent.hasExtra("id"))
//			surveyId = intent.getIntExtra("id", 0);
//		if(surveyId == 0)
//			surveyId = tempSurveyId;
		//requestQuestions(surveyId);
		String survey = PreferenceManager.getDefaultSharedPreferences(this).getString("survey", null);
		instanceId = PreferenceManager.getDefaultSharedPreferences(this).getInt("surveyid", 0);
		startSurvey(survey);
	}
		
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	if(index == 0 || (index == getQuestions().length)) {
				this.finish();
			}
			else {
				index--;
				Drawable thumb = getResources().getDrawable(R.drawable.seek_thumb_normal); 				
				surveySeekBar.setThumb(thumb); 
				populateSurvey(index);		
				displayLastAnswerInWhite(index);			
			}
	    }
	    return false;
//	    return super.onKeyDown(keyCode, event);
	}
	
	public void setQuestions(String[] newQuestions) {
		this.surveyQuestions = newQuestions;
	}
	
	public String[] getQuestions() {
		return this.surveyQuestions;
	}
	
	public void setAnswers(String[][] newAnswers) {
		this.answers = newAnswers;
	}
	
	public String getAnswers(int index) {
		if(index >= answers.length)
			return null;
		else
			return this.answers[index][0];
	}
	
	public void setSurveyAnswer(int index, int answer) {
		if(answer < 0 || index >= surveyAnswers.length)
			answer = 0;
		if (index < surveyAnswers.length)
			this.surveyAnswers[index] = answer;
	}
	
	public int[] getSurveyAnswers() {
		return this.surveyAnswers;
	}
	
	public int getSurveyAnswer(int index) {
		return this.surveyAnswers[index];
	}
	
	public void setIds(String[] newIds) {
		this.ids = newIds;
	}
	
	public String getId(int index) {
		return this.ids[index];
	}
	
	public void setDefaultAnswers(int[] types) {
		
		answers = new String[types.length][1];
		for(int i = 0; i < types.length; i++) {
			String[] defaultAnswers = null;
			
			switch(types[i]) {
			case 1:
				defaultAnswers = getResources().getStringArray(R.array.survey_answers1);
				break;
			case 2:
				defaultAnswers = getResources().getStringArray(R.array.survey_answers2);
				break;
			case 3:
				defaultAnswers = getResources().getStringArray(R.array.survey_answers3);
				break;
			case 4:
				defaultAnswers = getResources().getStringArray(R.array.survey_answers4);
				break;
			}
			
			String temp = "";
			for(int k = 0; k < defaultAnswers.length; k++) {
				temp += defaultAnswers[k];
				if(k != defaultAnswers.length-1) {
					temp += ",";
				}
			}			
			answers[i][0] = temp;
		}
	}
	
	//Populate the xml file with the corresponding questions
	public void populateSurvey(int index) {
		int size = getQuestions().length;
		System.out.println("index " + index);
		
		surveyTitle.setText(getString(R.string.survey_question_title) + " " + (index + 1)+ " of " + size);
		if(index < size)
			surveySeekBar.setProgress(index+1);
		if(index == size) {
			surveySeekBar.setThumb(null); 
		}
		if(index < size) {
			surveyQuestion.setText(getQuestions()[index]);			
			
			String temp = answers[index][0];
			temp = temp.replace("{", "");
			temp = temp.replace("}", "");
			temp = temp.replace("[", "");
			temp = temp.replace("]", "");
			temp = temp.replace("\"", "");		
			String[] options = temp.split(",");

			if (options.length > 0)
				answerOne.setText(options[0]);
			if (options.length > 1)
				answerTwo.setText(options[1]);
			if (options.length > 2)
				answerThree.setText(options[2]);
			else
			{
				answerThree.setVisibility(View.GONE);
				answerTwo.setTag("4");
				answerTwo.init(this);
			}
			if (options.length > 3)
				answerFour.setText(options[3]);
			else
			{
				answerFour.setVisibility(View.GONE);
				if (options.length == 3)
				{
					answerTwo.setTag("2");
					answerThree.setTag("4");
					answerTwo.init(this);
					answerThree.init(this);
				}
			}
			if (options.length > 4)
				answerFive.setText(options[4]);
			else
			{
				answerFive.setVisibility(View.GONE);
				if (options.length == 4)
				{
					answerTwo.setTag("1");
					answerThree.setTag("3");
					answerFour.setTag("4");
					answerTwo.init(this);
					answerThree.init(this);
					answerFour.init(this);
				}
			}
		}
	}

	public void displayLastAnswerInWhite(int index) {

		removeAllRadioBackgrounds();
		int lastAnswer = getSurveyAnswer(index);
		switch(lastAnswer) {
			case 1:
//				answerOne.setBackgroundColor(Color.WHITE);
				answerOne.highlight();
				break;
			case 2:
//				answerTwo.setBackgroundColor(Color.WHITE);
				answerTwo.highlight();
				break;
			case 3:
//				answerThree.setBackgroundColor(Color.WHITE);
				answerThree.highlight();
				break;
			case 4:
//				answerFour.setBackgroundColor(Color.WHITE);
				answerFour.highlight();
				break;
			case 5:
//				answerFive.setBackgroundColor(Color.WHITE);
				answerFive.highlight();
				break;
		}
	}
	
	public void removeAllRadioBackgrounds() {
//		answerOne.setBackgroundColor(Color.rgb(229, 229, 229)); 
		answerOne.unhighlight();
//		answerTwo.setBackgroundColor(Color.rgb(229, 229, 229)); 
		answerTwo.unhighlight();
//		answerThree.setBackgroundColor(Color.rgb(229, 229, 229)); 
		answerThree.unhighlight();
//		answerFour.setBackgroundColor(Color.rgb(229, 229, 229)); 
		answerFour.unhighlight();
//		answerFive.setBackgroundColor(Color.rgb(229, 229, 229)); 
		answerFive.unhighlight();
	}
	
/*	public void requestQuestions(int surveyId) {
		//Request questions from server, otherwise use strings file
		
		final String urlExtra = getUrlString(surveyId);
		if(urlExtra != null) {	
			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {				
					String responseContents = "";
					try { 
						String url = getApplicationContext().getString(R.string.MMC_URL_LIN) + urlExtra;
						//Timeouts
						HttpParams httpParameters = new BasicHttpParams();
						int timeoutConnection = 10000;
						HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
						int timeoutSocket = 10000;
						HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);	
						
						DefaultHttpClient client = new DefaultHttpClient(httpParameters);						
						HttpGet get = new HttpGet(url); 					
						get.setHeader("Content-Type", "application/json; charset=utf-8");
						HttpResponse response = null;
						response = client.execute(get);
						responseContents = EntityUtils.toString(response.getEntity());	
						System.out.println(url);
						System.out.println(responseContents);						
					} catch(Exception e) {
						System.out.println(e);
						//Server request failed					
					}
					return responseContents;
				}
				
				@Override
				protected void onPostExecute(String result) {
					if(!validateResponse(result)) {
						setQuestions(getResources().getStringArray(R.array.survey_questions));
						int[] defaultAnswerTypes = {3,2,4,2,2,1};
						setDefaultAnswers(defaultAnswerTypes);						
					}
					
					surveySeekBar.setMax(getQuestions().length+1);
					surveySeekBar.setProgress(0);
					surveyAnswers = new int[getQuestions().length];
					populateSurvey(index);	
					
					radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					    public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
					    	
					    	removeAllRadioBackgrounds();
							
					    	if(checkedId == 0) 
					    		return;
					    					    	
					    	int size = getQuestions().length;
					    	
							//Save answer to the current question
							if(index < size)
								storeAnswer();	
							
							
							index++;	
							
							//Show the next question
							if(size > index) {
//								displayAnswerInWhite(index);	
//								root.invalidate();
								 try {
									Thread.sleep(200);		
								} catch (InterruptedException e) { }
								 populateSurvey(index);	
							}	
							//End the survey
							else if(index == size) {
								sendAnswers();
								done();
							}				
					    }
					});
				}
			}.execute((Void[])null); 
		}
	} */

	public void startSurvey(String result) {
		if(!validateResponse(result)) {
			setQuestions(getResources().getStringArray(R.array.survey_questions));
			int[] defaultAnswerTypes = {3,2,4,2,2,1};
			setDefaultAnswers(defaultAnswerTypes);	
			String[] defaultIds = {"1","2","3","4","5","6"};
			setIds(defaultIds);
		}
		
		surveySeekBar.setMax(getQuestions().length+1);
		surveySeekBar.setProgress(0);
		surveyAnswers = new int[getQuestions().length];
		populateSurvey(index);	
		
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
		    public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
		    	
		    	removeAllRadioBackgrounds();
				
		    	if(checkedId == 0) 
		    		return;
		    					    	
		    	int size = getQuestions().length;
		    	
				//Save answer to the current question
				if(index < size)
					storeAnswer();					
				
				index++;	
				
				//Show the next question
				if(size > index) {
//					displayAnswerInWhite(index);	
//					root.invalidate();
					 try {
						Thread.sleep(200);		
					} catch (InterruptedException e) { }
					 populateSurvey(index);	
				}	
				//End the survey
				else if(index == size) {
					sendAnswers();
					done();
					PreferenceManager.getDefaultSharedPreferences(SatisfactionSurvey.this).edit().putBoolean(
            				PreferenceKeys.Miscellaneous.SURVEY_COMMAND, false).commit();
				}	
		    }
		});
	}
	
/*	private String getUrlString(int surveyId) {
		String apiKey = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(PreferenceKeys.User.API_KEY, null);
		if(apiKey != null) {
//			return "/api/osm/building?longitude=" + longitude + "&latitude=" + latitude + "&precision=10&apiKey=" + apiKey;
			return "/api/surveys/questions?surveyid=" + tempSurveyId + "&apiKey=" + apiKey;
		}		
		return null;
	} */

	public boolean validateResponse(String response) {
		JSONObject json = null;
		JSONArray jsonArray;	
		
		if(response == null)
			return false;
		
		try {
			json = new JSONObject(response);	
			
			String success = json.getString("success");
			
			if(success.equals("false"))
				return false;
			
			jsonArray = json.getJSONArray("values");
			if (jsonArray.length() == 0)
				return false;
			String[][] answers = new String[jsonArray.length()][1];
			String[] questions = new String[jsonArray.length()];
			String[] ids = new String[jsonArray.length()];
			
			for(int i = 0; i < jsonArray.length(); i++) {	
				json = jsonArray.getJSONObject(i);
				answers[i][0] = json.getString("answers");
				questions[i] = json.getString("question");
				ids[i] = json.getString("id"); //question ids
				if (surveyId == 0)
				{
					surveyId = json.getInt("surveyid");
				}
			}
			setQuestions(questions);
			setAnswers(answers);
			setIds(ids);
		}
		catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void sendAnswers() {
		
		new Thread(new Runnable() {
	        public void run() {

				JSONObject respData = toJSON();
				Carrier currentCarrier = ReportManager.getInstance(getApplicationContext()).getCurrentCarrier();
				int carrierid = 0;
				if (currentCarrier != null)
					carrierid = currentCarrier.ID;
				try {
					respData.put("carrierid", carrierid);
					byte[] respBytes = respData.toString().getBytes();

					URL url = new URL(getString(R.string.MMC_URL_LIN) + "/api/surveys/responses");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(15000);
					conn.setRequestMethod("POST");
					conn.setDoInput(true);
					conn.setDoOutput(true);
					conn.setFixedLengthStreamingMode(respBytes.length);

					conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

					//open
					conn.connect();

					//setup send
					OutputStream os = new BufferedOutputStream(conn.getOutputStream());
					os.write(respBytes);
					//clean up
					os.flush();
					String responseContents = WebReporter.readString(conn);

				} catch (Exception e1) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, "SatisfactionSurvey", "sendAnswers", "exception", e1);
				}
			}
		}).start();
	}	
	
	public void storeAnswer() {
		int selectedAnswer = radioGroup.getCheckedRadioButtonId();
		if (selectedAnswer == R.id.answerOne) {//				answerOne.setBackgroundColor(Color.WHITE);
			answerOne.highlight();
			selectedAnswer = 1;

		} else if (selectedAnswer == R.id.answerTwo) {//				answerTwo.setBackgroundColor(Color.WHITE);
			answerTwo.highlight();
			selectedAnswer = 2;

		} else if (selectedAnswer == R.id.answerThree) {//				answerThree.setBackgroundColor(Color.WHITE);
			answerThree.highlight();
			selectedAnswer = 3;

		} else if (selectedAnswer == R.id.answerFour) {//				answerFour.setBackgroundColor(Color.WHITE);
			answerFour.highlight();
			selectedAnswer = 4;

		} else if (selectedAnswer == R.id.answerFive) {//				answerFive.setBackgroundColor(Color.WHITE);
			answerFive.highlight();
			selectedAnswer = 5;

		}
		setSurveyAnswer(index, selectedAnswer);
		radioGroup.check(0);
	}
	
	public JSONObject toJSON() {
		int size = getQuestions().length;
		JSONObject data = null;
		JSONObject allData = null;
		String apiKey = MMCService.getApiKey(this);
		JSONArray jsonArray = new JSONArray();
		try {
			for (int i = 0; i < size; i++) {
				data = new JSONObject();			
				data.put("questionid", getId(i)); //question ids
				data.put("value", getSurveyAnswer(i));			
				jsonArray.put(data);
			}
			allData = new JSONObject();		
			allData.put("surveyid", surveyId);
			allData.put("instanceid", instanceId);
			
			allData.put("apiKey", apiKey);
			allData.put("answers", jsonArray);
		} catch (JSONException e) {
//			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "toJSON", e.getMessage());
		}		
		return allData;
	}	
	
	public void surveyBackActionClicked(View view){
		this.finish();
	}
	
	public void done() {		
		MarginLayoutParams linearParams = (MarginLayoutParams) surveyQuestion.getLayoutParams();
		linearParams.leftMargin *= 10;
		linearParams.rightMargin *= 10;
		linearParams.topMargin = 80;
		surveyQuestion.setLayoutParams(linearParams);	
		
		surveyQuestion.setBackgroundColor(Color.WHITE);
		surveyQuestion.setText(getString(R.string.survey_done1) + "\n" + getString(R.string.survey_done2));
//		surveySeekBar.setBackgroundColor(Color.rgb(61, 61, 61));
		surveySeekBar.setMax(getQuestions().length);
//		surveySeekBar.setBackgroundColor(Color.rgb(61, 61, 61));
//		Drawable thumb = getResources().getDrawable(R.drawable.ic_stat_notification_icon); 
		// TODO; following line throws NullPointerException -- fix this
//		surveySeekBar.setThumb(null); 
//		thumb.mutate().setAlpha(0);
		surveyTitle.setText(getString(R.string.survey_done_title));
		questionsLayout.setVisibility(View.GONE);
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("survey", null).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("surveyid", 0).commit();
	}
	
	public void delay() {		
//		new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(2000);  
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
	}
	
	private String getUrlString(int surveyId) {
		int tempSurveyId = 0;
		//Request questions from server, otherwise use strings file
		String apiKey = MMCService.getApiKey(this);//DefaultSharedPreferences(this).getString(PreferenceKeys.User.API_KEY, null);
		if(apiKey != null) {
			if(surveyId != 0)
				tempSurveyId = surveyId;
			return "/api/surveys/questions?instanceid=" + tempSurveyId + "&apiKey=" + apiKey;
		}
		return apiKey;
	}
	
	public void requestQuestions(int surveyId) {
		//Request questions from server, otherwise use strings file
		
		final String urlExtra = getUrlString(surveyId);
		if(urlExtra != null) {	
			TaskHelper.execute(
			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {				
					String responseContents = "";
					try { 
						String url = getString(R.string.MMC_URL_LIN) + urlExtra;
						responseContents = WebReporter.getHttpURLResponse(url, false);
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
					} catch(Exception e) {
						System.out.println("error");				
					}
					return responseContents;
				}
				
				@Override
				protected void onPostExecute(String result) {
					if(result != null)
						PreferenceManager.getDefaultSharedPreferences(SatisfactionSurvey.this).edit().putString("survey", result).commit();		
				}			
			});
		}
	}
}
