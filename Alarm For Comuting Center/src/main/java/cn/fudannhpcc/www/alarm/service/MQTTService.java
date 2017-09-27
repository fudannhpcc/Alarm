package cn.fudannhpcc.www.alarm.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.activity.MainActivity;
import cn.fudannhpcc.www.alarm.commonclass.Constants;
import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.commonclass.PahoMqttClient;

public class MQTTService extends Service {

    public static final String PREFS_NAME = "AppSettings";

    public static final String WARNINGTITLE[] = {"机房温度探测器报警","计算节点运行故障报警","计算节点宕机报警"};
    public static final int WARNINGSOUNDID[] = {R.raw.temperaturewarning,R.raw.clusterwarning,R.raw.shutdownwarning};


    private int pendingNotificationsCount = 0;
//    private int LOOPNUM = 0;
//    private int LOOPMAX = 1;

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;

    private static final List<String> mqtttype = new ArrayList(Arrays.asList("fudannhpcc/iot/","fudannhpcc/cluster/","fudannhpcc/alarm/"));



    private static MQTTService instance;
    static public MQTTService getInstance() {
        return instance;
    }

    private Context context;

    private boolean iService = true;

    private int NOTIFY_ID = 1883;

    private Map<String,Object> SprefsMap;

    public MQTTService() {
        instance = this;
        SprefsMap = new HashMap<String,Object>();
    }

    private Messenger activityMessenger;
    private Messenger activityMessengerReply;


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("DemoLog-Service", "ServiceHandler -> handleMessage");
            if(msg.what == RECEIVE_MESSAGE_CODE){
                Bundle data = msg.getData();
                if(data != null){
                    int clickNotification = data.getInt("pendingNotificationsCount");
                    if ( clickNotification != -999 ) pendingNotificationsCount = clickNotification;
                    Log.d("DemoLog-Service", "MyService收到客户端如下信息: " + pendingNotificationsCount);
                }
                activityMessenger = msg.replyTo;
                Log.d("DemoLog-Service", "activityMessengerReply");
                activityMessengerReply = msg.replyTo;
                if(activityMessengerReply != null) {
                    Log.d("DemoLog-Service", "activityMessengerReply-send");
                    Message NotificationMessage = Message.obtain();
                    NotificationMessage.what = SEND_MESSAGE_CODE;
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("NotificationMessage", (Serializable) mNotificationList);
                    NotificationMessage.setData(bundle);
                    try {
                        activityMessengerReply.send(NotificationMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
//                Log.d("DemoLog-NotificationMessage", "MyService向客户端发送信息失败: " + e.getMessage());
                    }
                }
            }
            super.handleMessage(msg);
        }
    }

    private static final int RECEIVE_MESSAGE_CODE = 0x0001;
    private static final int SEND_MESSAGE_CODE = 0x0002;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
//        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    private static volatile boolean isRunning;

    @Override
    public boolean onUnbind(Intent intent) {
        isRunning = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

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
        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {
                android.util.Log.d(TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                setMessageNotification(s, new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
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
            Intent broadcastIntent = new Intent(String.valueOf(R.string.mqtt_restart_name));
            sendBroadcast(broadcastIntent);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SprefsMap = readFromPrefs();
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

    private List<Map<String, Object>> mNotificationList = new ArrayList<Map<String, Object>>();

    public void qmtt_notification(int NOTIFY_ID, int WARINGID, String title, String message, Uri WARNINGSOUND) {

        pendingNotificationsCount++;

        Map<String, Object> mNotificationMap = new HashMap<String, Object>();
        mNotificationMap.put("warningid", WARINGID);
        mNotificationMap.put("title", title);
        mNotificationMap.put("message", message);
        mNotificationMap.put("Count", pendingNotificationsCount);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String datestr = sdf.format(new Date());
        mNotificationMap.put("datetime", datestr);
        if (pendingNotificationsCount==1) mNotificationList.clear();
        mNotificationList.add(mNotificationMap);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
// 设置通知的基本信息：icon、标题、内容
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        builder.setSmallIcon(context.getResources().getIdentifier("ic_notification_smallicon", "mipmap", context.getPackageName()));
        builder.setLargeIcon(bitmap);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setContentInfo(pendingNotificationsCount + " 条新消息");
        builder.setAutoCancel(true);
        builder.setVibrate(new long[] {0,300,500,700});
        builder.setLights(0xff0000ff, 300, 0);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(false);
        builder.setSound(WARNINGSOUND);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        builder.setNumber(pendingNotificationsCount);

        if(activityMessenger != null) {
            Message NotificationMessage = Message.obtain();
            NotificationMessage.what = SEND_MESSAGE_CODE;
            Bundle bundle = new Bundle();
            bundle.putSerializable("NotificationMessage", (Serializable) mNotificationList);
            NotificationMessage.setData(bundle);
            try {
                activityMessenger.send(NotificationMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }

    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {
        int itype = 0;
        for(Iterator it = mqtttype.iterator(); it.hasNext(); itype++) {
            if (topic.toLowerCase().contains(((String) it.next()).toLowerCase())) break;
        }

        switch ( itype ) {
//            case 0:
//                android.util.Log.d(TAG, "MQTTService --- " + mqtttype.get(itype) + ": " + msg);
//                break;
//            case 1:
//                android.util.Log.d(TAG, "MQTTService --- " + mqtttype.get(itype) + ": " + msg);
//                break;
            case 2:
                Log.d(TAG, "MQTTService --- " + mqtttype.get(itype) + ": " + msg);
                int WARNINGID = 0;
                boolean mWarning = false;
                String title = null, message = null; Uri WARNINGSOUND = null;
                if ( msg.contains("温度报警") ) {
                    WARNINGID = 0;
                    Log.d(TAG, String.valueOf(WARNINGID));
                    title = WARNINGTITLE[WARNINGID];
                    message = "温度报警" + msg.split("温度报警")[1];
                    WARNINGSOUND = Uri.parse("android.resource://" + getPackageName() + "/" + WARNINGSOUNDID[WARNINGID]);
                    mWarning = true;
                }
                if ( msg.contains("节点宕机") ) {
                    WARNINGID = 2;
                    Log.d(TAG, String.valueOf(WARNINGID));
                    title = WARNINGTITLE[WARNINGID];
                    message = "节点宕机" + msg.split("节点宕机")[1];
                    WARNINGSOUND = Uri.parse("android.resource://" + getPackageName() + "/" + WARNINGSOUNDID[WARNINGID]);
                    mWarning = true;
                }
                if ( msg.contains("节点故障") ) {
                    WARNINGID = 2;
                    Log.d(TAG, String.valueOf(WARNINGID));
                    title = WARNINGTITLE[WARNINGID];
                    message = "节点故障" + msg.split("节点故障")[1];
                    WARNINGSOUND = Uri.parse("android.resource://" + getPackageName() + "/" + WARNINGSOUNDID[WARNINGID]);
                    mWarning = true;
                }
                if ( mWarning ) qmtt_notification(NOTIFY_ID, WARNINGID, title, message, WARNINGSOUND);
                break;
            default:
                break;
        }
    }
}
