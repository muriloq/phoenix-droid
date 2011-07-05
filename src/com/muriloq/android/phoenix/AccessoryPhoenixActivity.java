package com.muriloq.android.phoenix;

import android.os.Bundle;

import com.muriloq.android.phoenix.controller.AccessoryController;

public class AccessoryPhoenixActivity extends PhoenixActivity {

  private AccessoryController mAccessoryController;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAccessoryController=new AccessoryController(this);
    view.setController(mAccessoryController);
    
    mAccessoryController.handleCreate(this);
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    mAccessoryController.handlePause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mAccessoryController.handleDestroy(this);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    mAccessoryController.handleResume(this);
  }
  
  @Override
  public Object onRetainNonConfigurationInstance() {
    return mAccessoryController.handleRetainNonConfigurationInstance(this);
  }
}