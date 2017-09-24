package cn.fudannhpcc.www.alarm.commonclass;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cn.fudannhpcc.www.alarm.commonclass.MQTTService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent coreservice = new Intent(context, CoreService.class);
            coreservice.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(coreservice);
        }
    }
}
