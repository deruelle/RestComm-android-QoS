package com.cortxt.app.MMC.ActivitiesOld.CustomViews.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.TroubleSpot.TroubleSpot;
import com.cortxt.app.MMC.Utils.ScalingUtility;

public class TroubleImpactFragment extends Fragment {

	ImpactClickHandler mClickHandler = null;
	TroubleImpactSelectListener mSelectListener = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// View view = getActivity().findViewById(R.id.eventhistoryContainer);
		// ScalingUtility.getInstance(getActivity()).scaleView(view);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.frag_trouble_impact, null);

		((RelativeLayout) view.findViewById(R.id.lowImpactLayout)).setOnClickListener(mClickHandler);
		((RelativeLayout) view.findViewById(R.id.MediumImpactTrouble)).setOnClickListener(mClickHandler);
		((RelativeLayout) view.findViewById(R.id.HighImpactTroubleLayout)).setOnClickListener(mClickHandler);
	
		Bundle b = getArguments();
		if(b != null && b.containsKey("highlightItem") && !b.getBoolean("highlightItem")) {
			ScalingUtility.getInstance(getActivity()).scaleView(view);
		}
		return view;
	}

	class ImpactClickHandler implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if(!((TroubleSpot)getActivity()).typeSelected) {
				// select trouble type first
				// TODO move this string to strings.xml
				Toast.makeText(getActivity(), "Please select issue type first", Toast.LENGTH_SHORT).show();
				return;
			}

			if (mSelectListener != null) {
				mSelectListener.onTroubleImpactSelected((String) v.getTag());
				View view = getView();
				((RelativeLayout) view.findViewById(R.id.lowImpactLayout)).setBackgroundColor(Color.TRANSPARENT);
				((RelativeLayout) view.findViewById(R.id.MediumImpactTrouble)).setBackgroundColor(Color.TRANSPARENT);
				((RelativeLayout) view.findViewById(R.id.HighImpactTroubleLayout)).setBackgroundColor(Color.TRANSPARENT);

				Bundle b = getArguments();
				boolean highlight = true;
				if(b != null)
					highlight = b.getBoolean("highlightItem", true);
				if(highlight)
					v.setBackgroundColor(Color.WHITE);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mClickHandler = new ImpactClickHandler();
		try {
			mSelectListener = (TroubleImpactSelectListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement " + TroubleImpactSelectListener.class.getName());
		}
	}

	public interface TroubleImpactSelectListener {
		public void onTroubleImpactSelected(String selectedImpact);
	}
}
