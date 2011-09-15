package com.muriloq.android.phoenix.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.Button;

import com.muriloq.android.phoenix.ButtonState;
import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;
import com.muriloq.android.phoenix.R;


public class ScreenController extends Controller {

  private static final String TAG = "PHOENIX";
  
  private View mControllerView;
  public boolean mPlay; //To stop AutoFire
  private Handler mHandler=new Handler();
  private Button mBtFire; 

  OnTouchListener onTouch = new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN ||
            event.getAction()==MotionEvent.ACTION_UP) {
          if (event.getX()<v.getWidth()/2) {
             getInputListener().onJoystick(Direction.LEFT, event.getAction()==MotionEvent.ACTION_DOWN?ButtonState.RELEASE:ButtonState.PRESS);
          }else {       	  
             getInputListener().onJoystick(Direction.RIGHT, event.getAction()==MotionEvent.ACTION_DOWN?ButtonState.RELEASE:ButtonState.PRESS);              
          }
          return true;
        }
        return false;
      }
    };
    
  public ScreenController(Context context) {
    LayoutInflater factory = LayoutInflater.from(context);
    mPlay=true;
    mControllerView=factory.inflate(R.layout.screen_controller, null);
    mControllerView.setOnKeyListener(new KeyListener());
   // mControllerView.setOnTouchListener(
    mBtFire =(Button) mControllerView.findViewById(R.id.fire);
    mBtFire.setOnClickListener(new View.OnClickListener() {
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
    mControllerView.setOnTouchListener(onTouch);
    
  }

  @Override
  public void setReadyListener(ReadyListener controllerListener) {
    super.setReadyListener(controllerListener);
    // we are always ready, so call listener immediately 
    if (controllerListener!=null) controllerListener.controlerReady();
  }
  
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
      if(mPlay)
    	  mHandler.postDelayed(this, pressed?50:200);
    }
  }

  protected void startAutofire() {
	 mPlay=true;
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
  
  public void showScore(byte player, byte[] bcdScore) {
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

	@Override
	public void handleDestroy() {
    mPlay=false;
	}

	@Override
	public void handlePause() {
    mPlay=false;
	}

	@Override
	public void handleResume() {
		if(!mBtFire.isEnabled()) {			
			startAutofire();
		}
	}
}