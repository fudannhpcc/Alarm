package cn.fudannhpcc.www.alarm.commonclass;

import android.content.Context;
import android.content.SharedPreferences;
import cn.fudannhpcc.www.alarm.commonclass.Log;

public class AppSettings {
    private static AppSettings instance;

    public int settingsVersion = 0;
    public Boolean adfree = false;

    public String keep_alive;
    public String server;
    public String port;
    public String username;
    public String password;
    public String server_topic;
    public boolean server_mode;

    public String push_notifications_subscribe_topic;
    public boolean connection_in_background;

    boolean settingsLoaded = false;

    Context context;

    private AppSettings() {
        Log.d(getClass().getName(), "ConnectionSettings_CHANGE()");
    }

    public static AppSettings getInstance() {
        if (null == instance) {
            Log.d("Connection settings", "instance = new AppSettings()");
            instance = new AppSettings();
        }
        return instance;
    }

    public void readFromPrefs(Context con) {
        context = con;
        if (settingsLoaded) return;
        settingsLoaded = true;

        Log.d(getClass().getName(), "readFromPrefs()");

        SharedPreferences sprefs = con.getSharedPreferences("mysettings", Context.MODE_PRIVATE);

        adfree = sprefs.getBoolean("adfree", false);

        server = sprefs.getString("connection_server", "");
        Log.d(getClass().getName(), sprefs.getString("connection_server", ""));
        port = sprefs.getString("connection_port", "");
        username = sprefs.getString("connection_username", "");
        password = sprefs.getString("connection_password", "");
        server_topic = sprefs.getString("connection_server_topic", "");
        push_notifications_subscribe_topic = sprefs.getString("connection_push_notifications_subscribe_topic", "");
        keep_alive = sprefs.getString("keep_alive", "60");
        connection_in_background = sprefs.getBoolean("connection_in_background", false);
        server_mode = sprefs.getBoolean("server_mode", false);

        settingsVersion = sprefs.getInt("settingsVersion", 0);

        //settingsVersion=0;

        if (server.equals("")) {
            server = "m21.cloudmqtt.com";
            port = "16796";
            username = "ejoxlycf";
            password = "odhSFqxSDACF";
            //3.0 subscribe_topic = "out/wcs/#";
            push_notifications_subscribe_topic = "out/wcs/push_notifications/#";
            keep_alive = "60";
            connection_in_background = false;
        }
    }

    public void saveConnectionSettingsToPrefs(Context con) {
        Log.d(getClass().getName(), "saveConnectionSettingsToPrefs()");

        SharedPreferences sprefs = con.getSharedPreferences("mysettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sprefs.edit();
        ed.putBoolean("adfree", adfree);
        ed.putString("connection_server", server);
        ed.putString("connection_port", port);
        ed.putString("connection_username", username);
        ed.putString("connection_password", password);
        //3.0 ed.putString("connection_subscribe_topic", subscribe_topic);
        ed.putString("connection_server_topic", server_topic);
        ed.putString("connection_push_notifications_subscribe_topic", push_notifications_subscribe_topic);
        ed.putString("keep_alive", keep_alive);
        ed.putBoolean("connection_in_background", connection_in_background);
        ed.putBoolean("server_mode", server_mode);

        ed.putInt("settingsVersion", settingsVersion);

        if (!ed.commit()) {
            android.util.Log.d(getClass().getName(), "commit failure!!!");
        }
    }
}
