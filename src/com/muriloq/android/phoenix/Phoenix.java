package com.muriloq.android.phoenix;


import java.util.ArrayList;
import java.util.Arrays;

import com.muriloq.phoenix.i8080;

//import gwt.g2d.client.graphics.Color;
//import gwt.g2d.client.graphics.KnownColor;
//import gwt.g2d.client.graphics.Surface;
//import gwt.g2d.client.graphics.canvas.CanvasElement;
//import gwt.g2d.client.graphics.canvas.ImageDataAdapter;
//
//import com.allen_sauer.gwt.voices.client.Sound;
//import com.allen_sauer.gwt.voices.client.Sound.LoadState;
//import com.allen_sauer.gwt.voices.client.SoundController;
//import com.google.gwt.event.dom.client.KeyCodes;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;


/*
	Phoenix Arcade Emulator for GWT
    Official Home-page: http://gwt-phoenix.appspot.com

    Emulator by Murilo Saraiva de Queiroz (muriloq@gmail.com) based on 
    Phoenix Emulator by Richard Davies (R.Davies@dcs.hull.ac.uk) and MAME 
    project, by Nicola Salmoria (MC6489@mclink.it) and others.

    The emulator structure, and many solutions are based in Jasper, the 
    Java Spectrum Emulator, by Adam Davidson & Andrew Pollard.
        Used with permission.
    
    The machine architecture information is from Ralph Kimmlingen
    (ub2f@rz.uni-karlsruhe.de).

Phoenix Hardware Specification
Resolution 26x8 = 208 columns x 32x8 = 256 lines
Phoenix memory map

  0000-3fff 16Kb Program ROM
  4000-43ff 1Kb Video RAM Charset A (4340-43ff variables)
  4400-47ff 1Kb Work RAM
  4800-4bff 1Kb Video RAM Charset B (4840-4bff variables)
  4c00-4fff 1Kb Work RAM
  5000-53ff 1Kb Video Control write-only (mirrored)
  5400-47ff 1Kb Work RAM
  5800-5bff 1Kb Video Scroll Register (mirrored)
  5c00-5fff 1Kb Work RAM
  6000-63ff 1Kb Sound Control A (mirrored)
  6400-67ff 1Kb Work RAM
  6800-6bff 1Kb Sound Control B (mirrored)
  6c00-6fff 1Kb Work RAM
  7000-73ff 1Kb 8bit Game Control read-only (mirrored)
  7400-77ff 1Kb Work RAM
  7800-7bff 1Kb 8bit Dip Switch read-only (mirrored)
  7c00-7fff 1Kb Work RAM
  
  memory mapped ports:
  
    read-only:
    7000-73ff IN
    7800-7bff Dip-Switch Settings (DSW)
    
    * IN (all bits are inverted)
    * bit 7 : barrier
    * bit 6 : Left
    * bit 5 : Right
    * bit 4 : Fire
    * bit 3 : -
    * bit 2 : Start 2
    * bit 1 : Start 1
    * bit 0 : Coin
    
    * Dip-Switch Settings (DSW)
    * bit 7 : VBlank
    * bit 6 : free play (pleiads only)
    * bit 5 : attract sound 0 = off 1 = on (pleiads only?)
    * bit 4 : coins per play	0 = 1 coin	1 = 2 coins
    * bit 3 :\ bonus
    * bit 2 :/ 00 = 3000	01 = 4000  10 = 5000  11 = 6000
    * bit 1 :\ number of lives
    * bit 0 :/ 00 = 3  01 = 4  10 = 5  11 = 6
     
    Pallete
    0 bit 5 of video ram value (divides 256 chars in 8 color sections)
    1 bit 6 of video ram value (divides 256 chars in 8 color sections)
    2 bit 7 of video ram value (divides 256 chars in 8 color sections)
    3 bit 0 of pixelcolor  (either from CHAR-A or CHAR-B, depends on Bit5)
    4 bit 1 of pixelcolor  (either from CHAR-A or CHAR-B, depends on Bit5) 
    5 0 = CHAR-A, 1 = CHAR-B
    6 palette flag (see video control reg.)
    7 always 0
*/



