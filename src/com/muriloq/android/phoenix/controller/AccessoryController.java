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
import java.lang.ref.SoftReference;
import java.util.BitSet;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;

import com.muriloq.android.phoenix.ButtonState;
import com.muriloq.android.phoenix.ButtonType;
import com.muriloq.android.phoenix.Direction;

public class AccessoryController extends Controller {
	private static final String TAG = "PHOENIX";

	private static final String ACTION_USB_PERMISSION = "phoenixdroid.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	private SoftReference<Activity> mActivity;
	private Thread readThread;

	public AccessoryController(Activity activity) {
	  mActivity=new SoftReference<Activity>(activity);
    handleCreate();
  }
	
  public void showScore(byte player, byte[] bcdScore) {
	  sendCommand(player, (byte) 0, bcdScore);
  }
  
  public View createControllerWidget() {
    return null; 
  }

  private void registerDettachFilter() {
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    mActivity.get().registerReceiver(mUsbReceiver, filter);
  }
  
	protected void handleCreate() {
		mUsbManager = (UsbManager) mActivity.get().getSystemService(Context.USB_SERVICE);//UsbManager.getInstance(activity);

		registerDettachFilter();
    if (mActivity.get().getLastNonConfigurationInstance() != null) {
      UsbAccessory accessory = (UsbAccessory) mActivity.get().getLastNonConfigurationInstance();
      openAccessory(accessory);
    }
    Log.d(TAG, "accessory controller on handleCreate... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
	}

	
  public static boolean hasAccessoryAttached(Activity activity) {
  	UsbAccessory[] accessories=((UsbManager) activity.getSystemService(Context.USB_SERVICE)).getAccessoryList();
    Log.i(TAG, "looking for conected accessories: "+accessories);
  	return accessories!=null;
  }

  protected void requestPermission() {
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
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                mActivity.get(), 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(accessory, permissionIntent);
            mPermissionRequestPending = true;
          }
        }
      }
    } else {
      Log.d(TAG, "mAccessory is null");
    }
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
          mPermissionRequestPending = false;
          UsbAccessory accessory = getAccessoryFromPermissionRequestIntent(intent);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            openAccessory(accessory);
          } else {
            Log.d(TAG, "permission denied for accessory " + accessory);
          }
        }
      } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
        closeAccessory();
      }
    }
  };

  private UsbAccessory getAccessoryFromPermissionRequestIntent(Intent intent) {
    return (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);//UsbManager.getAccessory(intent);
  }
	
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			
			readThread=new Thread(null, new Runnable() {
			  public void run() {
			    readADKLoop();
			  }
			}, "ADKConnect");
			readThread.start();

			Log.d(TAG, "accessory open OK, will call setReady");
	
			setReady();
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) mFileDescriptor.close();
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

  private final byte BUTTON_FIRE=0;
  private final byte BUTTON_SHIELD=1;
  private final byte BUTTON_DOWN=2;
  private final byte BUTTON_LEFT=3;
  private final byte BUTTON_UP=4;
  private final byte BUTTON_RIGHT=5;
 
  public void readADKLoop() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
		  if (mInputStream==null) {
        Log.w(TAG, "null mInputStream... stopping read loop");
		    break;
		  }
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
			  Log.w(TAG, "error while reading mInputStream... stopping read loop", e);
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				Log.d(TAG, "got message from accessory:"+len+"bytes - first is 0x"+Integer.toHexString(buffer[i])+"\n");

				if (buffer[i]==1) {  // buttons
          if (len == 3) {
            Message m = Message.obtain(mHandler, buffer[i]);
            m.obj = new SwitchMsg(buffer[i + 1], buffer[i + 2]);
            mHandler.sendMessage(m);
          }
          i += len;
				} else if (buffer[i]==2) {  // coin
            mHandler.sendMessage(Message.obtain(mHandler, buffer[i]));
            i += len;
				} else {
				  String message="unknown msg: " + buffer[i];
			    Log.d(TAG, message);
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
			case 1:  // all buttons except coin
				handleSwitchMessage((SwitchMsg) msg.obj);
				break;
      case 2:  // coin
        handleCoinMessage(msg);
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
        Log.e(TAG, "write failed "+e.getMessage());
			}
		}
	}

	protected BitSet bufferedState=new BitSet(8);
	protected boolean specialCodeCoin=false;
	
  protected void handleSwitchMessage(SwitchMsg o) {
    Log.d(TAG, "button pressed="+o.getButton()+" state="+o.getState());
    if (getInputListener()!=null) {
      bufferedState.set(o.getButton(), o.getState()==1);
      checkSpecialCode();
      ButtonState state=o.getState()==0?ButtonState.PRESS:ButtonState.RELEASE;
      switch (o.getButton()) {
      case (BUTTON_FIRE): 
        getInputListener().onButton(ButtonType.FIRE, state);
        getInputListener().onButton(ButtonType.START1, state);
        break;
      case (BUTTON_SHIELD): 
        getInputListener().onButton(ButtonType.SHIELD, state);
        getInputListener().onButton(ButtonType.START2, state);
        break;
      case (BUTTON_DOWN): 
      case (BUTTON_UP): break;
      case (BUTTON_LEFT): getInputListener().onJoystick(Direction.LEFT, state); break;
      case (BUTTON_RIGHT): getInputListener().onJoystick(Direction.RIGHT, state); break;
      }
    }
  }
  
  protected void checkSpecialCode() {
    if (bufferedState.get(BUTTON_FIRE) && 
        bufferedState.get(BUTTON_SHIELD) && 
        bufferedState.get(BUTTON_UP)) {
      if (!specialCodeCoin) {
        getInputListener().onCoinInserted();
        specialCodeCoin=true;
      }
    } else {
      specialCodeCoin=false;
    }
  }
  
  protected void handleCoinMessage(Message o) {
    String message="coin inserted data="+o.getData();
    Log.d(TAG, message);
    if (getInputListener()!=null) {
      getInputListener().onCoinInserted();
    }
  }

  
  // Lifecycle methods:
  @Override
  public void handleResume() {
    requestPermission();
  }

  @Override
  public Object getObjectToRetain() {
    if (mAccessory != null) {
      return mAccessory;
    }
    return null;
  }
  
  @Override
  public void handlePause() {
    closeAccessory();
  }

  @Override
  public void handleDestroy() {
    Log.d(TAG, "destroyed... mAccessory "+(mAccessory==null?"null":"NOT null")+"\n");
    try {
      mActivity.get().unregisterReceiver(mUsbReceiver);
    } catch (IllegalArgumentException ex) {}
  }

  
  
}
