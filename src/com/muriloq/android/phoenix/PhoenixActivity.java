package com.muriloq.android.phoenix;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import com.muriloq.android.phoenix.controller.Controller;
import com.muriloq.android.phoenix.controller.ScreenController;

public class PhoenixActivity extends Activity {
  
  public static final String TAG="PHOENIX";
  
  public static final String CONTROLLER_TYPE_KEY="_controller";
  public static final Integer CONTROLLER_AUTOSELECT=0;
  public static final Integer CONTROLLER_ACCESSORY=1;
  public static final Integer CONTROLLER_TOUCH=2;
  
  protected PhoenixGame mView;
  protected Controller mController;
  
  // test cached
  private Boolean mAccessoryLibPresent; 

  private boolean hasAccessoryLib() {
    if (mAccessoryLibPresent==null) {
      try {
        Class.forName("com.android.future.usb.UsbAccessory");
      } catch (Exception e) {
        mAccessoryLibPresent=false;
      }
      mAccessoryLibPresent=true;
    }
    return mAccessoryLibPresent;
  }
  
  private Controller getAttachedAccessory() {
    try {
      @SuppressWarnings("unchecked")
      Class<Controller> clazz=(Class<Controller>) Class.forName("com.muriloq.android.phoenix.controller.AccessoryControler");
      Method m=clazz.getMethod("hasAccessoryAttached", Activity.class);
      Boolean hasAccessoryAttached=(Boolean) m.invoke(null, this);
      if (hasAccessoryAttached) {
        Log.i(TAG, "Creating AccessoryController");
        return clazz.getConstructor(Activity.class).newInstance(this);
      }
    } catch (Exception e) {
      Log.w("could not create Accessory Controller!", e);
    }
    return null;
  }
  
  protected Controller createController() {
    int type=getIntent().getIntExtra(CONTROLLER_TYPE_KEY, CONTROLLER_AUTOSELECT);

    if (type==CONTROLLER_AUTOSELECT) {
      type=hasAccessoryLib()?CONTROLLER_ACCESSORY:CONTROLLER_TOUCH;
    }
    if (type==CONTROLLER_ACCESSORY) {
      Controller controller=getAttachedAccessory();
      if (controller!=null) {
        return controller;
      }
    }
    Log.i(TAG, "Creating ScreenController");
    return new ScreenController(this);
  }

  protected void setController(Controller controller) {
    this.mController=controller;
    mView.setController(mController);
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LayoutInflater factory = LayoutInflater.from(this);

    // Set game layout
    mView = (PhoenixGame) factory.inflate(R.layout.main, null);
    setContentView(mView);

    setController(createController());


    mView.fire(); 
  }


  @Override
  protected void onStop() {
    super.onStop();
    mView.onStop();
    // onstop should not destroy mController instance, because if the stop is
    // caused by configuration change, a new instance will be called with
    // the controller instance, which should improve load time
  }

  @Override
  protected void onPause() {
    super.onPause();
    mView.onPause(); 
    mController.handlePause();
  }

  @Override
  protected void onRestart() {
    super.onRestart();
    mView.onRestart();
    mController.handleResume(this);

  }

  @Override
  protected void onDestroy(){
    super.onDestroy(); 
    mView.onDestroy();
    mController.handleDestroy(this);
    //     android.os.Debug.stopMethodTracing();

  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    return super.onRetainNonConfigurationInstance();
  }
}