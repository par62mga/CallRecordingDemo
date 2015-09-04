package com.example.callrecordingdemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Phil Robertson on 8/31/2015.
 */
public class CallRecordingService extends Service {
    private final String LOG_TAG = CallRecordingService.class.getSimpleName();

    // public strings used to set up call recording service intents
    public static final String ACTION_START = "com.example.pkrobertson.callrecordingservice.start";
    public static final String ACTION_PLAY  = "com.example.pkrobertson.callrecordingservice.play";
    public static final String ACTION_STOP  = "com.example.pkrobertson.callrecordingservice.stop";

    private static final String FILE_NAME  = "VOIPRecording";

    private static String mRecordingText = "";

    private AudioManager mAudioManager;
    private AudioManager.OnAudioFocusChangeListener mAudioFocus;

    private MediaRecorder mMediaRecorder;
    private MediaPlayer   mMediaPlayer;

    // IBinder used to allow connections to the service
    private final IBinder mBinder = new CallRecordingServiceBinder();

    // CallRecordingServiceBinder -- used to handle connections to the service
    private class CallRecordingServiceBinder extends Binder {
        CallRecordingService getService () {
            return CallRecordingService.this;
        }
    }

    private void appendLog (String toAppend) {
        Log.d(LOG_TAG, toAppend);
        mRecordingText = mRecordingText.concat(toAppend + "\n");
    }

    private String getFileName () {
        return (new File (getFilesDir(), FILE_NAME)).toString();
    }

    private void startRecording() {
        String fileName = getFileName ();

        appendLog("startRecording: setting up media recorder with fileName ==> " + fileName);
        mMediaRecorder = new MediaRecorder ();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(fileName);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        boolean success = true;
        try {
            mMediaRecorder.prepare ();
        } catch (IOException e) {
            appendLog ("Media recorder prepare() failed: " + e.toString());
            success = false;
        }
        if (success) {
            mMediaRecorder.start();
        } else {
            mMediaRecorder.release ();
            mMediaRecorder = null;
        }
    }

    private void stopRecording () {
        if ( mMediaRecorder == null ) {
            return;
        }

        appendLog("stopRecording: releasing media recorder...");
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }

    private void startPlayback () {
        String fileName = getFileName ();

        appendLog("startPlayback: setting up media player with fileName ==> " + fileName);
        mMediaPlayer = new MediaPlayer ();
        try {
            mMediaPlayer.setDataSource(fileName);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            appendLog("Media player prepare() failed: " + e.toString());
        }
    }

    private void stopPlayback () {
        if ( mMediaPlayer == null ) {
            return;
        }

        appendLog("stopPlayback: releasing media player...");
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    private void startMonitoring () {
        appendLog ("startMonitoring: Starting audio focus monitor...");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioFocus = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        appendLog("Transient lost focus, auto-starting recorder...");
                        mAudioManager.abandonAudioFocus(mAudioFocus);
                        mAudioFocus = null;
                        startRecording ();
                        break;

                    case AudioManager.AUDIOFOCUS_GAIN:
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        appendLog("Lost audio focus...");
                        mAudioManager.abandonAudioFocus(mAudioFocus);
                        mAudioFocus = null;
                        break;

                    default:
                        break;
                }
            }
        };

        int result = mAudioManager.requestAudioFocus(
                mAudioFocus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            appendLog ("Audio focus request granted...");
        }
    }

    private void stopMonitoring () {
        if (mAudioFocus == null) {
            return;
        }

        appendLog("stopMonitoring: Stopping audio focus monitor...");
        mAudioManager.abandonAudioFocus(mAudioFocus);
        mAudioFocus = null;
    }

    private void stopServices () {
        stopMonitoring();
        stopRecording();
        stopPlayback ();
    }

    /**
     * CallRecordingService public methods
     */
    public CallRecordingService () {
    }

    @Override
    public IBinder onBind (Intent arguments) {
        Log.d(LOG_TAG, "OnBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind (Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        // automatic rebind not allowed
        return true;
    }

    @Override
    public void onDestroy () {
        Log.d(LOG_TAG, "onDestroy()");
        appendLog("Shutting down services...");
        stopServices ();
    }

    public int onStartCommand (Intent intent, int flags, int startid) {

        // stop anything already in progress...
        stopServices();

        if (intent.getAction().equals(ACTION_START)) {
            Log.d(LOG_TAG, "Received ACTION_START Intent");
            // clear out logs whenever monitoring is started
            mRecordingText = "Received ACTION_START Intent\n";
            startMonitoring ();
        } else if (intent.getAction().equals(ACTION_PLAY)) {
            Log.d(LOG_TAG, "Received ACTION_PLAY Intent");
            appendLog("Received ACTION_PLAY Intent");
            startPlayback ();
        } else if (intent.getAction().equals(ACTION_STOP)) {
            Log.d(LOG_TAG, "Received ACTION_STOP Intent");
            appendLog ("Received ACTION_STOP Intent");
        }
        return START_NOT_STICKY;
    }

    public static String getLogText () {
        return mRecordingText;
    }
}



