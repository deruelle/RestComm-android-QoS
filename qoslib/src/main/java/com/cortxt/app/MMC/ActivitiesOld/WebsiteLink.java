package com.cortxt.app.MMC.ActivitiesOld;

import com.cortxt.app.MMC.Activities.GetStarted1;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.UtilsOld.CommonIntentBundleKeysOld;
import com.cortxt.app.MMC.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

/**
 * This class can be used to display a web link to any static text. 
 * Therefore, this activity gets used to show screens like "help",
 * "terms and conditions" etc.
 * @author Abhin
 *
 */
public class WebsiteLink extends MMCTrackedActivityOld{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.websitelink);
		
		//TextView title = (TextView) findViewById(R.id.Website_Title);
		WebView webView = (WebView) findViewById(R.id.webview);
		
		Bundle bundle = this.getIntent().getExtras();
		//WebAddressOld webAddress = (WebAddressOld) bundle.getSerializable(CommonIntentBundleKeysOld.Miscellaneous.WEB_ADDRESS);
		String webAddress = bundle.getString(CommonIntentBundleKeysOld.Miscellaneous.WEB_ADDRESS);
		//title.setText(getString(WebAddressOld.TERMS_AND_CONDITIONS.getTitleTextResource()));
		//webView.loadUrl(getString(WebAddressOld.TERMS_AND_CONDITIONS.getUrlResource()));
		if (bundle != null){
			//title.setText(getString(webAddress.getTitleTextResource()));
			//webView.loadUrl(getString(webAddress.getUrlResource()));
			webView.loadUrl(webAddress);
		}
	}
	@Override
	public void onBackPressed() {
		Intent intent = null;
		boolean isRegistered = ReportManager.getInstance(getApplicationContext()).isAuthorized();
		if (isRegistered)
		{
			super.onBackPressed();
			return;
		}
		else
			intent = new Intent(this, GetStarted1.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
