package com.cortxt.app.MMC.Activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.com.mmcextension.EventTriggers.SpeedTestTrigger;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.felipecsl.gifimageview.library.GifImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

public class RawTest extends MMCTrackedActivityOld {

	private ResultsListener mResultsListener;
	private TextView textEnterCommand, textResponse;
	private EditText editCmd1, editCmd2, editCmd3, editKey;
    private GifImageView gifView;
	private View view;
    private boolean bAnimating = false;
    private int prevPlayProgress = 0;

	private static final String TAG = RawTest.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view  = inflater.inflate(R.layout.raw_test, null, false);
		this.setContentView(view);

		MMCActivity.customizeTitleBar(this, view, R.string.eventtype_audioTest, R.string.eventtype_audioTest);

		textEnterCommand = (TextView)view.findViewById(R.id.textEnterCommand);
		editCmd1 = (EditText)view.findViewById(R.id.editCmd1);
		editCmd2 = (EditText)view.findViewById(R.id.editCmd2);
		editCmd3 = (EditText)view.findViewById(R.id.editCmd3);
		editKey = (EditText)view.findViewById(R.id.editKey);
		textResponse = (TextView)view.findViewById(R.id.textResponseText);
		Button startButton=(Button)view.findViewById(R.id.startButton);

		editCmd1.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start,
										  int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start,
									  int before, int count) {
				setCmd2Length();
			}
		});

		editCmd3.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
			}

			public void beforeTextChanged(CharSequence s, int start,
										  int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start,
									  int before, int count) {
				setCmd2Length();
			}
		});

		mResultsListener = new ResultsListener();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.cortxt.app.MMC.intent.RAWRIL_RESPONSE");
		registerReceiver(mResultsListener, intentFilter);
	}

	public void setCmd2Length ()
	{
		int a1, a3;
		a1 = editCmd1.getText().toString().length();
		a3 = editCmd3.getText().toString().length();

		if (a1 != 4 || (a1 + a3) % 2 != 0)
		{
			editCmd2.setText ("XXXX");
			return;
		}
		char c = (char)('0' + 2 + (char)(a1/2) + (char)(a3/2));
		String s2 = "000" + c;
		editCmd2.setText(s2);
	}

    public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function
    }

    public void startClicked (View button)
    {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName("com.cortxt.app.rilreader", "com.cortxt.app.rilreader.RadioLog.RadioLogService"));
		intent.putExtra("cmd", "startone");
		intent.putExtra("appname", this.getPackageName());
		intent.putExtra("svcmode", true);

		//SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		String value = editCmd1.getText().toString();
		value += editCmd2.getText().toString() + editCmd3.getText().toString();
		intent.putExtra("testmsg", value);

		if (Build.VERSION.SDK_INT < 20)
			intent.putExtra("needlogcat", true);

		this.startService(intent);
    }

	public void sendKeyClicked (View button)
	{
		Intent intent = new Intent();
		intent.setComponent(new ComponentName("com.cortxt.app.rilreader", "com.cortxt.app.rilreader.RadioLog.RadioLogService"));
		intent.putExtra("cmd", "startone");
		intent.putExtra("appname", this.getPackageName());
		intent.putExtra("svcmode", true);

		//SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		String value = editKey.getText().toString();
		intent.putExtra("sendkey", value);

		if (Build.VERSION.SDK_INT < 20)
			intent.putExtra("needlogcat", true);

		this.startService(intent);
	}

	/**
	 * BroadcastReceiver that listens to results of speed test
	 * @author nasrullah
	 *
	 */
	class ResultsListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("com.cortxt.app.MMC.intent.RAWRIL_RESPONSE")) {
				if (intent.hasExtra("EXTRA_RESPONSE")) {
					String response = intent.getStringExtra("EXTRA_RESPONSE");
					textResponse.setText (response);
				}
			}
		}
	}
}
 