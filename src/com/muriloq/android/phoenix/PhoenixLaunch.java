package com.muriloq.android.phoenix;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class PhoenixLaunch extends Activity {
	static final String TAG = "Phoenix";

	static Intent createIntent(Activity activity) {
		Intent intent= new Intent(activity, AccessoryPhoenixActivity.class);
		return intent;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = createIntent(this);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to start Phoenix activity", e);
		}
		finish();
	}
}
