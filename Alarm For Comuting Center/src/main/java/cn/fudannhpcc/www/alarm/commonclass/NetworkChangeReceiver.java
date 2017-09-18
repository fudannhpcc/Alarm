package cn.fudannhpcc.www.alarm.commonclass;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;

import static cn.fudannhpcc.www.alarm.activity.MainActivity.ConnectStatus;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) ((Activity)context).findViewById(R.id.connection_status_RGBLed);
        try
        {
            if (isOnline(context)) {
                ConnectStatus(true);
                connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
            } else {
                ConnectStatus(false);
                connectionStatusRGBLEDView.setColorLight(MyColors.getRed());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

}
