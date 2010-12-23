package com.muriloq.android.phoenix;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.muriloq.phoenix.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

public class PhoenixActivity extends Activity {

    private Phoenix phoenix;

    private long sleepTime;
    private long timeNow;
    private long timeBefore;
    private long interruptCounter;

    private Timer timer = new Timer();
    private TimerTask timerTask;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Surface surface = new Surface(Phoenix.WIDTH_PIXELS * Phoenix.SCALE_PIXELS, Phoenix.HEIGHT_PIXELS * Phoenix.SCALE_PIXELS);
        
        surface.getElement().setAttribute("style", "width: " + Phoenix.WIDTH_PIXELS + "px;");
        surface.getElement().focus();

        phoenix = new Phoenix(surface);
        phoenix.loadRoms(loadRoms());
        phoenix.initSFX();
        phoenix.decodeChars();
        phoenix.hiload();

        timerTask = new TimerTask() {
            @Override
            public void run() {
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
                        if (phoenix.isRealSpeed() && (sleepTime > 0)) {
                            timer.schedule(timerTask, (int) sleepTime);
                        } else {
                            timer.schedule(timerTask, 1);
                        }
                        break;
                    }
                    phoenix.execute();
                }
            }
        };

        // Start program execution
        timer.schedule(timerTask, 1);
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
        InputStream is  = new FileInputStream(name);        
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        phoenix.doKey(1, event.getKeyCode());
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        phoenix.doKey(0, event.getKeyCode());
        return false;
    }
    
    
}