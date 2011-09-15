package com.muriloq.android.phoenix;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import com.muriloq.android.phoenix.controller.AccessoryController;
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
        Log.i(TAG, "FOUND com.android.future.usb.UsbAccessory");
        mAccessoryLibPresent=true;
      } catch (Exception e) {
	      try {
	          Class.forName("android.hardware.usb.UsbAccessory");
	          Log.i(TAG, "FOUND android.hardware.usb.UsbAccessory");
	          mAccessoryLibPresent=true;
	        } catch (Exception e2) {
		      Log.i(TAG, "no UsbAccessory lib found");
	          mAccessoryLibPresent=false;
	        }
      }
    }
    return mAccessoryLibPresent;
  }
  
  private Controller getAttachedAccessory() {
    try {
      /*
      @SuppressWarnings("unchecked")
      Class<Controller> clazz=(Class<Controller>) Class.forName("com.muriloq.android.phoenix.controller.AccessoryControler");
      Method m=clazz.getMethod("hasAccessoryAttached", Activity.class);
      Boolean hasAccessoryAttached=(Boolean) m.invoke(null, this);
      if (hasAccessoryAttached) {
        Log.i(TAG, "Creating AccessoryController");
        return clazz.getConstructor(Activity.class).newInstance(this);
      }*/
      if (AccessoryController.hasAccessoryAttached(this)) {
        return new AccessoryController(this);
      }
    } catch (Throwable e) {
    	// debug:
      while (e.getCause()!=null) {
        e=e.getCause();
        Log.w(TAG, e);
      }
      Log.w(TAG, e);
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
    
    mController.setReadyListener(new Controller.ReadyListener() {
      public void controlerReady() {
        Log.d(TAG, "controller READY, starting game engine!");
        mView.fire(); 
      }
    });
  }


  @Override
  protected void onPause() {
    super.onPause();
    mView.onPause(); 
    mController.handlePause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mController.handleResume();
    mView.onRestart();
  }
  
  @Override
  protected void onDestroy(){
    super.onDestroy(); 
    mController.handleDestroy();
    //     android.os.Debug.stopMethodTracing();

  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    if (mController!=null) {
      return mController.getObjectToRetain();
    }
    return null;
  }

}