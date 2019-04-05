package com.anonymous.anonymous.utils;

import com.connectycube.videochat.RTCSession;


public class RTCSessionManager {

    private static RTCSessionManager instance;

    private RTCSession currentSession;

    private RTCSessionManager() {
    }

    public static RTCSessionManager getInstance(){
        if (instance == null){
            instance = new RTCSessionManager();
        }
        return instance;
    }

    public RTCSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(RTCSession currentSession) {
        this.currentSession = currentSession;
    }
}