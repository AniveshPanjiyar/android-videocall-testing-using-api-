package com.anonymous.anonymous.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.anonymous.anonymous.R;
import com.anonymous.anonymous.adapters.OpponentsAdapter;
import com.anonymous.anonymous.utils.CollectionsUtils;
import com.anonymous.anonymous.utils.Consts;
import com.anonymous.anonymous.utils.RTCSessionManager;
import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;
import com.connectycube.videochat.RTCClient;
import com.connectycube.videochat.RTCSession;
import com.connectycube.videochat.RTCTypes;
import com.connectycube.videochat.callbacks.RTCClientSessionCallbacksImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class OpponentsActivity extends BaseActivity {
    private static final String TAG = OpponentsActivity.class.getSimpleName();

    private static final String EXTRA_USERS_LOGINS = "usersLogins";

    private OpponentsAdapter opponentsAdapter;
    private ListView opponentsListView;
    private ConnectycubeUser currentUser;
    private ArrayList<String> usersLogins;
    private ArrayList<ConnectycubeUser> users;

    public static void start(Context context, ArrayList<String> usersLogins) {
        Intent intent = new Intent(context, OpponentsActivity.class);
        intent.putStringArrayListExtra(EXTRA_USERS_LOGINS, usersLogins);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opponents);

        initFields();
        initDefaultActionBar();
        initUi();
        loadUsers();
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.list_opponents);
    }

    private void initFields() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usersLogins = extras.getStringArrayList(EXTRA_USERS_LOGINS);
        }
        currentUser = ConnectycubeChatService.getInstance().getUser();
    }

    public void initDefaultActionBar() {
        setActionBarTitle(currentUser.getLogin());
        setActionbarSubTitle(String.format(getString(R.string.subtitle_text_logged_in_as), currentUser.getLogin()));
    }

    private void initRTCClient() {
        RTCClient rtcClient = RTCClient.getInstance(getApplicationContext());
        // Add signalling manager
        ConnectycubeChatService.getInstance().getVideoChatWebRTCSignalingManager().addSignalingManagerListener((signaling, createdLocally) -> {
            if (!createdLocally) {
                rtcClient.addSignaling(signaling);
            }
        });

        // Add service as callback to RTCClient
        rtcClient.addSessionCallbacksListener(new RTCClientCallback());
        rtcClient.prepareToProcessCalls();
    }

    private void loadUsers() {
        showProgressDialog(R.string.dlg_loading_opponents);
        ConnectycubeUsers.getUsersByLogins(usersLogins, null).performAsync(new EntityCallback<ArrayList<ConnectycubeUser>>() {
            @Override
            public void onSuccess(ArrayList<ConnectycubeUser> connectycubeUsers, Bundle bundle) {
                hideProgressDialog();
                initUsersList(connectycubeUsers);
                initRTCClient();
            }

            @Override
            public void onError(ResponseException e) {
                hideProgressDialog();
                showErrorSnackbar(R.string.loading_users_error, e, v -> loadUsers());
            }
        });
    }

    private void initUi() {
        opponentsListView = findViewById(R.id.list_opponents);
    }

    private void initUsersList(ArrayList<ConnectycubeUser> connectycubeUsers) {
        users = connectycubeUsers;
        initOpponentsAdapter();
    }

    private void initOpponentsAdapter() {
        List<ConnectycubeUser> opponents = new ArrayList<>(users);
        opponents.remove(ConnectycubeChatService.getInstance().getUser());
        //Log.e(TAG, "The opponents are "+opponents);
        opponentsAdapter = new OpponentsAdapter(this, opponents);
        opponentsAdapter.setSelectedItemsCountsChangedListener(this::updateActionBar);
        opponentsListView.setAdapter(opponentsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (opponentsAdapter != null && !opponentsAdapter.getSelectedItems().isEmpty()) {
            getMenuInflater().inflate(R.menu.activity_selected_opponents, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_opponents, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.settings:
                showSettings();
                return true;

            case R.id.log_out:
                logOut();
                return true;

            case R.id.start_video_call:
                if (isLoggedInChat()) {
                    startCall(true);
                }
                return true;

            case R.id.start_audio_call:
                if (isLoggedInChat()) {
                    startCall(false);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isLoggedInChat() {
        if (!ConnectycubeChatService.getInstance().isLoggedIn()) {
            Toast.makeText(this, R.string.dlg_signal_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showSettings() {
        SettingsActivity.start(this);
    }

    private void startCall(boolean isVideoCall) {
        if (opponentsAdapter.getSelectedItems().size() > Consts.MAX_OPPONENTS_COUNT) {
            Toast.makeText(this, String.format(getString(R.string.error_max_opponents_count),
                    Consts.MAX_OPPONENTS_COUNT), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "startCall()");
        ArrayList<Integer> opponentsIDsList = CollectionsUtils.getIdsSelectedOpponents(opponentsAdapter.getSelectedItems());
        RTCTypes.ConferenceType conferenceType = isVideoCall
                ? RTCTypes.ConferenceType.CONFERENCE_TYPE_VIDEO
                : RTCTypes.ConferenceType.CONFERENCE_TYPE_AUDIO;

        RTCClient rtcClient = RTCClient.getInstance(getApplicationContext());

        RTCSession newRtcSession = rtcClient.createNewSessionWithOpponents(opponentsIDsList, conferenceType);

        RTCSessionManager.getInstance().setCurrentSession(newRtcSession);
        CallActivity.start(this, getAllOpponents(), false);
        Log.d(TAG, "conferenceType = " + conferenceType);
    }

    private ArrayList<ConnectycubeUser> getAllOpponents() {
        Set<ConnectycubeUser> allOpponents = new HashSet<>();
        List<Integer> opponentsIdsList = new ArrayList<>(RTCSessionManager.getInstance().getCurrentSession().getOpponents());

        opponentsIdsList.add(RTCSessionManager.getInstance().getCurrentSession().getCallerID());
        for (Integer id : opponentsIdsList) {
            for (ConnectycubeUser user : users) {
                if (user.getId().equals(id)) {
                    allOpponents.add(user);
                }
            }
        }
        return new ArrayList<>(allOpponents);
    }

    private void initActionBarWithSelectedUsers(int countSelectedUsers) {
        setActionBarTitle(String.format(getString(
                countSelectedUsers > 1
                        ? R.string.tile_many_users_selected
                        : R.string.title_one_user_selected),
                countSelectedUsers));
    }

    private void updateActionBar(int countSelectedUsers) {
        if (countSelectedUsers < 1) {
            initDefaultActionBar();
        } else {
            removeActionbarSubTitle();
            initActionBarWithSelectedUsers(countSelectedUsers);
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
            logOut();
    }

    private class RTCClientCallback extends RTCClientSessionCallbacksImpl {

        @Override
        public void onReceiveNewSession(RTCSession session) {
            Log.d(TAG, "onReceiveNewSession to WebRtcSessionManager");
            if (RTCSessionManager.getInstance().getCurrentSession() == null) {
                RTCSessionManager.getInstance().setCurrentSession(session);
                CallActivity.start(OpponentsActivity.this, getAllOpponents(), true);
            }
        }

        @Override
        public void onSessionClosed(RTCSession session) {
            Log.d(TAG, "onSessionClosed WebRtcSessionManager");
            if (session.equals(RTCSessionManager.getInstance().getCurrentSession())) {
                RTCSessionManager.getInstance().setCurrentSession(null);
            }
        }
    }

    private void logOut() {
        chatLogout();
        startLoginActivity();
    }

    private void chatLogout() {
        ConnectycubeChatService.getInstance().destroy();
    }


    private void startLoginActivity() {
        LoginActivity.start(this);
        finish();
    }
}