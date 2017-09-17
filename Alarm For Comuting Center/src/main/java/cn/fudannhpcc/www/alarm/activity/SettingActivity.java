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
    //CheckBox notifications_service;
    CheckBox connection_in_background;
    CheckBox server_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final ImageView help_topic = (ImageView) findViewById(R.id.help_push_topic);
        help_topic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                switch (arg1.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        System.out.println("topic down");
//                        help.setImageBitmap(res.getDrawable(R.drawable.img_down));
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:{
                        System.out.println("topic cancel");
//                        v.setImageBitmap(res.getDrawable(R.drawable.img_up));
                        break;
                    }
                }
                return true;
            }
        });
        final ImageView help_server_mode = (ImageView) findViewById(R.id.help_application_server_mode);
        help_server_mode.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                switch (arg1.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        System.out.println("server down");
//                        help.setImageBitmap(res.getDrawable(R.drawable.img_down));
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:{
                        System.out.println("server cancel");
//                        v.setImageBitmap(res.getDrawable(R.drawable.img_up));
                        break;
                    }
                }
                return true;
            }
        });

    }

    public boolean validateUrl(String adress) {
        if (adress.endsWith(".xyz") ) return true;
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
//                if (!validateUrl(server.getText().toString())) {
//                    Toast.makeText(getApplicationContext(), "Server address is incorrect!", Toast.LENGTH_SHORT).show();
//                    return false;
//                }
                AppSettings settings = AppSettings.getInstance();
                settings.server = server.getText().toString();
//                settings.port = port.getText().toString();
//                settings.username = username.getText().toString();
//                settings.password = password.getText().toString();
//                settings.server_topic = server_topic.getText().toString();
//                settings.push_notifications_subscribe_topic = push_notifications_subscribe_topic.getText().toString();
//                settings.connection_in_background = connection_in_background.isChecked();
//                settings.server_mode = server_mode.isChecked();

//                settings.saveConnectionSettingsToPrefs(this);
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