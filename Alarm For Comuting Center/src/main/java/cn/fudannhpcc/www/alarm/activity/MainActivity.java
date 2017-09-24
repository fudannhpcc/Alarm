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
import android.os.IBinder;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.commonclass.CoreService;
import cn.fudannhpcc.www.alarm.commonclass.CustomDialog;
import cn.fudannhpcc.www.alarm.commonclass.Log;
import cn.fudannhpcc.www.alarm.commonclass.MQTTService;
import cn.fudannhpcc.www.alarm.commonclass.MyColors;
import cn.fudannhpcc.www.alarm.commonclass.NetworkChangeReceiver;
import cn.fudannhpcc.www.alarm.commonclass.ServiceUtils;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;

public class MainActivity extends AppCompatActivity {

    private CustomDialog CustomDialog;
    private Intent intentSettingActivity;
    private BroadcastReceiver mNetworkReceiver;

    static Activity thisActivity = null;

    private boolean isService = false;
    private String CoreServiceName = "";
    private String MQTTServiceName = "";

    TextView mqtt_message_echo = null;
    TextView system_log = null;

    String notificationMessage = null;

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

        mqtt_message_echo = (TextView) findViewById(R.id.mqtt_message_echo);
        system_log = (TextView) findViewById(R.id.system_log);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notificationMessage = extras.getString("NotificationMessage");
            mqtt_message_echo.setText(notificationMessage);
        }

    }

    MQTTService mqttService;
    boolean mqttBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, mqttConnection, Context.BIND_AUTO_CREATE);
        Log.d("onStart()","HELLO");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("onStop()","HELLO");
        // Unbind from the service
        if (mqttBound) {
            unbindService(mqttConnection);
            mqttBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume()","HELLO");
        Log.d(String.valueOf(mqttBound),"HELLO");
        Toast.makeText(this, String.valueOf(mqttBound), Toast.LENGTH_SHORT).show();
        if (mqttBound) {
            // Call a method from the LocalService.
            // However, if this call were something that might hang, then this request should
            // occur in a separate thread to avoid slowing down the activity performance.
            mqttService.setpendingNotificationsCount();
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mqttConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("onServiceConnected","HELLO");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MQTTService.LocalBinder binder = (MQTTService.LocalBinder) service;
            mqttService = binder.getService();
            mqttBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("onServiceDisconnected","HELLO");
            mqttBound = false;
        }
    };

     @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy","HELLO");
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
                system_log.setText("点击了--确定--按钮");
                finish();
            }
        });
        CustomDialog.setNoOnclickListener("取消", new CustomDialog.onNoOnclickListener() {
            @Override
            public void onNoClick() {
                Toast.makeText(MainActivity.this,"点击了--取消--按钮",Toast.LENGTH_LONG).show();
                system_log.setText("点击了--取消--按钮");
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
