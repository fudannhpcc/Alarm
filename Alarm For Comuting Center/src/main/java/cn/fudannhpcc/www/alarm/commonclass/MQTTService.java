package cn.fudannhpcc.www.alarm.commonclass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.activity.MainActivity;

public class MQTTService extends Service implements CallbackMQTTClient.IMQTTMessageReceiver {

    public static final String PREFS_NAME = "AppSettings";

    private static MQTTService instance;
    static public MQTTService getInstance() {
        return instance;
    }

    private Context context;

    private boolean iService = true;

    private int NOTIFY_ID = 1883;

    CallbackMQTTClient callbackMQTTClient;

    private Map<String,Object> SprefsMap;

    public MQTTService() {
        instance = this;
        SprefsMap = new HashMap<String,Object>();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mqttBinder;
    }

    private final IBinder mqttBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        SprefsMap = readFromPrefs();

        new Thread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    Thread.sleep(10000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                Iterator it = SprefsMap.keySet().iterator();
//                while(it.hasNext()) {
//                    String key = (String)it.next();
//                    System.out.println("key:" + key);
//                    System.out.println("value:" + SprefsMap.get(key));
//                }
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            public void run() {
                                Toast.makeText(context,"MQTT服务启动", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        iService = false;
        stopForeground(true);
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(context, "MQTT服务退出",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        boolean iservice = (boolean)SprefsMap.get("connection_server_mode");
        if ( iservice ) {
            Intent broadcastIntent = new Intent("cn.fudannhpcc.www.alarm.commonclass.MqttRestarterBroadcastReceiver");
            sendBroadcast(broadcastIntent);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        new Handler(Looper.getMainLooper()).post(
//                new Runnable() {
//                    public void run() {
//                        Toast.makeText(context, "onStartCommand",Toast.LENGTH_SHORT).show();
//                    }
//                }
//        );
        new Thread(new Runnable() {
            @Override
            public void run() {
            if ( iService ) {
                String title = "中心集群故障报警";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String datestr = sdf.format(new Date());
                String message = datestr + ": 这是测试";
                qmtt_notification(NOTIFY_ID, title, message);
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            public void run() {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String datestr = sdf.format(new Date());
                                Toast.makeText(context, datestr, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
            }
        }).start();
        return START_STICKY;
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

    public void qmtt_notification(int NOTIFY_ID, String title, String message) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
// 设置通知的基本信息：icon、标题、内容
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        builder.setSmallIcon(R.drawable.help);
        builder.setLargeIcon(bitmap);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle(title);
        builder.setContentText(message);
//        builder.setContentInfo(pendingNotificationsCount + " 条新消息");
        builder.setAutoCancel(true);
        builder.setVibrate(new long[] {0,300,500,700});
        builder.setLights(0xff0000ff, 300, 0);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(false);
        Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning);
        builder.setSound(sound);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

//        builder.setNumber(pendingNotificationsCount);

        // 设置通知的点击行为：这里启动一个 Activity
        Intent intent = new Intent(this, MainActivity.class);
        try {
            intent.putExtra("NotificationMessage", message);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        // 发送通知 id 需要在应用内唯一
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }
}
