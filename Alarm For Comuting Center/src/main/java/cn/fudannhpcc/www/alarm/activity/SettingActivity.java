package cn.fudannhpcc.www.alarm.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Method;

import cn.fudannhpcc.www.alarm.R;

public class SettingActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "AppSettings";

    private EditText hostname;
    private CheckBox protocol_tcp;
    private CheckBox protocol_ssl;
    private CheckBox protocol_xyz;
    private EditText port;
    private EditText username;
    private EditText password;
    private EditText push_notifications_subscribe_topic;
    private CheckBox in_background;
    private CheckBox server_mode;
    private EditText server_topic;
    private EditText keep_alive;

    private String mqtt_protocol = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        hostname = (EditText) findViewById(R.id.editText_hostname);
        protocol_tcp = (CheckBox) findViewById(R.id.checkBox_tcp_protocol);
        protocol_ssl = (CheckBox) findViewById(R.id.checkBox_ssl_protocol);
        protocol_xyz = (CheckBox) findViewById(R.id.checkBox_xyz_protocol);
        port = (EditText) findViewById(R.id.editText_port);
        username = (EditText) findViewById(R.id.editText_username);
        password = (EditText) findViewById(R.id.editText_password);
        push_notifications_subscribe_topic = (EditText) findViewById(R.id.editText_push_notifications_subscribe_topic);
        in_background = (CheckBox) findViewById(R.id.checkBox_connection_in_background);
        server_mode = (CheckBox) findViewById(R.id.checkBox_server_mode);
        server_topic = (EditText) findViewById(R.id.editText_server_topic);
        keep_alive = (EditText) findViewById(R.id.editText_keep_alive);

        SharedPreferences sprefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hostname.setText(sprefs.getString(getString(R.string.connection_hostname),""));
        protocol_tcp.setChecked(sprefs.getBoolean(getString(R.string.connection_protocol_tcp),false));
        protocol_ssl.setChecked(sprefs.getBoolean(getString(R.string.connection_protocol_ssl),false));
        protocol_xyz.setChecked(sprefs.getBoolean(getString(R.string.connection_protocol_xyz),false));
        port.setText(sprefs.getString(getString(R.string.connection_port),""));
        username.setText(sprefs.getString(getString(R.string.connection_username),""));
        password.setText(sprefs.getString(getString(R.string.connection_password),""));
        push_notifications_subscribe_topic.setText(sprefs.getString(getString(R.string.connection_push_notifications_subscribe_topic),""));
        in_background.setChecked(sprefs.getBoolean(getString(R.string.connection_in_background),false));
        server_mode.setChecked(sprefs.getBoolean(getString(R.string.connection_server_mode),false));
        server_topic.setText(sprefs.getString(getString(R.string.connection_server_topic),""));
        keep_alive.setText(String.valueOf(sprefs.getInt(getString(R.string.connection_keep_alive),60)));

        if (protocol_tcp.isChecked()) mqtt_protocol = "tcp://";
        if (protocol_ssl.isChecked()) mqtt_protocol = "ssl://";
        if (protocol_xyz.isChecked()) mqtt_protocol = "xyz://";

        if (hostname.getText().toString().equals("")) {
            hostname.setText("fudannhpcc.cn");
            port.setText("1883");
            username.setText("nhpcc");
            password.setText("odhSFqxSDACF");
            push_notifications_subscribe_topic.setText("fudannhpcc/warning/#");
            server_topic.setText("fudannhpcc/cluster/#");
        }

        protocol_tcp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (protocol_tcp.isChecked()) {
                    protocol_ssl.setChecked(false);
                    protocol_xyz.setChecked(false);
                    mqtt_protocol = "tcp://";
                }
            }
        });
        protocol_ssl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (protocol_ssl.isChecked()) {
                    protocol_tcp.setChecked(false);
                    protocol_xyz.setChecked(false);
                    mqtt_protocol = "ssl://";
                }
            }
        });
        protocol_xyz.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (protocol_xyz.isChecked()) {
                    protocol_tcp.setChecked(false);
                    protocol_ssl.setChecked(false);
                    mqtt_protocol = "xyz://";
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                SharedPreferences sprefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor Editor = sprefs.edit();
                Editor.putString(getString(R.string.connection_hostname), hostname.getText().toString().replace(" ", ""));
                Editor.putBoolean(getString(R.string.connection_protocol_tcp), protocol_tcp.isChecked());
                Editor.putBoolean(getString(R.string.connection_protocol_ssl), protocol_ssl.isChecked());
                Editor.putBoolean(getString(R.string.connection_protocol_xyz), protocol_xyz.isChecked());
                Editor.putString(getString(R.string.connection_port), port.getText().toString().replace(" ", ""));
                Editor.putString(getString(R.string.connection_username), username.getText().toString().replace(" ", ""));
                Editor.putString(getString(R.string.connection_password), password.getText().toString().replace(" ", ""));
                Editor.putString(getString(R.string.connection_push_notifications_subscribe_topic), push_notifications_subscribe_topic.getText().toString().replace(" ", ""));
                Editor.putBoolean(getString(R.string.connection_in_background), in_background.isChecked());
                Editor.putBoolean(getString(R.string.connection_server_mode), server_mode.isChecked());
                Editor.putString(getString(R.string.connection_server_topic), server_topic.getText().toString().replace(" ", ""));
                Editor.putInt(getString(R.string.connection_keep_alive), Integer.parseInt(keep_alive.getText().toString().replace(" ", "")));
                if ( !protocol_tcp.isChecked() && !protocol_ssl.isChecked() && !protocol_xyz.isChecked() ) {
                    String errmsg = "网络协议必须选择一个";
                    Toast.makeText(this, errmsg, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (hostname.getText().toString().replace(" ", "").equals("")) {
                    String errmsg = "中心集群域名不能为空或全为空格";
                    Toast.makeText(this, errmsg, Toast.LENGTH_SHORT).show();
                    break;
                }
                if (port.getText().toString().replace(" ", "").equals("")) {
                    String errmsg = "通讯端口不能为空或全为空格";
                    Toast.makeText(this, errmsg, Toast.LENGTH_SHORT).show();
                    break;
                }
                Editor.putString(getString(R.string.connection_mqtt_server), mqtt_protocol + hostname.getText().toString().replace(" ", ""));
                if (!Editor.commit()) {
                    Toast.makeText(this, "commit failure!!!", Toast.LENGTH_SHORT).show();
                    break;
                }
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
                return true;
            case R.id.back:
                Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
                finish();
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
}