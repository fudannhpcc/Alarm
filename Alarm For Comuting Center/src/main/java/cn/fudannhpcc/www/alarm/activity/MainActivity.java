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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.fudannhpcc.www.alarm.R;
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
    private BroadcastReceiver mNetworkReceiver;

    static Activity thisActivity = null;

    private boolean isService = false;
    private boolean isMQTTService = false;
    private String CoreServiceName = "";
    private String MQTTServiceName = "";

    Messenger mqttService = null;
    Messenger mActivityMessenger = null;
    boolean mqttBound = false;

    ListView mqtt_message_echo;

    String notificationMessage = null;
    Intent intent;

    public static final int WARNINGIMG[] = {R.mipmap.ic_temperature,R.mipmap.ic_error,R.mipmap.ic_shutdown};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thisActivity = this;
        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();

        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.connection_status_RGBLed);
        connectionStatusRGBLEDView.setColorLight(MyColors.getRed());

        CoreServiceName = getString(R.string.core_service_name);
        MQTTServiceName = getString(R.string.mqtt_service_name);

        isService = ServiceUtils.isServiceRunning(getApplicationContext(),CoreServiceName);

        mqtt_message_echo = (ListView) findViewById(R.id.mqtt_message_echo);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notificationMessage = extras.getString("NotificationMessage");
//            mqtt_message_echo.setText(notificationMessage);
        }

        mActivityMessenger = new Messenger(mMessengerHandler);
        intent = new Intent(this, MQTTService.class);

        isMQTTService = ServiceUtils.isServiceRunning(getApplicationContext(),MQTTServiceName);

//        if ( isMQTTService ) mqttBound = true;

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
            int WARNINGID = -1;
            for (String s : set) {
                if ( s.equals("warningid") ) {
                    WARNINGID = Integer.parseInt(String.valueOf(tempMap.get(s)));
                }
                else {
                    map.put(s, String.valueOf(tempMap.get(s)));
                }
            }
            map.put("img", WARNINGIMG[WARNINGID]);
            list.add(map);
        }
        Collections.reverse(list);
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.activity_list_item, new String[] { "img", "title", "datetime", "message" },
                new int[] { R.id.img, R.id.title, R.id.datetime, R.id.message });
        adapter.notifyDataSetChanged();
        mqtt_message_echo.setAdapter(adapter);
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
