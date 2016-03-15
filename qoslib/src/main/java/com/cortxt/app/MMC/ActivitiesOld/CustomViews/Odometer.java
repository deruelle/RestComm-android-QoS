package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.MMCLogger;

public class Odometer extends TableLayout 
{
	private int NUM_DIGITS = 6;
	private int LOW_DIGIT = 0;
	private int MAX_DIGITS = 13;
	
	private long mCurrentValue;
	private double persecond = 0.0f;
	
	private OdometerSpinner[] mDigitSpinners;
	
	private OnValueChangeListener mValueChangeListener;
	private Timer countUpTimer;
	private CountUpTimerTask countUpTimerTask;
	private boolean bStopped = false;
	private boolean timerRunning =false;
	public static final String TAG = Odometer.class.getSimpleName();
	
	public Odometer(Context context)
	{
		super(context);

		initialize();
	}

	public Odometer(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		initialize();
	}

	private float density;
	
	private void initialize()
	{
		mDigitSpinners = new OdometerSpinner[MAX_DIGITS];
		density = getContext().getResources().getDisplayMetrics().density;
		// Inflate the view from the layout resource.
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li;
		li = (LayoutInflater)getContext().getSystemService(infService);
		li.inflate(R.layout.widget_odometer, this, true); 
		
		mDigitSpinners[0] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_1);
		mDigitSpinners[1] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_10);
		mDigitSpinners[2] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_100);
		mDigitSpinners[3] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_1000);
		mDigitSpinners[4] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_10000);
		mDigitSpinners[5] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_100000);
		mDigitSpinners[6] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_1000000);
		mDigitSpinners[7] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_10000000);
		mDigitSpinners[8] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_100000000);
		mDigitSpinners[9] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_1000000000);
		mDigitSpinners[10] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_10000000000);
		mDigitSpinners[11] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_100000000000);
		mDigitSpinners[12] = (OdometerSpinner) findViewById(R.id.widget_odometer_spinner_1000000000000);
		
		for(OdometerSpinner s : mDigitSpinners)
		{
			s.setOnDigitChangeListener(new OdometerSpinner.OnDigitChangeListener()
			{
				public void onDigitChange(OdometerSpinner s, int newDigit)
				{
					updateValue();
				}

				@Override
				public void onAnimationDone(OdometerSpinner sender, int newDigit) {
					// TODO Auto-generated method stub
					// If everybodies initial animation is done, start the new animation which simulates a realtime counter
					int i;
					for (i=0; i<NUM_DIGITS; i++)
						if (mDigitSpinners[LOW_DIGIT+i].isAnimating())
							return;
					
					beginCountUpAnimation();
					//for (i=0; i<NUM_DIGITS; i++)
					//	mDigitSpinners[LOW_DIGIT+i].countUpAnimation(persecond/Math.pow(10.0, i), i);
				}
			});
		}
	}
	
	public void stop ()
	{
		try{
			if (countUpTimer != null && timerRunning == true)
			{
				countUpTimer.cancel();
				timerRunning =false;
				countUpTimer = null;
			}
			for (int i=0; i<NUM_DIGITS; i++)
				mDigitSpinners[LOW_DIGIT+i].stopAnimating ();
			countUpTimerTask = null;
		}
		catch (Exception e){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "displayFact", "error in displayFact ", e);	
		}
		bStopped = true;
	}
	
	private void updateValue()
	{
		int tempValue = 0;
		int factor = 1;
		
		for(OdometerSpinner s : mDigitSpinners)
		{
			tempValue += (s.getCurrentDigit() * factor);
			factor *= 10;
		}
		
		long old = mCurrentValue;
		mCurrentValue = tempValue;
		
		if(old != mCurrentValue && mValueChangeListener != null)
			mValueChangeListener.onValueChange(this, mCurrentValue);
	}
	
	public void setDigits(int value)
	{
		NUM_DIGITS = value/2*2+1;
		if (value == 10)
			NUM_DIGITS = 10;
		LOW_DIGIT = (MAX_DIGITS - NUM_DIGITS)/2;
		
		int i;
		for (i=LOW_DIGIT; i<NUM_DIGITS+LOW_DIGIT; i++)
		{
			mDigitSpinners[i].setVisibility(View.VISIBLE);
			mDigitSpinners[i].setIndex (i-LOW_DIGIT);
		}
		for (;i<MAX_DIGITS; i++)
			mDigitSpinners[i].setVisibility(View.INVISIBLE);
		for (i=0;i<LOW_DIGIT; i++)
			mDigitSpinners[i].setVisibility(View.INVISIBLE);
		
	}
	public void setValue(long value, long value1)
	{
		long old = mCurrentValue;
		long startValue = 0;
		
		
		// Predict the current value if given 2 values from the previous two midnights
		if (value1 > 0 && value > value1)
		{
			persecond = (double)(value - value1) / (24.0*3600.0);
			Date dt = new Date();
			int hours = dt.getHours();
			int timezone = dt.getTimezoneOffset()/60;
			hours = (hours + (dt.getTimezoneOffset()/60)) % 24;
			double midnight = (hours*3600+dt.getMinutes()*60+dt.getSeconds());
			value = value + (long)(persecond*midnight);
			bStopped = false;
		}
		else
			persecond = 0.0;
		mCurrentValue = value;
		long tempValue = mCurrentValue;
		long tempValue1 = startValue;
		int i;
		try{
		for( i = NUM_DIGITS-1; i > 0; --i)
		{
			int factor = (int)Math.pow(10, i);
			
			long digitVal1 = (long) Math.floor(tempValue1/ factor);
			tempValue1 -= (digitVal1 * factor);
			//mDigitSpinners[i+LOW_DIGIT].setStartDigit((int)digitVal1, true);
			long digitVal = (long) Math.floor(tempValue / factor);
			tempValue -= (digitVal * factor);
			mDigitSpinners[i+LOW_DIGIT].setStartDigit((int)digitVal, true);
			//mDigitSpinners[i+LOW_DIGIT].changeToDigit((int)digitVal);
		}
		mDigitSpinners[LOW_DIGIT].setStartDigit((int)tempValue, true);
		
		//mDigitSpinners[LOW_DIGIT].setStartDigit((int)tempValue1, true);
		//mDigitSpinners[LOW_DIGIT].changeToDigit((int)tempValue);
		}
		catch (Exception e){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "setValue", "error in setValue ", e);	
		}
		beginCountUpAnimation();
		if(old != mCurrentValue && mValueChangeListener != null)
			mValueChangeListener.onValueChange(this, mCurrentValue);
	}
	
	/*
	 * When all digits have reached their initial value, perform an animation where it counts up like an odometer
	 * the lowest digit spins fastest and the rest count up on each tenth spin
	 */
	public void beginCountUpAnimation()
	{
		if (countUpTimerTask == null && !bStopped && persecond > 0)
		{
			try
			{
				countUpTimer = new Timer();
				countUpTimerTask = new CountUpTimerTask();
				countUpTimer.scheduleAtFixedRate(countUpTimerTask, OdometerSpinner.frameDelay, OdometerSpinner.frameDelay);
				timerRunning = true;
			}
			catch (Exception e) {}
		}
	}
	/**
	 * This is the timer task that continuously spins the spinner at a constant rate
	 */
	class CountUpTimerTask extends TimerTask 
	{
		//double perSecond = 0.0;
		double firstDigit = 0.0;
		double fromVal =0f, currVal = 0f;
		long _duration = 0;
		int lastIntVal = 0;
		int digit = 0;
		double increment = 0f;
		public CountUpTimerTask ()
		{
			int i;
			for (i=0; i<NUM_DIGITS; i++)
				mDigitSpinners[LOW_DIGIT+i].initCountUp(persecond/Math.pow(10.0, i), i);
		}
		@Override
		public void run() 
		{
			int i;
			boolean freeze = true;
			for (i=0; i<NUM_DIGITS; i++)
			{
				if (i == 0 || mDigitSpinners[LOW_DIGIT+i-1].bPastNine || persecond/Math.pow(10.0, i) > 1.0)
				{
					freeze = false;
					if (mDigitSpinners[LOW_DIGIT+i-1].bPastNine == true)
						mDigitSpinners[LOW_DIGIT+i-1].bPastNine = false;
				}
				else
					freeze = true;
				mDigitSpinners[LOW_DIGIT+i].runCountUp(freeze);
			}
			
		}
	}
	
	public long getValue()
	{	
		return mCurrentValue;
	}
	
	public void setOnValueChangeListener(OnValueChangeListener listener)
	{
		mValueChangeListener = listener;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// get width and height size and mode
		int wSpec = MeasureSpec.getSize(widthMeasureSpec);
		
		int hSpec = MeasureSpec.getSize(heightMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		
		//int paddingtotal = (int)(NUM_DIGITS/3*2*density);
		// calculate max height from width
		float contentHeight = ((float)(wSpec) / NUM_DIGITS) 
				* OdometerSpinner.IDEAL_ASPECT_RATIO;
		
		int maxHeight = (int)Math.ceil(contentHeight);
		
		int width = wSpec;
		int height = hSpec;
		
		if(maxHeight < hSpec)
		{
			height = maxHeight;
		}
		
		setMeasuredDimension(width, height);
	}
	
	public interface OnValueChangeListener
	{
		abstract void onValueChange(Odometer sender, long newValue);
	}
	
}
