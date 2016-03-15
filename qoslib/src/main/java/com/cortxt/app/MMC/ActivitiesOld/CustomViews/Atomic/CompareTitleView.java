package com.cortxt.app.MMC.ActivitiesOld.CustomViews.Atomic;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class CompareTitleView extends TextView {

	private Context context = null;
	private int titleColor = 0xff5d5d5d;
	private Paint paint = null;
	private Rect textScale = new Rect();
	private String widest = "Call Stats Benchmark";
	
	public CompareTitleView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public CompareTitleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	private void init() {
		float screenDensityScale = getContext().getResources().getDisplayMetrics().density;
		float textSize = 24f * screenDensityScale;
		paint = new Paint();
		paint.setColor(titleColor);
		paint.setTextSize(textSize);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		Typeface robotoRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
		paint.setTypeface(robotoRegular);
		
		// find widest title
		Resources res = getResources();
		String titles[] = {res.getString(R.string.mystats_ranking), res.getString(R.string.mystats_mycallstats), res.getString(R.string.mystats_callstats), res.getString(R.string.mystats_dataspeeds)};
		List<String> a = Arrays.asList(titles);
		widest = Collections.max(a, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return a.length() - b.length();
			}
		});
		
		text = (String) getText();
		setText("");
		// setBackgroundColor(Color.GREEN);
		setTitleColor();
	}

	private String text = null;
	
	private void setTitleColor() {
		String colorStr = getResources().getString(R.string.CUSTOM_COMPARE_TABS_FG_COLOR);
		colorStr = colorStr.length() > 0 ? colorStr  : "3D3D3D";
		int color = Integer.parseInt(colorStr, 16) + (0xff000000);
		setTextColor(color);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		textScale.setEmpty();

		paint.getTextBounds(widest, 0, widest.length(), textScale);
		// vertically center the title text
		int top = getHeight()/2 + textScale.height()/2;
		int left = 0;
		if(text!=null){
			canvas.drawText(text, left + getWidth() / 2, top, paint);
		}
	}
}
