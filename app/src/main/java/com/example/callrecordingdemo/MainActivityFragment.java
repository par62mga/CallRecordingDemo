package com.example.callrecordingdemo;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    // used to refresh logs shown in text view every 1/2 second
    private static final int UPDATE_MESSAGE  = 42;
    private static final int UPDATE_INTERVAL = 500; /* msec */

    private Button   mButtonStart;
    private Button   mButtonStop;
    private Button   mButtonPlay;
    private Button   mButtonEnd;
    private TextView mTextViewLog;

    // ideally a Handler should be declared static to avoid leaks. In this case we're taking care
    // to instantiate one time only when PlayTracksFragment is instantiated
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == UPDATE_MESSAGE) {
                if (mTextViewLog != null) {
                    mTextViewLog.setText(CallRecordingService.getLogText());
                }

                // polling used to update view every 100ms when playing
                // when idle, poll every 250ms
                message = obtainMessage(UPDATE_MESSAGE);
                sendMessageDelayed(message, UPDATE_INTERVAL);
            }
        }
    };

    private void sendStartIntent (String action) {
        Intent startIntent = new Intent(getActivity(), CallRecordingService.class);
        startIntent.setAction(action);
        getActivity().startService(startIntent);
    }

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mButtonStart = (Button)   rootView.findViewById(R.id.button_start);
        mButtonStop  = (Button)   rootView.findViewById(R.id.button_stop);
        mButtonPlay  = (Button)   rootView.findViewById(R.id.button_play);
        mButtonEnd   = (Button)   rootView.findViewById(R.id.button_end);
        mTextViewLog = (TextView) rootView.findViewById(R.id.textview_log);

        // handles "start monitoring" click
        mButtonStart.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send ACTION_START intent to the recording/playback service
                sendStartIntent(CallRecordingService.ACTION_START);
            }
        });

        // handles "stop monitoring/playback" click
        mButtonStop.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send ACTION_STOP intent to the recording/playback service
                sendStartIntent(CallRecordingService.ACTION_STOP);
            }
        });

        // handles "start playback" click
        mButtonPlay.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send ACTION_PLAY intent to the recording/playback service
                sendStartIntent (CallRecordingService.ACTION_PLAY);
            }
        });

        // handles "end service" click
        mButtonEnd.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send stop service/destroy intent to the recording/playback service
                getActivity().stopService(new Intent(getActivity(), CallRecordingService.class));
            }
        });

        return rootView;
    }

    @Override
    public void onResume () {
        mHandler.sendEmptyMessage(UPDATE_MESSAGE);
        super.onResume ();
    }

    @Override
    public void onPause () {
        mHandler.removeMessages(UPDATE_MESSAGE);
        super.onPause ();
    }

    @Override
    public void onDestroy () {
        mHandler.removeMessages(UPDATE_MESSAGE);
        super.onDestroy();
    }
}


