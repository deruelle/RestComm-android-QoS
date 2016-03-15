package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

public class StatWebView extends StatViewNew {

	private static final String TAG = NerdView.class.getSimpleName();
	private WebView mWebView;
	private StatWebView statWebView;
	private LegendViewNew legendView;
	// progress dialog
    private ProgressDialog progressDialog;
	
	/*
	 * End private variables
	 * ========================================================
	 * Start constructors (and their helper methods)
	 */
	public StatWebView (Context context) {
		super(context);
		this.context = context;
		//init();
		
	}

	public StatWebView (Context context, AttributeSet attrs){
		super(context, attrs);
		this.context = context;
		//init();
	}
	
	public void inflateChild (ViewFlipper parent)
	{
		LinearLayout featureLayout = (LinearLayout) View.inflate(this.getContext(),R.layout.webstats, parent);
	}
	@Override
	public void init() {
		//LinearLayout featureLayout = (LinearLayout) View.inflate(this.getContext(),R.layout.homefeature,null);
		
		//LinearLayout featureLayout = (LinearLayout) findViewById(R.id.stats_webview01);
		//this.inflate (this.getContext(),R.layout.webstats,null);
		mWebView = (WebView) findViewById(R.id.stats_webview);
		//mLoadingIndicator = (ProgressBar) findViewById(R.id.stats_loadingindicator);
		legendView = (LegendViewNew) findViewById(R.id.stats_legend);
		
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setBackgroundColor(0);
		mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		mWebView.getSettings().setSupportZoom(false);
		mWebView.addJavascriptInterface(new JavaScriptInterface(), "javaInterface");
		
	}
	
	public int getIndex (int index)
	{
		return statIndex;
	}
	
	@Override
	public void setIndex (int index)
	{
		statIndex = index;
		mWebView.loadUrl(CompareNew.STATS_URLS[statIndex]);
		
		// set the client
        mWebView.setWebViewClient(new BasicWebViewCient());
	}
	@Override
	public void reload ()
	{
		//mLoadingIndicator.setVisibility(View.VISIBLE);
		loadStatPage ();
	}
	@Override
	public void setParent (CompareNew parent)
	{
		super.setParent(parent);
		loadStatPage ();
	}
	private void loadStatPage ()
	{
		mWebView.loadUrl(CompareNew.STATS_URLS[statIndex]);
		//mLoadingIndicator.setVisibility(View.VISIBLE);
		
		legendView.setStatScreen (statIndex);
	}
	@Override
	public void loadStats ()
	{
		mWebView.loadUrl("javascript:setStats(" + mCompare.getStats() + ")");
		hideLoadingIndicator();
	}
	private void showLocalStats ()
	{
		try{
		legendView.setPieLegend (mCompare.getStats().getJSONObject("yourphone"));
		}catch (Exception e) {}
		mWebView.loadUrl("javascript:setStats(" + mCompare.getStats() + ")");
	}
	
	// Web view client
	private class BasicWebViewCient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }

	    @Override
	    public void onLoadResource(WebView view, String url) {
	        if (progressDialog == null) {
	            progressDialog = new ProgressDialog(mCompare);
	            progressDialog.setMessage("Locating");
	            progressDialog.show();
	        }
	    }

	    @Override
	    public void onPageFinished(WebView view, String url) {
	        if (progressDialog != null && progressDialog.isShowing()) {
	            progressDialog.dismiss();
	        }
	        mWebView.invalidate();
	        mWebView.buildDrawingCache (true);
			Bitmap mScreenshot = mWebView.getDrawingCache();
			mWebView.destroyDrawingCache ();
	    }
	}
	
	final class JavaScriptInterface {
		public void getStats() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if(CompareNew.STATS_URLS[statIndex].equals(CompareNew.MYCALLSTATS_PAGE_URL))
						showLocalStats ();
					else if (mCompare.getStats() != null)
					{
						mWebView.loadUrl("javascript:setStats(" + mCompare.getStats() + ")");
						//mGetStatsTask = new GetStatsTask();
						//mGetStatsTask.execute(statsLocation);
						//showAddress (location);
					}
				}
			});
		}
		
		public String getString(String name) {
			if (name.equals("downloadspeed"))
			{
				int speedtitle = R.string.mystats_downloadspeed;
				switch (mCompare.getSpeedTier())
				{
				case 0: speedtitle = R.string.mystats_downloadspeed; break;
				case 1: case 2: speedtitle = R.string.mystats_downloadspeed2G; break;
				case 3: case 4: speedtitle = R.string.mystats_downloadspeed3G; break;
				case 5: speedtitle = R.string.mystats_downloadspeedLTE; break;
				case 10: speedtitle = R.string.mystats_downloadspeedWifi; break;
				}
				return mCompare.getString(speedtitle);
			}
			return mCompare.getString((int)CompareNew.STRINGS.get(name));
		}
		
		public void hideLoadingIndicator() {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//mLoadingIndicator.setVisibility(View.GONE);
				}}, 1500);
			
		}
	}
	
	
	
}
