package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class SurveyButton extends RadioButton {

	int[] colors = { 0xff006600, 0xff009933, 0xffffcc00, 0xffff9900, 0xffff0033 };
	int paddingleft = 10;

	ShapeDrawable r2 = null;
	LayoutInflater inflater = null;

	public SurveyButton(Context context) {
		super(context);
		init(context);
	}

	public SurveyButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void init(Context c) {
		// setPadding(30, 30, 30, 30);

		inflater = LayoutInflater.from(c);
		paddingleft = (int)(10*getResources().getDisplayMetrics().density);
		ShapeDrawable r1 = new ShapeDrawable();

		int tag = Integer.parseInt((String) getTag());
		r1.getPaint().setColor(colors[tag]);

		r1.setPadding(paddingleft, 0, 0, 0);

		r2 = new ShapeDrawable();
		r2.getPaint().setColor(0xffe5e5e5);
		r2.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
				getPaddingBottom());

		setTextColor(0xff666666);
		Drawable[] layers = { r1, r2 };
		LayerDrawable ld = new LayerDrawable(layers);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			setBackground(ld);
		} else {
			setBackgroundDrawable(ld);
		}
	}

	private void scaleContent() {
		float size = getHeight() > 60 ? 20 : getHeight()/3;
		setTextSize(size);
		setPadding(getPaddingLeft(), 0, 0, 0);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		scaleContent();
	}

	public void highlight() {
		r2.getPaint().setColor(0xffffffff);
	}

	public void unhighlight() {
		r2.getPaint().setColor(0xffe5e5e5);
	}

}
