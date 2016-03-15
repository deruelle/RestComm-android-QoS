package com.cortxt.app.MMC.Sampling.Transit;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.Utils.ScalingUtility;

public class TransitSamplingSettings extends MMCTrackedActivityOld {
	
	private CheckBox autoSpeedtestCheckBox;
	private CheckBox submitStationCheckBox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_settings, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
		
		autoSpeedtestCheckBox = (CheckBox) view.findViewById(R.id.autoSpeedtestCheckBox);
		submitStationCheckBox = (CheckBox) view.findViewById(R.id.submitStationCheckBox);
		
		autoSpeedtestCheckBox.setOnCheckedChangeListener(autoSpeedtestCheckChanged);
		submitStationCheckBox.setOnCheckedChangeListener(submitStationCheckChanged); 
	}
	
	public void onExit(View view) {
		this.finish();
	}
	
	android.widget.CompoundButton.OnCheckedChangeListener autoSpeedtestCheckChanged = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton view, boolean isChecked) {
			//TODO do something with setting
		}
	};
	
	android.widget.CompoundButton.OnCheckedChangeListener submitStationCheckChanged = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton view, boolean isChecked) {
			//TODO do something with setting
		}
	};
}
