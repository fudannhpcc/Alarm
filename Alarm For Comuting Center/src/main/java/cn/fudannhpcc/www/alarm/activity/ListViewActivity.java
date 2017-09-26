package cn.fudannhpcc.www.alarm.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
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

    private HashMap<String, Object> listviewItem;
    private TextView listview_activity_title;
    private ImageView listview_activity_image;
    private TextView listview_activity_datetime;
    private TextView listview_activity_message;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);

        Intent intent = getIntent();
        listviewItem = (HashMap<String, Object>) intent.getSerializableExtra("listviewItem");
        String title = (String)listviewItem.get("title");
        int imageid = (int) listviewItem.get("img");
        String datetime = (String)listviewItem.get("datetime");
        String message = (String)listviewItem.get("message");

        listview_activity_title = (TextView) findViewById(R.id.listview_activity_title);
        listview_activity_title.setText(title);
        listview_activity_image = (ImageView) findViewById(R.id.listview_activity_image);
        listview_activity_image.setImageResource(imageid);
        listview_activity_datetime = (TextView) findViewById(R.id.listview_activity_datetime);
        listview_activity_datetime.setText(datetime);
        listview_activity_message = (TextView) findViewById(R.id.listview_activity_message);
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
