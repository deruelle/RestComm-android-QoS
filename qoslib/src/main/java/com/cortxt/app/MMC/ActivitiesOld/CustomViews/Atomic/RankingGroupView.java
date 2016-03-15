package com.cortxt.app.MMC.ActivitiesOld.CustomViews.Atomic;

import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.LegendViewNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.NerdView;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatChartNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatViewNew;

public class RankingGroupView extends StatViewNew {

	private static final String TAG = NerdView.class.getSimpleName();
	private LegendViewNew legendView;
	private ProgressDialog progressDialog;

	public RankingGroupView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public RankingGroupView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	StatChartNew[] childViews = null;

	@Override
	public void init() {
		// page title is included in XML layout already
		
		childViews = new StatChartNew[3];
		childViews[0] = (StatChartNew) findViewById(R.id.stats_chart);
		childViews[1] = (StatChartNew) findViewById(R.id.stats_chart2);
		childViews[2] = (StatChartNew) findViewById(R.id.stats_chart3);

		legendView = (LegendViewNew) findViewById(R.id.stats_legend);
	}

	public int getIndex(int index) {
		return statIndex;
	}

	@Override
	public void setIndex(int index) {
		statIndex = index;
	}

	@Override
	public void reload() {
		// mLoadingIndicator.setVisibility(View.VISIBLE);
		loadStatPage();
	}

	@Override
	public void show() {
		for (StatChartNew rcv : childViews) {
			if (rcv != null) {
				rcv.beginAnimation(800, 800, mHandler);
			}
		}
	}

	@Override
	public void onSingleTap(MotionEvent e) {
		Rect outRect = new Rect();
		for (StatChartNew rcv : childViews) {
			if(rcv == null)
				continue;
			rcv.getHitRect(outRect);
			rcv.onSingleTap(e.getX() - outRect.left, e.getY() - outRect.top);
		}
	}

	@Override
	public void setParent(CompareNew parent) {
		super.setParent(parent);

	}

	private void loadStatPage() {
		loadStats();
		if (legendView != null)
			legendView.setStatScreen(statIndex);
	}

	@Override
	public void loadStats() {
		for (StatChartNew rcv : childViews) {
			if (rcv != null)
				rcv.setStats(mCompare.getStats());
		}
		try {
			JSONObject yourStats = mCompare.getStats().getJSONObject(
					"yourphone");
			if (legendView != null)
				legendView.setPieLegend(yourStats);
		} catch (Exception e) {
		}
		hideLoadingIndicator();
	}
}
