package cn.fudannhpcc.www.alarm.commonclass;

import android.app.Application;

public class NotificationNum extends Application {
    private static int pendingNotificationsCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static int getPendingNotificationsCount() {
        return pendingNotificationsCount;
    }

    public static void setPendingNotificationsCount(int pendingNotifications) {
        pendingNotificationsCount = pendingNotifications;
    }
}