package com.anonymous.anonymous;

import android.app.Application;

import com.connectycube.auth.session.ConnectycubeSettings;
import com.connectycube.core.ServiceZone;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.List;

import static com.anonymous.anonymous.utils.Consts.MAX_OPPONENTS_COUNT;
import static com.anonymous.anonymous.utils.Consts.MIN_OPPONENTS_COUNT;
import static com.anonymous.anonymous.utils.UserConfig.getAllUsersFromFile;


public class App extends Application {

    // ConnectyCube Application credentials
    //
    private final static String APPLICATION_ID = "281";
    private final static String AUTH_KEY = "eCKNTnGrHjQyaGU";
    private final static String AUTH_SECRET = "OOunUKtGtz4dHhc";
    private final static String ACCOUNT_KEY = "HW7UBP7Do1guk4xfzyq9";

    private final static String USER_CONFIG_FILE_NAME = "user_config.json";
    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();
        checkEndpoints();
        checkUserJson();
        initCredentials();
    }

    private void initApplication() {
        instance = this;
    }

    private void checkEndpoints() {
        if (APPLICATION_ID.isEmpty() || AUTH_KEY.isEmpty() || AUTH_SECRET.isEmpty()) {
            throw new AssertionError(getString(R.string.error_credentials_empty));
        }
    }

    private void checkUserJson() {
        List<ConnectycubeUser> users = getAllUsersFromFile(USER_CONFIG_FILE_NAME, this);
        if (users.isEmpty() || users.size() < MIN_OPPONENTS_COUNT || users.size() > MAX_OPPONENTS_COUNT) {
            throw new AssertionError(getString(R.string.error_users_empty));
        }
    }

    private void initCredentials() {
        ConnectycubeSettings.getInstance().init(this, APPLICATION_ID, AUTH_KEY, AUTH_SECRET);
        ConnectycubeSettings.getInstance().setAccountKey(ACCOUNT_KEY);

        // Uncomment and put your Api and Chat servers endpoints if you want to point the sample
        // against your own server.
//        ConnectycubeSettings.getInstance().setEndpoints("https://your_api_endpoint.com", "your_chat_endpoint", ServiceZone.PRODUCTION);
//        ConnectycubeSettings.getInstance().setZone(ServiceZone.PRODUCTION);
    }
}