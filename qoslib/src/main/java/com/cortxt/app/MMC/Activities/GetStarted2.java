package com.cortxt.app.MMC.Activities;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.conn.HttpHostConnectException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.DeveloperScreenOld;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.securepreferences.SecurePreferences;

public class GetStarted2 extends MMCTrackedActivityOld {
	private EditText mEmail;
	private CheckBox mAcceptTerms;
	private TextView mPolicyLinks;
	private TextView emailEnterStaticText;
	private TextView morePolicyText;
	private TextView acceptAgreementText;
	private Button  continueButton;
	private AsyncTask<Object, Void, Boolean> mRegisterTask;
	private ProgressDialog mProgressDialog;
	private Handler handler = null;
    boolean signedOut = false;  // back here because user hit the sign-out menu
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view  = inflater.inflate(R.layout.registration_screen, null, false);
        setContentView(view);
        MMCActivity.customizeTitleBar (this,view,R.string.dashboard_register, R.string.dashcustom_register);
		int checkAccept = getResources().getInteger(R.integer.REG_ACCEPT);
		int showPolicy = getResources().getInteger(R.integer.REG_SHOWPOLICY);

		mEmail = (EditText) findViewById(R.id.emailEditBox);
		mAcceptTerms = (CheckBox) findViewById(R.id.acceptCheckBox);
		mPolicyLinks = (TextView) findViewById(R.id.privacyTextOne);
		emailEnterStaticText=(TextView)findViewById(R.id.emailEnterStaticText);
		morePolicyText=(TextView)findViewById(R.id.MoreAboutPolicyText);
		acceptAgreementText=(TextView)findViewById(R.id.AcceptAgreementText);
		continueButton=(Button)findViewById(R.id.startButton);
		
		applyFonts();
		
