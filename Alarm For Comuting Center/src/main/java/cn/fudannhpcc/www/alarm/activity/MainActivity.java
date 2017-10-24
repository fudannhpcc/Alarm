package cn.fudannhpcc.www.alarm.activity;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


import cn.fudannhpcc.www.alarm.R;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.commonclass.PahoMqttClient;

import cn.fudannhpcc.www.alarm.commonclass.Constants;
import cn.fudannhpcc.www.alarm.receiver.HomeKeyObserver;
import cn.fudannhpcc.www.alarm.receiver.PowerKeyObserver;
import cn.fudannhpcc.www.alarm.service.CoreService;
import cn.fudannhpcc.www.alarm.commonclass.CustomDialog;
import cn.fudannhpcc.www.alarm.service.MQTTService;
import cn.fudannhpcc.www.alarm.commonclass.MyColors;
import cn.fudannhpcc.www.alarm.receiver.NetworkChangeReceiver;
import cn.fudannhpcc.www.alarm.receiver.ServiceUtils;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;
import util.UpdateAppUtils;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private CustomDialog CustomDialog;
    private Intent intentSettingActivity;

    static Activity thisActivity = null;

    private boolean isService = false;
    private String CoreServiceName = "";
    private String MqttServiceName = "";

    Messenger mqttService = null;
    boolean mqttBound = false;

    String notificationMessage = null;

    public static final int WARNINGIMG[] = {R.mipmap.ic_warning0,R.mipmap.ic_warning1,R.mipmap.ic_warning2};

    public static final String PREFS_NAME = "AppSettings";

    private MqttAndroidClient client;
    private PahoMqttClient pahoMqttClient;

    private static final int REQ_TTS_STATUS_CHECK = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);

        init();

        pahoMqttClient = new PahoMqttClient();
        client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        thisActivity = this;
        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();

        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.connection_status_RGBLed);
        connectionStatusRGBLEDView.setColorLight(MyColors.getRed());

        Toast.makeText(this, "启动服务", Toast.LENGTH_SHORT).show();
        Intent coreservice_intent = new Intent(this, CoreService.class);
        coreservice_intent.setAction(CoreServiceName);
        startService(coreservice_intent);

        CoreServiceName = getString(R.string.core_service_name);
        MqttServiceName = getString(R.string.mqtt_service_name);

        mqtt_message_echo = (ListView) findViewById(R.id.mqtt_message_echo);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notificationMessage = extras.getString("NotificationMessage");
        }

        mActivityMessenger = new Messenger(mMessengerHandler);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_TTS_STATUS_CHECK) {
            switch (resultCode) {
                //这个返回结果表明TTS Engine可以用
                case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                    tts = new TextToSpeech(this, (TextToSpeech.OnInitListener) this);
                    break;
                case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
                //需要的语音数据已损坏
                case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                //缺少需要语言的语音数据
                case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
                //缺少需要语言的发音数据
                //这三种情况都表明数据有错,重新下载安装需要的数据
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                    break;
                case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
                //检查失败
                default:
                    Log.d("TTS", "Got a failure. TTS not available");
            }
        }
        else {
            //其他Intent返回的结果
        }
    }

    @Override
    public void onInit(int status) {
        // 如果装载TTS引擎成功
        if (status == TextToSpeech.SUCCESS) {
            // 设置使用美式英语朗读
            int result = tts.setLanguage(Locale.CHINESE);
            Constants.TTS_SUPPORT = true;
            // 如果不支持所设置的语言
            if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE) {
                Toast.makeText(MainActivity.this,"TTS暂时不支持这种语言的朗读！", Toast.LENGTH_LONG).show();
                Constants.TTS_SUPPORT = false;
            }
        }
    }

    boolean monStart = false;
    @Override
    protected void onStart() {
        super.onStart();
        monStart = true;
    }

    boolean monResume = false;
    @Override
    protected void onResume() {
        try {
            Intent intent = new Intent(this, MQTTService.class);
            bindService(intent, mqttConnection, Context.BIND_AUTO_CREATE);
            monResume = true;
        }catch (Exception e) {
        }
        monResume = true;
        super.onResume();
    }

    boolean monPause = false;
    UpdateTimerTask updateTimerTask;
    @Override
    protected void onPause() {
        monPause = true;
        super.onPause();
    }

    boolean monStop = false;
    @Override
    protected void onStop() {
        super.onStop();
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Constants.PENDINGNOTIFICATIONCCOUNT = true;
        AppUpdateTimer = new Timer("UpdateTimer",true);
        updateTimerTask = new UpdateTimerTask();
        AppUpdateTimer.schedule(updateTimerTask, 1000L);
        monStop = true;
        if ( POWERKEY || HOMEKEY ) mqttBound = false;
        if (mqttBound) {
            unbindService(mqttConnection);
            mqttBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttBound) {
            unbindService(mqttConnection);
            mqttBound = false;
        }
        unregisterNetworkChanges();
        mHomeKeyObserver.stopListen();
        mPowerKeyObserver.stopListen();
        if (tts != null) {
            tts.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem serviceitem = menu.findItem(R.id.service);
        if (isService) {
            serviceitem.setTitle(getString(R.string.stopservice));
            serviceitem.setIcon(R.mipmap.ic_stopservice);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
        }
        else {
            serviceitem.setTitle(getString(R.string.startservice));
            serviceitem.setIcon(R.mipmap.ic_startservice);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if ( newversion ) {
            menu.findItem(R.id.update).setVisible(true);
            Constants.UPDATE_VISIBLE = true;
        }
        else {
            menu.findItem(R.id.update).setVisible(false);
            Constants.UPDATE_VISIBLE = false;
        }
        MenuItem item = menu.findItem(R.id.silent);
        if ( Constants.SILENT_SWITCH ) item.setIcon(R.mipmap.ic_silent_off);
        else item.setIcon(R.mipmap.ic_silent_on);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ( ! init_finish ) return super.onOptionsItemSelected(item);
        RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
        switch (item.getItemId()) {
            case R.id.silent:
                if ( Constants.SILENT_SWITCH ) {
                    item.setIcon(R.mipmap.ic_silent_on);
                    Constants.SILENT_SWITCH = false;
                    Toast.makeText(this, "关闭静音", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "打开静音", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.mipmap.ic_silent_off);
                    Constants.SILENT_SWITCH = true;
                }
                break;
            case R.id.service:
                if ( item.getTitle() == getString(R.string.startservice) ) {
                    Toast.makeText(this, "启动服务", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.mipmap.ic_stopservice);
                    mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
                    item.setTitle(getString(R.string.stopservice));
                    Intent coreservice_intent = new Intent(this, CoreService.class);
                    coreservice_intent.setAction(CoreServiceName);
                    startService(coreservice_intent);
                    return true;
                }
                else {
                    Toast.makeText(this, "关闭服务", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.mipmap.ic_startservice);
                    mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
                    Intent coreservice_intent = new Intent(this, CoreService.class);
                    coreservice_intent.setAction(CoreServiceName);
                    stopService(coreservice_intent);
                    item.setTitle(getString(R.string.startservice));
                }
                break;
            case R.id.setting:
                Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show();
                intentSettingActivity = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intentSettingActivity);
                return true;
            case R.id.deletemag:
                Toast.makeText(this, "删除信息", Toast.LENGTH_SHORT).show();
                Constants.PENDINGNOTIFICATIONCCOUNT = false;
                Constants.MESSAGECLEAR = true;
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                mqtt_message_adapter = new SimpleAdapter(this, list,
                        R.layout.activity_list_item, new String[] { "img", "title", "datetime", "message" },
                        new int[] { R.id.img, R.id.title, R.id.datetime, R.id.message });
                mqtt_message_adapter.notifyDataSetChanged();
                mqtt_message_echo.setAdapter(mqtt_message_adapter);
                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                return true;
            case R.id.update:
                if ( ! newversion ) return true;
                RequestQueue queue = Volley.newRequestQueue(this);
                JsonObjectRequest mJsonObjectRequest = new JsonObjectRequest(
                        Constants.UPDATE_URL + "/updatecheck.json",
                        null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    ServerApkUrl = response.getString("url");
                                    ServerVerCode = response.getInt("verCode");
                                    ServerVerName = response.getString("verName");
                                    ServerUpdateMessage = response.getString("updateMessage");
                                    checkAndUpdate(false);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(mJsonObjectRequest);
                return true;
            case R.id.exit:
                Toast.makeText(this, "退出", Toast.LENGTH_SHORT).show();
                showChangeLangDialog(getString(R.string.exittitle),getString(R.string.exitmessage));
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private HomeKeyObserver mHomeKeyObserver;
    private PowerKeyObserver mPowerKeyObserver;
    Timer MqttClientTimer = new Timer();
    Timer AppUpdateTimer = new Timer();
    boolean HOMEKEY = false;
    boolean POWERKEY = false;

    boolean init_finish = false;
    private TextToSpeech tts;

    private void init() {

        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkPermission())
            {
                // Code for above or equal 23 API Oriented Device
                // Your Permission granted already .Do next code
            } else {
                requestPermission(); // Code for permission
            }
        }
        else
        {

            // Code for Below 23 API Oriented Device
            // Do next code
        }

        /*  判断是否第一次启动程序 */
        if (isFirstStart()) {
//            Toast.makeText(this, "第一次启动", Toast.LENGTH_SHORT).show();
        } else {
            Bundle extrasInBackground = getIntent().getExtras();
            if (extrasInBackground != null && extrasInBackground.getBoolean("MainActivityInBackground", false)) {
                moveTaskToBack(true);
            }
//            Toast.makeText(this, "不是第一次启动", Toast.LENGTH_SHORT).show();
        }

        /*  启动MQTT客户端连接 */
        MqttClientTimer.schedule(mqttclienttask, 0, 3000);

        /*  锁定 Home 键 */
        mHomeKeyObserver = new HomeKeyObserver(this);
        mHomeKeyObserver.setHomeKeyListener(new HomeKeyObserver.OnHomeKeyListener() {
            @Override
            public void onHomeKeyPressed() {
                System.out.println("----> 按下Home键");
                HOMEKEY = true;
            }

            @Override
            public void onHomeKeyLongPressed() {
                System.out.println("----> 长按Home键");
                HOMEKEY = true;
            }
        });
        mHomeKeyObserver.startListen();

        /*  锁定 Power 键 */
        mPowerKeyObserver = new PowerKeyObserver(this);
        mPowerKeyObserver.setHomeKeyListener(new PowerKeyObserver.OnPowerKeyListener() {
            @Override
            public void onPowerKeyPressed() {
                System.out.println("----> 按下电源键");
                POWERKEY = true;
            }
        });
        mPowerKeyObserver.startListen();
    }

    /* 开始： 判断程序是不是第一次启动 */
    private boolean isFirstStart() {
        SharedPreferences sprefs = this.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = sprefs.getBoolean("First_Start", true);
        SharedPreferences.Editor Editor = sprefs.edit();
        if (isFirstRun) {
            Editor.putBoolean("First_Start", false);
            Editor.putString(getString(R.string.connection_hostname), "fudannhpcc.cn");
            Editor.putBoolean(getString(R.string.connection_protocol_tcp), true);
            Editor.putBoolean(getString(R.string.connection_protocol_ssl), false);
            Editor.putBoolean(getString(R.string.connection_protocol_tls), false);
            Editor.putString(getString(R.string.connection_port), "18883");
            Editor.putString(getString(R.string.connection_username), "nhpcc");
            Editor.putString(getString(R.string.connection_password), "rtfu2002");
            Editor.putString(getString(R.string.connection_push_notifications_subscribe_topic), "fudannhpcc/alarm/");
            Editor.putString(getString(R.string.connection_server_topic), "");
            Editor.putInt(getString(R.string.connection_keep_alive), 30);
            Editor.putString(getString(R.string.connection_mqtt_server), "tcp://fudannhpcc.cn:18883");
            Editor.putString(getString(R.string.connection_update_url), "http://www.fudannhpcc.cn/apkupdate");
            if (!Editor.commit()) {
                Toast.makeText(this, "commit failure!!!", Toast.LENGTH_SHORT).show();
            }
            Constants.SUBSCRIBE_TOPIC = "fudannhpcc/alarm/";
            Constants.USERNAME = "nhpcc";
            Constants.PASSWORD = "rtfu2002";
            Constants.MQTT_BROKER_URL = "tcp://fudannhpcc.cn:18883";
            Constants.UPDATE_URL = "http://www.fudannhpcc.cn/apkupdate";
        }
        else {
            Constants.SUBSCRIBE_TOPIC = sprefs.getString(getString(R.string.connection_push_notifications_subscribe_topic),"fudannhpcc/alarm/");
            Constants.USERNAME = sprefs.getString(getString(R.string.connection_username),"nhpcc");
            Constants.PASSWORD = sprefs.getString(getString(R.string.connection_password),"rtfu2002");
            Constants.MQTT_BROKER_URL = sprefs.getString(getString(R.string.connection_mqtt_server),"tcp://fudannhpcc.cn:18883");
            Constants.UPDATE_URL = sprefs.getString(getString(R.string.connection_update_url),"http://www.fudannhpcc.cn/apkupdate");
        }
        Constants.CLIENT_ID = getRandomString(8);
        return isFirstRun;
    }

    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(52);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
    /* 结束： 判断程序是不是第一次启动 */

    private class UpdateTimerTask extends TimerTask {
        @Override
        public void run() {
            if ( Constants.UPDATE_VISIBLE ) return;
            /*  检查是否有新版本出来 */
            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            JsonObjectRequest mJsonObjectRequest = new JsonObjectRequest(
                    Constants.UPDATE_URL + "/updatecheck.json",
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                ServerApkUrl = response.getString("url");
                                ServerVerCode = response.getInt("verCode");
                                ServerVerName = response.getString("verName");
                                ServerUpdateMessage = response.getString("updateMessage");
                                checkAndUpdate(true);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });
            queue.add(mJsonObjectRequest);        }
    }

    /* 开始： MQTT客户端连接认证 */
    TimerTask mqttclienttask = new TimerTask() {
        @Override
        public void run() {
            isService = ServiceUtils.isServiceRunning(getApplicationContext(),CoreServiceName);
            invalidateOptionsMenu();
            if ( mqttBound ) {
                String topic = Constants.SUBSCRIBE_TOPIC_CLIENT;
                if (!topic.isEmpty()) {
                    try {
                        pahoMqttClient.subscribe(client, topic, 1);
                        client.setCallback(new MqttCallbackExtended() {
                            @Override
                            public void connectComplete(boolean reconnect, String serverURI) {
                                if (reconnect) {
                                    String topic = Constants.SUBSCRIBE_TOPIC_CLIENT;
                                    try {
                                        pahoMqttClient.subscribe(client, topic, 1);
                                    } catch (MqttException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                }
                            }

                            @Override
                            public void connectionLost(Throwable cause) {
                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Message message = new Message();
                    if ( Constants.SUBSCRIBE_STATUS ) message.what = 999;
                    else message.what = -999;
                    mHandler.sendMessage(message);
                }
            }
        }
    };
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 999) {
                init_finish = true;
                MqttClientTimer.cancel();
            }
            super.handleMessage(msg);
        }
    };
    /* 结束： MQTT客户端连接认证 */

    /*  开始： 绑定服务通讯 */
    Messenger mActivityMessenger = null;
    private static final int SEND_MESSAGE_CODE = 0x0001;
    private static final int RECEIVE_MESSAGE_CODE = 0x0002;

    private ServiceConnection mqttConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mqttService = new Messenger(service);
            mqttBound = true;

            Message message = Message.obtain();
            message.what = SEND_MESSAGE_CODE;
            Bundle data = new Bundle();
            int clickNotification = -999;
            if (!monPause) clickNotification = 0;
            data.putInt("pendingNotificationsCount", clickNotification);
            message.setData(data);
            message.replyTo = mActivityMessenger;
            try {
                mqttService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mqttService = null;
            mqttBound = false;
        }
    };


    private Handler mMessengerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECEIVE_MESSAGE_CODE){
                Bundle data = msg.getData();
                if(data != null){
                    ArrayList<HashMap<String, Object>> mNotificationList =
                            (ArrayList<HashMap<String, Object>>) data.getSerializable("NotificationMessage");
                    UpdateListView(mNotificationList);
                }
            }
            super.handleMessage(msg);
        }
    };

    ListView mqtt_message_echo;
    SimpleAdapter mqtt_message_adapter;
    private Intent intentListViewActivity;
    private void UpdateListView(ArrayList<HashMap<String, Object>> mNotificationList) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        assert mNotificationList != null;
        for (HashMap<String, Object> tempMap : mNotificationList) {
            Map<String, Object> map = new HashMap<String, Object>();
            Set<String> set = tempMap.keySet();
            int WARNINGID = 0;
            for (String s : set) {
                if ( s == "warningid") WARNINGID = (int)tempMap.get(s);
                else map.put(s, String.valueOf(tempMap.get(s)));
            }
            if ( WARNINGID > 0 ) map.put("img", WARNINGIMG[WARNINGID-1]);
            else map.put("img", WARNINGIMG[0]);
            list.add(map);
        }
        Collections.reverse(list);
        mqtt_message_adapter = new SimpleAdapter(this, list,
                R.layout.activity_list_item, new String[] { "img", "title", "datetime", "message" },
                new int[] { R.id.img, R.id.title, R.id.datetime, R.id.message });
        mqtt_message_adapter.notifyDataSetChanged();
        mqtt_message_echo.setAdapter(mqtt_message_adapter);

        mqtt_message_echo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> objItem = (HashMap<String, Object>) mqtt_message_adapter.getItem(position);
                intentListViewActivity = new Intent(MainActivity.this, ListViewActivity.class);
                intentListViewActivity.putExtra("listviewItem", objItem);
                startActivity(intentListViewActivity);
            }
        });

    }
    /*  结束： 绑定服务通讯 */

    /* 开始： 退出对话框 */
    public void showChangeLangDialog(String title, String message) {
        CustomDialog = new CustomDialog(MainActivity.this);
        CustomDialog.setTitle(title);
        CustomDialog.setMessage(message);
            CustomDialog.setYesOnclickListener("确定", new CustomDialog.onYesOnclickListener() {
                @Override
                public void onYesClick() {
                    CustomDialog.dismiss();
                    unbindService(mqttConnection);
                    mqttBound = false;
                    Intent coreservice_intent = new Intent(MainActivity.this, CoreService.class);
                    coreservice_intent.setAction(CoreServiceName);
                    stopService(coreservice_intent);
                    Intent mqttservice_intent = new Intent(MainActivity.this, MQTTService.class);
                    mqttservice_intent.setAction(MqttServiceName);
                    stopService(mqttservice_intent);
                    moveTaskToBack(true);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            });
        CustomDialog.setNoOnclickListener("取消", new CustomDialog.onNoOnclickListener() {
            @Override
            public void onNoClick() {
                CustomDialog.dismiss();
            }
        });
        CustomDialog.show();
    }
    /* 结束： 退出对话框 */

    /* 开始： 监控网络状况 */
    private BroadcastReceiver mNetworkReceiver;
    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
    /* 结束： 监控网络状况 */


    /* 开始： 程序更新 */
    private String ServerApkUrl;
    private String ServerVerName;
    private int ServerVerCode;
    private String ServerUpdateMessage;

    private PackageInfo getVersionCode() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean newversion = false;
    private void checkAndUpdate(boolean only) {
        PackageInfo packageInfo = getVersionCode();
        if ( packageInfo == null ) return;

        boolean tmpversion = false;
        int localversionCode = packageInfo.versionCode;
        if ( localversionCode < ServerVerCode ) tmpversion = true;
        else {
            if ( Float.parseFloat(packageInfo.versionName) < Float.parseFloat(ServerVerName)) tmpversion = true;
        }

        newversion = tmpversion;
        invalidateOptionsMenu();

        if ( newversion ) {
            if ( only ) {
                if ( ! Constants.SILENT_SWITCH ) {
                    if ( ! Constants.UPDATE_VISIBLE ) {
                        if (Constants.TTS_SUPPORT) {
                            String textToConvert = "有新版本出来... 去更新吧";
                            tts.speak(textToConvert, TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
                return;
            }
            else {
                realUpdate();
                Toast.makeText(this, "点击确认后将在后台下载更新！", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if ( ! only ) Toast.makeText(this,"当前版本是最新版",Toast.LENGTH_LONG).show();
        }
    }


    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
    private void realUpdate() {
        trimCache(MainActivity.this);
        UpdateAppUtils.from(MainActivity.this)
                .checkBy(UpdateAppUtils.CHECK_BY_VERSION_NAME)
                .serverVersionName(ServerVerName)
                .serverVersionCode(ServerVerCode)
                .updateInfo(ServerUpdateMessage)
                .showNotification(true)
                .apkPath(ServerApkUrl)
                .downloadBy(UpdateAppUtils.DOWNLOAD_BY_APP)
                .isForce(false)
                .update();
    }

    /* 结束： 程序更新 */

    private static final int PERMISSION_REQUEST_CODE = 2222;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            Constants.STORAGE_ACCESS = true;
            onSaveRawtoExternal();
            return true;
        } else {
            Constants.STORAGE_ACCESS = false;
            return false;
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "存储读写未授权通知声音被关闭", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Constants.STORAGE_ACCESS = true;
                    onSaveRawtoExternal();
                    Toast.makeText(this, "存储读写已授权", Toast.LENGTH_SHORT).show();
                    Log.d("Permission", "Permission Granted, Now you can use local drive .");
                } else {
                    Constants.STORAGE_ACCESS = false;
                    Toast.makeText(this, "存储读写未授权", Toast.LENGTH_SHORT).show();
                    Log.d("Permission", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    private void onSaveRawtoExternal() {
        InputStream ins = getResources().openRawResource(R.raw.warning);
        OutputStream os = null;
        try {
            os = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "warning.wav");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        try {
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                try {
                    os.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
