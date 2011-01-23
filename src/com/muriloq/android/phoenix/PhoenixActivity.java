package com.muriloq.android.phoenix;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.muriloq.android.phoenix.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;

public class PhoenixActivity extends Activity {
	private PhoenixLayout view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater factory = LayoutInflater.from(this);
        
        // Set game layout
        view = (PhoenixLayout) factory.inflate(R.layout.main, null);
        setContentView(view);
        
       
        initialize(); 

//      android.os.Debug.startMethodTracing("/tmp/Phoenix");
        view.fire(); 
        
        // Enable view key events
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
        
	}

    
    @Override
    protected void onStop() {
    	super.onStop();
    	view.onStop();  
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	view.onPause(); 
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	view.onRestart();
    }
    
    @Override
    protected void onDestroy(){
        super.onDestroy(); 
//     android.os.Debug.stopMethodTracing();

    }
    
    private void initialize() {
        Phoenix phoenix = view.getPhoenix();
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
        InputStream is = getAssets().open(name);
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
}