		if (showPolicy == 0) {
			if (morePolicyText != null)
				morePolicyText.setVisibility(View.GONE);
			if (mPolicyLinks != null)
				mPolicyLinks.setVisibility(View.GONE);
			
			ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams)emailEnterStaticText.getLayoutParams();
			margins.topMargin = 200;
			emailEnterStaticText.setLayoutParams(margins);
		} else {
			morePolicyText.setMovementMethod(LinkMovementMethod.getInstance());
			int customPrivacy = (this.getResources().getInteger(R.integer.CUSTOM_PRIVACY));
			if (customPrivacy == 0) {
//				String links = getString(R.string.getstarted_policy_links);
//				links = links.replace("&amp;", "&");
//				morePolicyText.setText(Html.fromHtml(links));
				morePolicyText.setText((Html.fromHtml(getString(R.string.disclaimer_html))));
				mPolicyLinks.setText(Html.fromHtml(getString(R.string.privacy_registration_text_html)));
			} else {
				String links = getString(R.string.getstarted_custom_policy_links);
				links = links.replace("&amp;", "&");
				morePolicyText.setText(Html.fromHtml(links));
				mPolicyLinks.setText(Html.fromHtml(getString(R.string.privacy_custom_registration_text)));
			}
		}
		if (checkAccept == 0)
		{
			if (mAcceptTerms != null)
				mAcceptTerms.setVisibility(View.GONE);
			if (acceptAgreementText != null)	
				acceptAgreementText.setVisibility(View.GONE);
		}
		int customRegister = (this.getResources().getInteger(R.integer.CUSTOM_REGISTER));
		if (customRegister == 1)
			emailEnterStaticText.setText(R.string.getstarted_custom_enteremail);
		int customRegButtonText = (this.getResources().getInteger(R.integer.CUSTOM_REGBUTTONTEXT));
		if (customRegButtonText == 1 && continueButton != null)
			continueButton.setText(R.string.getstarted_customcontinue);
		
        ScalingUtility.getInstance(this).scaleView(view);

        // Could be considered as 'signed-out' with a known login. Display the login
        SharedPreferences securePrefs = MMCService.getSecurePreferences(this);
        signedOut = securePrefs.getBoolean(PreferenceKeys.Miscellaneous.SIGNED_OUT, false);
        String login =  securePrefs.getString(PreferenceKeys.User.USER_EMAIL, null);
        if (signedOut && login != null)
        {
            mEmail.setText (login);
        }
		
		handler = new Handler();
	}
	public void applyFonts(){
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, emailEnterStaticText, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, mPolicyLinks, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, morePolicyText, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, acceptAgreementText, this);
	}

	public void registerClicked(View button) {
		String email = mEmail.getText().toString();
		if(validateEmail(email)) {
			mRegisterTask = new AsyncTask<Object, Void, Boolean>() {
				@Override
				protected void onPreExecute() {
					super.onPreExecute();
					DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							mRegisterTask.cancel(true);
						}
					};
					mProgressDialog = ProgressDialog.show(GetStarted2.this, getString(R.string.getstarted_dialog_title), getString(R.string.getstarted_dialog_message), true, true, cancelListener);
				}
				
				@Override
				protected Boolean doInBackground(Object... params) {
					try {
						Context context = GetStarted2.this.getApplicationContext();
						ReportManager manager = ReportManager.getInstance(context);
						
						String email = (String) params[0];
						MMCDevice device = manager.getDevice();
						
						manager.authorizeDevice(email, true);
						
						return true;
					}
					catch (final MMCException e) {
						
						handler.post(new Runnable() {
							@Override
							public void run(){
								String error = getString(R.string.getstarted_register_error);
								String message = e.getMessage();
								if (e.getMessage().equals("api key was empty"))
									message = getString(R.string.getstarted_register_apikeyempty);
								if (e.getCause() instanceof UnknownHostException || e.getCause() instanceof HttpHostConnectException)
									message = getString(R.string.getstarted_register_unknownhost);
								else if (e.getCause() instanceof IOException)
									message = getString(R.string.getstarted_register_ioexception);
								try{
								new AlertDialog.Builder(GetStarted2.this).setTitle(error).setMessage(message).setNeutralButton(R.string.GenericText_Close, null).show();
								} catch (Exception e)
								{}
							}});
						
						return false;
					}
				}
				
				@Override
				protected void onPostExecute(Boolean succeded) {
					super.onPostExecute(succeded);
					
					if(succeded) { 
						Intent bgServiceIntent = new Intent(GetStarted2.this, MMCService.class);
						startService(bgServiceIntent);
						
						Intent getStarted3Intent = new Intent(GetStarted2.this, Dashboard.class);
						startActivity(getStarted3Intent);
					}
					else {
						Toast.makeText(GetStarted2.this, R.string.getstarted_register_error, Toast.LENGTH_LONG).show();
					}
					try{
					mProgressDialog.dismiss();}
					catch (Exception e)
					{}
				}
			};
			
			mRegisterTask.execute(email);
		}
		else {
			Toast.makeText(GetStarted2.this, getString(R.string.getstarted_invalidemail), Toast.LENGTH_LONG).show();
		}
	}
	public void registerClickedOld(View button) {
		String email = mEmail.getText().toString();
		if(mAcceptTerms.isChecked() || getResources().getInteger(R.integer.REG_ACCEPT) == 0) {
			if(validateEmail(email)) {
				mRegisterTask = new AsyncTask<Object, Void, Boolean>() {
					@Override
					protected void onPreExecute() {
						super.onPreExecute();
						DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								mRegisterTask.cancel(true);
							}
						};
						mProgressDialog = ProgressDialog.show(GetStarted2.this, getString(R.string.getstarted_dialog_title), getString(R.string.getstarted_dialog_message), true, true, cancelListener);
					}
					
					@Override
					protected Boolean doInBackground(Object... params) {
						try {
							Context context = GetStarted2.this.getApplicationContext();
							ReportManager manager = ReportManager.getInstance(context);
							
							String email = (String) params[0];
							MMCDevice device = manager.getDevice();
							
							manager.authorizeDevice(email, true);
							
							//sendSMSToServer();
							
							return true;
						}
						catch (final MMCException e) {
							
							handler.post(new Runnable() {
								@Override
								public void run(){
									String error = getString(R.string.getstarted_register_error);
									String message = e.getMessage();
									if (e.getMessage().equals("api key was empty"))
										message = getString(R.string.getstarted_register_apikeyempty);
									if (e.getCause() instanceof UnknownHostException || e.getCause() instanceof HttpHostConnectException)
										message = getString(R.string.getstarted_register_unknownhost);
									else if (e.getCause() instanceof IOException)
										message = getString(R.string.getstarted_register_ioexception);
									try{
									new AlertDialog.Builder(GetStarted2.this).setTitle(error).setMessage(message).setNeutralButton(R.string.GenericText_Close, null).show();
									} catch (Exception e)
									{}
								}});
							
							return false;
						}
					}
					
					@Override
					protected void onPostExecute(Boolean succeded) {
						super.onPostExecute(succeded);
						
						if(succeded) {
							Intent bgServiceIntent = new Intent(GetStarted2.this, MMCService.class);
							startService(bgServiceIntent);
							
							Intent getStarted3Intent = new Intent(GetStarted2.this, Dashboard.class);
							startActivity(getStarted3Intent);
						}
						else {
							Toast.makeText(GetStarted2.this, R.string.getstarted_register_error, Toast.LENGTH_LONG).show();
						}
						try{
						mProgressDialog.dismiss();}
						catch (Exception e)
						{}
					}
				};
				
				mRegisterTask.execute(email);
			}
			else {
				Toast.makeText(GetStarted2.this, getString(R.string.getstarted_invalidemail), Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.getstarted_mustacceptterms), Toast.LENGTH_LONG).show();
		}
	}
	
	public boolean validateEmail(String email) {
		return email.matches("^(?:[\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\`\\{\\|\\}\\~]+\\.)*[\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\`\\{\\|\\}\\~]+@(?:(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-](?!\\.)){0,61}[a-zA-Z0-9]?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9\\-](?!$)){0,61}[a-zA-Z0-9]?)|(?:\\[(?:(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\]))$");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.log, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.dashboard_menu_log) {
			Intent startDevScreenIntent = new Intent(this, DeveloperScreenOld.class);
			startActivity(startDevScreenIntent);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	//@Override
	//public void onBackPressed() {
//		Intent intent = new Intent(this, GetStarted1.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		startActivity(intent);
	//}
	
	public void sendSMSToMMCPhone() {
//		String phoneNumber = "14039920127";
//		TelephonyManager telephonyManager;
//		try {
//			String message = "\"apiKey\":\"" + PreferenceManager.getDefaultSharedPreferences(this).getString(WebReporter.getAPI_KEY_PREFERENCE(), null) + "\"";
//	//		String message = PreferenceManager.getDefaultSharedPreferences(this).getString(WebReporter.getAPI_KEY_PREFERENCE(), null);
//			telephonyManager =(TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
//			
//			if (telephonyManager.getNetworkOperator() != null && telephonyManager.getNetworkOperator().length() >= 4) {
//				message += ",\"mcc\":" + telephonyManager.getNetworkOperator().substring(0, 3);
//				message += ",\"mnc\":" + telephonyManager.getNetworkOperator().substring(3);
//			}
//			SmsManager sms = SmsManager.getDefault();
//	        sms.sendTextMessage(phoneNumber, null, message, null, null); 
//		}
//		catch(Exception e) {
//			System.out.println(e);
//		}
	}
}
