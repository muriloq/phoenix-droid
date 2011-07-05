package com.muriloq.android.phoenix.controller;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;

import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.R;


public class ScreenController extends Controller {

  private static final String TAG = "ScreenController";
  
  private View mControllerView;
  
  public ScreenController(Context context) {
    LayoutInflater factory = LayoutInflater.from(context);
    
    mControllerView=factory.inflate(R.layout.screen_controller, null);
    mControllerView.setOnTouchListener(new TouchController());
    mControllerView.setOnKeyListener(new KeyListener());
    mControllerView.findViewById(R.id.fire).setOnTouchListener(new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        return handleButtonTouch(ButtonType.FIRE, event);
      }
    });
    mControllerView.findViewById(R.id.start1).setOnTouchListener(new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        return handleButtonTouch(ButtonType.START1, event);
      }
    });
    mControllerView.findViewById(R.id.coin).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (getInputListener()!=null) {
          getInputListener().onCoinInserted();
        }
      }
    });
  }

  protected boolean handleButtonTouch(ButtonType button, MotionEvent event) {
    if (getInputListener()!=null) {
      if (event.getAction()==MotionEvent.ACTION_DOWN) {
        getInputListener().onButtonPress(button);
      } else {
        getInputListener().onButtonRelease(button);
      }
      return true;
    }
    return false;
  }
  
  @Override
  public View createControllerWidget() {
    return mControllerView;
  }
  
  public void showScore(int score) {
    // don't need to do anything here. score will be shown by the ROM itself
  }

  protected class TouchController implements OnTouchListener {
    Float limit1, limit2, limit3;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (getInputListener()==null) {
        return false;
      }
      if (limit1==null) {
        limit1=v.getWidth()*0.25f;
        limit2=v.getWidth()*0.50f;
        limit3=v.getWidth()*0.75f;
      }
      ButtonType type=null;
      if (event.getX()<limit1) {
        type=ButtonType.START1;
      } else if (event.getX()<limit2) {
        type=ButtonType.START2;
      } else if (event.getX()<limit3) {
        type=ButtonType.SHIELD;
      } else {
        type=ButtonType.FIRE;
      }
      
      if (getInputListener()!=null) {
        if (event.getAction()==MotionEvent.ACTION_DOWN) {
          getInputListener().onButtonPress(type);
          Log.d(TAG, "button "+type.name()+" pressed");
        } else {
          getInputListener().onButtonRelease(type);
          Log.d(TAG, "button "+type.name()+" released");
        }
      }
      return true;
    }
  }
  

  protected class KeyListener implements OnKeyListener {
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (getInputListener()==null) {
        return false;
      }
      
      ButtonType button=null;
      
      switch (keyCode) {
        // TODO: check if coin inserted also needs to handle DOWN and UP
        case KeyEvent.KEYCODE_DPAD_UP: 
          Log.d(TAG, "coin inserted");
          getInputListener().onCoinInserted(); 
          break;
        case KeyEvent.KEYCODE_MENU: 
          button=ButtonType.FIRE; 
          break;
        case KeyEvent.KEYCODE_DPAD_CENTER: 
          button=ButtonType.START1; 
          break;
      }

      if (button!=null) {
        if (event.getAction()==MotionEvent.ACTION_DOWN) {
          Log.d(TAG, "button "+button.name()+" pressed");
          getInputListener().onButtonPress(button);
        } else {
          Log.d(TAG, "button "+button.name()+" released");
          getInputListener().onButtonRelease(button);
        }
      }
      return true;
    }
  }
  
  
}