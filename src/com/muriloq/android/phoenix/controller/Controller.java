package com.muriloq.android.phoenix.controller;

import android.app.Activity;
import android.view.View;

import com.muriloq.android.phoenix.ButtonState;
import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;

public abstract class Controller {
  public static final byte HI_SCORE = 0;
  public static final byte PLAYER_ONE = 1;
  public static final byte PLAYER_TWO = 2;
	  
  public interface InputListener {
    public void onButton(ButtonType button, ButtonState state);
    public void onJoystick(Direction direction, ButtonState state);
    public void onCoinInserted();
  }
  
  protected InputListener inputListener;
  
  public void setInputListener(InputListener inputListener) {
    this.inputListener=inputListener;
  }
  
  public InputListener getInputListener() {
    return inputListener;
  }
  
  public abstract void showScore(byte player, byte[] bcdScore);
  public abstract View createControllerWidget();


  // put other interfaces, like to blinking android's eyes sometimes, 
  // or to light some rbg leds when the user wins

  
  public Object handleRetainNonConfigurationInstance(Activity activity) {
    return null;
  }

  public void handleDestroy(Activity activity) {
  }

  public void handleResume(Activity activity) {
  }

  public void handlePause() {
  }
   
}