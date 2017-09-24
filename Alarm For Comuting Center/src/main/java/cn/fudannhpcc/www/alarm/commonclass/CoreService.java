package cn.fudannhpcc.www.alarm.commonclass;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import cn.fudannhpcc.www.alarm.R;

public class CoreService extends Service {

    public static final String PREFS_NAME = "AppSettings";
    private Context context;
    private Map<String,Object> SprefsMap;

    private int keepaliveTimer = -1;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SprefsMap = readFromPrefs();
        keepaliveTimer = Integer.parseInt(SprefsMap.get(getString(R.string.connection_keep_alive)).toString());
        keepMeAlive();
    }

    @Override
    public void onDestroy() {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "CORE服务退出",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        String MQTTerviceName = getString(R.string.mqtt_service_name);
        boolean isService = ServiceUtils.isServiceRunning(getApplicationContext(),MQTTerviceName);
        Log.d("MQTTerviceName",String.valueOf(isService));
        if ( !isService ) this.startService(new Intent(this,MQTTService.class));
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "CoreService onStartCommand()",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        return START_STICKY;
    }

    public void keepMeAlive() {
        final long now = System.currentTimeMillis();
        final long intervalMillis = 1000 * keepaliveTimer;
        final long triggerAtMillis = now + intervalMillis;
        final Intent intent = new Intent(this, MQTTService.class);
        final PendingIntent operation = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, operation);
    }

    public Map<String,Object> readFromPrefs() {
        context = getApplicationContext();
        SharedPreferences sprefs = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE);
        Map<String,Object> sprefsMap = new HashMap<String,Object>();
        sprefsMap.put("connection_hostname",sprefs.getString(getString(R.string.connection_hostname), ""));
        sprefsMap.put("connection_protocol_tcp",sprefs.getBoolean(getString(R.string.connection_protocol_tcp), false));
        sprefsMap.put("connection_protocol_ssl",sprefs.getBoolean(getString(R.string.connection_protocol_ssl), false));
        sprefsMap.put("connection_protocol_xyz",sprefs.getBoolean(getString(R.string.connection_protocol_xyz), false));
        sprefsMap.put("connection_port",sprefs.getString(getString(R.string.connection_port), ""));
        sprefsMap.put("connection_username",sprefs.getString(getString(R.string.connection_username), ""));
        sprefsMap.put("connection_password",sprefs.getString(getString(R.string.connection_password), ""));
        sprefsMap.put("connection_push_notifications_subscribe_topic",sprefs.getString(getString(R.string.connection_push_notifications_subscribe_topic), ""));
        sprefsMap.put("connection_server_topic",sprefs.getString(getString(R.string.connection_server_topic), ""));
        sprefsMap.put("connection_username",sprefs.getString(getString(R.string.connection_username), ""));
        sprefsMap.put("connection_username",sprefs.getString(getString(R.string.connection_username), ""));
        sprefsMap.put("connection_in_background",sprefs.getBoolean(getString(R.string.connection_in_background), false));
        sprefsMap.put("connection_server_mode",sprefs.getBoolean(getString(R.string.connection_server_mode), false));
        sprefsMap.put("connection_keep_alive",sprefs.getInt(getString(R.string.connection_keep_alive), 60));
        if (sprefsMap.get("connection_hostname").equals("")) {
            sprefsMap.put("connection_hostname","fudannhpcc.cn");
            sprefsMap.put("connection_port","1883");
            sprefsMap.put("connection_username","nhpcc");
            sprefsMap.put("connection_password","rtfu2002");
            sprefsMap.put("connection_push_notifications_subscribe_topic","fudannhpcc/warning/#");
            sprefsMap.put("connection_server_topic","fudannhpcc/cluster/#");
        }
        return sprefsMap;
    }

}
