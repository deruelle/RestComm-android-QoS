package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import java.util.Timer;
import java.util.TimerTask;

import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * OdometerSpinner represents a single digit 'spinner' in an Odometer.
 * It displays digits 0-9 and wraps at 10 (...8-9-0-1...)
 * 
 * @author Kevin Dion - kevindion.com
 * @version 1.0, Dec 24, 2010
 *
 */
public class OdometerSpinner extends View 
{
	public static final float IDEAL_ASPECT_RATIO = 1.66f;
	
	private float mWidth;
	private float mHeight;
	
	private GradientDrawable mBGGrad;
	
	private float mDigitX;
	
	private float mDigitY;
	private int mCurrentDigit;
	static long frameDelay = 50;
	private String mDigitString;
	private Paint mDigitPaint;
	private Timer animateTimer;
	//private AnimateTimerTask animateTimerTask;
	private boolean timerRunning = true;
	public static final String TAG = OdometerSpinner.class.getSimpleName();
	
	private int index = 0;
	private int mDigitAbove;
	private int mDigitBelow;
	private float mDigitAboveY;
	private float mDigitBelowY;
	private String mDigitAboveString;
	private String mDigitBelowString;
	private long lastDraw = 0;
	
	private float mTouchStartY;
	private float mTouchLastY;
	private float density;
	boolean bAnimating = false;
	private int mDigitPaintColor = 0xff666666; // digits color
	
	private OnDigitChangeListener mDigitChangeListener;
	
	/*
	 * Simple constructor used when creating a view from code.
	 */
	public OdometerSpinner(Context context)
	{
		super(context);
		
		initialize();
	}

	/*
	 * This is called when a view is being constructed from an XML file, 
	 * supplying attributes that were specified in the XML file. 
	 */
	public OdometerSpinner(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		
		initialize();
	}

	/*
	 * Perform inflation from XML and apply a class-specific base style. 
	 * This constructor of View allows subclasses to use their own base 
	 * style when they are inflating.
	 */
	public OdometerSpinner(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		
		initialize();
	}
	
	/*
	 * Initialize all of our class members and variables
	 */
	private void initialize()
	{
		/*
		 *  Setup our background gradient to have a top-to-bottom orientation
		 *  and go from black to a medium gray to black again.
		 *  Colors here are of the form 0xAARRGGBB.
		 *  AA: alpha - 00 is transparent, FF is opaque
		 */
		mBGGrad = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
				new int[] { 0xFF000000, 0xFFAAAAAA, 0xFF000000 });
		
		density = getContext().getResources().getDisplayMetrics().density;
		
		frameDelay = 25;
		/*
		 * The Paint used to draw the digit string. We set it to be 
		 * anti-aliased, white and centered horizontally.
		 */
		mDigitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mDigitPaint.setColor(mDigitPaintColor);
		mDigitPaint.setTextAlign(Align.CENTER);
		mDigitPaint.setTypeface(FontsUtil.getCustomFont(MmcConstants.font_Light, getContext()));
		
		setStartDigit(0, true);
	}

	public int getCurrentDigit()
	{
		return mCurrentDigit;
	}
	
	public void setStartDigit(int digit, boolean invalidate)
	{
		/*
		 *  Basic range limiting - in a production widget,
		 *  you might want to throw an exception if the number passed
		 *  if less than 0 or greater than 9
		 */
		int newVal = digit;
		
		if(newVal < 0)
			newVal = 0;
		if(newVal > 9)
			newVal = 9;
		
		int old = mCurrentDigit;
		mCurrentDigit = newVal;
		
		if(mCurrentDigit != old && mDigitChangeListener != null)
			mDigitChangeListener.onDigitChange(this, mCurrentDigit);
		
		mDigitAbove = mCurrentDigit + 1;
		
		if(mDigitAbove > 9)
			mDigitAbove = 0;
		
		mDigitBelow = mCurrentDigit - 1;
		
		if(mDigitBelow < 0)
			mDigitBelow = 9;

		mDigitString = String.valueOf(mCurrentDigit);
		mDigitAboveString = String.valueOf(mDigitAbove);
		mDigitBelowString = String.valueOf(mDigitBelow);
		
		setDigitYValues();
		if (invalidate)
			invalidate();
	}
	
