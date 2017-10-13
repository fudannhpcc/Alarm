package cn.fudannhpcc.www.alarm.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cn.fudannhpcc.www.alarm.commonclass.PahoMqttClient;
import feature.Callback;

import cn.fudannhpcc.www.alarm.commonclass.Constants;
import cn.fudannhpcc.www.alarm.receiver.HomeKeyObserver;
import cn.fudannhpcc.www.alarm.receiver.PowerKeyObserver;
import cn.fudannhpcc.www.alarm.service.CoreService;
import cn.fudannhpcc.www.alarm.commonclass.CustomDialog;
import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.service.MQTTService;
import cn.fudannhpcc.www.alarm.commonclass.MyColors;
import cn.fudannhpcc.www.alarm.receiver.NetworkChangeReceiver;
import cn.fudannhpcc.www.alarm.receiver.ServiceUtils;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;
import customview.ConfirmDialog;
import util.UpdateAppUtils;

public class MainActivity extends AppCompatActivity {

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        pahoMqttClient = new PahoMqttClient();
        client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        thisActivity = this;
        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();

        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.connection_status_RGBLed);
        connectionStatusRGBLEDView.setColorLight(MyColors.getRed());

        CoreServiceName = getString(R.string.core_service_name);
        MqttServiceName = getString(R.string.mqtt_service_name);

        isService = ServiceUtils.isServiceRunning(getApplicationContext(),CoreServiceName);

        mqtt_message_echo = (ListView) findViewById(R.id.mqtt_message_echo);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notificationMessage = extras.getString("NotificationMessage");
        }

        mActivityMessenger = new Messenger(mMessengerHandler);

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
    @Override
    protected void onPause() {
        monPause = true;
        super.onPause();
    }

    boolean monStop = false;
    @Override
    protected void onStop() {
        super.onStop();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.service);
        if (isService) {
            item.setTitle(getString(R.string.stopservice));
            item.setIcon(R.mipmap.ic_stopservice);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
        }
        else {
            item.setTitle(getString(R.string.startservice));
            item.setIcon(R.mipmap.ic_startservice);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
        switch (item.getItemId()) {
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
            case R.id.update:
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
    boolean HOMEKEY = false;
    boolean POWERKEY = false;

    private void init() {
        /*  判断是否第一次启动程序 */
        if (isFirstStart()) {
//            Toast.makeText(this, "第一次启动", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(this, "不是第一次启动", Toast.LENGTH_SHORT).show();
        }

        /*  启动MQTT客户端连接 */
        MqttClientTimer.schedule(task, 0, 10000);

        /*  检查是否有新版本出来 */
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
        queue.add(mJsonObjectRequest);


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

    /* 开始： MQTT客户端连接认证 */


    TimerTask task = new TimerTask() {
        @Override
        public void run() {
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

            String topic = Constants.SUBSCRIBE_TOPIC_CLIENT;

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

    private void checkAndUpdate(boolean only) {
        PackageInfo packageInfo = getVersionCode();
        if ( packageInfo == null ) return;

        int localversionCode = packageInfo.versionCode;
        boolean iupdate = false;
        if ( localversionCode < ServerVerCode ) iupdate = true;
        else {
            if ( Float.parseFloat(packageInfo.versionName) < Float.parseFloat(ServerVerName)) iupdate = true;
        }
        if ( iupdate ) {
            if ( only ) {
                Dialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle("应用版本检查")
                        .setMessage("\t\t有新版本 " + ServerVerName + " 更新！\n\t\t目前版本号：" + packageInfo.versionName )
                        .setIcon(R.mipmap.ic_launcher)
                        .setNegativeButton("知道啦", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).
                        create();
                alertDialog.show();
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
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_GRANTED) {
//                realUpdate();
//            } else {//申请权限
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//            }
//        }
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
//        SharedPreferences sprefs = getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        SharedPreferences.Editor editor = sprefs.edit();
//        editor.clear();
//        editor.commit();
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

    //权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    realUpdate();
                } else {
                    new ConfirmDialog(this, new Callback() {
                        @Override
                        public void callback(int position) {
                            if (position==1){
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                                startActivity(intent);
                            }
                        }
                    }).setContent("暂无读写SD卡权限\n是否前往设置？").show();
                }
                break;
        }

    }
    /* 结束： 程序更新 */

}
