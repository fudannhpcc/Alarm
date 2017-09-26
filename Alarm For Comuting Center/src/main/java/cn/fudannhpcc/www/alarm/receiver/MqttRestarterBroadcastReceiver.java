package cn.fudannhpcc.www.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import cn.fudannhpcc.www.alarm.service.MQTTService;

public class MqttRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        new Handler(Looper.getMainLooper()).post(
            new Runnable() {
                public void run() {
                    Toast.makeText(context, "重新启动服务",Toast.LENGTH_SHORT).show();
                }
            }
        );
        context.startService(new Intent(context, MQTTService.class));
    }
}
