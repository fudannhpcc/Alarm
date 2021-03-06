package cn.fudannhpcc.www.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PowerKeyObserver {
    private Context mContext;
    private OnPowerKeyListener mOnPowerKeyListener;
    private PowerKeyBroadcastReceiver mPowerKeyBroadcastReceiver;
    public PowerKeyObserver(Context context) {
        this.mContext = context;
    }

    //注册广播接收者
    public void startListen(){
        IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mPowerKeyBroadcastReceiver=new PowerKeyBroadcastReceiver();
        mContext.registerReceiver(mPowerKeyBroadcastReceiver, mIntentFilter);
        System.out.println("----> 开始监听");
    }

    //取消广播接收者
    public void stopListen(){
        if (mPowerKeyBroadcastReceiver!=null) {
            mContext.unregisterReceiver(mPowerKeyBroadcastReceiver);
            System.out.println("----> 停止监听");
        }
    }

    // 对外暴露接口
    public void setHomeKeyListener(OnPowerKeyListener powerKeyListener) {
        mOnPowerKeyListener = powerKeyListener;
    }

    // 回调接口
    public interface OnPowerKeyListener {
        public void onPowerKeyPressed();
    }

    //广播接收者
    class PowerKeyBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mOnPowerKeyListener.onPowerKeyPressed();
            }
        }
    }

}

