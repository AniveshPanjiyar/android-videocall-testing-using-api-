package com.anonymous.anonymous.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.anonymous.anonymous.R;
import com.connectycube.chat.ConnectycubeChatService;
import com.connectycube.chat.connections.tcp.TcpChatConnectionFabric;
import com.connectycube.chat.connections.tcp.TcpConfigurationBuilder;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;

import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.anonymous.utils.UserConfig.getAllUsersFromFile;


public class LoginActivity extends BaseActivity {

    private String TAG = LoginActivity.class.getSimpleName();
    private List<ConnectycubeUser> users;
    private ArrayAdapter<String> userAdapter;
    private ListView listUsers;

    public static void start(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initUsers();
        initChatService();
        initUI();
        initUserAdapter();
    }

    @Override
    protected View getSnackbarAnchorView() {
        return findViewById(R.id.login_activity);
    }

    private void initUI() {
        listUsers = findViewById(R.id.list_users);
    }

    private void initUsers() {
        users = getAllUsersFromFile("user_config.json", this);
    }

    private void initUserAdapter() {
        List<String> userList = new ArrayList<>(users.size());
        for (int i = 0; i < users.size(); i++) {
            userList.add(users.get(i).getLogin());
        }
        userAdapter = new ArrayAdapter<>(this, R.layout.list_item_user, userList);
        listUsers.setAdapter(userAdapter);
        listUsers.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listUsers.setOnItemClickListener((parent, view, position, id) -> loginTo(users.get(position)));
    }

    private void initChatService() {
        ConnectycubeChatService.setConnectionFabric(new TcpChatConnectionFabric(new TcpConfigurationBuilder().setSocketTimeout(0)));
        ConnectycubeChatService.setDebugEnabled(true);
    }

    private void loginTo(ConnectycubeUser user) {
        showProgressDialog(R.string.dlg_login);
        ConnectycubeUsers.signIn(user).performAsync(new EntityCallback<ConnectycubeUser>() {
            @Override
            public void onSuccess(ConnectycubeUser connectycubeUser, Bundle bundle) {
                user.setId(connectycubeUser.getId());
                loginToChat(user);
            }

            @Override
            public void onError(ResponseException ex) {
                hideProgressDialog();
                Toast.makeText(LoginActivity.this, getString(R.string.login_chat_login_error, ex.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginToChat(ConnectycubeUser user) {
        if (!ConnectycubeChatService.getInstance().isLoggedIn()) {
            ConnectycubeChatService.getInstance().login(user, new EntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {
                    hideProgressDialog();
                    proceedToOpponentsActivity();
                }

                @Override
                public void onError(ResponseException ex) {
                    hideProgressDialog();
                    Toast.makeText(LoginActivity.this, getString(R.string.login_chat_login_error, ex.getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void proceedToOpponentsActivity() {
        ArrayList<String> usersLogins = new ArrayList<>();
        for (ConnectycubeUser user : users) {
            usersLogins.add(user.getLogin());
        }
        OpponentsActivity.start(this, usersLogins);
    }
}
