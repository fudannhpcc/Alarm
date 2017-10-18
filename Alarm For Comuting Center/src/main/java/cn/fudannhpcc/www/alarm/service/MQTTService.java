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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.activity.MainActivity;
import cn.fudannhpcc.www.alarm.commonclass.Constants;
import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.commonclass.PahoMqttClient;

public class MQTTService extends Service {

    public static final String PREFS_NAME = "AppSettings";

    public static final String WARNINGTITLE = "集群故障: ";
    public static int WARNINGID = 0;


    private int pendingNotificationsCount = 0;

    private static final String TAG = "MqttService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;

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

    private TextToSpeech tts;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        SprefsMap = readFromPrefs();
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            public void run() {
                                Toast.makeText(context,"MQTT服务启动", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        }).start();
        Log.d("CLIENT_ID",Constants.CLIENT_ID);
        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
//                Log.d(TAG, "connectComplete");
            }

            @Override
            public void connectionLost(Throwable throwable) {
//                Log.d(TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, new String(mqttMessage.getPayload()));
//                Log.d(TAG, String.valueOf(Constants.STORAGE_ACCESS) + " " + String.valueOf(Constants.TTS_SUPPORT));
                setMessageNotification(s, new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
//                Log.d(TAG, "deliveryComplete");
            }
        });
    }

    @Override
    public void onDestroy() {
        iService = false;
//        stopForeground(true);
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    public void run() {
                        Toast.makeText(context, "MQTT服务退出",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        try {
            iService = (boolean) SprefsMap.get("connection_server_mode");
        } catch ( Exception e ) {

        }
        if ( iService ) {
            Intent broadcastIntent = new Intent(String.valueOf(R.string.mqtt_restart_name));
            sendBroadcast(broadcastIntent);
        }
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SprefsMap = readFromPrefs();
        if ( Constants.TTS_SUPPORT && Constants.STORAGE_ACCESS ) {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    // 如果装载TTS引擎成功
                    if (status == TextToSpeech.SUCCESS) {
                        // 设置使用美式英语朗读
                        int result = tts.setLanguage(Locale.CHINESE);
                        // 如果不支持所设置的语言
                        if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE) {
//                            Toast.makeText(context, "TTS暂时不支持这种语言的朗读！", Toast.LENGTH_LONG).show();
                        }
                        tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                            public void onUtteranceCompleted(String utteranceId) {
//                                Log.d(TAG, "Speech Completed! :" + utteranceId);
                                WARNINGSOUND = Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/notification.wav");
                                qmtt_notification(NOTIFY_ID, title, message, WARNINGSOUND);
                            }
                        });
                    }
                }
            });
        }
        return START_STICKY;
    }

    public Map<String,Object> readFromPrefs() {
        context = getApplicationContext();
        SharedPreferences sprefs = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE);
        Map<String,Object> sprefsMap = new HashMap<String,Object>();
        sprefsMap.put("connection_hostname",sprefs.getString(getString(R.string.connection_hostname), ""));
        sprefsMap.put("connection_protocol_tcp",sprefs.getBoolean(getString(R.string.connection_protocol_tcp), false));
        sprefsMap.put("connection_protocol_ssl",sprefs.getBoolean(getString(R.string.connection_protocol_ssl), false));
        sprefsMap.put("connection_protocol_xyz",sprefs.getBoolean(getString(R.string.connection_protocol_tls), false));
        sprefsMap.put("connection_port",sprefs.getString(getString(R.string.connection_port), ""));
        sprefsMap.put("connection_username",sprefs.getString(getString(R.string.connection_username), ""));
        sprefsMap.put("connection_password",sprefs.getString(getString(R.string.connection_password), ""));
        sprefsMap.put("connection_push_notifications_subscribe_topic",sprefs.getString(getString(R.string.connection_push_notifications_subscribe_topic), ""));
        sprefsMap.put("connection_server_topic",sprefs.getString(getString(R.string.connection_server_topic), ""));
        sprefsMap.put("connection_mqtt_server",sprefs.getString(getString(R.string.connection_mqtt_server), ""));
        if (sprefsMap.get("connection_hostname").equals("")) {
            sprefsMap.put("connection_hostname","fudannhpcc.cn");
            sprefsMap.put("connection_port","18883");
            sprefsMap.put("connection_username","nhpcc");
            sprefsMap.put("connection_password","rtfu2002");
            sprefsMap.put("connection_push_notifications_subscribe_topic","fudannhpcc/alarm/");
            sprefsMap.put("connection_server_topic","fudannhpcc/cluster/#");
        }
        Constants.SUBSCRIBE_TOPIC = sprefs.getString(getString(R.string.connection_push_notifications_subscribe_topic), "fudannhpcc/alarm/");
        Constants.USERNAME = sprefs.getString(getString(R.string.connection_username), "nhpcc");
        Constants.PASSWORD = sprefs.getString(getString(R.string.connection_password), "rtfu2002");
        Constants.MQTT_BROKER_URL = sprefs.getString(getString(R.string.connection_mqtt_server), "tcp://fudannhpcc.cn:18883");
        return sprefsMap;
    }

    private List<Map<String, Object>> mNotificationList = new ArrayList<Map<String, Object>>();

    public void qmtt_notification(int NOTIFY_ID, String title, String message, Uri WARNINGSOUND) {

        pendingNotificationsCount++;

        Map<String, Object> mNotificationMap = new HashMap<String, Object>();
        mNotificationMap.put("title", title);
        mNotificationMap.put("message", message);
        mNotificationMap.put("Count", pendingNotificationsCount);
        mNotificationMap.put("warningid", WARNINGID);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String datestr = sdf.format(new Date());
        mNotificationMap.put("datetime", datestr);
        if ( Constants.PENDINGNOTIFICATIONCCOUNT ) {
            Constants.PENDINGNOTIFICATIONCCOUNT = false;
            pendingNotificationsCount = 1;
        }
        else {
            if (pendingNotificationsCount == 1) mNotificationList.clear();
            if ( Constants.MESSAGECLEAR ) {
                pendingNotificationsCount = 1;
                mNotificationList.clear();
                Constants.MESSAGECLEAR = false;
            }
        }
        mNotificationList.add(mNotificationMap);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
// 设置通知的基本信息：icon、标题、内容
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(context, R.color.transparent));
        builder.setSmallIcon(R.drawable.ic_stat_name);
        builder.setLargeIcon(bitmap);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setContentInfo(pendingNotificationsCount + " 条新消息");
