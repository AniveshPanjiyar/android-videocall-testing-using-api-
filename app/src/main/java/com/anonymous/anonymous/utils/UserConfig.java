package com.anonymous.anonymous.utils;

import android.content.Context;

import com.connectycube.users.model.ConnectycubeUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserConfig {

    public static List<ConnectycubeUser> getAllUsersFromFile(String fileName, Context context) {
        String jsonStringUsers = null;
        try {
            jsonStringUsers = getJsonAsString(fileName, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> userMap = new Gson().fromJson(jsonStringUsers, type);
        List<ConnectycubeUser> users = new ArrayList<>();
        for (Map.Entry<String, String> entry : userMap.entrySet()) {
            String login = entry.getKey();
            String password = entry.getValue();
            users.add(new ConnectycubeUser(login, password));
        }
        return users;
    }

    private static String getJsonAsString(String fileName, Context context) throws IOException {
        StringBuilder buf = new StringBuilder();
        InputStream json = context.getAssets().open(fileName);
        BufferedReader in = new BufferedReader(new InputStreamReader(json, "UTF-8"));
        String str;
        while ((str = in.readLine()) != null) {
            buf.append(str);
        }
        in.close();
        return buf.toString();
    }
}