package com.cortxt.app.MMC.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;

public class GetStarted3 extends MMCTrackedActivityOld {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.getstarted3);
		
		//setTitle(getString(R.string.getstarted_title_get), getString(R.string.getstarted_title_started));
	}

	public void okClicked(View button) {
		Intent intent = new Intent(this, Dashboard.class);
		startActivity(intent);
	}
}
