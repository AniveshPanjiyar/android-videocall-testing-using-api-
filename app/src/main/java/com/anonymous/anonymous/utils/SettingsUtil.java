package com.anonymous.anonymous.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.anonymous.anonymous.R;
import com.connectycube.videochat.RTCConfig;
import com.connectycube.videochat.RTCMediaConfig;

import java.util.List;


public class SettingsUtil {

    private static final String TAG = SettingsUtil.class.getSimpleName();

    private static void setSettingsForMultiCall() {
        RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.QVGA_VIDEO.width);
        RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.QVGA_VIDEO.height);
        RTCMediaConfig.setVideoHWAcceleration(false);
        RTCMediaConfig.setVideoCodec(null);
    }

    public static void setSettingsStrategy(List<Integer> users, SharedPreferences sharedPref, Context context) {
        setCommonSettings(sharedPref, context);
        if (users.size() < 2) {
            setSettingsFromPreferences(sharedPref, context);
        } else {
            setSettingsForMultiCall();
        }
    }

    private static void setCommonSettings(SharedPreferences sharedPref, Context context) {
        String audioCodecDescription = getPreferenceString(sharedPref, context, R.string.pref_audiocodec_key,
                R.string.pref_audiocodec_def);
        RTCMediaConfig.AudioCodec audioCodec = RTCMediaConfig.AudioCodec.ISAC.getDescription()
                .equals(audioCodecDescription) ?
                RTCMediaConfig.AudioCodec.ISAC : RTCMediaConfig.AudioCodec.OPUS;
        RTCMediaConfig.setAudioCodec(audioCodec);
        Log.v(TAG, "audioCodec = " + RTCMediaConfig.getAudioCodec());
    }

    private static void setSettingsFromPreferences(SharedPreferences sharedPref, Context context) {
        RTCMediaConfig.setVideoHWAcceleration(true);
        setDefaultVideoQuality();

        Log.v(TAG, "resolution = " + RTCMediaConfig.getVideoHeight() + "x" + RTCMediaConfig.getVideoWidth());

        // Get start bitrate.
        int startBitrate = getPreferenceInt(sharedPref, context,
                R.string.pref_startbitratevalue_key,
                R.string.pref_startbitratevalue_default);
        RTCMediaConfig.setVideoStartBitrate(startBitrate);
        Log.v(TAG, "videoStartBitrate = " + RTCMediaConfig.getVideoStartBitrate());

        int videoCodecItem = Integer.parseInt(getPreferenceString(sharedPref, context, R.string.pref_videocodec_key, "0"));
        for (RTCMediaConfig.VideoCodec codec : RTCMediaConfig.VideoCodec.values()) {
            if (codec.ordinal() == videoCodecItem) {
                RTCMediaConfig.setVideoCodec(codec);
                Log.v(TAG, "videoCodecItem = " + RTCMediaConfig.getVideoCodec());
                break;
            }
        }
    }

    private static void setDefaultVideoQuality() {
        RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.VGA_VIDEO.width);
        RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.VGA_VIDEO.height);
    }

    private static String getPreferenceString(SharedPreferences sharedPref, Context context, int strResKey, int strResDefValue) {
        return sharedPref.getString(context.getString(strResKey), context.getString(strResDefValue));
    }

    private static String getPreferenceString(SharedPreferences sharedPref, Context context, int strResKey, String strResDefValue) {
        return sharedPref.getString(context.getString(strResKey), strResDefValue);
    }

    public static int getPreferenceInt(SharedPreferences sharedPref, Context context, int strResKey, int strResDefValue) {
        return sharedPref.getInt(context.getString(strResKey), Integer.valueOf(context.getString(strResDefValue)));
    }
}
