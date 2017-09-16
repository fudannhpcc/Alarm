package cn.fudannhpcc.www.alarm.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.commonclass.Utilites;
import cn.fudannhpcc.www.alarm.commonclass.Log;

public class MainActivity extends AppCompatActivity {

    MenuItem menuItem_add_new_widget;
    MenuItem menuItem_clear_dashboard;
    Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getName(), "onCreate()");
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
//        menuItem_add_new_widget = menu.findItem(R.id.Add_new_widget);
//        menuItem_clear_dashboard = menu.findItem(R.id.Clean_dashboard);


//        updatePlayPauseMenuItem();

//        AppSettings appSettings = AppSettings.getInstance();
//        if (appSettings.server == null) return true;
//        if (appSettings.server.equals("ravend.asuscomm.com")) {
//            optionsMenu.findItem(R.id.request_prices).setVisible(true);
//            optionsMenu.findItem(R.id.action_board).setVisible(true);
//            optionsMenu.findItem(R.id.action_lists).setVisible(true);
//        }
//
//        getDashboardViewMode();

        return super.onCreateOptionsMenu(menu);
    }

    void updatePlayPauseMenuItem() {

//        MenuItem menuItemPlayPause = optionsMenu.findItem(R.id.Edit_play_mode);

//        MenuItem menuItemAutoCreateWidgets = optionsMenu.findItem(R.id.auto_create_widgets);

//        if (presenter.isEditMode()) {
//            menuItemPlayPause.setIcon(R.drawable.ic_play);
//            menuItemAutoCreateWidgets.setVisible(presenter.getUnusedTopics().length > 0);
//
//        } else {
//            menuItemPlayPause.setIcon(R.drawable.ic_pause);
//            menuItemAutoCreateWidgets.setVisible(false);
//        }
//
//        menuItem_add_new_widget.setVisible(presenter.isEditMode());
//        menuItem_clear_dashboard.setVisible(presenter.isEditMode());

    }

}