//	public void changeToDigit(int digit)
//	{
//		
//		int nextDigit = digit;
//		if (digit == mCurrentDigit)
//			return;
//		if (digit < mCurrentDigit)
//			nextDigit = digit + 10;
//		animateTimerTask = new AnimateTimerTask(mCurrentDigit, nextDigit, 1500);
//		animateTimer = new Timer();
//		animateTimer.scheduleAtFixedRate(animateTimerTask, frameDelay, frameDelay);
//		bAnimating = true;
//		timerRunning = true;
//		frozen = false;
//		postInvalidateDelayed(50);
//	}
	
	double perSecond = 0.0;
	double firstDigit = 0.0;
	double fromVal =0f, currVal = 0f;
	long _duration = 0;
	int lastIntVal = 0;
	int digit = 0;
	public boolean bPastNine = false;
	double increment = 0f;
	boolean frozen = false;  // freeze spinner on digit until told to animate again
	
	public void initCountUp (double perSec, int _digit)
	{
		perSecond = perSec;
		firstDigit = mCurrentDigit;
		currVal = firstDigit;
		lastIntVal = (int)currVal;
		increment = perSecond * (double)frameDelay / 1000;
		digit = _digit;
		bAnimating = true;
		if (digit > 0 && perSecond <= 1.0)
			frozen = true;
		postInvalidateDelayed(50);
	}
	
	public void runCountUp(boolean freeze) 
	{
		int i;
		
		if (mHeight == 0)
			return;
		
		
		double delta = 0;
		if (frozen == true)
		{
			delta = 0;
		}
		if (digit > 0 && perSecond <= 1.0 && freeze == false && frozen == true)
		{
			frozen = false;
			if (lastIntVal%10 == 9)
				bPastNine = true;
			postInvalidate();
		}
		else if (lastDraw + 200 < System.currentTimeMillis())
			postInvalidate();
		
		if (frozen == false)
		{
			// On the first digit, animate it smoothly
			if (digit == 0 || perSecond > 1.0)
			{
				currVal += increment;
				delta = -increment * mHeight;
			}
			else
			{
				double fastIncrement = perSecond;
				double maxIncrement = perSecond * Math.pow(10, digit);
				while (fastIncrement < 1.0 )
					fastIncrement *= 10.0;
				fastIncrement = Math.min(fastIncrement, maxIncrement);
				fastIncrement = fastIncrement * (double)frameDelay / 1000;
				delta = -fastIncrement * mHeight;
				currVal += fastIncrement;
			}
		}
		//double delta = -increment * mHeight;
		//mTouchLastY = currentY;
		
		mDigitY -= delta;
		mDigitAboveY -= delta;
		mDigitBelowY -= delta;
		
		// calculate the overall delta (beginning to now)
		double totalDelta = ((double)lastIntVal-currVal)*mHeight;
		
		// If we have scrolled an entire number, change numbers while 
		// keeping the scroll
		if(Math.abs(totalDelta) > mHeight )
		{
			
			// need to either increase or decrease value
			double postDelta = Math.abs(totalDelta) - mHeight;
			int skippedDigits = (int)(postDelta / mHeight);
			if (skippedDigits >= 1)
				mDigitAbove = (lastIntVal + skippedDigits);
				
			if (digit > 0 && perSecond <= 1.0)
				frozen = true;
			else if (mDigitAbove >= 9 && frozen == false)
				bPastNine = true;
			
			if(totalDelta > 0)
			{
				// go DOWN a number
				setStartDigit(mDigitBelow, false);
				lastIntVal -= (1+skippedDigits);
				mTouchStartY -= mHeight;
				
				mDigitY -= postDelta;
				mDigitBelowY -= postDelta;
				mDigitAboveY -= postDelta;
			}
			else
			{
				
				
				mDigitAbove = mDigitAbove % 10;
				// go UP a number
				setStartDigit(mDigitAbove, false);
				lastIntVal += (1 + skippedDigits);
				
				mTouchStartY += mHeight;
				if (skippedDigits == 0 && !frozen)
				{
					mDigitY += postDelta;
					mDigitBelowY += postDelta;
					mDigitAboveY += postDelta;
				}
				else
					currVal = lastIntVal;
			}
		}
		
	}
	
	public boolean isAnimating ()
	{
		return bAnimating;
	}
	
	public void stopAnimating ()
	{
		bAnimating = false;
		if (animateTimer != null && timerRunning == true)
		{
			animateTimer.cancel ();
			timerRunning = false;
			animateTimer = null;
		}
		setStartDigit((int)currVal, false);
	}
	/**
	 * This is the timer task that animates the spinner from one number upwards to another number and stops
	 */
