package com.mycompany.solarpos.util;

import com.mycompany.solarpos.model.User;

public class Session {
    private static User currentUser;

    private Session() { }

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }
    public static void clear() { currentUser = null; }
}