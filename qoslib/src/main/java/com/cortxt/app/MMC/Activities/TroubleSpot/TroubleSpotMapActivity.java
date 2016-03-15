package com.cortxt.app.MMC.Activities.TroubleSpot;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedMapActivityOld;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.google.android.maps.MapView;

public class TroubleSpotMapActivity extends MMCTrackedMapActivityOld {

	public MapView mMapView = null;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.troublespot_map_activity, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);

		mMapView = (MapView) findViewById(R.id.troublespot_mapview);
	}
}
