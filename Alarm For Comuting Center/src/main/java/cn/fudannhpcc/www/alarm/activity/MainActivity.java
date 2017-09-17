package cn.fudannhpcc.www.alarm.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Process;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Method;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.commonclass.CustomDialog;

public class MainActivity extends AppCompatActivity {

    private CustomDialog CustomDialog;
    private Intent intentSettingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        Process.killProcess(Process.myPid());
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.start:
                if ( item.getTitle() == getString(R.string.start) ) {
                    Toast.makeText(this, "开始", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_stop);
                    item.setTitle(getString(R.string.stop));
                }
                else {
                    Toast.makeText(this, "结束", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.ic_start);
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
                onDestroy();
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
}
