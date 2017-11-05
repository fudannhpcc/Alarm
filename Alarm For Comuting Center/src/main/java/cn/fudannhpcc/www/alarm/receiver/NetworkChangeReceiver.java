package cn.fudannhpcc.www.alarm.receiver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.activity.MainActivity;
import cn.fudannhpcc.www.alarm.commonclass.MyColors;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;


public class NetworkChangeReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(context == null || !(context instanceof MainActivity))
            return;

        AppCompatActivity yourActivity = (AppCompatActivity) context;
        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) yourActivity.findViewById(R.id.connection_status_RGBLed);
        try
        {
            if (isOnline(context)) {
                connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
            } else {
                connectionStatusRGBLEDView.setColorLight(MyColors.getRed());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline(Context context) {
        try {
            //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                //获得ConnectivityManager对象
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                //获取ConnectivityManager对象对应的NetworkInfo对象
                //获取WIFI连接的信息
                NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                //获取移动数据连接的信息
                NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
//                    Toast.makeText(context, "WIFI已连接,移动数据已连接", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (wifiNetworkInfo.isConnected() && !dataNetworkInfo.isConnected()) {
//                    Toast.makeText(context, "WIFI已连接,移动数据已断开", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (!wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
//                    Toast.makeText(context, "WIFI已断开,移动数据已连接", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
//                    Toast.makeText(context, "WIFI已断开,移动数据已断开", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            //API大于23时使用下面的方式进行网络监听
            else {
                //获得ConnectivityManager对象
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                //获取所有网络连接的信息
                Network[] networks = connMgr.getAllNetworks();
                //用于存放网络连接信息
                List<String> nettype = new ArrayList<String>();
                //通过循环将网络信息逐个取出来
                boolean iconn = false;
                for (Network network : networks) {
                    //获取ConnectivityManager对象对应的NetworkInfo对象
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                    if (networkInfo.isConnected()) iconn = true;
                    String type = networkInfo.getTypeName();
                    if (type.equalsIgnoreCase("WIFI")) {
                        nettype.add("WIFI已连接");
                    } else if (type.equalsIgnoreCase("MOBILE")) {
                        nettype.add("移动数据已连接");
                    }
                }
                if (iconn) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < nettype.size(); i++){
                        if (i == nettype.size() - 1){
                            sb.append(nettype.get(i));
                        }
                        else{
                            sb.append(nettype.get(i)).append(", ");
                        }
                    }
//                    Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show();
                }
                return iconn;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
}
