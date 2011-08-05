package com.muriloq.android.phoenix.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;

import com.muriloq.android.phoenix.ButtonState;
import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;
import com.muriloq.android.phoenix.R;


public class ScreenController extends Controller {

  private static final String TAG = "PHOENIX";
  
  private View mControllerView;
  
  public ScreenController(Context context) {
    LayoutInflater factory = LayoutInflater.from(context);
    
    mControllerView=factory.inflate(R.layout.screen_controller, null);
    mControllerView.setOnKeyListener(new KeyListener());
    mControllerView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN ||
            event.getAction()==MotionEvent.ACTION_UP) {
          if (event.getX()<v.getWidth()/2) {
            getInputListener().onJoystick(Direction.LEFT, event.getAction()==MotionEvent.ACTION_DOWN?ButtonState.PRESS:ButtonState.RELEASE);
          }
          return true;
        }
        return false;
      }
    });
    
    mControllerView.findViewById(R.id.fire).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        v.setEnabled(false);
        startAutofire();
      }
    });
    mControllerView.findViewById(R.id.start1).setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        handleButtonClick(ButtonType.START1);
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

  private Handler mHandler=new Handler();
  
  class RestoreButtonState implements Runnable {
    private ButtonType type;
    public RestoreButtonState(ButtonType type) {
      this.type=type;
    }
    public void run() {
      getInputListener().onButton(type, ButtonState.RELEASE);
    }
  }

  class AutoFire implements Runnable {
    boolean pressed; 
    public void run() {
      getInputListener().onButton(ButtonType.FIRE, 
          pressed?ButtonState.RELEASE:ButtonState.PRESS);
      pressed=!pressed;
      mHandler.postDelayed(this, pressed?50:200);
    }
  }

  protected void startAutofire() {
    mHandler.post(new AutoFire());
  }
  
  protected boolean handleButtonClick(ButtonType button) {
    if (getInputListener()!=null) {
      getInputListener().onButton(button, ButtonState.PRESS);
      mHandler.postDelayed(new RestoreButtonState(button), 100);
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
        ButtonState state=event.getAction()==MotionEvent.ACTION_DOWN?ButtonState.PRESS:ButtonState.RELEASE;
        Log.d(TAG, "button "+button.name()+" pressed");
        getInputListener().onButton(button, state);
      }
      return true;
    }
  }
 
}