package com.anonymous.anonymous.utils;

import com.connectycube.core.helper.StringifyArrayList;
import com.connectycube.users.model.ConnectycubeUser;

import java.util.ArrayList;
import java.util.Collection;


public class CollectionsUtils {

    public static String makeStringFromUsersFullNames(ArrayList<ConnectycubeUser> allUsers) {
        StringifyArrayList<String> usersNames = new StringifyArrayList<>();

        for (ConnectycubeUser usr : allUsers) {
            usersNames.add(usr.getLogin());
        }
        return usersNames.getItemsAsString().replace(",", ", ");
    }

    public static ArrayList<Integer> getIdsSelectedOpponents(Collection<ConnectycubeUser> selectedUsers) {
        ArrayList<Integer> opponentsIds = new ArrayList<>();
        if (!selectedUsers.isEmpty()) {
            for (ConnectycubeUser user : selectedUsers) {
                opponentsIds.add(user.getId());
            }
        }

        return opponentsIds;
    }
}
