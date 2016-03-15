package com.cortxt.app.MMC.Sampling.Transit;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class TransitMenuFragment extends ListFragment {
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		View view = inflater.inflate(R.layout.frag_eventhistory, null, false);

		return null;//view;
	}

	public void toggleHistory() {

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

	}
}
