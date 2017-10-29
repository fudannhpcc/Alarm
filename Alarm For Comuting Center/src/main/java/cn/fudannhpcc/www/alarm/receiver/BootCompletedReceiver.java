package cn.fudannhpcc.www.alarm.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Objects;

import cn.fudannhpcc.www.alarm.activity.MainActivity;
import cn.fudannhpcc.www.alarm.service.CoreService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }
//            Intent coreservice = new Intent(context, CoreService.class);
//            coreservice.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startService(coreservice);
        Intent newIntent = new Intent(context, MainActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        newIntent.getExtras().putBoolean("MainActivityInBackground",true);
        context.startActivity(newIntent);
    }
}
