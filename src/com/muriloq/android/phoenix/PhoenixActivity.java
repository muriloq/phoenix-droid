package com.muriloq.android.phoenix;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;

public class PhoenixActivity extends Activity {
  protected PhoenixGame view;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LayoutInflater factory = LayoutInflater.from(this);

    // Set game layout
    view = (PhoenixGame) factory.inflate(R.layout.main, null);
    setContentView(view);

    view.fire(); 
  }


  @Override
  protected void onStop() {
    super.onStop();
    view.onStop();  
  }

  @Override
  protected void onPause() {
    super.onPause();
    view.onPause(); 
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    view.onRestart();
  }

  @Override
  protected void onDestroy(){
    super.onDestroy(); 
    view.onDestroy();
    //     android.os.Debug.stopMethodTracing();

  }

}