package com.muriloq.android.phoenix;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/****************************************************************************
 *
 * Phoenix sound hardware simulation - still very ALPHA!
 *
 * If you find errors or have suggestions, please mail me.
 * Juergen Buchmueller <pullmoll@t-online.de>
 *
 ****************************************************************************/

public class Sound implements Runnable {
    public final int VMIN = 0;
    public final int VMAX = 0xff;
    private final float C18a = 0.01e-6f;
    private final float C18b = 0.48e-6f;
    private final float C18c = 1.01e-6f;
    private final float C18d = 1.48e-6f;
    private final float C20 = 10.0e-6f;
    private final float C22 = 100.0e-6f;
    private final float C24 = 6.8e-6f;
    private final float C25 = 6.8e-6f;
    private final float C7 = 6.8e-6f;
    private final float C7_MAX = (VMAX * 551 / 500);
    private final float C7_MIN = (VMAX * 254 / 500);
//    private final float C7_DIFF = (C7_MAX - C7_MIN);
//    private final int R22 = 47000;
    private final int R22pR24 = 19388;
    private final int R23 = 100000;
//    private final int R24 = 33000;
    private final int R40 = 47000;
    private final int R41 = 100000;
    private final int R42 = 10000;
    private final int R43 = 570000;
    private final int R44 = 570000;
    private final int R45 = 51000;
    private final int R46 = 51000;
    private final int R49 = 1000;
    private final int R50 = 1000;
    private final int R51 = 330;
    private final int R52 = 20000;
    private final int R53 = 330;
    private final int R54 = 47000;
    private final int RP = 27777;
    private final float rate[][] = {
            { VMAX * 2 / 3 / (0.693f * (R40 + R41) * C18a), VMAX * 2 / 3 / (0.693f * (R40 + R41) * C18b),
                    VMAX * 2 / 3 / (0.693f * (R40 + R41) * C18c), VMAX * 2 / 3 / (0.693f * (R40 + R41) * C18d) },
            { VMAX * 2 / 3 / (0.693f * R41 * C18a), VMAX * 2 / 3 / (0.693f * R41 * C18b), VMAX * 2 / 3 / (0.693f * R41 * C18c),
                    VMAX * 2 / 3 / (0.693f * R41 * C18d) } };

    private int sound_latch_a;
    private int sound_latch_b;

//    private int channel;

    private int tone1_vco1_cap;
    private int tone1_level;
    private int tone2_level;

    private int[] poly18;

    private int t1v1Output;
    private int t1v1Counter;
    private int t1v1Level;

    private int t1v2Output;
    private int t1v2Counter;
    private int t1v2Level;

    private int t1vCounter;
    private int t1vLevel;
    private int t1vRate;
    private int t1vCharge;

    private int t1Counter;
    private int t1Divisor;
    private int t1Output;

    private int t2vCounter;
    private int t2vLevel;

    private int t2Counter;
    private int t2Divisor;
    private int t2Output;

    private int c24Counter;
    private int c24Level;

    private int c25Counter;
    private int c25Level;

    private int nCounter;
    private int nPolyoffs;
    private int nPolybit;
    private int nLowpass_counter;
    private int nLowpass_polybit;

    private int sampleRate = 50000;
    private int playback_freq = 44100;
    private int buffer_size = 50000;
    private short[] buffer;

    private int music_buffer_size = 20000;
    private byte[] music_buffer;

//    private Mixer mixer;
//    private SourceDataLine line;
//    private SourceDataLine musicLine;

    private Thread thread;
    private boolean running = true;
    private TMS36XX music;
	private AudioTrack audiotrack;