//        builder.setSubText(pendingNotificationsCount + " 条新消息");
        builder.setAutoCancel(true);
        if ( Constants.SILENT_SWITCH ) {
            builder.setVibrate(new long[]{0,0,0,0});
            builder.setSound(null);
            builder.setLights(0,0,0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_SECRET);
            }
        }
        else {
            builder.setVibrate(new long[]{0, 300, 500, 700});
            if ( Constants.TTS_SUPPORT && Constants.STORAGE_ACCESS) builder.setSound(WARNINGSOUND);
            else builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setLights(0xff0000ff, 300, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
        }
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(false);
        builder.setCategory(Notification.CATEGORY_MESSAGE);
        builder.setColor(0xff0000);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
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

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }

    String title = null, message = null; Uri WARNINGSOUND = null;
    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {
        if (topic.toLowerCase().contains(Constants.SUBSCRIBE_TOPIC.toLowerCase())) {
            title = WARNINGTITLE;
            String voicetitle = "";
            message = msg.split("]")[1].trim();
            String messagelist[] = message.split("\n");
            WARNINGID = 0;
            for(String tmp : messagelist) {
                tmp = tmp.trim();
                if (tmp.contains("温控报警")) {
                    title += "温控 ";
                    WARNINGID += 1;
                    String[] aa = ((tmp.split("：")[1]).trim()).split(" ");
                    int iaa = aa.length;
                    voicetitle += ".. " + iaa + "个温控报警";
                }
                if (tmp.contains("节点故障")) {
                    title += "运行 ";
                    WARNINGID += 1;
                    String[] bb = ((tmp.split("：")[1]).trim()).split(" ");
                    int ibb = bb.length;
                    voicetitle += ".. " + ibb + "个节点运行故障";
                }
                if (tmp.contains("宕机节点")) {
                    title += "宕机 ";
                    WARNINGID += 1;
                    String[] cc = ((tmp.split("：")[1]).trim()).split(" ");
                    int icc = cc.length;
                    voicetitle += ".. " + icc + "个节点宕机故障";
                }
            }
            message = message.trim();
            if ( Constants.TTS_SUPPORT && Constants.STORAGE_ACCESS ) {
                String textToConvert = "复旦大学高端计算中心.. 集群出现.. " + voicetitle + ".. 请速速查看";
                HashMap<String, String> myHashRender = new HashMap();
                String destinationFileName = Environment.getExternalStorageDirectory() + "/notification.wav";
                myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, textToConvert);
                tts.synthesizeToFile(textToConvert, myHashRender, destinationFileName);
            }
            else qmtt_notification(NOTIFY_ID, title, message, WARNINGSOUND);
        }
    }
}