public class Phoenix extends i8080 {
    private Paint paint = new Paint();
	private Bitmap backBitmap;
	private Bitmap frontBitmap;
    private Bitmap workBitmap;
    private Canvas workCanvas;
	
    private int characters[][]; // decoded characters, for each palette

//    private SoundController soundController = null; 
//    private Sound laserSFX = null;
//    private Sound explosionSFX = null;
//    private Sound blowSFX = null;
//    private Sound shieldSFX = null;
//    private Sound hitSFX = null;
    private int savedHiScore=0;

    public static final int WIDTH_PIXELS = 208;
    public static final int HEIGHT_PIXELS = 256;
    public float SCALE_PIXELS = 1;
    private int WIDTH = Phoenix.WIDTH_PIXELS;
	private int HEIGHT = Phoenix.HEIGHT_PIXELS;

    private byte[] chr = new byte [0x2000]; // CHARSET roms

    private boolean vBlank = false;    
    private int scrollRegister   = 0;
    private int oldScrollRegister  = 0;
    private int palette = 0;
    
    private boolean backgroundRefresh = true;
    private boolean foregroundRefresh = true;
    private ArrayList<Integer> dirtyForeground = new ArrayList<Integer>();
    private ArrayList<Integer> dirtyBackground = new ArrayList<Integer>();
    
    private int[] gameControl = new int [8];
    private int interruptCounter = 0;
    
    private boolean autoFrameSkip=false;
    private boolean realSpeed=true;
    private boolean mute=false;
    private int frameSkip = 1;
    public  long timeOfLastFrameInterrupt = 0;
    private long  timeNow;
    private long  timeBefore;
    private float framesPerSecond;
    public int msPerFrame = 1000/60;
    
    // R, G, B, Alpha
    int OPAQUE_BLACK = Color.argb(255, 0,0,0); // opaque!
    int BLACK = Color.argb(0, 0, 0, 0); // transparent!
    int WHITE = Color.argb(0xff, 0xdb, 0xdb, 0xdb);
    int RED = Color.argb(0xff, 0xff, 0, 0);
    int GREEN = Color.argb(0xff, 0, 0xff, 0);
    int BLUE = Color.argb(0xff, 0x24, 0x24, 0xdb);
    int CYAN = Color.argb(0xff, 0, 0xff, 0xdb);
    int YELLOW = Color.argb(0xff, 0xff, 0xff, 00);
    int PINK = Color.argb(0xff, 0xff, 0xb6, 0xdb);
    int ORANGE = Color.argb(0xff, 0xff, 0xb6, 0x49);
    int LTPURPLE = Color.argb(0xff, 0xff, 0x24, 0xb6);
    int DKORANGE = Color.argb(0xff, 0xff, 0xb6, 0x00);
    int DKPURPLE = Color.argb(0xff, 0xb6, 0x24, 0xff);
    int DKCYAN = Color.argb(0xff, 0x00, 0xdb, 0xdb);
    int DKYELLOW = Color.argb(0xff, 0xdb, 0xdb, 0x00);
    int BLUISH = Color.argb(0xff, 0x95, 0x95, 0xff);
    int PURPLE = Color.argb(0xff, 0xff, 0x00, 0xff);
    
