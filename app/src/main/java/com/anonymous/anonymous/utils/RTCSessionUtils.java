package com.anonymous.anonymous.utils;

import android.util.SparseArray;

import com.anonymous.anonymous.R;
import com.connectycube.videochat.RTCTypes;

public class RTCSessionUtils {

    private static final SparseArray<Integer> peerStateDescriptions = new SparseArray<>();

    static {
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_PENDING.ordinal(), R.string.opponent_pending);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_CONNECTING.ordinal(), R.string.text_status_connect);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_CHECKING.ordinal(), R.string.text_status_checking);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_CONNECTED.ordinal(), R.string.text_status_connected);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_DISCONNECTED.ordinal(), R.string.text_status_disconnected);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_CLOSED.ordinal(), R.string.opponent_closed);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_DISCONNECT_TIMEOUT.ordinal(), R.string.text_status_disconnected);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_NOT_ANSWER.ordinal(), R.string.text_status_no_answer);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_NOT_OFFER.ordinal(), R.string.text_status_no_answer);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_REJECT.ordinal(), R.string.text_status_rejected);
        peerStateDescriptions.put(
                RTCTypes.RTCConnectionState.RTC_CONNECTION_HANG_UP.ordinal(), R.string.text_status_hang_up);
    }

    public static Integer getStatusDescriptionResource(RTCTypes.RTCConnectionState connectionState) {
        return peerStateDescriptions.get(connectionState.ordinal());
    }
}