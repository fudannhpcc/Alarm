package cn.fudannhpcc.www.alarm.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


import cn.fudannhpcc.www.alarm.R;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, ActionBar.TabListener {

    private CustomDialog CustomDialog;
    private Intent intentSettingActivity;

    @SuppressLint("StaticFieldLeak")
    private static Activity thisActivity = null;

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

    private HashMap<String, TextView> TextViewMap = new HashMap<String, TextView>(16);
    private static final HashMap<String, Object[]> TextViewIDMap = new HashMap<String, Object[]>() {{
        put("timestamp", new Object[]{R.id.textView_lbl_temperature,"",""});
        put("dawningA", new Object[]{R.id.textView_text_dawningA,"http://www.fudannhpcc.cn/upload/WebEnvRes.dawningA.png","获取温控探头曲线"});
        put("dawningB", new Object[]{R.id.textView_text_dawningB,"http://www.fudannhpcc.cn/upload/WebEnvRes.dawningB.png","获取温控探头曲线"});
        put("dawningC", new Object[]{R.id.textView_text_dawningC,"http://www.fudannhpcc.cn/upload/WebEnvRes.dawningC.png","获取温控探头曲线"});
        put("inspur", new Object[]{R.id.textView_text_dawningD,"http://www.fudannhpcc.cn/upload/WebEnvRes.inspur.png","获取温控探头曲线"});
        put("NodeInfo", new Object[]{R.id.textView_text_alive,"http://www.fudannhpcc.cn/upload/WebResouces.png","获取计算资源使用图形"});
        put("NodesNum", new Object[]{R.id.textView_text_nodesnum,"http://www.fudannhpcc.cn/upload/WebHistory.png","获取当月计算资源使用状况"});
    }};

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        switch ((String) tab.getText()) {
            case "集群故障":
                mqtt_message_echo.setVisibility(View.VISIBLE);
                break;
            case "中心网站":
                openweb();
                break;
            default:
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        switch ((String) tab.getText()) {
            case "集群故障":
                mqtt_message_echo.setVisibility(View.GONE);
                break;
            case "中心网站":
                webView.setVisibility(View.GONE);
                break;
            default:
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    private void openweb () {
        webView.setVisibility(View.VISIBLE);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setLoadWithOverviewMode(true);
        webView.loadUrl("http://www.fudannhpcc.cn/resources.php");
        final PackageManager pm = this.getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                        || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);
        webView.getSettings().setDisplayZoomControls(!supportsMultiTouch);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar actionBar= getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.mipmap.ic_launcher);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#a159ff")));
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#228B22")));

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
        webView = (WebView) findViewById(R.id.webView);

        mqtt_message_echo.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        ActionBar.Tab TabOne = actionBar.newTab();
        TabOne.setText("集群故障").setTabListener(this);
        TabOne.setIcon(R.mipmap.ic_warning);
        TabOne.setTabListener(this);
        actionBar.addTab(TabOne);

        ActionBar.Tab TabFour = actionBar.newTab();
        TabFour.setText("中心网站").setTabListener(this);
        TabFour.setIcon(R.mipmap.ic_home);
        TabFour.setTabListener(this);
        actionBar.addTab(TabFour);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            notificationMessage = extras.getString("NotificationMessage");
        }

        mActivityMessenger = new Messenger(mMessengerHandler);

        for (Object o : TextViewIDMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            Object[] obj = (Object[]) entry.getValue();
            int id = (int) obj[0];
            TextViewMap.put(key, (TextView) findViewById(id));
        }

        for (Object o : TextViewMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final TextView sensor = (TextView) entry.getValue();
            if ( ! key.equals("timestamp") && !key.equals("NodeInfo") && !key.equals("NodesNum") ) sensor.setText("");
            sensor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if ( ! sensor.getText().toString().equals("") ) {
                    Object[] obj = TextViewIDMap.get(key);
                    String url = (String) obj[1];
                    String title = (String) obj[2];
                    if ( ! url.equals("") ) showImage(url,title);
                }
                }
            });
        }
    }

    public void showImage(String IMAGEURL, final String TITLE) {
        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
            }
        });

        final ProgressDialog progressDialog = new ProgressDialog(this);
        final PhotoView imageView = new PhotoView(this);
        Picasso.with(this)
                .load(IMAGEURL)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .into(imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                    }

                    @Override
                    public void onError() {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();
                        builder.dismiss();
                        Toast.makeText(getApplicationContext(), TITLE + "图形失败", Toast.LENGTH_SHORT).show();
                    }
                });

        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();

        progressDialog.setMessage("图片加载中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
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
            else {
                Toast.makeText(MainActivity.this,"谷歌文本朗读功能开启！", Toast.LENGTH_LONG).show();
            }
            RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.ttspermission_RGBLed);
            if ( Constants.TTS_SUPPORT ) connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
            else connectionStatusRGBLEDView.setColorLight(MyColors.getRed());

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
        }catch (Exception ignored) {
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
        assert notificationManager != null;
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
                break;
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
                assert notificationManager != null;
                notificationManager.cancelAll();
                break;
            case R.id.tts:
                if (Constants.TTS_SUPPORT) {
                    String textToConvert = "谷歌文本朗读可以正常工作！";
                    tts.speak(textToConvert, TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    Toast.makeText(this, "谷歌文本朗读不能正常工作！", Toast.LENGTH_SHORT).show();
                }
                break;
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
                break;
            case R.id.exit:
                Toast.makeText(this, "退出", Toast.LENGTH_SHORT).show();
                showChangeLangDialog(getString(R.string.exittitle),getString(R.string.exitmessage));
                break;
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
                    @SuppressLint("PrivateApi") Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
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

    private Animation animtext;

    private void init() {
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (!checkPermission()) {
                requestPermission(); // Code for permission
            }
        }

        /*  判断是否第一次启动程序 */
        if (!isFirstStart()) {
            Bundle extrasInBackground = getIntent().getExtras();
            if (extrasInBackground != null && extrasInBackground.getBoolean("MainActivityInBackground", false)) {
                moveTaskToBack(true);
            }
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

        animtext = new AlphaAnimation(0.2f, 1.0f);
        animtext.setDuration(3000); //You can manage the blinking time with this parameter
        animtext.setStartOffset(0);
        animtext.setRepeatMode(Animation.REVERSE);
        animtext.setRepeatCount(Animation.INFINITE);

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
        Constants.CLIENT_ID = getRandomString();
        return isFirstRun;
    }

    public static String getRandomString(){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random=new Random();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<8;i++){
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
    @SuppressLint("HandlerLeak")
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

    @SuppressLint("HandlerLeak")
    private Handler mMessengerHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECEIVE_MESSAGE_CODE){
                Bundle data = msg.getData();
                if(data != null){
                    if (data.containsKey("NotificationMessage")) {
                        ArrayList<HashMap<String, Object>> mNotificationList =
                                (ArrayList<HashMap<String, Object>>) data.getSerializable("NotificationMessage");
                        UpdateListView(mNotificationList);
                    }
                    else if (data.containsKey("TemperatureMessage")) {
                        String message[] = data.getString("TemperatureMessage").trim().split("\t");
                        for (Object o : TextViewMap.entrySet()) {
                            Map.Entry entry = (Map.Entry) o;
                            final String key = (String) entry.getKey();
                            final TextView sensor = (TextView) entry.getValue();
                            if ( key.equals(message[0]) ) {
                                String htmlString="<u>" + message[1] + " \u2103</u>";
                                sensor.setText(Html.fromHtml(htmlString));
                                sensor.setTextColor(getResources().getColor(R.color.colorAccent));
                            }
                            else if (!key.equals("timestamp") && !key.equals("NodeInfo") && !key.equals("NodesNum")) {
                                sensor.setTextColor(getResources().getColor(R.color.text_black));
                            }
                        }
                    }
                    else if (data.containsKey("NodeinfoMessage")) {
                        String message[] = data.getString("NodeinfoMessage").trim().split(" ");
                        TextView sensor = TextViewMap.get("NodeInfo");
                        String htmlString="<u><font color='red'>" + String.valueOf(message.length-1) + "</font></u>";
                        sensor.setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
                        sensor.startAnimation(animtext);
                    }
                    else if (data.containsKey("NodesnumMessage")) {
                        String message[] = data.getString("NodesnumMessage").trim().split(" ");
                        TextView sensor = TextViewMap.get("NodesNum");
                        String htmlString="<u><font color='red'>" + message[1] + "</font></u>";
                        sensor.setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
                        sensor.startAnimation(animtext);
                    }
                    DateFormat df = new SimpleDateFormat("HH:mm:ss");
                    String date = df.format(Calendar.getInstance().getTime());
                    TextView sensorTime = TextViewMap.get("timestamp");
                    sensorTime.setText(date);
                }
            }
            super.handleMessage(msg);
        }
    };

    ListView mqtt_message_echo;
    WebView webView;
    SimpleAdapter mqtt_message_adapter;
    private Intent intentListViewActivity;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void UpdateListView(ArrayList<HashMap<String, Object>> mNotificationList) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        assert mNotificationList != null;
        for (HashMap<String, Object> tempMap : mNotificationList) {
            Map<String, Object> map = new HashMap<String, Object>();
            Set<String> set = tempMap.keySet();
            int WARNINGID = 0;
            for (String s : set) {
                if (Objects.equals(s, "warningid")) WARNINGID = (int)tempMap.get(s);
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
                    System.exit(0);
                    android.os.Process.killProcess(android.os.Process.myPid());
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
            PackageInfo packageInfo;
            packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
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
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        assert dir != null;
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.filepermission_RGBLed);
        if (result == PackageManager.PERMISSION_GRANTED) {
            connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
            Constants.STORAGE_ACCESS = true;
            makeDirectory();
            onSaveRawtoExternal("warning.wav");
            onSaveRawtoExternal("notts.wav");
            return true;
        } else {
            Constants.STORAGE_ACCESS = false;
            connectionStatusRGBLEDView.setColorLight(MyColors.getRed());
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Constants.STORAGE_ACCESS = true;
                    makeDirectory();
                    onSaveRawtoExternal("warning.wav");
                    onSaveRawtoExternal("notts.wav");
                    Toast.makeText(this, "存储读写已授权", Toast.LENGTH_SHORT).show();
                    Log.d("Permission", "Permission Granted, Now you can use local drive .");
                } else {
                    Constants.STORAGE_ACCESS = false;
                    Toast.makeText(this, "存储读写未授权", Toast.LENGTH_SHORT).show();
                    Log.d("Permission", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) findViewById(R.id.filepermission_RGBLed);
        if ( Constants.STORAGE_ACCESS ) connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
        else connectionStatusRGBLEDView.setColorLight(MyColors.getRed());
    }

    private void makeDirectory() {
        String folder = "AlarmSound";
        File f = new File(Environment.getExternalStorageDirectory(), folder);
        if (!f.exists()) {
            final boolean mkdirs = f.mkdirs();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void onSaveRawtoExternal(String voice) {
        InputStream ins = null;
        if (Objects.equals(voice, "warning.wav")) {
            ins = getResources().openRawResource(R.raw.warning);
        }
        else if (Objects.equals(voice, "notts.wav")) {
            ins = getResources().openRawResource(R.raw.notts);
        }
        else {
            return;
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "AlarmSound/" + voice);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        try {
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                try {
                    assert os != null;
                    os.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            assert os != null;
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