    // pallete x charset x character = color 
    // 4 colors per pixel * 8 groups of characters * 2 charsets * 2 pallettes
    int colorTable[]={
        /* charset A pallette A */
        BLACK,BLACK,CYAN,CYAN,      // Background, Unused, Letters, asterisks
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Numbers/Ship, Ship edge
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Ship, Ship edge/bullets
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,WHITE,PURPLE,YELLOW,  // Background, Explosions
        BLACK,PURPLE,GREEN,WHITE,   // Background, Barrier
        /* charset A pallette B */
        BLACK,BLUE,CYAN,CYAN,       // Background, Unused, Letters, asterisks
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Numbers/Ship, Ship edge
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Ship, Ship edge/bullets
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,WHITE,RED,PURPLE,     // Background, Explosions
        BLACK,PURPLE,GREEN,WHITE,   // Background, Barrier
        /* charset B pallette A */
        BLACK,RED,BLUE,WHITE,           // Background, Starfield
        BLACK,PURPLE,BLUISH,DKORANGE,   // Background, Planets
        BLACK,DKPURPLE,GREEN,DKORANGE,  // Background, Mothership: turrets, u-body, l-body
        BLACK,BLUISH,DKPURPLE,LTPURPLE, // Background, Motheralien: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, shell
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        /* charset B pallette B */
        BLACK,RED,BLUE,WHITE,           // Background, Starfield
        BLACK,PURPLE,BLUISH,DKORANGE,   // Background, Planets
        BLACK,DKPURPLE,GREEN,DKORANGE,  // Background, Mothership: turrets, upper body, lower body
        BLACK,BLUISH,DKPURPLE,LTPURPLE, // Background, Motheralien: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, shell
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
    };
	private int sleepTime;
    private boolean scrollRefresh;
    private PhoenixGame view;

	

//    private Sound sound;

    public Phoenix(PhoenixGame view){
        // Phoenix runs at 0.74 Mhz (?)
        super(0.74);
//		this.canvas = new Canvas(surface);
        this.view = view; 
//
//        for ( int i=0;i<8;i++ ) gameControl[i]=1;
//        sound = new Sound();
    }


    public void setActualDimensions(int width, int height) {
        this.SCALE_PIXELS = HEIGHT_PIXELS/ (float) height;
        this.WIDTH = width;
        this.HEIGHT = (int) (HEIGHT_PIXELS*WIDTH/WIDTH_PIXELS);
        workBitmap = Bitmap.createBitmap(WIDTH_PIXELS, HEIGHT_PIXELS, Bitmap.Config.ARGB_8888);
        workCanvas = new Canvas(workBitmap);
    }


    /** Byte access */
    public void pokeb( int addr, int newByte ) {

        addr &= 0xffff;

        if ( addr >=  0x5800 && addr <= 0x5bff ) {
            scrollRegister = newByte;
            if ( scrollRegister != oldScrollRegister ) {
                oldScrollRegister = scrollRegister;
                scrollRefresh = true;
            }
        }
        // 26x32
        // 4000-43ff 1Kb Video RAM Charset A (4340-43ff variables)
        // 4800-4bff 1Kb Video RAM Charset B (4840-4bff variables)
        
        // Write on foreground
        if ( (addr >= 0x4000) && (addr <= 0x4340) ){
            dirtyForeground.add(addr);
            foregroundRefresh = true; 
        }
        
        if ( (addr >= 0x4800)&&(addr <= 0x4b40) ) {
            dirtyBackground.add(addr);
            backgroundRefresh = true;  
        }

        if ( addr >= 0x5000 && addr <= 0x53ff ) {
            palette = newByte & 0x01; 
        }
        
        if ( addr >= 0x6000 && addr <= 0x63ff) {
            if ( peekb(addr)!=newByte ) {
                mem[addr] = newByte;
                // sound.updateControlA((byte)newByte);
                if (!isMute()) {
//                    if ( newByte==143 ) explosionSFX.play ();
//                    if ( (newByte>101)&&(newByte<107) ) laserSFX.play ();
//                    if ( newByte==80 ) blowSFX.play ();
                }
                // canvasGraphics.setFocus(true);
            }
        }

        if ( addr >= 0x6800 && addr <= 0x6bff) {
            if ( peekb(addr)!=newByte ) {
                mem[ addr ] = newByte;
                // sound.updateControlB((byte) newByte);
                if (!isMute()){
//                    if ( newByte==12 ) shieldSFX.play ();
//                    if ( newByte==2 ) hitSFX.play ();
                }
                // canvasGraphics.setFocus(true);
            }
        }

        // Hi Score Saving - Thanks MAME ! :)
        if ( addr == 0x438c ) {
            if ( newByte == 0x0f ) {
                mem[addr]=newByte;
                int hiScore = getScore(0x4388);
                if ( hiScore > savedHiScore ) hisave();
                if ( hiScore < savedHiScore ) hiload();
            }
        }

        if ( addr >= 0x4000 ) {   // 0x0000 - 0x3fff Program ROM 
            mem [addr]=newByte; 
        }

        return;
    }

