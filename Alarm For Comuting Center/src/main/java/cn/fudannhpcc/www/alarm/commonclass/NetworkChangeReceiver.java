package cn.fudannhpcc.www.alarm.commonclass;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.fudannhpcc.www.alarm.R;
import cn.fudannhpcc.www.alarm.customview.RGBLEDView;

import static cn.fudannhpcc.www.alarm.activity.MainActivity.ConnectStatus;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        RGBLEDView connectionStatusRGBLEDView = (RGBLEDView) ((Activity)context).findViewById(R.id.connection_status_RGBLed);
        RGBLEDView mqttbrokerStatusRGBLEDView = (RGBLEDView) ((Activity)context).findViewById(R.id.mqtt_broker_status_RGBLed);
        TextView system_log = (TextView) ((Activity)context).findViewById(R.id.system_log);
        try
        {
            if (isOnline(context)) {
                ConnectStatus(true);
                connectionStatusRGBLEDView.setColorLight(MyColors.getGreen());
                mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getGreen());
                system_log.setText("网络连通啦");
            } else {
                ConnectStatus(false);
                connectionStatusRGBLEDView.setColorLight(MyColors.getRed());
                mqttbrokerStatusRGBLEDView.setColorLight(MyColors.getRed());
                system_log.setText("网络断开啦");
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
                    Toast.makeText(context, "WIFI已连接,移动数据已连接", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (wifiNetworkInfo.isConnected() && !dataNetworkInfo.isConnected()) {
                    Toast.makeText(context, "WIFI已连接,移动数据已断开", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (!wifiNetworkInfo.isConnected() && dataNetworkInfo.isConnected()) {
                    Toast.makeText(context, "WIFI已断开,移动数据已连接", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(context, "WIFI已断开,移动数据已断开", Toast.LENGTH_SHORT).show();
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
                for (int i = 0; i < networks.length; i++) {
                    //获取ConnectivityManager对象对应的NetworkInfo对象
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
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
                            sb.append(nettype.get(i) + ", ");
                        }
                    }
                    Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show();
                }
                return iconn;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
}
