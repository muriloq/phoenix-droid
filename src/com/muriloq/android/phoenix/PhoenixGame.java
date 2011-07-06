package com.muriloq.android.phoenix;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;

import com.muriloq.android.phoenix.controller.Controller;

public class PhoenixGame extends LinearLayout implements Controller.InputListener {

  private Phoenix phoenix;

  private boolean stop = false;
  private boolean pause = false;
  private boolean destroy = false;
  private long sleepTime;
  private long timeNow;
  private long timeBefore;
  private long interruptCounter;

  private Thread thread;

  // used to handle button release, because buttons only have a click event,
  // but emulator requires press+release events
  private ScheduledExecutorService scheduler;

  public PhoenixGame(Context context) {
    super(context);
  }
  public PhoenixGame(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.phoenix = new Phoenix(this);
    scheduler=Executors.newScheduledThreadPool(10);
    Display display = ((Activity) context).getWindowManager().getDefaultDisplay(); 
    this.phoenix.setActualDimensions(display.getWidth(), display.getHeight());
    initialize();
  }

  private void initialize() {
    phoenix.loadRoms(loadRoms());
    phoenix.initSFX();
    phoenix.hiload();
    phoenix.decodeChars(null);
  }

  public byte[] loadRoms() {
    byte buffer[] = new byte[0x6000];
    try {
      loadRom("program.rom", buffer, 0, 0x4000);
      loadRom("graphics.rom", buffer, 0x4000, 0x2000);
    } catch (Exception e){
      throw new RuntimeException("Error loading ROMs");
    }
    return buffer; 
  }

  private void loadRom(String name, byte[] buffer, int offset, int len) throws IOException {
    int readbytes = 0;
    System.out.print("Reading ROM "+name+"...");
    InputStream is = getContext().getAssets().open(name);
    BufferedInputStream bis = new BufferedInputStream(is, len);
    int n = len;
    int toRead = len;
    while (toRead > 0) {
      int nRead = bis.read(buffer, offset + n - toRead, toRead);
      toRead -= nRead;
      readbytes += nRead;
    }
    System.out.println(readbytes + " bytes");
  }
  public void setController(Controller controller) {
    controller.setInputListener(this);
    View controllerView=controller.createControllerWidget();
    if (controllerView!=null) this.addView(controllerView, this.getChildCount());
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    try {
      phoenix.setActualDimensions(getWidth(),getHeight());
    } 
    catch (Exception e) {
      // bug
      e.printStackTrace();
    }
  }

  public void fire() {
    this.thread = new Thread(new Runnable(){


      @Override
      public void run() {
        while(!stop) {
          timeBefore = System.currentTimeMillis();
          boolean busy = false;
          while (!destroy) {
            phoenix.cycles++;
            int pc = phoenix.PC();

            // After rendering a frame, the program enters in a
            // busy wait
            // that we don't need emulate. Skipping it increases
            // performance
            // drastically. Here's the 8085 asm code:
            //
            // 128 MVI H,78h
            // 130 MOV A,(HL) // HL=0x78** : memory mapped
            // dipswitches and VSYNC register
            // 131 AND A,80H // BIT 7 is true during VBLANK
            // 133 JZ 128 // busy wait until VBLANK
            //
            // Testing if VBLANK is true actually resets VBLANK
            // (it's a test and set operation).
            // So we need to run the busy wait at least once:
            // that's why we need the "busy" flag.
            if ((!busy) && (pc == 128))
              busy = true;
            else if (busy && (pc == 128)) {
              phoenix.cycles = 0;
            }

            if (phoenix.cycles == 0) {
              phoenix.interrupt();
              interruptCounter++;
              timeNow = System.currentTimeMillis();
              int msPerFrame = (int) (timeNow - timeBefore);
              sleepTime = 1000 / 60 - msPerFrame;

              phoenix.cycles = -phoenix.cyclesPerInterrupt;

              if (phoenix.isAutoFrameSkip()) {
                if (phoenix.getFramesPerSecond() > 60) {
                  int frameSkip = phoenix.getFrameSkip();
                  phoenix.setFrameSkip(frameSkip > 1 ? frameSkip - 1 : 1);
                } else if (phoenix.getFramesPerSecond() < 60) {
                  int frameSkip = phoenix.getFrameSkip();
                  phoenix.setFrameSkip(frameSkip < 5 ? frameSkip + 1 : 5);
                }
              }

              if (phoenix.isRealSpeed() || (sleepTime < 0)) {
                sleepTime = 1;
              }

              // Check if should wait
              synchronized (this) {
                while (pause) {
                  try {
                    wait();
                  } catch (Exception e) {
                  }
                }
              }

              try {
                if (sleepTime > 0)
                  Thread.sleep(sleepTime);
              } catch (InterruptedException e) {
              } 
            }
            phoenix.execute();
          }
        }
      }
    });
    thread.start();
  }

  public void setPhoenix(Phoenix phoenix) {
    this.phoenix = phoenix;
  }

  public Phoenix getPhoenix() {
    return phoenix;
  }


  @Override
  protected void onDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    phoenix.onDraw(canvas);
  }

  public void onStop() {
    this.stop = true; 
  }

  public void onPause() {
    synchronized(thread){
      this.pause = true;
    }
  }

  public void onRestart() {
    synchronized (thread) {
      this.pause = false;
      thread.notify();
    }
  }

  public void onDestroy(){
    destroy = true;
  }

  
  protected int convertButtonToControlPos(ButtonType button) {
    switch (button) {
    case SHIELD: return Phoenix.CONTROL_DOWN; 
    case START1: return Phoenix.CONTROL_START1; 
    case START2: return Phoenix.CONTROL_START2; 
    default: return Phoenix.CONTROL_FIRE;
    }
  }

  protected int convertJoystickToControlPos(Direction direction) {
    switch (direction) {
    case DOWN: return Phoenix.CONTROL_DOWN; 
    case RIGHT: return Phoenix.CONTROL_RIGHT; 
    case LEFT: return Phoenix.CONTROL_LEFT; 
    // this game has no use for UP joystick
    default: return -1;
    }
  }

  @Override
  public void onButton(ButtonType button, ButtonState state) {
    phoenix.setGameControlFlags(convertButtonToControlPos(button), state==ButtonState.RELEASE);
  }
  @Override
  public void onCoinInserted() {
    // TODO: check if this is the best way to handle press+release events
    phoenix.setGameControlFlags(Phoenix.CONTROL_COIN, true);
    scheduler.schedule(new Runnable() {
      public void run() {
        phoenix.setGameControlFlags(Phoenix.CONTROL_COIN, false);
      }
    }, 1, TimeUnit.SECONDS);
  }
  @Override
  public void onJoystick(Direction direction, ButtonState state) {
    int control=convertJoystickToControlPos(direction);
    if (control>=0) {
      phoenix.setGameControlFlags(control, state==ButtonState.PRESS);
    }
  }
  
}