    /** Word access */
    public void pokew( int addr, int word ) {
        addr &= 0xffff;
        int _mem[] = mem;
        if ( addr >= 0x4000 ) {
            _mem[ addr ] = word & 0xff;
            if ( ++addr != 65536 ) {
                _mem[ addr ] = word >> 8;
            }
        }
        return;
    }

	public int peekb(int addr) {
        addr &= 0xffff;
        
        // are we reading dip switch memory ?
        if (addr >= 0x7800 && addr <= 0x7bff) { 
            // if SYNC bit of switch is 1
            if (vBlank) { 
                vBlank = false; // set it to 0
                return 128;     // return value where bit 7 is 1
            } else
                return 0;       // return value where bit 7 is 0
        }
        
        // are we reading the joystick ?
        if (addr >= 0x7000 && addr <= 0x73ff) {
            int c = 0;
            for (int i = 0; i < 8; i++)
                c |= gameControl[i] << i;
            return c;
        }

        // we are reading a standard memory address
        else
            return mem[addr];
    }

    public int peekw( int addr ) {
        addr &= 0xffff;
        int t = peekb( addr );
        addr++;
        return t | (peekb( addr ) << 8);
    }

    
    public void initSFX() {
//        soundController = new SoundController();
//        this.laserSFX = loadSFX("laser"); 
//        this.explosionSFX = loadSFX("explo");
//        this.blowSFX = loadSFX("blow");
//        this.shieldSFX = loadSFX("shield");
//        this.hitSFX = loadSFX("hit");
    }


//    public Sound loadSFX(String name) {
//        Sound sfx = soundController.createSound(Sound.MIME_TYPE_AUDIO_OGG_VORBIS, name+".ogg");
//        sfx.play();
//        if (LoadState.LOAD_STATE_NOT_SUPPORTED == sfx.getLoadState()){
//            sfx = soundController.createSound(Sound.MIME_TYPE_AUDIO_MPEG_MP3, name+".mp3");
//            sfx.play();
//            if (LoadState.LOAD_STATE_NOT_SUPPORTED == sfx.getLoadState()){
//                sfx = soundController.createSound(Sound.MIME_TYPE_AUDIO_WAV_PCM, name+".wav");
//                sfx.play();
//            }
//        }
//        System.out.println("Loaded "+sfx.getMimeType()+", "+sfx.getSoundType()+", "+sfx.getLoadState());
//        return sfx;
//    }

    // The Hi Score is BCD (Binary Coded Decimal).
    // We convert this to integer here.
    public int getScore(int Addr) {
        int score=0;
        score += (int) (peekb (Addr+3)/16) * 10     + (peekb (Addr+3) % 16);
        score += (int) (peekb (Addr+2)/16) * 1000   + (peekb (Addr+2) % 16)*100;
        score += (int) (peekb (Addr+1)/16) * 100000   + (peekb (Addr+1) % 16)*10000;
        score += (int) (peekb (Addr)  /16) * 10000000 + (peekb (Addr) % 16)*1000000;
        return score;
    }

    public void hisave () {
        // Hi score saving. Again, thanks MAME project... :)
        int OneScore=getScore(0x4380);
        int TwoScore=getScore(0x4384);
        int HiScore=getScore(0x4388);
        int HiAddress = 0x4388;
        if ( OneScore > HiScore ) HiAddress=0x4380;
        if ( TwoScore > HiScore ) HiAddress=0x4384;


//        try {
//            // URL baseURL = applet.getDocumentBase();
//            OutputStream os;
//            /*
//            if (baseURL != null) {
//            URL scoreURL = new URL (baseURL, "hiscore.sav");
//            URLConnection connection = new URLConnection (scoreURL);
//            os = connection.getOutputStream();				
//            }
//            else {
//            */
//            File scoreFile = new File ("hiscore.sav");
//            os = new FileOutputStream (scoreFile);
//            //}
//            for ( int i=0;i<4;i++ ) {
//                os.write ((byte) peekb(HiAddress+i));
//            }
//            os.flush();
//            os.close();
//        } catch ( Exception e ) {
//            System.out.println ("Error saving high score");
//        }
        savedHiScore = getScore (HiAddress);
        System.out.println ("High Score: "+savedHiScore+" saved.");
    }

