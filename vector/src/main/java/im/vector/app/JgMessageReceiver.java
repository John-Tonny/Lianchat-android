package im.vector.app;

import cn.jpush.android.service.JPushMessageReceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import cn.jpush.android.api.CmdMessage;
import cn.jpush.android.api.CustomMessage;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.JPushMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;
import timber.log.Timber;

public class JgMessageReceiver extends JPushMessageReceiver {

    @Override
    public void onMessage(Context context, CustomMessage customMessage) {
        Timber.i(" [onMessage] " + customMessage);
        Intent intent = new Intent("com.jiguang.demo.message");
        intent.putExtra("msg", customMessage.message);
        context.sendBroadcast(intent);
    }

    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
        Timber.i(" [onNotifyMessageOpened] " + message);
        /*
        try{
            //打开自定义的Activity
            Intent i = new Intent(context, TestActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(JPushInterface.EXTRA_NOTIFICATION_TITLE,message.notificationTitle);
            bundle.putString(JPushInterface.EXTRA_ALERT,message.notificationContent);
            i.putExtras(bundle);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
            context.startActivity(i);
        }catch (Throwable throwable){

        }
         */
    }

    @Override
    public void onMultiActionClicked(Context context, Intent intent) {
        Timber.i(" [onMultiActionClicked] 用户点击了通知栏按钮");
        String nActionExtra = intent.getExtras().getString(JPushInterface.EXTRA_NOTIFICATION_ACTION_EXTRA);

        //开发者根据不同 Action 携带的 extra 字段来分配不同的动作。
        if (nActionExtra == null) {
            Timber.i("ACTION_NOTIFICATION_CLICK_ACTION nActionExtra is null");
            return;
        }
        if (nActionExtra.equals("my_extra1")) {
            Timber.i(" [onMultiActionClicked] 用户点击通知栏按钮一");
        } else if (nActionExtra.equals("my_extra2")) {
            Timber.i(" [onMultiActionClicked] 用户点击通知栏按钮二");
        } else if (nActionExtra.equals("my_extra3")) {
            Timber.i(" [onMultiActionClicked] 用户点击通知栏按钮三");
        } else {
            Timber.i(" [onMultiActionClicked] 用户点击通知栏按钮未定义");
        }
    }

    @Override
    public void onNotifyMessageArrived(Context context, NotificationMessage message) {
        Timber.i(" [onNotifyMessageArrived] " + message);
    }

    @Override
    public void onNotifyMessageDismiss(Context context, NotificationMessage message) {
        Timber.i(" [onNotifyMessageDismiss] " + message);
    }

    @Override
    public void onRegister(Context context, String registrationId) {
        Timber.i(" [onRegister] " + registrationId);
        Intent intent = new Intent("com.jiguang.demo.register");
        context.sendBroadcast(intent);
    }

    @Override
    public void onConnected(Context context, boolean isConnected) {
        Timber.i(" [onConnected] " + isConnected);
        String rid = JPushInterface.getRegistrationID(context);
        Timber.i(" [rid] " + rid);
        // Message msg = Message.obtain();
        // msg.obj = rid;
        // VectorApplication.getPushHandler().sendMessage(msg);
    }

    @Override
    public void onCommandResult(Context context, CmdMessage cmdMessage) {
        Timber.i(" [onCommandResult] " + cmdMessage);
    }

    @Override
    public void onTagOperatorResult(Context context, JPushMessage jPushMessage) {
        // TagAliasOperatorHelper.getInstance().onTagOperatorResult(context,jPushMessage);
        super.onTagOperatorResult(context, jPushMessage);
    }

    @Override
    public void onCheckTagOperatorResult(Context context, JPushMessage jPushMessage) {
        // TagAliasOperatorHelper.getInstance().onCheckTagOperatorResult(context,jPushMessage);
        super.onCheckTagOperatorResult(context, jPushMessage);
    }

    @Override
    public void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
        // TagAliasOperatorHelper.getInstance().onAliasOperatorResult(context,jPushMessage);
        super.onAliasOperatorResult(context, jPushMessage);
    }

    @Override
    public void onMobileNumberOperatorResult(Context context, JPushMessage jPushMessage) {
        // TagAliasOperatorHelper.getInstance().onMobileNumberOperatorResult(context,jPushMessage);
        super.onMobileNumberOperatorResult(context, jPushMessage);
    }

    @Override
    public void onNotificationSettingsCheck(Context context, boolean isOn, int source) {
        super.onNotificationSettingsCheck(context, isOn, source);
        Timber.i(" [onNotificationSettingsCheck] isOn:" + isOn + ",source:" + source);
    }

}
