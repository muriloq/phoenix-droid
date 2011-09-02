/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muriloq.android.phoenix.controller;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.muriloq.android.phoenix.ButtonState;
import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;
import com.muriloq.android.phoenix.R;

public class AccessoryController extends Controller implements Runnable {
	private static final String TAG = "PHOENIX";

	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	private Handler mUiHandler;
	
	private UsbManager mUsbManager;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	private TextView mLogPanel;
	private View mControllerView;
	
	public AccessoryController(Activity activity) {
	  mUiHandler=new Handler();
    LayoutInflater factory = LayoutInflater.from(activity);
    mControllerView=factory.inflate(R.layout.accessory_controller, null);
    mLogPanel=(TextView) mControllerView.findViewById(R.id.log);
    mLogPanel.append("accessory controller constructed\n");
    Button clear=(Button) mControllerView.findViewById(R.id.clear);
    clear.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        mLogPanel.setText("");
      }
    });
    handleCreate(activity);
  }
	
  public void showScore(byte player, byte[] bcdScore) {
	  sendCommand(player, (byte) 0, bcdScore);
  }
  
  public View createControllerWidget() {
    return mControllerView; 
  }

  private void registerDettachFilter(Activity activity) {
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    activity.registerReceiver(mUsbReceiver, filter);
  }
  
	protected void handleCreate(Activity activity) {

		mUsbManager = UsbManager.getInstance(activity);
		
		registerDettachFilter(activity);
		
		if (activity.getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) activity.getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}

		enableControls(false);
    mLogPanel.append("accessory controller created... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
	}

	
  @Override
	public Object handleRetainNonConfigurationInstance(Activity activity) {
		if (mAccessory != null) {
			return mAccessory;
		}
		return null;
	}

  public static boolean hasAccessoryAttached(Activity activity) {
    return UsbManager.getInstance(activity).getAccessoryList()!=null;
  }

  @Override
	public void handleResume(Activity activity) {

		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
					  PendingIntent mPermissionIntent = PendingIntent.getBroadcast(
					      activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
    mLogPanel.append("resumed... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
	}

  @Override
	public void handlePause() {
    mLogPanel.append("paused... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
		closeAccessory();
	}

  @Override
	public void handleDestroy(Activity activity) {
    mLogPanel.append("destroyed... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
	  activity.unregisterReceiver(mUsbReceiver);
	}

	
	
	
	
	
  protected class SwitchMsg {
    private byte button;
    private byte state;

    public SwitchMsg(byte button, byte state) {
      this.button = button;
      this.state = state;
    }

    public byte getButton() {
      return button;
    }

    public byte getState() {
      return state;
    }
  }

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbAccessory accessory = UsbManager.getAccessory(intent);
          if (intent.getBooleanExtra(
              UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            openAccessory(accessory);
          } else {
            Log.d(TAG, "permission denied for accessory "
                + accessory);
          }
          mPermissionRequestPending = false;
        }
      } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
        UsbAccessory accessory = UsbManager.getAccessory(intent);
        if (accessory != null && accessory.equals(mAccessory)) {
          closeAccessory();
        }
      }
    }
  };

	
	
	
	
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "DemoKit");
			thread.start();
	    mLogPanel.append("accessory opened\n");
			Log.d(TAG, "accessory opened");
			enableControls(true);
		} else {
      mLogPanel.append("accessory open fail\n");
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	protected void enableControls(boolean enable) {
	}

	protected void logToWindow(final String message) {
	  mUiHandler.post(new Runnable() {
      public void run() {
        mLogPanel.setText(message);
      }
    });
	}
	
  private final byte BUTTON_FIRE=0;
  private final byte BUTTON_SHIELD=1;
  private final byte BUTTON_DOWN=2;
  private final byte BUTTON_LEFT=3;
  private final byte BUTTON_UP=4;
  private final byte BUTTON_RIGHT=5;
 
  public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				logToWindow("got message from accessory:"+len+"bytes - first is 0x"+Integer.toHexString(buffer[i])+"\n");

				if (buffer[i]==1) {  // buttons
          if (len == 3) {
            Message m = Message.obtain(mHandler, buffer[i]);
            m.obj = new SwitchMsg(buffer[i + 1], buffer[i + 2]);
            mHandler.sendMessage(m);
          }
          i += len;
				} else {
				  String message="unknown msg: " + buffer[i];
			    Log.d(TAG, message);
			    logToWindow(message+"\n");
					i = len;
					break;
				}
			}

		}
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:  // the only message type currently supported
				SwitchMsg o = (SwitchMsg) msg.obj;
				handleSwitchMessage(o);
				break;
			}
		}
	};

	public void sendCommand(byte command, byte target, byte[] value) {
		byte[] buffer = new byte[2+value.length];
		
		buffer[0] = command;
		buffer[1] = target;
		for (int i=0; i < value.length; i++){
			buffer[2+i] = value[i];
		}
		
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	protected void handleSwitchMessage(SwitchMsg o) {
    String message="button pressed="+o.getButton()+" state="+o.getState();
    Log.d(TAG, message);
    logToWindow(message+"\n");
    if (getInputListener()!=null) {
      ButtonState state=o.getState()==0?ButtonState.PRESS:ButtonState.RELEASE;
      switch (o.getButton()) {
      case (BUTTON_FIRE): 
        getInputListener().onButton(ButtonType.FIRE, state);
        getInputListener().onButton(ButtonType.START1, state);
        break;
      case (BUTTON_SHIELD): getInputListener().onButton(ButtonType.SHIELD, state);break;
      case (BUTTON_DOWN): getInputListener().onJoystick(Direction.DOWN, state); break;
      case (BUTTON_UP): 
        getInputListener().onJoystick(Direction.UP, state); 
        if (state==ButtonState.RELEASE) getInputListener().onCoinInserted(); 
        break;
      case (BUTTON_LEFT): getInputListener().onJoystick(Direction.LEFT, state); break;
      case (BUTTON_RIGHT): getInputListener().onJoystick(Direction.RIGHT, state); break;
      }
    }
	}

}
