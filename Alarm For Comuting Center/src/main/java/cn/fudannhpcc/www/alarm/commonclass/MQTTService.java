package cn.fudannhpcc.www.alarm.commonclass;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.fudannhpcc.www.alarm.R;

public class MQTTService extends Service implements CallbackMQTTClient.IMQTTMessageReceiver {

    public static final String PREFS_NAME = "AppSettings";

    private static MQTTService instance;
    static public MQTTService getInstance() {
        return instance;
    }

    private Context context;

    CallbackMQTTClient callbackMQTTClient;

    private Map<String,Object> SprefsMap;

    public MQTTService() {
        instance = this;
        SprefsMap = new HashMap<String,Object>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        SprefsMap = readFromPrefs();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Iterator it = SprefsMap.keySet().iterator();
//                while(it.hasNext()) {
//                    String key = (String)it.next();
//                    System.out.println("key:" + key);
//                    System.out.println("value:" + SprefsMap.get(key));
//                }
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            public void run() {
                                Toast.makeText(context,"服务启动啦", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(context, "onDestroy()",Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(context, "onStartCommand",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    new Handler(Looper.getMainLooper()).post(
                            new Runnable() {
                                public void run() {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    String datestr = sdf.format(new Date());
                                    Toast.makeText(context, datestr, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                } while(true);
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
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

