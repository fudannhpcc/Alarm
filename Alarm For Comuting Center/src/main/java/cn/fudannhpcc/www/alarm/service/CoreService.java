package cn.fudannhpcc.www.alarm.service;

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
import java.util.Map;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.receiver.ServiceUtils;

public class CoreService extends Service {

    public static final String PREFS_NAME = "AppSettings";

    private int keepaliveTimer = -1;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, Object> sprefsMap = readFromPrefs();
        keepaliveTimer = Integer.parseInt(sprefsMap.get(getString(R.string.connection_keep_alive)).toString());
//        keepMeAlive();
        UpdateAlive();
    }

    @Override
    public void onDestroy() {
//        new Handler(Looper.getMainLooper()).post(
//                new Runnable() {
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), "CORE服务退出",Toast.LENGTH_SHORT).show();
//                    }
//                }
//        );
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        String MQTTerviceName = getString(R.string.mqtt_service_name);
        boolean isService = ServiceUtils.isServiceRunning(getApplicationContext(),MQTTerviceName);
        if ( !isService ) this.startService(new Intent(this,MQTTService.class));
//        new Handler(Looper.getMainLooper()).post(
//                new Runnable() {
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), "Core服务保护启动",Toast.LENGTH_SHORT).show();
//                    }
//                }
//        );
        return START_STICKY;
    }

//    public void keepMeAlive() {
//        final long now = System.currentTimeMillis();
//        final long intervalMillis = 1000 * keepaliveTimer;
//        final long triggerAtMillis = now + intervalMillis;
//        final Intent intent = new Intent(this, MQTTService.class);
//        final PendingIntent operation = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
//        assert alarm != null;
//        alarm.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, operation);
//    }

    private static final String ACTION_CHECK_UPDATE = "cn.fudannhpcc.www.alarm.service.CHECK_UPDATE";

    public void UpdateAlive() {
        final long now = System.currentTimeMillis();
        final long intervalMillis = 1000 * keepaliveTimer;
        final long triggerAtMillis = now + intervalMillis;
        final Intent intent = new Intent(this, MQTTService.class);
        intent.setAction(ACTION_CHECK_UPDATE);
        final PendingIntent operation = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        assert alarm != null;
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, operation);
    }

    public Map<String,Object> readFromPrefs() {
        Context context = getApplicationContext();
        SharedPreferences sprefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Map<String,Object> sprefsMap = new HashMap<String,Object>();
        sprefsMap.put("connection_keep_alive",sprefs.getInt(getString(R.string.connection_keep_alive), 60));
        return sprefsMap;
    }

}
