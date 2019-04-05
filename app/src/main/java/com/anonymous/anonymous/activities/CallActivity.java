package com.anonymous.anonymous.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anonymous.anonymous.R;
import com.anonymous.anonymous.fragments.AudioConversationFragment;
import com.anonymous.anonymous.fragments.BaseConversationFragment;
import com.anonymous.anonymous.fragments.ConversationFragmentCallbackListener;
import com.anonymous.anonymous.fragments.IncomeCallFragment;
import com.anonymous.anonymous.fragments.IncomeCallFragmentCallbackListener;
import com.anonymous.anonymous.fragments.ScreenShareFragment;
import com.anonymous.anonymous.fragments.VideoConversationFragment;
import com.anonymous.anonymous.utils.Consts;
import com.anonymous.anonymous.utils.RTCSessionManager;
import com.anonymous.anonymous.utils.RingtonePlayer;
import com.anonymous.anonymous.utils.SettingsUtil;
import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.AppRTCAudioManager;
import com.connectycube.videochat.BaseSession;
import com.connectycube.videochat.RTCCameraVideoCapturer;
import com.connectycube.videochat.RTCClient;
import com.connectycube.videochat.RTCConfig;
import com.connectycube.videochat.RTCScreenCapturer;
import com.connectycube.videochat.RTCSession;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.SignalingSpec;
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacks;
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.callbacks.RTCSignalingCallback;
import com.connectycube.videochat.exception.RTCSignalException;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.webrtc.CameraVideoCapturer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CallActivity extends BaseActivity implements RTCClientSessionCallbacks, RTCSessionStateCallback<RTCSession>, RTCSignalingCallback,
        IncomeCallFragmentCallbackListener, ConversationFragmentCallbackListener,
        ScreenShareFragment.OnSharingEvents {

    private static final String TAG = CallActivity.class.getSimpleName();

    public static final String INCOME_CALL_FRAGMENT = "income_call_fragment";
    private final static int PERMISSIONS_FOR_CALL_REQUEST = 3;

    private RTCSession currentSession;
    private ArrayList<ConnectycubeUser> allOpponents;
    private Runnable showIncomingCallWindowTask;
    private Handler showIncomingCallWindowTaskHandler;
    private boolean isInComingCall;
    private RTCClient rtcClient;
    private OnChangeAudioDevice onChangeAudioDeviceCallback;
    private ConnectionListener connectionListener;
    private SharedPreferences sharedPref;
    private RingtonePlayer ringtonePlayer;
    private LinearLayout connectionView;
    private AppRTCAudioManager audioManager;
    private ArrayList<CurrentCallStateCallback> currentCallStateCallbackList = new ArrayList<>();
    private boolean callStarted;
    private boolean isVideoCall;
    private long expirationReconnectionTime;

    public static void start(Context context, ArrayList<ConnectycubeUser> allOpponents,
                             boolean isIncomingCall) {

        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(Consts.EXTRA_IS_INCOMING_CALL, isIncomingCall);
        intent.putExtra(Consts.EXTRA_OPPONENTS, allOpponents);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        parseIntentExtras();
        initCurrentSession();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        initRTCClient();
        initAudioManager();

        ringtonePlayer = new RingtonePlayer(this, R.raw.beep);
        connectionView = (LinearLayout) View.inflate(this, R.layout.connection_popup, null);
        checkCameraPermissionAndStart();
    }

    private void startAudioManager() {
        audioManager.start((selectedAudioDevice, availableAudioDevices) -> {
            Log.d(TAG, "Audio device switched to  " + selectedAudioDevice);

            if (onChangeAudioDeviceCallback != null) {
                onChangeAudioDeviceCallback.audioDeviceChanged(selectedAudioDevice);
            }
        });
    }

    private void startScreenSharing(final Intent data) {
        ScreenShareFragment screenShareFragment = ScreenShareFragment.newIntstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, screenShareFragment, ScreenShareFragment.TAG).addToBackStack(null).commitAllowingStateLoss();
        currentSession.getMediaStreamManager().setVideoCapturer(new RTCScreenCapturer(data, null));
    }

    private void returnToCamera() {
        try {
            currentSession.getMediaStreamManager().setVideoCapturer(new RTCCameraVideoCapturer(this, null));
        } catch (RTCCameraVideoCapturer.RTCCameraCapturerException e) {
            Log.i(TAG, "Error: device doesn't have camera");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        Log.i(TAG, "onActivityResult requestCode=" + requestCode + ", resultCode= " + resultCode);
        if (requestCode == RTCScreenCapturer.REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                startScreenSharing(data);
                Log.i(TAG, "Starting screen capture");
            }
        }
    }

    private void startSuitableFragment(boolean isInComingCall) {
        if (isInComingCall) {
            initIncomingCallTask();
            addIncomeCallFragment();
        } else {
            startAudioManager();
            ringtonePlayer.play(true);
            addConversationFragment(false);
        }
    }

    @Override
    protected View getSnackbarAnchorView() {
        return null;
    }

    private void parseIntentExtras() {
        isInComingCall = Objects.requireNonNull(getIntent().getExtras()).getBoolean(Consts.EXTRA_IS_INCOMING_CALL);
        allOpponents = (ArrayList<ConnectycubeUser>) getIntent().getExtras().getSerializable(Consts.EXTRA_OPPONENTS);
    }

    private void initAudioManager() {
        audioManager = AppRTCAudioManager.create(this);

        isVideoCall = RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO.equals(currentSession.getConferenceType());
        if (isVideoCall) {
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
            Log.d(TAG, "AppRTCAudioManager.AudioDevice.SPEAKER_PHONE");
        } else {
            audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
            audioManager.setManageSpeakerPhoneByProximity(true);
            Log.d(TAG, "AppRTCAudioManager.AudioDevice.EARPIECE");
        }

        audioManager.setOnWiredHeadsetStateListener((plugged, hasMicrophone) -> {
            if (callStarted) {
                showToast("Headset " + (plugged ? "plugged" : "unplugged"));
            }
        });

        audioManager.setBluetoothAudioDeviceStateListener(connected -> {
            if (callStarted) {
                showToast("Bluetooth " + (connected ? "connected" : "disconnected"));
            }
        });
    }

    private void initRTCClient() {
        rtcClient = RTCClient.getInstance(this);

        rtcClient.setCameraErrorHandler(new CameraVideoCapturer.CameraEventsHandler() {
            @Override
            public void onCameraError(final String s) {
                showToast("Camera error: " + s);
            }

            @Override
            public void onCameraDisconnected() {
                showToast("Camera onCameraDisconnected: ");
            }

            @Override
            public void onCameraFreezed(String s) {
                showToast("Camera freezed: " + s);
                hangUpCurrentSession();
            }

            @Override
            public void onCameraOpening(String s) {
                Log.d(TAG, "Camera aOpening: " + s);
            }

            @Override
            public void onFirstFrameAvailable() {
                Log.d(TAG, "onFirstFrameAvailable");
            }

            @Override
            public void onCameraClosed() {
            }
        });


        // Configure
        //
        RTCConfig.setMaxOpponentsCount(Consts.MAX_OPPONENTS_COUNT);
        SettingsUtil.setSettingsStrategy(currentSession.getOpponents(), sharedPref, CallActivity.this);
        RTCConfig.setDebugEnabled(true);

        // Add activity as callback to RTCClient
        rtcClient.addSessionCallbacksListener(this);
        // Start mange RTCSessions according to VideoCall parser's callbacks
        rtcClient.prepareToProcessCalls();
        connectionListener = new ConnectionListener();
        ConnectycubeChatService.getInstance().addConnectionListener(connectionListener);
    }

    private void setExpirationReconnectionTime() {
        int reconnectHangUpTimeMillis = SettingsUtil.getPreferenceInt(sharedPref, this, R.string.pref_disconnect_time_interval_key,
                R.string.pref_disconnect_time_interval_default_value) * 1000;
        expirationReconnectionTime = System.currentTimeMillis() + reconnectHangUpTimeMillis;
    }

    private void hangUpAfterLongReconnection() {
        if (expirationReconnectionTime < System.currentTimeMillis()) {
            hangUpCurrentSession();
        }
    }

    private void showNotificationPopUp(final int text, final boolean show) {
        runOnUiThread(() -> {
            if (show) {
                ((TextView) connectionView.findViewById(R.id.notification)).setText(text);
                if (connectionView.getParent() == null) {
                    ((ViewGroup) CallActivity.this.findViewById(R.id.fragment_container)).addView(connectionView);
                }
            } else {
                ((ViewGroup) CallActivity.this.findViewById(R.id.fragment_container)).removeView(connectionView);
            }
        });

    }

    private void initIncomingCallTask() {
        showIncomingCallWindowTaskHandler = new Handler(Looper.myLooper());
        showIncomingCallWindowTask = () -> {
            if (currentSession == null) {
                return;
            }

            RTCSession.RTCSessionState currentSessionState = currentSession.getState();
            if (RTCSession.RTCSessionState.RTC_SESSION_NEW.equals(currentSessionState)) {
                rejectCurrentSession();
            } else {
                ringtonePlayer.stop();
                hangUpCurrentSession();
            }
            showToast("Call was stopped by timer");
        };
    }


    private RTCSession getCurrentSession() {
        return currentSession;
    }

    public void rejectCurrentSession() {
        if (getCurrentSession() != null) {
            getCurrentSession().rejectCall(new HashMap<>());
        }
    }

    public void hangUpCurrentSession() {
        ringtonePlayer.stop();
        if (getCurrentSession() != null) {
            getCurrentSession().hangUp(new HashMap<>());
        }
    }

    private void setAudioEnabled(boolean isAudioEnabled) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            currentSession.getMediaStreamManager().getLocalAudioTrack().setEnabled(isAudioEnabled);
        }
    }

    private void setVideoEnabled(boolean isVideoEnabled) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            currentSession.getMediaStreamManager().getLocalVideoTrack().setEnabled(isVideoEnabled);
        }
    }

    private void startIncomeCallTimer(long time) {
        showIncomingCallWindowTaskHandler.postAtTime(showIncomingCallWindowTask, SystemClock.uptimeMillis() + time);
    }

    private void stopIncomeCallTimer() {
        Log.d(TAG, "stopIncomeCallTimer");
        showIncomingCallWindowTaskHandler.removeCallbacks(showIncomingCallWindowTask);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ConnectycubeChatService.getInstance().removeConnectionListener(connectionListener);
    }

    public void initCurrentSession() {
        currentSession = RTCSessionManager.getInstance().getCurrentSession();
        if (currentSession != null) {
            Log.d(TAG, "Init new RTCSession");
            this.currentSession.addSessionCallbacksListener(CallActivity.this);
            this.currentSession.addSignalingCallback(CallActivity.this);
        }
    }

    public void releaseCurrentSession() {
        Log.d(TAG, "Release current session");
        if (currentSession != null) {
            this.currentSession.removeSessionCallbacksListener(CallActivity.this);
            this.currentSession.removeSignalingCallback(CallActivity.this);
            rtcClient.removeSessionsCallbacksListener(CallActivity.this);
            this.currentSession = null;
        }
    }

    private void checkCameraPermissionAndStart() {
        if (!isCallPermissionsGranted()) {
            requestCameraPermission();
        } else {
            startSuitableFragment(isInComingCall);
        }
    }

    private boolean isCallPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSIONS_FOR_CALL_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_FOR_CALL_REQUEST:
                if (!isCallPermissionsGranted()) {
                    Log.d(TAG, "showToastDeniedPermissions");
                    Toast.makeText(this, getString(R.string.denied_permission_message, Arrays.toString(permissions)), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    startSuitableFragment(isInComingCall);
                }
        }
    }

    // ---------------Chat callback methods implementation  ----------------------//

    @Override
    public void onReceiveNewSession(final RTCSession session) {
        Log.d(TAG, "Session " + session.getSessionID() + " are income");
        if (getCurrentSession() != null) {
            Log.d(TAG, "Stop new session. Device now is busy");
            session.rejectCall(null);
        }
    }

    @Override
    public void onUserNotAnswer(RTCSession session, Integer userID) {
        if (!session.equals(getCurrentSession())) {
            return;
        }
        ringtonePlayer.stop();
    }

    @Override
    public void onUserNoActions(RTCSession rtcSession, Integer integer) {
        startIncomeCallTimer(0);
    }

    @Override
    public void onCallAcceptByUser(RTCSession session, Integer userId, Map<String, String> userInfo) {
        if (!session.equals(getCurrentSession())) {
            return;
        }
        ringtonePlayer.stop();
    }

    @Override
    public void onCallRejectByUser(RTCSession session, Integer userID, Map<String, String> userInfo) {
        if (!session.equals(getCurrentSession())) {
            return;
        }

    }

    @Override
    public void onConnectionClosedForUser(RTCSession session, Integer userID) {
    }

    @Override
    public void onStateChanged(RTCSession rtcSession, BaseSession.RTCSessionState rtcSessionState) {

    }

    @Override
    public void onConnectedToUser(RTCSession session, final Integer userID) {
        callStarted = true;
        notifyCallStateListenersCallStarted();
        if (isInComingCall) {
            stopIncomeCallTimer();
        }
        Log.d(TAG, "onConnectedToUser() is started");
    }

    @Override
    public void onSessionClosed(final RTCSession session) {

        Log.d(TAG, "Session " + session.getSessionID() + " start stop session");

        if (session.equals(getCurrentSession())) {
            Log.d(TAG, "Stop session");

            if (audioManager != null) {
                audioManager.stop();
            }
            ringtonePlayer.stop();

            releaseCurrentSession();
            finish();
        }
    }

    @Override
    public void onSessionStartClose(final RTCSession session) {
        if (session.equals(getCurrentSession())) {
            session.removeSessionCallbacksListener(CallActivity.this);
            notifyCallStateListenersCallStopped();
        }
    }

    @Override
    public void onDisconnectedFromUser(RTCSession session, Integer userID) {

    }

    private void showToast(final int message) {
        Toast.makeText(CallActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToast(final String message) {
        Toast.makeText(CallActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceiveHangUpFromUser(final RTCSession session, final Integer userID, Map<String, String> map) {
        if (session.equals(getCurrentSession())) {

            if (userID.equals(session.getCallerID())) {
                hangUpCurrentSession();
                Log.d(TAG, "initiator hung up the call");
            }

            String participantName = "";
            for (ConnectycubeUser user : allOpponents) {
                if (user.getId().equals(userID)) {
                    participantName = user.getLogin();
                }
            }

            showToast("User " + participantName + " " + getString(R.string.text_status_hang_up) + " conversation");
        }
    }

    private void addIncomeCallFragment() {
        Log.d(TAG, "RTCSession in addIncomeCallFragment is " + currentSession);

        if (currentSession != null) {
            IncomeCallFragment fragment = new IncomeCallFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Consts.EXTRA_OPPONENTS, allOpponents);
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, INCOME_CALL_FRAGMENT).commitAllowingStateLoss();
        } else {
            Log.d(TAG, "SKIP addIncomeCallFragment method");
        }
    }

    private void addConversationFragment(boolean isIncomingCall) {
        BaseConversationFragment conversationFragment = BaseConversationFragment.newInstance(
                isVideoCall
                        ? new VideoConversationFragment()
                        : new AudioConversationFragment(),
                allOpponents, isIncomingCall);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, conversationFragment,
                conversationFragment.getClass().getSimpleName()).commitAllowingStateLoss();
    }

    @Override
    public void onSuccessSendingPacket(SignalingSpec.SignalCMD signalCMD, Integer integer) {
    }

    @Override
    public void onErrorSendingPacket(SignalingSpec.SignalCMD signalCMD, Integer userId, RTCSignalException e) {
        showToast(R.string.dlg_signal_error);
    }

    public void notifyAboutCurrentAudioDevice() {
        onChangeAudioDeviceCallback.audioDeviceChanged(audioManager.getSelectedAudioDevice());
    }

    ////////////////////////////// IncomeCallFragmentCallbackListener ////////////////////////////

    @Override
    public void onAcceptCurrentSession() {
        if (currentSession != null) {
            startAudioManager();
            addConversationFragment(true);
        } else {
            Log.d(TAG, "SKIP addConversationFragment method");
        }
    }

    @Override
    public void onRejectCurrentSession() {
        rejectCurrentSession();
    }
    //////////////////////////////////////////   end   /////////////////////////////////////////////


    @Override
    public void onBackPressed() {
        android.support.v4.app.Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(ScreenShareFragment.TAG);
        if (fragmentByTag instanceof ScreenShareFragment) {
            returnToCamera();
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    ////////////////////////////// ConversationFragmentCallbackListener ////////////////////////////

    @Override
    public void addRTCClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.addSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    @Override
    public void addRTCSessionEventsCallback(RTCSessionEventsCallback eventsCallback) {
        RTCClient.getInstance(this).addSessionCallbacksListener(eventsCallback);
    }

    @Override
    public void onSetAudioEnabled(boolean isAudioEnabled) {
        setAudioEnabled(isAudioEnabled);
    }

    @Override
    public void onHangUpCurrentSession() {
        hangUpCurrentSession();
    }

    @TargetApi(21)
    @Override
    public void onStartScreenSharing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        RTCScreenCapturer.requestPermissions(CallActivity.this);
    }

    @Override
    public void onSwitchCamera(CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler) {
        ((RTCCameraVideoCapturer) (currentSession.getMediaStreamManager().getVideoCapturer()))
                .switchCamera(cameraSwitchHandler);
    }

    @Override
    public void onSetVideoEnabled(boolean isNeedEnableCam) {
        setVideoEnabled(isNeedEnableCam);
    }

    @Override
    public void onSwitchAudio() {
        Log.v(TAG, "onSwitchAudio(), SelectedAudioDevice() = " + audioManager.getSelectedAudioDevice());
        if (audioManager.getSelectedAudioDevice() != AppRTCAudioManager.AudioDevice.SPEAKER_PHONE) {
            audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        } else {
            if (audioManager.getAudioDevices().contains(AppRTCAudioManager.AudioDevice.BLUETOOTH)) {
                audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.BLUETOOTH);
            } else if (audioManager.getAudioDevices().contains(AppRTCAudioManager.AudioDevice.WIRED_HEADSET)) {
                audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.WIRED_HEADSET);
            } else {
                audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.EARPIECE);
            }
        }
    }

    @Override
    public void removeRTCClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks) {
        if (currentSession != null) {
            currentSession.removeSessionCallbacksListener(clientConnectionCallbacks);
        }
    }

    @Override
    public void removeRTCSessionEventsCallback(RTCSessionEventsCallback eventsCallback) {
        RTCClient.getInstance(this).removeSessionsCallbacksListener(eventsCallback);
    }

    @Override
    public void addCurrentCallStateCallback(CurrentCallStateCallback currentCallStateCallback) {
        currentCallStateCallbackList.add(currentCallStateCallback);
    }

    @Override
    public void removeCurrentCallStateCallback(CurrentCallStateCallback currentCallStateCallback) {
        currentCallStateCallbackList.remove(currentCallStateCallback);
    }

    @Override
    public void addOnChangeAudioDeviceCallback(OnChangeAudioDevice onChangeDynamicCallback) {
        this.onChangeAudioDeviceCallback = onChangeDynamicCallback;
        notifyAboutCurrentAudioDevice();
    }

    @Override
    public void removeOnChangeAudioDeviceCallback(OnChangeAudioDevice onChangeDynamicCallback) {
        this.onChangeAudioDeviceCallback = null;
    }

    @Override
    public void onStopPreview() {
        onBackPressed();
    }

    //////////////////////////////////////////   end   /////////////////////////////////////////////
    private class ConnectionListener extends AbstractConnectionListener {
        @Override
        public void connectionClosedOnError(Exception e) {
            showNotificationPopUp(R.string.connection_was_lost, true);
            setExpirationReconnectionTime();
        }

        @Override
        public void reconnectionSuccessful() {
            showNotificationPopUp(R.string.connection_was_lost, false);
        }

        @Override
        public void reconnectingIn(int seconds) {
            Log.i(TAG, "reconnectingIn " + seconds);
            if (!callStarted) {
                hangUpAfterLongReconnection();
            }
        }
    }

    public interface OnChangeAudioDevice {
        void audioDeviceChanged(AppRTCAudioManager.AudioDevice newAudioDevice);
    }


    public interface CurrentCallStateCallback {
        void onCallStarted();

        void onCallStopped();
    }

    private void notifyCallStateListenersCallStarted() {
        for (CurrentCallStateCallback callback : currentCallStateCallbackList) {
            callback.onCallStarted();
        }
    }

    private void notifyCallStateListenersCallStopped() {
        for (CurrentCallStateCallback callback : currentCallStateCallbackList) {
            callback.onCallStopped();
        }
    }
}