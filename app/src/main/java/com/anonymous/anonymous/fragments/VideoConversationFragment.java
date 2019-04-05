package com.anonymous.anonymous.fragments;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.anonymous.anonymous.R;
import com.anonymous.anonymous.adapters.OpponentsFromCallAdapter;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.BaseSession;
import com.connectycube.videochat.RTCSession;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.callbacks.RTCClientVideoTracksCallback;
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback;
import com.connectycube.videochat.callbacks.RTCSessionStateCallback;
import com.connectycube.videochat.view.RTCSurfaceView;
import com.connectycube.videochat.view.RTCVideoTrack;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class VideoConversationFragment extends BaseConversationFragment implements Serializable, RTCClientVideoTracksCallback<RTCSession>,
        RTCSessionStateCallback<RTCSession>, RTCSessionEventsCallback {
    private static final int SPAN_COUNT = 2;
    private static final long TRACK_INITIALIZE_DELAY = 500;

    private String TAG = VideoConversationFragment.class.getSimpleName();

    private ToggleButton cameraToggle;
    private ToggleButton cameraSwitch;
    private CameraState cameraState = CameraState.DISABLED_FROM_USER;
    private RecyclerView recyclerView;
    private SparseArray<OpponentsFromCallAdapter.ViewHolder> opponentViewHolders;

    private OpponentsFromCallAdapter opponentsAdapter;
    private boolean isRemoteShown;

    private int amountOpponents;
    private boolean isCurrentCameraFront;


    @Override
    protected void configureOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.grey_transparent_50));
        allOpponentsTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        ringingTextView.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
    }

    @Override
    protected void configureActionBar() {
        actionBar = ((AppCompatActivity) getActivity()).getDelegate().getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    protected void configureToolbar() {
        toolbar.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.black_transparent_50));
        toolbar.setTitleTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        toolbar.setSubtitleTextColor(ContextCompat.getColor(getActivity(), R.color.white));
    }

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_video_conversation;
    }

    @Override
    protected void initFields() {
        super.initFields();
        amountOpponents = opponents.size() - 1;
        timerChronometer = Objects.requireNonNull(getActivity()).findViewById(R.id.timer_chronometer_action_bar);
    }

    public void setDuringCallActionBar() {
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(currentUser.getLogin());

        actionBar.setSubtitle(getString(R.string.opponents, String.valueOf(amountOpponents)));
        actionButtonsEnabled(true);
    }

    private void initVideoTrackSListener() {
        if (currentSession != null) {
            currentSession.addVideoTrackCallbacksListener(this);
        }
    }

    private void removeVideoTrackSListener() {
        if (currentSession != null) {
            currentSession.removeVideoTrackCallbacksListener(this);
        }
    }

    @Override
    protected void actionButtonsEnabled(boolean inability) {
        super.actionButtonsEnabled(inability);
        cameraToggle.setEnabled(inability);
        cameraSwitch.setEnabled(inability);
        // inactivate toggle buttons
        cameraToggle.setActivated(inability);
        cameraSwitch.setActivated(inability);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        conversationFragmentCallbackListener.addRTCClientConnectionCallback(this);
        conversationFragmentCallbackListener.addRTCSessionEventsCallback(this);
        initVideoTrackSListener();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setHasOptionsMenu(true);
    }

    @Override
    protected void initViews(View view) {
        super.initViews(view);
        Log.i(TAG, "initViews");
        opponentViewHolders = new SparseArray<>(opponents.size());
        recyclerView = view.findViewById(R.id.recycler_view_opponents);
        isRemoteShown = false;
        isCurrentCameraFront = true;
        initRecyclerView();

        cameraToggle = view.findViewById(R.id.toggle_camera);
        cameraSwitch = view.findViewById(R.id.switch_camera);
        cameraToggle.setVisibility(View.VISIBLE);
        cameraSwitch.setVisibility(View.VISIBLE);

        restoreSessionIfNeed();
    }

    private void initRecyclerView() {
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), R.dimen.grid_item_divider));
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
        layoutManager.setReverseLayout(false);
        SpanSizeLookupImpl spanSizeLookup = new SpanSizeLookupImpl();
        spanSizeLookup.setSpanIndexCacheEnabled(false);
        layoutManager.setSpanSizeLookup(spanSizeLookup);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        initAdapter();

        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = recyclerView.getHeight();
                if (height != 0) {
                    if (isRemoteShown) {
                        height /= 2;
                    }
                    updateAllCellHeight(height);
                    recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    private void initAdapter() {
        int cellSizeWidth = 0;
        int cellSizeHeight = screenHeight();
        ArrayList<ConnectycubeUser> connectycubeUsers = new ArrayList<>();
        opponentsAdapter = new OpponentsFromCallAdapter(getContext(), connectycubeUsers, cellSizeWidth, cellSizeHeight);
        recyclerView.setAdapter(opponentsAdapter);
    }

    private int screenHeight() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int screenHeightPx = displayMetrics.heightPixels;
        Log.d(TAG, "screenWidthPx $screenHeightPx");
        return screenHeightPx;
    }

    private void updateAllCellHeight(int height) {
        for (ConnectycubeUser user : opponentsAdapter.getOpponents()) {
            OpponentsFromCallAdapter.ViewHolder holder = getViewHolderForOpponent(user.getId());
            opponentsAdapter.initCellHeight(holder, height);
        }
        opponentsAdapter.itemHeight = height;
    }

    private void restoreSessionIfNeed() {
        Log.d(TAG, "restoreSessionIfNeed");
        if (currentSession.getState() != BaseSession.RTCSessionState.RTC_SESSION_CONNECTED) {
            return;
        }
        onCallStarted();
        SparseArray<RTCVideoTrack> videoTracks = currentSession.getMediaStreamManager().getVideoTracks();

        for (int i = 0; i < videoTracks.size(); i++) {
            int userId = videoTracks.keyAt(i);
            RTCVideoTrack videoTrack = videoTracks.get(userId);
            if (currentSession.getPeerConnection(userId) != null && currentSession.getPeerConnection(userId).getState() != RTCTypes.RTCConnectionState.RTC_CONNECTION_CLOSED) {
                mainHandler.post(() -> {
                    onConnectedToUser(currentSession, userId);
                    onRemoteVideoTrackReceive(currentSession, videoTrack, userId);
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // If user changed camera state few times and last state was CameraState.ENABLED_FROM_USER
        // than we turn on cam, else we nothing change
        if (cameraState != CameraState.DISABLED_FROM_USER) {
            toggleCamera(true);
        }
    }

    @Override
    public void onPause() {
        // If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
        // than we turn off cam
        if (cameraState != CameraState.DISABLED_FROM_USER) {
            toggleCamera(false);
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        releaseViewHolders();
        removeConnectionStateListeners();
        removeVideoTrackSListener();
        releaseOpponentsViews();
    }

    private void releaseViewHolders() {
        if (opponentViewHolders != null) {
            opponentViewHolders.clear();
        }
    }

    private void removeConnectionStateListeners() {
        conversationFragmentCallbackListener.removeRTCClientConnectionCallback(this);
        conversationFragmentCallbackListener.removeRTCSessionEventsCallback(this);
    }

    protected void initButtonsListener() {
        super.initButtonsListener();

        cameraToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (cameraState != CameraState.DISABLED_FROM_USER) {
                toggleCamera(isChecked);
            }
        });
        cameraSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (cameraToggle.isChecked()) {
                switchCamera();
            } else {
                cameraSwitch.setChecked(!cameraSwitch.isChecked());
            }
        });
    }

    private void switchCamera() {
        if (cameraState == CameraState.DISABLED_FROM_USER) {
            return;
        }
        cameraToggle.setEnabled(false);
        conversationFragmentCallbackListener.onSwitchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean b) {
                Log.d(TAG, "camera switched, bool = " + b);
                isCurrentCameraFront = b;
                toggleCameraInternal();
            }

            @Override
            public void onCameraSwitchError(String s) {
                Log.d(TAG, "camera switch error " + s);
                Toast.makeText(VideoConversationFragment.this.getContext(), getString(R.string.camera_swicth_failed) + s, Toast.LENGTH_SHORT).show();
                cameraToggle.setEnabled(true);
            }
        });
    }

    private void toggleCameraInternal() {
        Log.d(TAG, "Camera was switched!");
        RTCSurfaceView localView = getViewHolderForOpponent(currentUser.getId()).opponentView;
        updateVideoView(localView, isCurrentCameraFront);
        toggleCamera(true);
    }

    private void toggleCamera(boolean isNeedEnableCam) {
        if (currentSession != null && currentSession.getMediaStreamManager() != null) {
            conversationFragmentCallbackListener.onSetVideoEnabled(isNeedEnableCam);
        }
        if (!cameraToggle.isEnabled()) {
            cameraToggle.setEnabled(true);
        }
    }

    ////////////////////////////  callbacks from RTCClientVideoTracksCallback ///////////////////
    @Override
    public void onLocalVideoTrackReceive(RTCSession rtcSession, final RTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive() run");
        cameraState = CameraState.NONE;
        setConnectycubeUserToAdapter(currentUser.getId());
        mainHandler.postDelayed(() -> setViewMultiCall(currentUser.getId(), videoTrack, false), TRACK_INITIALIZE_DELAY);
    }

    @Override
    public void onRemoteVideoTrackReceive(RTCSession session, final RTCVideoTrack videoTrack, final Integer userId) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userId);
        updateCellSizeIfNeed();
        setConnectycubeUserToAdapter(userId);
        mainHandler.postDelayed(() -> setViewMultiCall(userId, videoTrack, true), TRACK_INITIALIZE_DELAY);
    }

    private void updateCellSizeIfNeed() {
        int height = recyclerView.getHeight() / 2;
        if (!isRemoteShown) {
            isRemoteShown = true;

            setDuringCallActionBar();
            initCurrentConnectycubeUserCellHeight(height);
            opponentsAdapter.itemHeight = height;
        }
    }

    private void initCurrentConnectycubeUserCellHeight(int height) {
        OpponentsFromCallAdapter.ViewHolder holder = (OpponentsFromCallAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(0);
        if (holder != null) {
            opponentsAdapter.initCellHeight(holder, height);
        }
    }

    private void setViewMultiCall(int userId, RTCVideoTrack videoTrack, boolean remoteRenderer) {
        Log.d(TAG, "setViewMultiCall userId= " + userId);
        if (isSessionClosed()) {
            return;
        }
        OpponentsFromCallAdapter.ViewHolder itemHolder = getViewHolderForOpponent(userId);
        if (itemHolder != null) {
            RTCSurfaceView videoView = itemHolder.opponentView;
            Log.d(TAG, "setViewMultiCall fillVideoView");
            Log.d(TAG, "setViewMultiCall videoView height= " + videoView.getHeight());
            fillVideoView(videoView, videoTrack, remoteRenderer);
        }
    }

    private boolean isSessionClosed() {
        return currentSession != null && currentSession.getState().equals(BaseSession.RTCSessionState.RTC_SESSION_CLOSED);
    }

    private void setConnectycubeUserToAdapter(int userID) {
        ConnectycubeUser user = getConnectycubeUserById(userID);
        opponentsAdapter.add(user);
        recyclerView.requestLayout();
    }

    private ConnectycubeUser getConnectycubeUserById(int userId) {
        for (ConnectycubeUser user : opponents) {
            if (user.getId() == userId) {
                return user;
            }
        }
        return null;
    }

    private OpponentsFromCallAdapter.ViewHolder getViewHolderForOpponent(Integer userID) {
        OpponentsFromCallAdapter.ViewHolder holder = opponentViewHolders.get(userID);
        if (holder == null) {
            Log.d(TAG, "holder not found in cache");
            holder = findHolder(userID);
            if (holder != null) {
                opponentViewHolders.append(userID, holder);
            }
        }
        return holder;
    }

    private OpponentsFromCallAdapter.ViewHolder findHolder(Integer userId) {
        Log.d(TAG, "findHolder for " + userId);
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = recyclerView.getChildAt(i);
            OpponentsFromCallAdapter.ViewHolder childViewHolder = (OpponentsFromCallAdapter.ViewHolder) recyclerView.getChildViewHolder(childView);
            if (userId.equals(childViewHolder.userId)) {
                return childViewHolder;
            }
        }
        return null;
    }

    private void releaseOpponentsViews() {
        if (recyclerView != null) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            int childCount = layoutManager.getChildCount();
            Log.d(TAG, " releaseOpponentsViews for  " + childCount + " views");
            for (int i = 0; i < childCount; i++) {
                View childView = layoutManager.getChildAt(i);
                Log.d(TAG, " release View for  " + i + ", " + childView);
                OpponentsFromCallAdapter.ViewHolder childViewHolder = (OpponentsFromCallAdapter.ViewHolder) recyclerView.getChildViewHolder(childView);
                childViewHolder.opponentView.release();
            }
        }
    }

    private void fillVideoView(RTCSurfaceView videoView, RTCVideoTrack videoTrack,
                               boolean remoteRenderer) {
        videoTrack.removeRenderer(videoTrack.getRenderer());
        videoTrack.addRenderer(videoView);
        if (!remoteRenderer) {
            updateVideoView(videoView, isCurrentCameraFront);
        }
        
        //TODO: Add the open CV code here!
        videoTrack.addRenderer(new VideoSink() {
            @Override
            public void onFrame(VideoFrame videoFrame) {
        
                //apply openCV code here
                
                
                videoView.onFrame(videoFrame);
            }
        });
        
        Log.d(TAG, (remoteRenderer ? "remote" : "local") + " Track is rendering");
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }

    protected void updateVideoView(SurfaceViewRenderer surfaceViewRenderer, boolean mirror, RendererCommon.ScalingType scalingType) {
        
        
        Log.i(TAG, "updateVideoView mirror:" + mirror + ", scalingType = " + scalingType);
        surfaceViewRenderer.setScalingType(scalingType);
        surfaceViewRenderer.setMirror(mirror);
        surfaceViewRenderer.requestLayout();
    }

    private void setStatusForOpponent(int userId, final String status) {
        final OpponentsFromCallAdapter.ViewHolder holder = findHolder(userId);
        if (holder == null) {
            return;
        }
        holder.connectionStatus.setText(status);
    }

    private void cleanAdapter(int userId) {
        OpponentsFromCallAdapter.ViewHolder itemHolder = getViewHolderForOpponent(userId);
        if (itemHolder != null) {
            Log.d(TAG, "onConnectionClosedForConnectycubeUser opponentsAdapter.removeItem");
            opponentsAdapter.removeItem(itemHolder.getAdapterPosition());
            opponentViewHolders.remove(userId);
        }
    }

    ///////////////////////////////  RTCSessionConnectionCallbacks ///////////////////////////

    @Override
    public void onStateChanged(RTCSession rtcSession, BaseSession.RTCSessionState rtcSessionState) {

    }

    @Override
    public void onConnectedToUser(RTCSession rtcSession, final Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_connected));
    }

    @Override
    public void onConnectionClosedForUser(RTCSession rtcSession, Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_closed));

        Log.d(TAG, "onConnectionClosedForUser videoTrackMap.remove(userId)= " + userId);
        cleanAdapter(userId);
    }

    @Override
    public void onDisconnectedFromUser(RTCSession rtcSession, Integer integer) {
        setStatusForOpponent(integer, getString(R.string.text_status_disconnected));
    }

    //////////////////////////////////   end     //////////////////////////////////////////


    /////////////////// Callbacks from CallActivity.RTCSessionUserCallback //////////////////////
    @Override
    public void onUserNotAnswer(RTCSession session, Integer userId) {
        setStatusForOpponent(userId, getString(R.string.text_status_no_answer));
    }

    @Override
    public void onCallRejectByUser(RTCSession session, Integer userId, Map<String, String> userInfo) {
        setStatusForOpponent(userId, getString(R.string.text_status_rejected));
    }

    @Override
    public void onCallAcceptByUser(RTCSession session, Integer userId, Map<String, String> userInfo) {
        setStatusForOpponent(userId, getString(R.string.accepted));
    }

    @Override
    public void onReceiveHangUpFromUser(RTCSession session, Integer userId, Map<String, String> userInfo) {
        setStatusForOpponent(userId, getString(R.string.text_status_hang_up));
        Log.d(TAG, "onReceiveHangUpFromUser userId= " + userId);
    }

    @Override
    public void onSessionClosed(RTCSession session) {

    }

    //////////////////////////////////   end     //////////////////////////////////////////

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.conversation_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.screen_share:
                startScreenSharing();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScreenSharing() {
        conversationFragmentCallbackListener.onStartScreenSharing();
    }


    private enum CameraState {
        NONE,
        DISABLED_FROM_USER,
        ENABLED_FROM_USER
    }


    class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        DividerItemDecoration(@NonNull Context context, @DimenRes int dimensionDivider) {
            this.space = context.getResources().getDimensionPixelSize(dimensionDivider);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(space, space, space, space);
        }
    }

    private class SpanSizeLookupImpl extends GridLayoutManager.SpanSizeLookup {

        @Override
        public int getSpanSize(int position) {
            int itemCount = opponentsAdapter.getItemCount();
            if (itemCount == 4) {
                return 1;
            }
            if (itemCount == 3) {
                if (position % 3 > 0) {
                    return 1;
                }
            }
            return 2;
        }
    }
}