    public Sound() {
        int shiftreg = 0;
        poly18 = new int[1 << (18 - 5)];

        for (int i = 0; i < (1 << (18 - 5)); i++) {
            int bits = 0;
            for (int j = 0; j < 32; j++) {
                bits = (bits >> 1) | (shiftreg << 31);
                if (((shiftreg >> 16) & 1) == ((shiftreg >> 17) & 1))
                    shiftreg = (shiftreg << 1) | 1;
                else
                    shiftreg <<= 1;
            }
            poly18[i] = bits;
        }

        // channel = stream_init("Custom", 50, Machine->sampleRate, 0, phoenix_sound_update); if ( channel == -1 ) return 1;

        this.buffer = new short[buffer_size];
        this.music_buffer = new byte[music_buffer_size];

//      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
//      for (int i = 0; i < mixerInfo.length; i++) {
//          System.out.println(mixerInfo[i]);
//      }
//      this.mixer = AudioSystem.getMixer(mixerInfo[0]);
//
//        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, (float) playback_freq, 8, 1, 1, playback_freq, true);
//        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//        int minSize =AudioTrack.getMinBufferSize( 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_8BIT );        
//        if (!AudioSystem.isLineSupported(info)) {
//            System.out.println("DataLine not supported in this AudioSystem:" + info);
//        }
//
//        try {
//            // line = (SourceDataLine) AudioSystem.getLine(info);
//            line = (SourceDataLine) mixer.getLine(info);
//            musicLine = (SourceDataLine) mixer.getLine(info);
//            line.open(format, buffer_size);
//            line.start();
//            musicLine.open(format, buffer_size);
//            musicLine.start();
//            music = new TMS36XX(musicLine, music_buffer);
//      } catch (LineUnavailableException e) {
//      System.out.println("DataLine not available.");
//  }

        // Android
		audiotrack = new AudioTrack(AudioManager.STREAM_MUSIC, playback_freq,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, buffer_size,
				AudioTrack.MODE_STREAM);
		audiotrack.write(buffer, 0, buffer_size);
		thread = new Thread(this, "TIA");
		thread.start();
		audiotrack.play();

		AudioTrack musictrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				playback_freq, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_8BIT, music_buffer_size,
				AudioTrack.MODE_STREAM);
		musictrack.write(music_buffer, 0, music_buffer_size);
		music = new TMS36XX(musictrack, music_buffer);
		musictrack.play();

	}

    public final int tone1_vco1(int samplerate) {
        if (t1v1Output != 0) {
            if (t1v1Level > VMAX * 1 / 3) {
                t1v1Counter -= rate[1][tone1_vco1_cap];
                if (t1v1Counter <= 0) {
                    int steps = -t1v1Counter / samplerate + 1;
                    t1v1Counter += steps * samplerate;
                    if ((t1v1Level -= steps) <= VMAX * 1 / 3) {
                        t1v1Level = VMAX * 1 / 3;
                        t1v1Output = 0;
                    }
                }
            }
        } else {
            if (t1v1Level < VMAX * 2 / 3) {
                t1v1Counter -= rate[0][tone1_vco1_cap];
                if (t1v1Counter <= 0) {
                    int steps = -t1v1Counter / samplerate + 1;
                    t1v1Counter += steps * samplerate;
                    if ((t1v1Level += steps) >= VMAX * 2 / 3) {
                        t1v1Level = VMAX * 2 / 3;
                        t1v1Output = 1;
                    }
                }
            }
        }
        return t1v1Output;
    }

    public final int tone1_vco2(int samplerate) {
        if (t1v2Output != 0) {
            if (t1v2Level > VMIN) {
                t1v2Counter -= (int) (VMAX * 2 / 3 / (0.693 * R44 * C20));
                if (t1v2Counter <= 0) {
                    int steps = -t1v2Counter / samplerate + 1;
                    t1v2Counter += steps * samplerate;
                    if ((t1v2Level -= steps) <= VMAX * 1 / 3) {
                        t1v2Level = VMAX * 1 / 3;
                        t1v2Output = 0;
                    }
                }
            }
        } else {
            if (t1v2Level < VMAX) {
                t1v2Counter -= (int) (VMAX * 2 / 3 / (0.693 * (R43 + R44) * C20));
                if (t1v2Counter <= 0) {
                    int steps = -t1v2Counter / samplerate + 1;
                    t1v2Counter += steps * samplerate;
                    if ((t1v2Level += steps) >= VMAX * 2 / 3) {
                        t1v2Level = VMAX * 2 / 3;
                        t1v2Output = 1;
                    }
                }
            }
        }

        return t1v2Output;
    }

    public final int tone1_vco(int samplet1vRate, int vco1, int vco2) {
        int voltage;

        if (t1vLevel != t1vCharge) {
            t1vCounter -= t1vRate;
            while (t1vCounter <= 0) {
                t1vCounter += samplet1vRate;
                if (t1vLevel < t1vCharge) {
                    if (++t1vLevel == t1vCharge)
                        break;
                } else {
                    if (--t1vLevel == t1vCharge)
                        break;
                }
            }
        }

        if (vco2 != 0) {
            if (vco1 != 0) {
                t1vCharge = VMAX;
                t1vRate = (int) ((t1vCharge - t1vLevel) / (RP * C22));
                voltage = t1vLevel + (VMAX - t1vLevel) * R46 / (R46 + R42);
            } else {
                t1vCharge = VMAX * 27 / 50;
                if (t1vCharge >= t1vLevel)
                    t1vRate = (int) ((t1vCharge - t1vLevel) / (R45 * C22));
                else
                    t1vRate = (int) ((t1vLevel - t1vCharge) / ((R46 + R42) * C22));
                voltage = t1vLevel * R42 / (R46 + R42);
            }
        } else {
            if (vco1 != 0) {
                t1vCharge = VMAX * 23 / 50;
                if (t1vCharge >= t1vLevel)
                    t1vRate = (int) ((t1vCharge - t1vLevel) / ((R42 + R46) * C22));
                else
                    t1vRate = (int) ((t1vLevel - t1vCharge) / (R45 * C22));
                voltage = t1vLevel + (VMAX - t1vLevel) * R46 / (R42 + R46);
            } else {
                t1vCharge = VMIN;
                t1vRate = (int) ((t1vLevel - t1vCharge) / (RP * C22));
                voltage = t1vLevel * R42 / (R46 + R42);
            }
        }
        return 24000 * 1 / 3 + 24000 * 2 / 3 * voltage / 32768;
    }

    public final short tone1(int samplerate) {
        int vco1 = tone1_vco1(samplerate);
        int vco2 = tone1_vco2(samplerate);
        int frequency = tone1_vco(samplerate, vco1, vco2);

        if ((sound_latch_a & 15) != 15) {
            t1Counter -= frequency;
            while (t1Counter <= 0) {
                t1Counter += samplerate;
                if (++t1Divisor == 16) {
                    t1Divisor = sound_latch_a & 15;
                    t1Output ^= 1;
                }
            }
        }

        int t = (t1Output != 0) ? tone1_level : -tone1_level;
		return (short) ( t > 32767 ? 32767 : (t < -32768 ? -32768 : t) );
    }

    public final int tone2_vco(int samplerate) {
        if ((sound_latch_b & 0x10) == 0) {
            t2vCounter -= (C7_MAX - t2vLevel) * 12 / (R23 * C7) / 5;
            if (t2vCounter <= 0) {
                int n = (-t2vCounter / samplerate) + 1;
                t2vCounter += n * samplerate;
                if ((t2vLevel += n) > C7_MAX)
                    t2vLevel = (int) C7_MAX;
            }
        } else {
            t2vCounter -= (t2vLevel - C7_MIN) * 12 / (R22pR24 * C7) / 5;
            if (t2vCounter <= 0) {
                int n = (-t2vCounter / samplerate) + 1;
                t2vCounter += n * samplerate;
                if ((t2vLevel -= n) < C7_MIN)
                    t2vLevel = (int) C7_MIN;
            }
        }
        return (10212 * t2vLevel / 32768);
    }

    public final int tone2(int samplerate) {
        int frequency = tone2_vco(samplerate);

        if ((sound_latch_b & 15) != 15) {
            t2Counter -= frequency;
            while (t2Counter <= 0) {
                t2Counter += samplerate;
                if (++t2Divisor == 16) {
                    t2Divisor = sound_latch_b & 15;
                    t2Output ^= 1;
                }
            }
        }
        int t = (t2Output != 0) ? tone2_level : -tone2_level;
        return (short) ( t > 32767 ? 32767 : (t < -32768 ? -32768 : t) );
    }

    public final int update_c24(int samplerate) {
        if ((sound_latch_a & 0x40) != 0) {
            if (c24Level > VMIN) {
                c24Counter -= (int) ((c24Level - VMIN) / (R52 * C24));
                if (c24Counter <= 0) {
                    int n = -c24Counter / samplerate + 1;
                    c24Counter += n * samplerate;
                    if ((c24Level -= n) < VMIN)
                        c24Level = VMIN;
                }
            }
        } else {
            if (c24Level < VMAX) {
                c24Counter -= (int) ((VMAX - c24Level) / ((R51 + R49) * C24));
                if (c24Counter <= 0) {
                    int n = -c24Counter / samplerate + 1;
                    c24Counter += n * samplerate;
                    if ((c24Level += n) > VMAX)
                        c24Level = VMAX;
                }
            }
        }
        return VMAX - c24Level;
    }

    public final int update_c25(int samplerate) {
        if ((sound_latch_a & 0x80) != 0) {
            if (c25Level < VMAX) {
                c25Counter -= (int) ((VMAX - c25Level) / ((R50 + R53) * C25));
                if (c25Counter <= 0) {
                    int n = -c25Counter / samplerate + 1;
                    c25Counter += n * samplerate;
                    if ((c25Level += n) > VMAX)
                        c25Level = VMAX;
                }
            }
        } else {
            if (c25Level > VMIN) {
                c25Counter -= (int) ((c25Level - VMIN) / (R54 * C25));
                if (c25Counter <= 0) {
                    int n = -c25Counter / samplerate + 1;
                    c25Counter += n * samplerate;
                    if ((c25Level -= n) < VMIN)
                        c25Level = VMIN;
                }
            }
        }
        return c25Level;
    }

    public final short noise(int samplerate) {
        int vc24 = update_c24(samplerate);
        int vc25 = update_c25(samplerate);
        int sum = 0, level, frequency;

        if (vc24 < vc25)
            level = vc24 + (vc25 - vc24) / 2;
        else
            level = vc25 + (vc24 - vc25) / 2;

        frequency = 588 + 6325 * level / 32768;

        nCounter -= frequency;
        if (nCounter <= 0) {
            int n = (-nCounter / samplerate) + 1;
            nCounter += n * samplerate;
            nPolyoffs = (nPolyoffs + n) & 0x3ffff;
            nPolybit = (poly18[nPolyoffs >> 5] >> (nPolyoffs & 31)) & 1;
        }
        if (nPolybit == 0)
            sum += vc24;

        nLowpass_counter -= 400;
        if (nLowpass_counter <= 0) {
            nLowpass_counter += samplerate;
            nLowpass_polybit = nPolybit;
        }
        if (nLowpass_polybit == 0)
            sum += vc25;
        
        int t = sum;
        return (short) ( t > 32767 ? 32767 : (t < -32768 ? -32768 : t) );
    }

    public void process(short[] buffer, int length) {
        int bufferIndex = 0;
        while (length-- > 0) {
            int t = ((tone1(sampleRate) + tone2(sampleRate) + noise(sampleRate)) / 4);
            buffer[bufferIndex++] = (short) ( t > 32767 ? 32767 : (t < -32768 ? -32768 : t) );
        }
    }

    public void updateControlA(byte data) {
        if (data == sound_latch_a)
            return;

        // stream_update(channel,0);
        sound_latch_a = data;

        tone1_vco1_cap = (sound_latch_a >> 4) & 3;
        if ((sound_latch_a & 0x20) != 0)
            tone1_level = VMAX * 10000 / (10000 + 10000);
        else
            tone1_level = VMAX;
    }

    public void updateControlB(byte data) {
        if (data == sound_latch_b)
            return;

        // stream_update(channel,0);
        sound_latch_b = data;

        if ((sound_latch_b & 0x20) != 0)
            tone2_level = VMAX * 10 / 11;
        else
            tone2_level = VMAX;

        updateMusic(data);
    }

    /** Changes the tune that the MM6221AA is playing */
    public void updateMusic(byte data) {
        if (music != null)
            music.mm6221aa_tune_w(sound_latch_b >> 6);
    }

    public void run() {
        while (running) {
            process(buffer, buffer_size);
//            line.write(buffer, 0, buffer_size);
            audiotrack.write(buffer, 0, buffer_size);
        }
        audiotrack.flush();
        audiotrack.stop();
//        line.drain();
//        line.stop();
//        line.close();
//        line = null;
    }

    public void stop() {
        running = false;
        music.stop();
    }

}
