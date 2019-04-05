package com.anonymous.anonymous.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.anonymous.anonymous.R;
import com.anonymous.anonymous.activities.CallActivity;
import com.anonymous.anonymous.utils.CollectionsUtils;
import com.anonymous.anonymous.utils.RTCSessionManager;
import com.connectycube.chat.ConnectycubeChatService;

import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.RTCSession;

import java.util.ArrayList;

import static com.anonymous.anonymous.utils.Consts.EXTRA_IS_INCOMING_CALL;
import static com.anonymous.anonymous.utils.Consts.EXTRA_OPPONENTS;


public abstract class BaseConversationFragment extends BaseToolBarFragment implements CallActivity.CurrentCallStateCallback {

    private static final String TAG = BaseConversationFragment.class.getSimpleName();
    private boolean isIncomingCall;
    protected RTCSession currentSession;
    protected ArrayList<ConnectycubeUser> opponents;

    private ToggleButton micToggleVideoCall;
    private ImageButton handUpVideoCall;
    protected ConversationFragmentCallbackListener conversationFragmentCallbackListener;
    protected Chronometer timerChronometer;
    protected boolean isStarted;
    protected View outgoingOpponentsRelativeLayout;
    protected TextView allOpponentsTextView;
    protected TextView ringingTextView;
    protected ConnectycubeUser currentUser;

    public static BaseConversationFragment newInstance(BaseConversationFragment baseConversationFragment, ArrayList<ConnectycubeUser> opponentsList, boolean isIncomingCall) {
        Log.d(TAG, "isIncomingCall =  " + isIncomingCall);
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_IS_INCOMING_CALL, isIncomingCall);
        args.putSerializable(EXTRA_OPPONENTS, opponentsList);
        baseConversationFragment.setArguments(args);

        return baseConversationFragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            conversationFragmentCallbackListener = (ConversationFragmentCallbackListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ConversationFragmentCallbackListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversationFragmentCallbackListener.addCurrentCallStateCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        currentSession = RTCSessionManager.getInstance().getCurrentSession();
        if (currentSession == null) {
            Log.d(TAG, "currentSession = null onCreateView");
            return view;
        }
        initFields();
        initViews(view);
        initActionBar();
        initButtonsListener();
        prepareAndShowOutgoingScreen();

        return view;
    }

    private void prepareAndShowOutgoingScreen() {
        configureOutgoingScreen();
        allOpponentsTextView.setText(CollectionsUtils.makeStringFromUsersFullNames(opponents));
    }

    protected abstract void configureOutgoingScreen();

    private void initActionBar() {
        configureToolbar();
        configureActionBar();
    }

    protected abstract void configureActionBar();

    protected abstract void configureToolbar();

    protected void initFields() {
        currentUser = ConnectycubeChatService.getInstance().getUser();

        if (getArguments() != null) {
            isIncomingCall = getArguments().getBoolean(EXTRA_IS_INCOMING_CALL);
            opponents = (ArrayList<ConnectycubeUser>) getArguments().getSerializable(EXTRA_OPPONENTS);
        }

        Log.d(TAG, "opponents: " + opponents.toString());
        Log.d(TAG, "currentSession " + currentSession.toString());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (currentSession == null) {
            Log.d(TAG, "currentSession = null onStart");
            return;
        }

        if (currentSession.getState() != RTCSession.RTCSessionState.RTC_SESSION_CONNECTED) {
            if (isIncomingCall) {
                currentSession.acceptCall(null);
            } else {
                currentSession.startCall(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        conversationFragmentCallbackListener.removeCurrentCallStateCallback(this);
        super.onDestroy();
    }

    protected void initViews(View view) {
        micToggleVideoCall = view.findViewById(R.id.toggle_mic);
        handUpVideoCall = view.findViewById(R.id.button_hangup_call);
        outgoingOpponentsRelativeLayout = view.findViewById(R.id.layout_background_outgoing_screen);
        allOpponentsTextView = view.findViewById(R.id.text_outgoing_opponents_names);
        ringingTextView = view.findViewById(R.id.text_ringing);

        if (isIncomingCall) {
            hideOutgoingScreen();
        }
    }

    protected void initButtonsListener() {

        micToggleVideoCall.setOnCheckedChangeListener((buttonView, isChecked) -> conversationFragmentCallbackListener.onSetAudioEnabled(isChecked));

        handUpVideoCall.setOnClickListener(v -> {
            actionButtonsEnabled(false);
            handUpVideoCall.setEnabled(false);
            handUpVideoCall.setActivated(false);

            conversationFragmentCallbackListener.onHangUpCurrentSession();
            Log.d(TAG, "Call is stopped");
        });
    }

    protected void actionButtonsEnabled(boolean inability) {

        micToggleVideoCall.setEnabled(inability);

        // inactivate toggle buttons
        micToggleVideoCall.setActivated(inability);
    }

    private void startTimer() {
        if (!isStarted) {
            timerChronometer.setVisibility(View.VISIBLE);
            timerChronometer.setBase(SystemClock.elapsedRealtime());
            timerChronometer.start();
            isStarted = true;
        }
    }

    private void stopTimer() {
        if (timerChronometer != null) {
            timerChronometer.stop();
            isStarted = false;
        }
    }

    private void hideOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setVisibility(View.GONE);
    }

    @Override
    public void onCallStarted() {
        hideOutgoingScreen();
        startTimer();
        actionButtonsEnabled(true);
    }

    @Override
    public void onCallStopped() {
        if (currentSession == null) {
            Log.d(TAG, "currentSession = null onCallStopped");
            return;
        }
        stopTimer();
        actionButtonsEnabled(false);
    }
}