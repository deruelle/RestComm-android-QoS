package com.cortxt.app.MMC.Activities.TroubleSpot;

import android.app.Activity;

import com.cortxt.app.MMC.Activities.FragmentSupport.ActivityHostFragment;

public class TroubleSpotMapFragment extends ActivityHostFragment {
    
    @Override
    protected Class<? extends Activity> getActivityClass() {
        return TroubleSpotMapActivity.class;
    }
}