//	class AnimateTimerTask extends TimerTask {
//		float fromVal =0f, toVal=0f, currVal = 0f;
//		long _duration = 0;
//		int lastIntVal = 0;
//		float increment = 0f;
//		public AnimateTimerTask (int _from, float _to, long _duration)
//		{
//			fromVal = _from;
//			toVal = _to;
//			currVal = fromVal;
//			lastIntVal = (int)currVal;
//			increment = +(toVal - fromVal) * frameDelay / _duration;
//		}
//		@Override
//		public void run() {
//			if (mHeight == 0)
//				return;
//			currVal += increment;
//			if (currVal > toVal)
//			{
//				currVal = toVal;
//				bAnimating = false;
//				setStartDigit((int)currVal, false);
//				try{
//				if (animateTimer != null)
//					animateTimer.cancel ();
//					animateTimer = null;
//					timerRunning = false;
//				} catch (Exception e) 
//				{
//					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "AnimateTimerTask", "error in run ", e);	
//				}
//				
//				
//				mDigitChangeListener.onAnimationDone(OdometerSpinner.this, (int)currVal);
//				return;
//			}
//			float delta = -increment * mHeight;
//			//mTouchLastY = currentY;
//			
//			mDigitY -= delta;
//			mDigitAboveY -= delta;
//			mDigitBelowY -= delta;
//			
//			// calculate the overall delta (beginning to now)
//			float totalDelta = ((float)lastIntVal -currVal)*mHeight;
//			
//			// If we have scrolled an entire number, change numbers while 
//			// keeping the scroll
//			if(Math.abs(totalDelta) > mHeight )
//			{
//				// need to either increase or decrease value
//				float postDelta = Math.abs(totalDelta) - mHeight;
//				
//				if(totalDelta > 0)
//				{
//					// go DOWN a number
//					setStartDigit(mDigitBelow, false);
//					lastIntVal--;
//					mTouchStartY -= mHeight;
//					
//					mDigitY -= postDelta;
//					mDigitBelowY -= postDelta;
//					mDigitAboveY -= postDelta;
//				}
//				else
//				{
//					// go UP a number
//					setStartDigit(mDigitAbove, false);
//					lastIntVal++;
//					mTouchStartY += mHeight;
//					
//					mDigitY += postDelta;
//					mDigitBelowY += postDelta;
//					mDigitAboveY += postDelta;
//				}
//			}
//			
//			//OdometerSpinner.this.post (new Runnable() {
//			//	@Override
//			//	public void run() {
//			//		invalidate();
//			//		}});
//			
//		}
//	}
	
	
	
	private void setDigitYValues()
	{
		mDigitY = findCenterY(mCurrentDigit);
		mDigitAboveY = findCenterY(mDigitAbove) - mHeight;
		mDigitBelowY = mHeight + findCenterY(mDigitBelow);
	}
	private Rect bounds = new Rect();
	private float findCenterY(int digit)
	{
		String text = String.valueOf(digit);
		
		mDigitPaint.getTextBounds(text, 0, text.length(), bounds);
		
		int textHeight = Math.abs(bounds.height());
		
		float result = mHeight - ((mHeight - textHeight) / 2);
		
		return result;
	}
	public void setIndex (int _index)
	{
		index = _index;
		
	}

	public void setOnDigitChangeListener(OnDigitChangeListener listener)
	{
		mDigitChangeListener = listener;
	}

	/*
	 * This is where all of the drawing for the spinner is done. 
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		// if our super has to do any drawing, do that first
		super.onDraw(canvas);
		lastDraw = System.currentTimeMillis();
		
		// draw the background so it is below the digit
//		mBGGrad.draw(canvas);
		
		// draw the digit text using our calculated position and Paint
		canvas.drawText(mDigitString, mDigitX, mDigitY, mDigitPaint);

		canvas.drawText(mDigitAboveString, mDigitX, mDigitAboveY, mDigitPaint);
		canvas.drawText(mDigitBelowString, mDigitX, mDigitBelowY, mDigitPaint);
		
		if (bAnimating && !frozen)
			postInvalidateDelayed(80);
	}
	

	/*
	 * Measure the view and its content to determine the measured width and 
	 * the measured height.
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// get width and height size and mode
		int wSpec = MeasureSpec.getSize(widthMeasureSpec);
		
		int hSpec = MeasureSpec.getSize(heightMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		
		int width = wSpec;
		int height = hSpec;
		
		
		// ideal height for the number display
		int idealHeight = (int) ((wSpec) * IDEAL_ASPECT_RATIO);
		
		if(idealHeight < hSpec)
		{
			height = idealHeight;
		}
		
		setMeasuredDimension(width, height);
	}

	/*
	 * Called whenever the size of our View changes
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		
		mWidth = w;
		mHeight = h;
		int pad = 1;
		if(index%3==2)
			pad = (int)(2*density);
		// resize the background gradient
		mBGGrad.setBounds(pad, 0, w, h);
		
		// set the text paint to draw appropriately-sized text
		mDigitPaint.setTextSize(h);
		
		mDigitX = (mWidth+pad) / 2;
		
		setDigitYValues();
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// Pull out the Action value from the event for processing
		int action = event.getAction();
		
		if(action == MotionEvent.ACTION_DOWN)
		{
			mTouchStartY = event.getY();
			mTouchLastY = mTouchStartY;
			
			return true;
		}
		else if(action == MotionEvent.ACTION_MOVE)
		{
			float currentY = event.getY();
			
			float delta = mTouchLastY - currentY;
			mTouchLastY = currentY;
			
			mDigitY -= delta;
			mDigitAboveY -= delta;
			mDigitBelowY -= delta;
			
			// calculate the overall delta (beginning to now)
			float totalDelta = mTouchStartY - currentY;
			
			// If we have scrolled an entire number, change numbers while 
			// keeping the scroll
			if(Math.abs(totalDelta) > mHeight )
			{
				// need to either increase or decrease value
				float postDelta = Math.abs(totalDelta) - mHeight;
				
				if(totalDelta > 0)
				{
					// go DOWN a number
					setStartDigit(mDigitBelow, true);
					mTouchStartY -= mHeight;
					
					mDigitY -= postDelta;
					mDigitBelowY -= postDelta;
					mDigitAboveY -= postDelta;
				}
				else
				{
					// go UP a number
					setStartDigit(mDigitAbove, true);
					mTouchStartY += mHeight;
					
					mDigitY += postDelta;
					mDigitBelowY += postDelta;
					mDigitAboveY += postDelta;
				}
			}
			
			invalidate();
			
			return true;
		}
		else if(action == MotionEvent.ACTION_UP)
		{
			float currentY = event.getY();
			
			// delta: negative means a down 'scroll'
			float deltaY = mTouchStartY - currentY;
			
			int newValue = mCurrentDigit;
			
			if(Math.abs(deltaY) > (mHeight / 3) )
			{
				// higher numbers are 'above' the current, so a scroll down 
				// _increases_ the value
				if(deltaY < 0)
				{
					newValue = mDigitAbove;
				}
				else
				{
					newValue = mDigitBelow;
				}
			}
			
			setStartDigit(newValue, true);
			
			return true;
		}
		return false;
	}
	*/
	public interface OnDigitChangeListener
	{
		abstract void onDigitChange(OdometerSpinner sender, int newDigit);
		abstract void onAnimationDone(OdometerSpinner sender, int newDigit);
	}
}
