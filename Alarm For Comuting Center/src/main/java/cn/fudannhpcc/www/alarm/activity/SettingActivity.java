package cn.fudannhpcc.www.alarm.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.reflect.Method;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.commonclass.AppSettings;

public class SettingActivity extends AppCompatActivity {

    EditText server;
    EditText port;
    EditText username;
    EditText password;
    EditText server_topic;
    EditText push_notifications_subscribe_topic;
    CheckBox connection_in_background;
    CheckBox server_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        server = (EditText) findViewById(R.id.editText_server);
        port = (EditText) findViewById(R.id.editText_port);
        username = (EditText) findViewById(R.id.editText_username);
        password = (EditText) findViewById(R.id.editText_password);
        server_topic = (EditText) findViewById(R.id.editText_server_topic);
        push_notifications_subscribe_topic = (EditText) findViewById(R.id.editText_push_notifications_subscribe_topic);
        //notifications_service = (CheckBox) findViewById(R.id.checkBox_start_notifications_service);
        connection_in_background = (CheckBox) findViewById(R.id.checkBox_connection_in_background);
        server_mode = (CheckBox) findViewById(R.id.checkBox_server_mode);

        AppSettings appSettings = AppSettings.getInstance();
        server.setText(appSettings.server);
        port.setText(appSettings.port);
        username.setText(appSettings.username);
        password.setText(appSettings.password);
        server_topic.setText(appSettings.server_topic);
        push_notifications_subscribe_topic.setText(appSettings.push_notifications_subscribe_topic);
        connection_in_background.setChecked(appSettings.connection_in_background);
        server_mode.setChecked(appSettings.server_mode);

//        final ImageView help_topic = (ImageView) findViewById(R.id.help_push_topic);
//        help_topic.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View arg0, MotionEvent arg1) {
//                switch (arg1.getAction()) {
//                    case MotionEvent.ACTION_DOWN: {
//                        System.out.println("topic down");
////                        help.setImageBitmap(res.getDrawable(R.drawable.img_down));
//                        break;
//                    }
//                    case MotionEvent.ACTION_CANCEL:{
//                        System.out.println("topic cancel");
////                        v.setImageBitmap(res.getDrawable(R.drawable.img_up));
//                        break;
//                    }
//                }
//                return true;
//            }
//        });
//        final ImageView help_server_mode = (ImageView) findViewById(R.id.help_application_server_mode);
//        help_server_mode.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View arg0, MotionEvent arg1) {
//                switch (arg1.getAction()) {
//                    case MotionEvent.ACTION_DOWN: {
//                        System.out.println("server down");
////                        help.setImageBitmap(res.getDrawable(R.drawable.img_down));
//                        break;
//                    }
//                    case MotionEvent.ACTION_CANCEL:{
//                        System.out.println("server cancel");
////                        v.setImageBitmap(res.getDrawable(R.drawable.img_up));
//                        break;
//                    }
//                }
//                return true;
//            }
//        });

    }

    public boolean validateUrl(String adress) {
        if (adress.endsWith(".cn") ) return true;
        return Patterns.DOMAIN_NAME.matcher(adress).matches();
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
                Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
                System.out.println(server.getText().toString());
                if (!validateUrl(server.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "Server address is incorrect!", Toast.LENGTH_SHORT).show();
                    return false;
                }
                AppSettings appSettings = AppSettings.getInstance();
                appSettings.server = server.getText().toString();
                System.out.println(appSettings.server);
                appSettings.port = port.getText().toString();
                appSettings.username = username.getText().toString();
                appSettings.password = password.getText().toString();
                appSettings.server_topic = server_topic.getText().toString();
                appSettings.push_notifications_subscribe_topic = push_notifications_subscribe_topic.getText().toString();
                appSettings.connection_in_background = connection_in_background.isChecked();
                appSettings.server_mode = server_mode.isChecked();

                appSettings.saveConnectionSettingsToPrefs(this);
//
//                //MainActivity.presenter.restartService(this);
//                if(MainActivity.presenter!=null) {
//                    MainActivity.presenter.connectionSettingsChanged();
//                }

                finish();
                //MainActivity.connectToMQTTServer(getApplicationContext());
//                MainActivity.presenter.resetCurrentSessionTopicList();
//
//                MainActivity.presenter.subscribeToAllTopicsInDashboards(settings);
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