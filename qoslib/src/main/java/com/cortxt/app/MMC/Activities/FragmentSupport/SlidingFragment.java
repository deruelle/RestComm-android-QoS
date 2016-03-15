package com.cortxt.app.MMC.Activities.FragmentSupport;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;

public class SlidingFragment extends Fragment {
	
	View slidingPage = null;
	int customIcons = 0, customLabels = 0, lcolor = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle b = getArguments();
		String ff = MmcConstants.font_Regular;

		customIcons = getResources().getInteger(R.integer.CUSTOM_DASHICONS);
		customLabels = getResources().getInteger(R.integer.CUSTOM_DASHLABELS);
		String dashLabelColor = getResources().getString(R.string.DASH_LABELCOLOR);
		dashLabelColor = dashLabelColor.length() > 0 ? dashLabelColor : "666666";
		if (dashLabelColor.length() > 1)
			lcolor = Integer.parseInt(dashLabelColor, 16) + (0xff000000);

		int hideCompare = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getInt(PreferenceKeys.Miscellaneous.HIDE_COMPARE, 0);
		int hideMap = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getInt(PreferenceKeys.Miscellaneous.HIDE_MAP, 0);


		if(b != null) {
			int id = b.getInt("layoutId");
			slidingPage = inflater.inflate(id, null);

			if(id == R.layout.new_dashboard_icons1) {
				// its first view
				Button mapsIcon = (Button) slidingPage.findViewById(R.id.mapIcon);
				Button speedIcon = (Button) slidingPage.findViewById(R.id.speedIcon);		
				Button compareStatsIcon = (Button) slidingPage.findViewById(R.id.CompareStatsIcon);
				Button troubleTweetIcon = (Button) slidingPage.findViewById(R.id.TroubleTweetIcon);

				TextView mapsText = (TextView) slidingPage.findViewById(R.id.mapText);
				TextView speedText = (TextView) slidingPage.findViewById(R.id.SpeedText);
				TextView compareText = (TextView) slidingPage.findViewById(R.id.CompareText);
				TextView troubleTweetText = (TextView) slidingPage.findViewById(R.id.TroubleTweetText);

				FontsUtil.applyFontToTextView(ff, mapsText, getActivity());
				FontsUtil.applyFontToTextView(ff, speedText, getActivity());
				FontsUtil.applyFontToTextView(ff, compareText, getActivity());
				FontsUtil.applyFontToTextView(ff, troubleTweetText, getActivity());

				customizeIcon(mapsIcon, mapsText, getResources().getInteger(R.integer.DASH_MAPS) == 0 || hideMap == 1, R.drawable.dashcustom_mycoverage, R.string.dashcustom_maps);
				customizeIcon(speedIcon, speedText, getResources().getInteger(R.integer.DASH_SPEED) == 0, R.drawable.dashcustom_speedtest, R.string.dashcustom_speed);
				customizeIcon(compareStatsIcon, compareText, getResources().getInteger(R.integer.DASH_COMPARE) == 0 || hideCompare == 1, R.drawable.dashcustom_mystats, R.string.dashcustom_compare);
				customizeIcon(troubleTweetIcon, troubleTweetText, getResources().getInteger(R.integer.DASH_TROUBLETWEET) == 0, R.drawable.dashcustom_troublespot, R.string.dashcustom_trouble);
			} else if(id == R.layout.new_dashboard_icons2) {
				// its second view with Surveys icon -- we need to hide/unhide Sampling icon so we handle that here
				Button engineeringIcon = (Button) slidingPage.findViewById(R.id.engineeringIcon);
				Button settingsIcon = (Button) slidingPage.findViewById(R.id.SettingsIcon);
				Button rawDataIcon = (Button) slidingPage.findViewById(R.id.RawDataIcon);
				Button surveysIcon = (Button) slidingPage.findViewById(R.id.SurveysIcon);

				TextView engineeringText = (TextView) slidingPage.findViewById(R.id.engineeringText);
				TextView settingsText = (TextView) slidingPage.findViewById(R.id.SettingsText);
				TextView rawDataText = (TextView) slidingPage.findViewById(R.id.RawDataText);
				TextView surveysText = (TextView) slidingPage.findViewById(R.id.SurveysText);

				FontsUtil.applyFontToTextView(ff, engineeringText, getActivity());
				FontsUtil.applyFontToTextView(ff, settingsText, getActivity());
				FontsUtil.applyFontToTextView(ff, rawDataText, getActivity());
				FontsUtil.applyFontToTextView(ff, surveysText, getActivity());

				customizeIcon(engineeringIcon, engineeringText, getResources().getInteger(R.integer.DASH_ENGINEER) == 0, R.drawable.dashcustom_engineering, R.string.dashcustom_engineer);
				customizeIcon(settingsIcon, settingsText, getResources().getInteger(R.integer.DASH_SETTINGS) == 0, R.drawable.dashcustom_settings, R.string.dashcustom_settings);
				customizeIcon(rawDataIcon, rawDataText, getResources().getInteger(R.integer.DASH_RAWDATA) == 0, R.drawable.dashcustom_rawdata, R.string.dashcustom_rawdata);
				customizeIcon(surveysIcon, surveysText, getResources().getInteger(R.integer.DASH_SURVEYS) == 0, R.drawable.dashcustom_surveys, R.string.dashcustom_surveys);
				
				if(!Dashboard.showSurvey) {
					surveysIcon.setVisibility(View.INVISIBLE);
					surveysText.setVisibility(View.INVISIBLE);
                }

				if(!Dashboard.showSurvey && Dashboard.showMapping) {
					// show mapping on second screen
					Button mappingIcon = (Button) slidingPage.findViewById(R.id.mappingIcon);
					TextView mappingText = (TextView) slidingPage.findViewById(R.id.mappingText);
					
					mappingIcon.setVisibility(View.VISIBLE);
					mappingText.setVisibility(View.VISIBLE);
					FontsUtil.applyFontToTextView(ff, mappingText, getActivity());
					customizeIcon(mappingIcon, mappingText, getResources().getInteger(R.integer.DASH_SAMPLING) == 0, R.drawable.dashcustom_sampling, R.string.dashcustom_sampling);
				}
			} else if(id == R.layout.new_dashboard_icons3) {
				// its third view which contains Sampling icon -- we need to hide/unhide Surveys icon
				Button mappingIcon = (Button) slidingPage.findViewById(R.id.mappingIconOn3rd);
				TextView mappingText = (TextView) slidingPage.findViewById(R.id.mappingTextOn3rd);

                if (Dashboard.showSurvey && Dashboard.showMapping)
                {
                    mappingIcon.setVisibility(View.VISIBLE);
                    mappingText.setVisibility(View.VISIBLE);
                    FontsUtil.applyFontToTextView(ff, mappingText, getActivity());
                    customizeIcon(mappingIcon, mappingText, getResources().getInteger(R.integer.DASH_SAMPLING) == 0, R.drawable.dashcustom_sampling, R.string.dashcustom_sampling);

                }
                else
                {
                    mappingIcon.setVisibility(View.INVISIBLE);
                    mappingText.setVisibility(View.INVISIBLE);
                }

                //The Transit icon
                Button transitIcon = (Button) slidingPage.findViewById(R.id.transitIconOn3rd);
                TextView transitText = (TextView) slidingPage.findViewById(R.id.transitTextOn3rd);
                if (Dashboard.showTransit) {
                    transitIcon.setVisibility(View.VISIBLE);
                    transitText.setVisibility(View.VISIBLE);
                    FontsUtil.applyFontToTextView(ff, transitText, getActivity());                                    //TODO get dashcustom_transit
                    //customizeIcon(transitIcon, transitText, getResources().getInteger(R.integer.DASH_TRANSIT) == 0, R.drawable.dashcustom_transit, R.string.dashcustom_transit);
                }
                else
                {
                    transitIcon.setVisibility(View.INVISIBLE);
                    transitText.setVisibility(View.INVISIBLE);
                }

			}
		}
		return slidingPage;
	}
	
	/**
	 * Hides the icon (along with text), if disabled via feature toggle. Applies custom icon, text and/or text color according, if applicable
	 * 
	 * @param icon
	 * @param text
	 * @param hide whether to hide this icon
	 * @param customIconId resource id for custom icon to use
	 * @param customTextId resource text for custom icon to use
	 */
	private void customizeIcon(Button icon, TextView text, boolean hide, int customIconId, int customTextId) {
		if(icon == null) {
			return;
		}
		// hide the icon if disabled via feature toggle
		if (hide) {
			icon.setVisibility(View.GONE);
			if (text != null)
				text.setVisibility(View.GONE);
			return;
		}
		// apply custom icon, text and/or color if selected via feature toggle
		if (customIcons == 1)
			icon.setBackgroundResource(customIconId);
		if (customLabels == 1 && text != null)
			text.setText(getString(customTextId));
		if (lcolor != -1)
			text.setTextColor(lcolor);
	}
	
	public View getPage() {
		return slidingPage;
	}

}
