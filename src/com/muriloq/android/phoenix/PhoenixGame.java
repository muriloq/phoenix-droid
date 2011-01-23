package com.muriloq.android.phoenix;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class PhoenixGame extends LinearLayout {

    private Phoenix phoenix;
    
    private boolean stop = false;
    private boolean pause = false;
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
                        while (true) {
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

}
