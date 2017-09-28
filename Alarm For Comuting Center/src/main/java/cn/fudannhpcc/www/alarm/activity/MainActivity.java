package cn.fudannhpcc.www.alarm.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.commonclass.Constants;
import cn.fudannhpcc.www.alarm.commonclass.PahoMqttClient;
import cn.fudannhpcc.www.alarm.service.CoreService;
import cn.fudannhpcc.www.alarm.commonclass.CustomDialog;
import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.service.MQTTService;
import cn.fudannhpcc.www.alarm.commonclass.MyColors;
import cn.fudannhpcc.www.alarm.receiver.NetworkChangeReceiver;
import cn.fudannhpcc.www.alarm.receiver.ServiceUtils;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;

public class MainActivity extends AppCompatActivity {

    private CustomDialog CustomDialog;
    private Intent intentSettingActivity;
    private Intent intentListViewActivity;
    private BroadcastReceiver mNetworkReceiver;

    static Activity thisActivity = null;

    private boolean isService = false;
    private String CoreServiceName = "";
    private String MqttServiceName = "";

    Messenger mqttService = null;
    Messenger mActivityMessenger = null;
    boolean mqttBound = false;

    ListView mqtt_message_echo;
    SimpleAdapter mqtt_message_adapter;

    String notificationMessage = null;
    Intent intent;

//    private MqttAndroidClient client;
//    private String TAG = "MainActivity";
//    private PahoMqttClient pahoMqttClient;

    public static final int WARNINGIMG = R.mipmap.ic_notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        pahoMqttClient = new PahoMqttClient();
//        client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

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
//            mqtt_message_echo.setText(notificationMessage);
        }

        mActivityMessenger = new Messenger(mMessengerHandler);

//        Intent intent = new Intent(MainActivity.this, MQTTService.class);
//        startService(intent);

    }

    boolean monStart = false;
    @Override
    protected void onStart() {
        super.onStart();
        monStart = true;
        Log.d("onStart()","HELLO:" + String.valueOf(monStart));
    }

    boolean monResume = false;
    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, mqttConnection, Context.BIND_AUTO_CREATE);
        monResume = true;
        Log.d("onResume()","HELLO:" + String.valueOf(monResume));
        super.onResume();
    }

    boolean monPause = false;
    @Override
    protected void onPause() {
        monPause = true;
        Log.d("onPause()","HELLO:" + String.valueOf(monPause));
        super.onPause();
    }

    boolean monStop = false;
    @Override
    protected void onStop() {
        super.onStop();
        monStop = true;
        Log.d("onStop()","HELLO:" + String.valueOf(monStop));
        // Unbind from the service
        if (mqttBound) {
            unbindService(mqttConnection);
            mqttBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
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
//                Log.d("DemoLog-Client", "客户端向service发送信息");
                mqttService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
//                Log.d("DemoLog", "客户端向service发送消息失败: " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
//            Log.d("onServiceDisconnected","HELLO");
            mqttService = null;
            mqttBound = false;
        }
    };


    private Handler mMessengerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            Log.d("DemoLog-Client", "ClientHandler -> handleMessage");
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

    private void UpdateListView(ArrayList<HashMap<String, Object>> mNotificationList) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        assert mNotificationList != null;
        for (HashMap<String, Object> tempMap : mNotificationList) {
            Map<String, Object> map = new HashMap<String, Object>();
            Set<String> set = tempMap.keySet();
            for (String s : set) {
                map.put(s, String.valueOf(tempMap.get(s)));
            }
            map.put("img", WARNINGIMG);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Log.d("onDestroy","HELLO");
//        if (mqttBound) {
//            unbindService(mqttConnection);
//            mqttBound = false;
//        }
        unregisterNetworkChanges();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.start);
        if (isService) {
            item.setTitle(getString(R.string.stop));
            item.setIcon(R.drawable.ic_stop);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
        }
        else {
            item.setTitle(getString(R.string.start));
            item.setIcon(R.drawable.ic_start);
            RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
            mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) findViewById(R.id.mqtt_broker_status_RGBLed);
        switch (item.getItemId()) {
            case R.id.start:
                if ( item.getTitle() == getString(R.string.start) ) {
                    Toast.makeText(this, "启动服务", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_stop);
                    mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
                    item.setTitle(getString(R.string.stop));
                    Intent coreservice_intent = new Intent(this, CoreService.class);
                    coreservice_intent.setAction(CoreServiceName);
                    startService(coreservice_intent);
//                    Intent mqttservice_intent = new Intent(this, MQTTService.class);
//                    mqttservice_intent.setAction(MqttServiceName);
//                    startService(mqttservice_intent);
//                    String topic = Constants.SUBSCRIBE_TOPIC;
//                    if (!topic.isEmpty()) {
//                        try {
//                            pahoMqttClient.subscribe(client, topic, 1);
//                        } catch (MqttException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    finish();
//                    Intent intent = new Intent(this, MainActivity.class);
//                    startActivity(intent);
                    return true;
                }
                else {
                    Toast.makeText(this, "关闭服务", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_start);
                    mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
                    Intent coreservice_intent = new Intent(this, CoreService.class);
                    coreservice_intent.setAction(CoreServiceName);
                    stopService(coreservice_intent);
                    item.setTitle(getString(R.string.start));
                }
                break;
            case R.id.setting:
                Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show();
                intentSettingActivity = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intentSettingActivity);
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

    public void showChangeLangDialog(String title, String message) {
        CustomDialog = new CustomDialog(MainActivity.this);
        CustomDialog.setTitle(title);
        CustomDialog.setMessage(message);
        CustomDialog.setYesOnclickListener("确定", new CustomDialog.onYesOnclickListener() {
            @Override
            public void onYesClick() {
                Toast.makeText(MainActivity.this,"点击了--确定--按钮",Toast.LENGTH_LONG).show();
                CustomDialog.dismiss();
                finish();
            }
        });
        CustomDialog.setNoOnclickListener("取消", new CustomDialog.onNoOnclickListener() {
            @Override
            public void onNoClick() {
                Toast.makeText(MainActivity.this,"点击了--取消--按钮",Toast.LENGTH_LONG).show();
                CustomDialog.dismiss();
            }
        });
        CustomDialog.show();
    }

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

}
