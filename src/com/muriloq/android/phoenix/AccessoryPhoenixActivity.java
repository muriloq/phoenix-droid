package com.muriloq.android.phoenix;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AccessoryPhoenixActivity extends Activity {

  private static final String TAG="PHOENIX";
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent=new Intent(this, PhoenixActivity.class);
    intent.putExtra(PhoenixActivity.CONTROLLER_TYPE_KEY, PhoenixActivity.CONTROLLER_ACCESSORY);
    
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