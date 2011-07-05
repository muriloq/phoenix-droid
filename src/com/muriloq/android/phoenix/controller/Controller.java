package com.muriloq.android.phoenix.controller;

import android.view.View;

import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;

public abstract class Controller {
  
  public interface InputListener {
    public void onButtonPress(ButtonType button);
    public void onButtonRelease(ButtonType button);
    public void onJoystickPress(Direction direction);
    public void onJoystickRelease(Direction direction);
    public void onCoinInserted();
  }
  
  protected InputListener inputListener;
  
  public void setInputListener(InputListener inputListener) {
    this.inputListener=inputListener;
  }
  
  public InputListener getInputListener() {
    return inputListener;
  }
  
  public abstract void showScore(int score);
  public abstract View createControllerWidget();
  
  // put other cool interfaces, like to blinking android's eyes sometimes, 
  // or to light some rbg leds when the user wins

}