    public void hiload () {
        int HiAddress = 0x4388;
//        try {
//            // URL baseURL = applet.getDocumentBase();
//            InputStream is;
//            /*
//            if (baseURL != null) {
//            URL scoreURL = new URL (baseURL, "hiscore.sav");
//            URLConnection connection = new URLConnection (scoreURL);
//            os = connection.getOutputStream();				
//            }
//            else {
//            */
//            File scoreFile = new File ("hiscore.sav");
//            is = new FileInputStream (scoreFile);
//            //}
//            for ( int i=0;i<4;i++ ) mem [HiAddress+i]=is.read ();
//            is.close();
//        } catch ( Exception e ) {
//            System.out.println ("Error loading high score");
//        }
        // Force hi score atualizing
        pokeb(0x41e1, (peekb(0x4389) / 16)+0x20);
        pokeb(0x41c1, (peekb(0x4389) & 0xf)+0x20);
        pokeb(0x41a1, (peekb(0x438a) / 16)+0x20);
        pokeb(0x4181, (peekb(0x438a) & 0xf)+0x20);
        pokeb(0x4161, (peekb(0x438b) / 16)+0x20);
        pokeb(0x4141, (peekb(0x438b) & 0xf)+0x20);

        savedHiScore = getScore(HiAddress);
        System.out.println ("High Score: "+savedHiScore+" loaded.");
    }

    /** 
     * 0x0000 - 0x3FFF: Program ROM
     * 0x4000 - 0x5FFF: Graphics ROM
     * @param buffer
     */
    public void loadRoms(byte[] buffer){
        for ( int i=0;i<=0x3fff;i++ ) {
            mem[i]=(buffer[i]+256)&0xff;
        }
        for ( int i=0;i<=0x1fff;i++ ) {
            chr[i]=buffer[i+0x4000];
        }
    }
 
    public final int interrupt() {
        interruptCounter++;

        vBlank = true;

        if (interruptCounter % getFrameSkip() == 0)
            refreshScreen();

        // Update speed indicator every second
        if ((interruptCounter % 60) == 0) {
            timeNow = System.currentTimeMillis();
            msPerFrame = (int) (timeNow - timeBefore) + 1; // ms / frame
            framesPerSecond = 1000 / (msPerFrame / (float) 60); // frames / s
            timeBefore = timeNow;
        }

        return super.interrupt();
    }


