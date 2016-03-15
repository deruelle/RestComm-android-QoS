package com.cortxt.app.MMC.ActivitiesOld.CustomViews.Atomic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.AttributeSet;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatChartNew;
import com.cortxt.app.MMC.ServicesOld.MMCPhoneStateListenerOld;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class RankingCatView extends StatChartNew {

	/*
	 * ========================================================== Start private
	 * variables
	 */

	private static int BAR_SPACING = 10;

	public static final float TITLE_FONT_SIZE = 16.0f;
	public static final float RANK_FONT_SIZE = 14.0f;
	/**
	 * Tag for debugging
	 */
	private static final String TAG = RankingCatView.class.getSimpleName();

	// This is a general time variable that will be constantly "setToNow"
	Time time = new Time(Time.getCurrentTimezone());

	StatCategory[] categories = new StatCategory[4];
	int[] colors = { Color.argb(0xFF, 0x00, 0x66, 0xCC), Color.argb(0xFF, 0x99, 0x99, 0x99), Color.argb(0xCC, 0x99, 0x99, 0x99), Color.argb(0x99, 0x99, 0x99, 0x99),
			Color.argb(0x60, 0x99, 0x99, 0x99), Color.rgb(0x21, 0x54, 0x91) };
	int[] colors2 = { Color.argb(0, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00),
			Color.rgb(0x0, 0x0, 0x0) };

	float mYMax = 100;
	float[] colsX = new float[5];
	float[] rowsY = new float[4];
	private Bitmap[] icons = new Bitmap[4];
	// iCategories = 3, since we don't have to draw rankings for Upload Speed
	int iCategories = 3, iRankings = 3;
	private boolean bGradient = false;

	private Typeface robotoRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
	private Typeface robotoLight = FontsUtil.getCustomFont(MmcConstants.font_Light, context);
	private Typeface robotoMedium = FontsUtil.getCustomFont(MmcConstants.font_MEDIUM, context);

	int[] newTextColors = { 0xff5d5d5d, 0xff666666 };

	/*
	 * Text sizes for title, lable and percentages -- note that this might
	 * change in setHeightRatio depending on screen height. [0] for title, [1]
	 * for chart labels, [2] for percentages
	 */
	float[] newTextSize = { 24f * screenDensityScale, 18f * screenDensityScale, 14f * screenDensityScale };

	private String PAGE_TITLE = context.getString((int) CompareNew.STRINGS.get("ranking"));
	private String maxSectionTitle = "Lowest Dropped Calls";
	boolean contentScaled = false;
	float colSpace = 0f;
	float separator = 5f;
	float hSeparator = 6f;
//	float lineHeight = 2f;
	float logoHeight = 40f; // operator logo
	Rect textScale = new Rect();
	private Rect percentRect = new Rect();
	// get the widest operator name.. we need all highlighting rectangles of
	// same width
	private String widestOpName = "Verizon Wireless";
	private int opNameMaxLen = 18;
	private int highlightColor = 0xffffffff;
	private int highlightFGColor = 0xff666666;

	boolean opLogoOnTop = true;
	float maxRight = 0f;

	/*
	 * End private variables
	 * ========================================================== Start
	 * constructors
	 */
	public RankingCatView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public RankingCatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	private void init() {
		opLogoOnTop = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		screenDensityScale = getContext().getResources().getDisplayMetrics().density;
		paint = new Paint();
		paint.setAntiAlias(true);
		// CHART_TO_SCREEN_RATIO for this view is ignored -- see onMeasure() and onMeasureHeight() below
		CHART_LEFT_PADDING = 10;
		CHART_RIGHT_PADDING = 10;
		// CHART_MIN_HEIGHT = 390;
		// CHART_MAX_HEIGHT = 400;
		CHART_BOTTOM_PADDING = 0;
		BAR_SPACING = 10;
		setColors();

		float density = getResources().getDisplayMetrics().density;
		BitmapFactory.Options mNoScale = new BitmapFactory.Options();
		mNoScale.inScaled = false;
		icons[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ribbon, mNoScale);
		icons[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ribbon, mNoScale);
		icons[2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ribbon, mNoScale);
		icons[3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.ribbon, mNoScale);
		if (density != 1.5) {
			for (int i = 0; i < 4; i++)
				icons[i] = Bitmap.createScaledBitmap(icons[i], (int) (51.0 * density / 1.5), (int) (64.0 * density / 1.5), true);
		}
	}

	private void setColors() {
		int customHighlight = getResources().getInteger(R.integer.CUSTOM_RANKED_ITEM_HIGHLIGHT);
		if (customHighlight >= 0 && customHighlight <= 0xffffff) {
			highlightColor = customHighlight + 0xff000000;
		}
		int customFGHighlight = getResources().getInteger(R.integer.CUSTOM_RANKED_ITEM_HIGHLIGHT_FG);
		if (customHighlight >= 0 && customFGHighlight <= 0xffffff) {
			highlightFGColor = customFGHighlight + 0xff000000;
		}
		String headingColor = getContext().getResources().getString(R.string.HEADING_COLOR);
		headingColor = headingColor.length() > 0 ? headingColor : "5d5d5d";
		newTextColors[0] = Integer.parseInt(headingColor, 16) + (0xff000000);

	}

	@Override
	public void onSingleTap(float x, float y) {

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int calculatedWidth = measureWidth(widthMeasureSpec);
		int calculatedHeight = measureHeight(heightMeasureSpec);
		setMeasuredDimension(calculatedWidth, calculatedHeight);
	}

	private int measureHeight(int heightMeasureSpec) {
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);
		int measuredHeight = specSize;
		measuredHeight -= (CHART_BOTTOM_PADDING + CHART_TOP_PADDING) * screenDensityScale;
		if (screenDensityScale > 1.5)
			measuredHeight -= 20 * screenDensityScale;
		return measuredHeight;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		int i = 0;
		chartHeight -= 9 * screenDensityScale / 9;
		// if (screenDensityScale > 1.5)
		// chartHeight -= 10 * screenDensityScale;
		if (screenDensityScale < 1.5)
			iCategories = 3;
		for (i = 0; i < iCategories; i++) {
			rowsY[i] = (chartHeight) * i / iCategories + 6 * screenDensityScale;
		}
		colsX[0] = CHART_LEFT_PADDING * screenDensityScale;
		colsX[1] = CHART_LEFT_PADDING * screenDensityScale + chartWidth * 47 / 100;
		colsX[2] = colsX[1] + 12.0f * screenDensityScale;
		colsX[3] = colsX[1] + 65.0f * screenDensityScale;
		colsX[4] = CHART_LEFT_PADDING * screenDensityScale + chartWidth;
	}

	/*
	 * Compare Screen parent sends the json statistics to this view
	 */
	@Override
	public void setStats(JSONObject stats) {
		super.setStats(stats);
		if (stats == null)
			return;
		mStats = stats;
		// For Dataspeed rankings, detects if the phone is currently in 2G,3G
		// coverage etc
		int networkType = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();
		int speedTier = MMCPhoneStateListenerOld.getNetworkGeneration(networkType);

		String speedkey = "3G";
		switch (speedTier) {
		case 1:
		case 2:
			speedkey = "2G";
			break;
		case 5:
			speedkey = "LTE";
			break;
		}

		// Now for speed statistics, it will adjust the key for the type of
		// network these are keys in the main JSON statistics object
		String[] keys = { "covService", "dropPercent", "download" + speedkey, "upload" + speedkey };
		int id = getId();
		int keyIndex = -1;
		if (id == R.id.stats_chart) {
			keyIndex = 0;
		} else if (id == R.id.stats_chart2) {
			keyIndex = 1;
		} else if (id == R.id.stats_chart3) {
			keyIndex = 2;
		}
		// these are the titles for the 4 categories of statistics
		String[] titles = { context.getString(R.string.rankings_coverage), context.getString(R.string.rankings_dropped), speedkey + " " + context.getString(R.string.rankings_download),
				speedkey + " " + context.getString(R.string.rankings_upload) };

		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
		if (customTitles == 1) {
			String[] custTitles = { context.getString(R.string.rankings_custom_coverage), context.getString(R.string.rankings_custom_dropped),
					speedkey + " " + context.getString(R.string.rankings_custom_download), speedkey + " " + context.getString(R.string.rankings_custom_upload) };
			titles = custTitles;
		}
		boolean[] descending = { true, false, true, true };
		int i;

		// Build the 4 statistics categories ready to draw the 4 sections of the
		// rankings screen

		if (keyIndex > -1) {
			categories[0] = new StatCategory(mStats, keys[keyIndex], titles[keyIndex], descending[keyIndex]);
		}
		shaders = null;
		invalidate();
	}

	/*
	 * Set heights relative to screen height for texts, separators and bars
	 */
	private void scaleContent() {
		paint.setTextSize(newTextSize[2]);
		paint.setTypeface(robotoMedium);
		percentRect.setEmpty();
		paint.getTextBounds("99.9%", 0, 5, percentRect);

		chartHeight = getHeight();
		chartWidth = getWidth();

		float titleHeight = (chartHeight / 10f);

		// colSpace space available on screen for width of the horizontal column
		// colScale is width of horizontal column if it were not restricted by
		// screen size
		float colScale = newTextSize[0] + separator + separator + 4 * (separator + newTextSize[2]);
		float hRatio = (chartHeight / colScale) * screenDensityScale;
		if (hRatio < 0.7f)
			hRatio = 0.7f; // above line made text microscopic in ldpi (ratio =
							// 0.5)
		newTextSize[1] = 18f * hRatio;
		separator = 5f * hRatio;
		hSeparator = 6f * hRatio;

		float scaledWidth = calcTextWidth(PAGE_TITLE, robotoRegular, 24f);
		newTextSize[0] = calcFontSize(PAGE_TITLE, scaledWidth, titleHeight, robotoRegular, 24f * hRatio);
		newTextSize[1] = calcFontSize(maxSectionTitle, chartWidth * 0.65f, 16f * hRatio, robotoLight, 16f * hRatio);
		newTextSize[2] = calcFontSize(" 3 99.9% Verizon Wireless", chartWidth * 0.6f, 14f * hRatio, robotoLight, 14f * hRatio);

		percentRect.setEmpty();
		paint.setTextSize(newTextSize[2]);
		// again, to get text bounds after scaling contents
		paint.getTextBounds("99.9%", 0, 5, percentRect);

		// do this after second getTextBounds("99.9%"....)
		logoHeight = colSpace - separator * 2 - newTextSize[1];

		paint.setTextSize(newTextSize[2]);
		paint.setTypeface(robotoRegular);
		textScale.setEmpty();
		paint.getTextBounds("5", 0, 1, textScale);
		float percentLeft = CHART_LEFT_PADDING * screenDensityScale + textScale.width() + hSeparator;
		float opNameLeft = percentLeft + percentRect.width() + hSeparator;

		textScale.setEmpty();
		paint.getTextBounds(widestOpName + "g", 0, widestOpName.length() + 1, textScale);
		maxRight = opNameLeft + textScale.width() + hSeparator;
		
		contentScaled = true;
	}

	/*
	 * End Constructors
	 * =========================================================== Start
	 * over-ridden methods (from view)
	 */

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (categories[0] == null) {
			// stats not yet populated
			return;
		}
		if (!contentScaled) {
			scaleContent();
		}

		float top = 0;// CHART_TOP_PADDING * 3 * screenDensityScale;

		// TODO remove following lines
		// paint.setColor(Color.YELLOW);
		// canvas.drawRect(left, top, left + chartWidth, top + chartHeight,
		// paint);

		// Paint settings for title 'Ranking'
		paint.setColor(newTextColors[0]);
		paint.setTextSize(newTextSize[0]); // 24
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTypeface(robotoRegular);
		textScale.setEmpty();

		paint.setTextAlign(Paint.Align.LEFT);

		drawCategory(canvas, categories[0], top, "%");
	}

	private float drawCategory(Canvas canvas, StatCategory category, float top, String suffix) {
		float left = CHART_LEFT_PADDING * screenDensityScale;

		// TODO remove followning 2 lines
		// paint.setColor(0x330000ff);
		// canvas.drawRect(0, top, getRight(), getBottom(), paint);

		paint.setColor(newTextColors[0]);
		paint.setTextSize(newTextSize[1]); // 18
		paint.setTypeface(robotoLight);
		textScale.setEmpty();
		paint.getTextBounds("Best Coverage", 0, "Best Coverage".length(), textScale);

		if (opLogoOnTop) {
			// ---------- draw operator logo --------------------
			top = drawLogo(canvas, category.bestlogo, top);
//			top = drawLogo(canvas, BitmapFactory.decodeResource(getResources(), R.drawable.claro_logo), top);
		}

		top += separator;
		float iconRight = drawIcon(canvas, left, top + separator / 2);
		top += textScale.height(); // update y-coord for text label
		canvas.drawText(category.title, iconRight + hSeparator, top, paint);

		top += separator;
		canvas.drawLine(left, top, maxRight, top, paint);
		paint.getStrokeWidth();

		top += separator/2;


		if (!opLogoOnTop) {
			// ---------- draw operator logo --------------------
			drawLogo(canvas, category.bestlogo, top);
//			drawLogo(canvas, BitmapFactory.decodeResource(getResources(), R.drawable.claro_logo), top);
		}

		paint.setTextSize(newTextSize[2]);
		// ----------- draw rank items ----------------------
		float percent = animatePercentage();
		// draw rank items
		for (int i = 0; i < iRankings; i++) {
			Ranking ranking = null;
			if (category.rankings.size() > i)
				ranking = category.rankings.get(i);
			if (ranking == null)
				continue;
			if (percent < (i + 1) * 10) {
				break;
			}
			if (ranking != null) {
				String percentVal = "";
				// draw the value of the statistic to the left of the name (or
				// '-' if not applicable)
				if (ranking.value == -1 || ranking.value == 99999999 || (ranking.value == 0)) {
					percentVal = "- ";
				} else {
					if (ranking.value == 100f) {
						percentVal = "100";
					} else {
						percentVal = Float.toString(ranking.value);
					}
					percentVal += suffix;
				}
				top = drawRankItem(canvas, new String[] { i + 1 + "", percentVal, ranking.name }, left + hSeparator, top, maxRight, i == 0);
			}
		}
//		top = drawRankItem(canvas, new String[] { "2", "99.9%", "Verizon Wireless" }, top, false);
//		top = drawRankItem(canvas, new String[] { "3", "99.9%", "Claro" }, top, false);
//		top = drawRankItem(canvas, new String[] { "4", "99.9%", "Orange T-Mobile" }, top, false);
		return top;
	}

	private float drawRankItem(Canvas c, String[] data,  float left, float top, float maxRight, boolean highlight) {
		paint.setTypeface(robotoRegular);
		textScale.setEmpty();
		paint.getTextBounds("5", 0, 1, textScale);

		float percentLeft = left + textScale.width() + hSeparator;
		float opNameLeft = percentLeft + percentRect.width() + hSeparator;

		// highlight rectangle color
		if (highlight) {
			// highlight this rank item -- starting from chart's left coordinate
			paint.setColor(highlightColor);
			c.drawRect(left - hSeparator, top - separator/2, maxRight, top + textScale.height() + separator/2, paint);
			paint.setColor(highlightFGColor);
		} else {
			paint.setColor(newTextColors[1]);
		}
		top += percentRect.height();
		c.drawText(data[0], left, top, paint);

		// draw operator name
		c.drawText(data[2], opNameLeft, top, paint);
		// draw percentage in the middle
		paint.setTypeface(robotoMedium);
		paint.setTextAlign(Paint.Align.CENTER);
		percentLeft += percentRect.width() / 2f;
		c.drawText(data[1], percentLeft, top, paint);

		// we need only percentage center aligned
		paint.setTextAlign(Paint.Align.LEFT);
		return top + separator;
	}

	private float drawIcon(Canvas canvas, float left, float top) {
		float iconWidth = newTextSize[1];
		Bitmap s = scaleBitmap(icons[0], iconWidth, iconWidth);
		canvas.drawBitmap(s, left + (iconWidth - s.getWidth()) / 2, top, paint);
		return left + s.getWidth();
	}

	/**
	 * Draws the carrier logo for the top carrier
	 * 
	 * @param canvas
	 * @param logo
	 * @param top
	 */
	private float drawLogo(Canvas canvas, Bitmap logo, float top) {
		// make the logo a bit wider, since logo images have more width than
		// height
		float logoWidth = Math.min(0.2f * getWidth(), getHeight() - top);
		float left = getWidth() - logoWidth;
		if (opLogoOnTop) {
			logoWidth = Math.min(0.3f * getWidth(), getHeight() - top);
			left = getWidth() / 2 - logoWidth / 2;
		}

		// TODO remove next 2 lines
		// paint.setColor(Color.YELLOW);
		// canvas.drawRect(left, top, left + logoWidth, top+logoWidth, paint);

		if (logo != null) {
			Bitmap s = scaleBitmap(logo, logoWidth, logoWidth);
			if (opLogoOnTop) {
				// draw the logo at bottom|center_horizontal
				canvas.drawBitmap(s, left + (logoWidth - s.getWidth()) / 2, top + (logoWidth - s.getHeight())/2, paint);
			} else {
				// draw the logo at top|center_horizontal
				canvas.drawBitmap(s, left + (logoWidth - s.getWidth()) / 2, top, paint);
			}
			return top + logoWidth;
		}
		return top;
	}

	private Bitmap scaleBitmap(Bitmap in, float boxW, float boxH) {
		float w = (float) in.getWidth() * screenDensityScale / 1.5f;
		float h = (float) in.getHeight() * screenDensityScale / 1.5f;
		float drawW = 0.0f;
		float drawH = 0.0f;
		float boxRatio = boxW / boxH;
		float imgRatio = w / h;
		// Carrier logo may require some aspect ratio scaling because the online
		// carrier logo images have different
		// sizes and shapes
		if (imgRatio > boxRatio) {
			drawW = 8 * boxW / 10;
			if (Math.abs(drawW - w) < boxW / 6)
				drawW = w;
			drawH = drawW / imgRatio;
		} else {
			drawH = 8 * boxH / 10;
			if (Math.abs(drawH - h) < boxH / 6)
				drawH = h;
			drawW = drawH * imgRatio;
		}
		return Bitmap.createScaledBitmap(in, (int) drawW, (int) drawH, true);
	}

	/*
	 * End over-ridden methods
	 * ======================================================== Start helper
	 * classes
	 */

	/*
	 * There are 4 statistic categories on the ranking screen: Coverage %,
	 * Download Speed, Upload Speed, and Dropped Calls Each category has a
	 * sorted list of the rankings for up to 5 carriers
	 */
	class StatCategory {
		public String title;
		public static final int SERIES_DROPPED = 0;
		public static final int SERIES_FAILED = 0;
		public static final int SERIES_NORMAL = 0;
		public float dropped = 0, failed = 0, normal = 0, total = 0;
		public float[] series = new float[3];
		public Bitmap bestlogo = null;
		// the ranking are a sorted list of the 5 carriers values for this
		// statistic category
		public List<Ranking> rankings = new ArrayList<Ranking>(5);

		// Constructor extracts the statistics for the 5 carriers from the JSON
		// statistics object
		// and sorts them to give a top 5 ranking, ready to be drawn
		public StatCategory(JSONObject json, String statkey, String title, boolean descending) {
			this.title = title;
			// String[] carrkeys = {"yourcarrier", "carrier0", "carrier1",
			// "carrier2", "carrier3"};
			// for (int i=0; i<5; i++)
			// {
			// if (json.has(carrkeys[i]))
			Iterator<String> iter = json.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				if (key.length() > 10) {
					try {
						JSONObject stats = json.getJSONObject(key);
						if (stats.has(statkey)) {
							float value = 0.0f;
							if (statkey.equals("dropPercent") || statkey.equals("covService"))
								value = (float) (int) (stats.getDouble(statkey) * 10) / 10f;
							else
								value = (float) (stats.getInt(statkey) / 100000) / 10f;
							String opName = stats.getString("operator");
							if (opName.length() > opNameMaxLen) {
								opName = opName.substring(0, opNameMaxLen - 2) + "..";
							}
							rankings.add(new Ranking(opName, value, stats.getString("logo"), descending));
							widestOpName = opName.length() > widestOpName.length() ? opName : widestOpName;
						}
					} catch (Exception e) {
					}
				}
			}
			Collections.sort(rankings);// Sorts the array list
			if (rankings.size() > 0 && rankings.get(0) != null && rankings.get(0).logo.length() > 0) {
				String logoPath = RankingCatView.this.context.getApplicationContext().getFilesDir() + rankings.get(0).logo;
				try {
					// TODO remove next line
					// bestlogo = BitmapFactory.decodeResource(getResources(),
					// R.drawable.claro_logo);
					bestlogo = BitmapFactory.decodeFile(logoPath);
				} catch (OutOfMemoryError e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "OutOfMemoryError loading logo " + logoPath);
				} catch (Exception e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "error loading logo " + logoPath, e);
				}
			} else
				MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "no logo ");
		}
	}

	class Ranking implements Comparable<Ranking> {
		public float value;
		public String name;
		public String logo;
		public boolean descending;

		public Ranking(String _name, float _value, String _logo, boolean _descending) {
			name = _name;
			value = _value;
			logo = _logo;
			descending = _descending;
		}

		// Overriding the compareTo method
		public int compareTo(Ranking r) {
			if (descending)
				return (int) (r.value * 100 - value * 100);
			else {
				if (value == -1)
					return 1;
				else if (r.value == -1)
					return -1;
				return (int) (value * 100 - r.value * 100);
			}
		}

	}

	/*
	 * End helper classes
	 * ================================================================
	 */
}
