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

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.ScalingUtility;

public class TroubleTypeFragment extends Fragment {

	TroubleTypeSelectListener mSelectListener = null;
	TroubleTypeClickHandler mClickHandler = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.frag_trouble_type, null);

		((RelativeLayout) view.findViewById(R.id.droppedCallTrouble)).setOnClickListener(mClickHandler);
		((RelativeLayout) view.findViewById(R.id.FailedCallTrouble)).setOnClickListener(mClickHandler);
		((RelativeLayout) view.findViewById(R.id.DataSessionTrouble)).setOnClickListener(mClickHandler);
		((RelativeLayout) view.findViewById(R.id.noCoveragelayout)).setOnClickListener(mClickHandler);

		Bundle b = getArguments();
		if(b != null && b.containsKey("highlightItem") && !b.getBoolean("highlightItem")) {
			ScalingUtility.getInstance(getActivity()).scaleView(view);
		}
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mClickHandler = new TroubleTypeClickHandler();
		try {
			mSelectListener = (TroubleTypeSelectListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement " + TroubleTypeSelectListener.class.getName());
		}
	}

	class TroubleTypeClickHandler implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mSelectListener != null) {
				mSelectListener.onTroubleTypeSelected((String) v.getTag());
				View view = getView();
				((RelativeLayout) view.findViewById(R.id.droppedCallTrouble)).setBackgroundColor(Color.TRANSPARENT);
				((RelativeLayout) view.findViewById(R.id.FailedCallTrouble)).setBackgroundColor(Color.TRANSPARENT);
				((RelativeLayout) view.findViewById(R.id.DataSessionTrouble)).setBackgroundColor(Color.TRANSPARENT);
				((RelativeLayout) view.findViewById(R.id.noCoveragelayout)).setBackgroundColor(Color.TRANSPARENT);

				Bundle b = getArguments();
				boolean highlight = true;
				if(b != null)
					highlight = b.getBoolean("highlightItem", true);

				if(highlight)
					v.setBackgroundColor(Color.WHITE);
			}
		}
	}

	public interface TroubleTypeSelectListener {
		public void onTroubleTypeSelected(String selectedType);
	}
}