    public void refreshScreen () {
        if (backBitmap==null)
            this.backBitmap = Bitmap.createBitmap(WIDTH_PIXELS, HEIGHT_PIXELS, Config.ARGB_8888);
        
        if (frontBitmap == null)
            this.frontBitmap = Bitmap.createBitmap(WIDTH_PIXELS, HEIGHT_PIXELS, Config.ARGB_8888);

        if ( (!backgroundRefresh && !foregroundRefresh) && !scrollRefresh) return; 
        
//      4000-43ff 1Kb Video RAM Charset A (4340-43ff variables)
//      4800-4bff 1Kb Video RAM Charset B (4840-4bff variables)

        if (backgroundRefresh) {
            for (int a: dirtyBackground) {
                int base = a - 0x4800;
                int x = 25 - (base / 32);
                int y = base % 32;
                int character = mem[a];
                for ( int i=0;i<8;i++ ) {
                    for ( int j=0;j<8;j++ ) {
                        int c = characters[palette][character*64+j+i*8];
                        int px = x*8+j;
                        int py = y*8+i;
                        if ((px < 0) || (px >= WIDTH_PIXELS) || (py < 0) || (py >= HEIGHT_PIXELS))
                            continue;
                        backBitmap.setPixel(px,py, c);
                    }
                }
            }
            
//            backGraphics.drawBitmap(backImageData, 0, 0, paint);
//            backCanvas = backGraphics.getCanvas();
            backgroundRefresh = false;
            dirtyBackground.clear();
        }
         
        if (foregroundRefresh) {
            for (int a: dirtyForeground) {
                int base = a - 0x4000;
                int x = 25 - (base / 32);
                int y = base % 32;
                int character = mem[a];
                for ( int i=0;i<8;i++ ) {
                    for ( int j=0;j<8;j++ ) {
                        int c = characters[palette][64*256+character*64+j+i*8];
                        int px = x*8+j;
                        int py = y*8+i;
                        if ((px < 0) || (px >= WIDTH_PIXELS) || (py < 0) || (py >= HEIGHT_PIXELS))
                               continue;
                        frontBitmap.setPixel(px,py, c);
                    }
                }
            }
//            frontGraphics.putImageData(frontImageData, 0, 0);
//            frontCanvas = frontGraphics.getCanvas();
            foregroundRefresh = false;
            dirtyForeground.clear();
        }
        view.postInvalidate();
        
    }
    
    public void onDraw(Canvas canvas){
        if ((backBitmap==null) || (frontBitmap == null))
            return; 
          paint.setColor(OPAQUE_BLACK);
          workCanvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        
        workCanvas.drawBitmap(backBitmap, 0, HEIGHT_PIXELS-scrollRegister, null);

        
        workCanvas.drawBitmap(backBitmap, 0,-scrollRegister,null);
        
        scrollRefresh = false; 
        
        workCanvas.drawBitmap(frontBitmap, 0,0, null); 
        
        canvas.drawBitmap(workBitmap, new Rect(0,0,WIDTH_PIXELS, HEIGHT_PIXELS), new Rect(0,0,WIDTH,HEIGHT), null); 
        
        paint.setColor(DKYELLOW);
        canvas.drawText(Integer.toString((int)framesPerSecond),0,HEIGHT-16, paint);
        
        paint.setColor(GREEN);
        if (!isRealSpeed())
            canvas.drawText("S",WIDTH-24,HEIGHT-16, paint);
        
        if ( (isAutoFrameSkip()) && (getFrameSkip()!=1) )
            canvas.drawText("A",WIDTH-32,HEIGHT-16, paint);

        if (isMute())
            canvas.drawText("M",WIDTH-48,HEIGHT-16, paint);

        if (getFrameSkip() != 1)
            canvas.drawText(Integer.toString((int)getFrameSkip()),WIDTH-16,HEIGHT-16, paint);
    }

