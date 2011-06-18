package com.muriloq.android.phoenix;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class PhoenixGame extends LinearLayout {

    private Phoenix phoenix;
    
    private boolean stop = false;
    private boolean pause = false;
    private boolean destroy = false;
    private long sleepTime;
    private long timeNow;
    private long timeBefore;
    private long interruptCounter;

    private Thread thread;

    

    public PhoenixGame(Context context) {
        super(context);
    }
    public PhoenixGame(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.phoenix = new Phoenix(this);
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay(); 
        this.phoenix.setActualDimensions(display.getWidth(), display.getHeight());
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
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			doKey(1, KeyEvent.KEYCODE_MENU);
			break;

		case MotionEvent.ACTION_UP:
			doKey(0, KeyEvent.KEYCODE_MENU);
			break;
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		doKey(1, keyCode);
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		doKey(0, keyCode);
		return super.onKeyUp(keyCode, event);
	}
    
    protected void doKey(int down, int ascii){
    	switch (ascii) {
		case KeyEvent.KEYCODE_DPAD_UP:
			ascii='3';
			break;
		case KeyEvent.KEYCODE_MENU:
			ascii=32;
			break;	
		case KeyEvent.KEYCODE_DPAD_CENTER:
			ascii='1';
			break;
		}
    	Log.d("Game","Press "+ascii+" down "+down);
    	phoenix.doKey(down, ascii);
    }

}
