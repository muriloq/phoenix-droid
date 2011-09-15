package com.muriloq.android.phoenix.controller;

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

  public interface ReadyListener {
    public void controlerReady();
  }
  
  protected InputListener mInputListener;
  protected ReadyListener mReadyListener;
  protected boolean mRequiresListenerNotification;
  
  public ReadyListener getReadyListener() {
    return mReadyListener;
  }

  public void setReadyListener(ReadyListener readyListener) {
    mReadyListener=readyListener;
    if (mRequiresListenerNotification) {
      mReadyListener.controlerReady();
      mRequiresListenerNotification=false;
    }
  }
  
  protected void setReady() {
    if (mReadyListener!=null) {
      mReadyListener.controlerReady();
    }
    mRequiresListenerNotification=true;
  }
  
  public void setInputListener(InputListener inputListener) {
    this.mInputListener=inputListener;
  }
  
  public InputListener getInputListener() {
    return mInputListener;
  }
  
  public abstract void showScore(byte player, byte[] bcdScore);
  public abstract View createControllerWidget();

  // put other interfaces, like to blinking android's eyes sometimes, 
  // or to light some rbg leds when the user wins

  
  public Object getObjectToRetain() {
    return null;
  }

  public void handleDestroy() {
  }

  public void handleResume() {
  }

  public void handlePause() {
  }

  
}