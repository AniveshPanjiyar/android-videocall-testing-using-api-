package com.anonymous.anonymous.fragments;

import com.anonymous.anonymous.activities.CallActivity;
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;

import org.webrtc.CameraVideoCapturer;


public interface ConversationFragmentCallbackListener {

    void addRTCClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks);
    void removeRTCClientConnectionCallback(RTCSessionStateCallback clientConnectionCallbacks);

    void addRTCSessionEventsCallback(RTCSessionEventsCallback eventsCallback);
    void removeRTCSessionEventsCallback(RTCSessionEventsCallback eventsCallback);

    void addCurrentCallStateCallback (CallActivity.CurrentCallStateCallback currentCallStateCallback);
    void removeCurrentCallStateCallback (CallActivity.CurrentCallStateCallback currentCallStateCallback);

    void addOnChangeAudioDeviceCallback(CallActivity.OnChangeAudioDevice onChangeDynamicCallback);
    void removeOnChangeAudioDeviceCallback(CallActivity.OnChangeAudioDevice onChangeDynamicCallback);

    void onSetAudioEnabled(boolean isAudioEnabled);

    void onSetVideoEnabled(boolean isNeedEnableCam);

    void onSwitchAudio();

    void onHangUpCurrentSession();

    void onStartScreenSharing();

    void onSwitchCamera(CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler);
}
