package cn.fudannhpcc.www.alarm.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.HashMap;

import cn.fudannhpcc.www.alarm.R;


public class ListViewActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);

        android.support.v7.app.ActionBar actionBar= getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.mipmap.ic_launcher);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#a159ff")));

        Intent intent = getIntent();
        HashMap<String, Object> listviewItem = (HashMap<String, Object>) intent.getSerializableExtra("listviewItem");
        String title = (String) listviewItem.get("title");
        int imageid = (int) listviewItem.get("img");
        String datetime = (String) listviewItem.get("datetime");
        String message = (String) listviewItem.get("message");

        TextView listview_activity_title = (TextView) findViewById(R.id.listview_activity_title);
        listview_activity_title.setText(title);
        ImageView listview_activity_image = (ImageView) findViewById(R.id.listview_activity_image);
        listview_activity_image.setImageResource(imageid);
        TextView listview_activity_datetime = (TextView) findViewById(R.id.listview_activity_datetime);
        listview_activity_datetime.setText(datetime);
        TextView listview_activity_message = (TextView) findViewById(R.id.listview_activity_message);
        listview_activity_message.setText(message);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.listview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