    public void decodeChars (Canvas canvas) {
        int[] pixels = new int[WIDTH_PIXELS*HEIGHT_PIXELS];
        characters = new int[2][512*64];

        for ( int s=0;s<2;s++ ) {               // Charset
            for ( int c=0;c<256;c++ ) {         // Character
                byte[][] block = new byte[8][8];
                for ( int plane=0; plane<2;plane++ ) {  // Bit plane
                    for ( int line=0; line<8; line++ ) {  // line
                        byte b = (byte) chr[s*4096+c*8+plane*256*8+line];
                        byte[] bin= new byte[8];                         // binary representation 
                        bin[0]=(byte) ((b & 1)>>0);
                        bin[1]=(byte) ((b & 2)>>1);
                        bin[2]=(byte) ((b & 4)>>2);
                        bin[3]=(byte) ((b & 8)>>3);
                        bin[4]=(byte) ((b & 16)>>4);
                        bin[5]=(byte) ((b & 32)>>5);
                        bin[6]=(byte) ((b & 64)>>6);
                        bin[7]=(byte) ((b & 128)>>7);
                        for ( int col=0;col<8;col++ ) {   // Coluna
                            block[line][col]+= (1+(1-plane))*bin[col];
                            int pixelColorIndex = 0; 
                             pixelColorIndex = (( (c & 0xff) >> 5 )&0xff)*4; // bits 5-7 of video ram value
                             pixelColorIndex += block[line][col];            // pixel color
                             pixelColorIndex += (1-s) * 64;                  // charset

                             // Draw characters on screen
                             if ( (block[line][col]>0) ) {
                                 paint.setColor(colorTable[pixelColorIndex]);
                             } else {
                                 paint.setColor(BLACK);
                             }
                             int top = 7-line+(c%26)*8;
                             int left = col+((int)c/26)*8+s*160;
                             pixels[left*WIDTH_PIXELS+top]=paint.getColor(); 
                             // canvas.drawRect(top,left,top+1,left+1,paint);

                             // Palette A
                             int color = colorTable[pixelColorIndex];
                             if (color == BLACK) 
                                 color = Color.TRANSPARENT; 
                             characters[0][s*256*64+c*64+col*8+7-line] = color;
                             
                             // Palette B
                             color = colorTable[pixelColorIndex+32];
                             if (color == BLACK) 
                                 color = Color.TRANSPARENT; 
                             characters[1][s*256*64+c*64+col*8+7-line] = color;

                        } // for col
                    } // for line
                } // for plane
            } // for c

        } // for s
        if (canvas!=null) {
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, WIDTH_PIXELS, WIDTH_PIXELS, HEIGHT_PIXELS, Bitmap.Config.ARGB_8888);
            canvas.drawBitmap(bitmap, new Rect(0,0,WIDTH_PIXELS, HEIGHT_PIXELS), new Rect(0,0,WIDTH,HEIGHT),null);
        }
    }

    public final boolean doKey( int down, int ascii) {
    	switch ( ascii ) {
	        case '3':  	gameControl[0]=1-down;    break; // Coin
	        case '1':  	gameControl[1]=1-down;    break; // Start 1
	        case '2':  	gameControl[2]=1-down;    break; // Start 2
	        case 32 :  	gameControl[4]=1-down;    break; // Fire
	        case 'a': case 'A': if (down==0) this.autoFrameSkip = !autoFrameSkip; break;   // toggle auto frame skip
	        case 's': case 'S': if (down==0) this.realSpeed = !realSpeed; setFrameSkip(1); break;   // toggle speed limiter
	        case 'm': case 'M': if (down==0) this.mute = !mute; break;   // toggle speed limiter
	        case KeyEvent.KEYCODE_DPAD_RIGHT:   gameControl[5]=1-down;   break;   // Right
	        case KeyEvent.KEYCODE_DPAD_LEFT:   	 gameControl[6]=1-down;  break;  // Left
	        case KeyEvent.KEYCODE_DPAD_DOWN:   gameControl[7]=1-down;    break;  // Barrier
	        case KeyEvent.KEYCODE_MINUS:  setFrameSkip(getFrameSkip() - down); 
	            if ( getFrameSkip() < 1 ) setFrameSkip(1);             // Decrease frame skip
	            break;
	        case KeyEvent.KEYCODE_PLUS:  setFrameSkip(getFrameSkip() + down);       // Increase frame skip
	            break;
          
        }
    	Log.d("Pheonix","Gamecontrol "+ Arrays.toString(gameControl));
        return true;
    }

	public float getSleepTime() {
		return sleepTime;
	}

    public void setFrameSkip(int frameSkip) {
        this.frameSkip = frameSkip;
    }

    public int getFrameSkip() {
        return frameSkip;
    }


    public float getFramesPerSecond() {
        return framesPerSecond;
    }


    public void setAutoFrameSkip(boolean autoFrameSkip) {
        this.autoFrameSkip = autoFrameSkip;
    }


    public boolean isAutoFrameSkip() {
        return autoFrameSkip;
    }


    public void setRealSpeed(boolean realSpeed) {
        this.realSpeed = realSpeed;
    }


    public boolean isRealSpeed() {
        return realSpeed;
    }


    public void setMute(boolean mute) {
        this.mute = mute;
    }


    public boolean isMute() {
        return mute;
    }
	
}