package com.cortxt.app.MMC.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.WebsiteLink;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.UtilsOld.CommonIntentBundleKeysOld;

public class GetStarted1 extends MMCTrackedActivityOld {
	public static final String GETTING_STARTED_URI = "http://www.mymobilecoverage.com/client/android/2.0/gettingstarted.html";
	public static final String VISIT_WEBSITE_URI = "http://www.mymobilecoverage.com";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.getstarted1);
		
		//setTitle(getString(R.string.getstarted_title_get), getString(R.string.getstarted_title_started));
	}
	
	public void gettingStartedClicked(View button) {
		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GETTING_STARTED_URI));
		//startActivity(intent);
		Intent intent = new Intent (this, WebsiteLink.class);
		intent.putExtra(CommonIntentBundleKeysOld.Miscellaneous.WEB_ADDRESS, GETTING_STARTED_URI);
		startActivity(intent);
	}
	
	public void visitWebsiteClicked(View button) {
		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(VISIT_WEBSITE_URI));
		//startActivity(intent);
		Intent intent = new Intent (this, WebsiteLink.class);
		intent.putExtra(CommonIntentBundleKeysOld.Miscellaneous.WEB_ADDRESS, VISIT_WEBSITE_URI);
		startActivity(intent);
	}

	public void skipClicked(View button) {
		Intent intent = null;
		boolean isRegistered = ReportManager.getInstance(getApplicationContext()).isAuthorized();
		if (isRegistered)
		{
			intent = new Intent(this, Dashboard.class);
		}
		else
			intent = new Intent(this, GetStarted2.class);
		startActivity(intent);
	}
}
