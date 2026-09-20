package com.zzynes.flydrone.Radio;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;

@OnlyIn(Dist.CLIENT)
public final class RadioAudioClient {
    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int MAX_RECORD_MILLIS = 10000;
    private static final int BUFFER_SIZE = 2048;

    private static final AudioFormat FORMAT = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            SIGNED,
            BIG_ENDIAN
    );

    private static volatile boolean recording = false;
    private static TargetDataLine targetLine = null;
    private static ByteArrayOutputStream recordedBytes = null;
    private static Thread recordingThread = null;

    private RadioAudioClient() {
    }

    public static synchronized void startRecording() {
        stopRecording();

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(FORMAT);
            targetLine.start();

            recordedBytes = new ByteArrayOutputStream();
            recording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                long endTime = System.currentTimeMillis() + MAX_RECORD_MILLIS;

                while (recording && System.currentTimeMillis() < endTime) {
                    int read = targetLine.read(buffer, 0, buffer.length);
                    if (read > 0 && recordedBytes != null) {
                        synchronized (RadioAudioClient.class) {
                            if (recordedBytes != null) {
                                recordedBytes.write(buffer, 0, read);
                            }
                        }
                    }
                }

                recording = false;
            }, "FlyDroneRadioRecorder");

            recordingThread.setDaemon(true);
            recordingThread.start();
        } catch (Exception e) {
            recording = false;
            closeTargetLine();
            recordedBytes = null;
        }
    }

    public static synchronized byte[] stopRecording() {
        recording = false;
        closeTargetLine();

        byte[] data = new byte[0];

        if (recordedBytes != null) {
            data = recordedBytes.toByteArray();
            recordedBytes = null;
        }

        recordingThread = null;
        return data;
    }

    public static void play(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }

        Thread thread = new Thread(() -> {
            SourceDataLine sourceLine = null;

            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                sourceLine = (SourceDataLine) AudioSystem.getLine(info);
                sourceLine.open(FORMAT);
                sourceLine.start();
                sourceLine.write(data, 0, data.length);
                sourceLine.drain();
            } catch (Exception ignored) {
            } finally {
                if (sourceLine != null) {
                    sourceLine.stop();
                    sourceLine.close();
                }
            }
        }, "FlyDroneRadioPlayer");

        thread.setDaemon(true);
        thread.start();
    }

    private static void closeTargetLine() {
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
            targetLine = null;
        }
    }
}