package org.fraunhofer.cese.funf_sensor.Probe;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import edu.mit.media.funf.probe.Probe;

/**
 *
 */
public class SMSProbe extends Probe.Base implements Probe.PassiveProbe {

    private static final String TAG = "SMSProbe";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            long timestamp = 0;
            for (SmsMessage message : messages) {
                if (timestamp < message.getTimestampMillis()) {
                    timestamp = message.getTimestampMillis();
                }
            }

            intent = new Intent();
            intent.putExtra("SMSProbe Timestamp: ",timestamp);


            if (Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION.equals(intent.getAction())) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    Log.d(TAG, "SMS emergency received.");
                    intent.putExtra("SMS Action", "SMS emergency received");
                }

            } else if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    Log.d(TAG, "SMS received.");
                    intent.putExtra("SMS Action", "SMS received");
                }
            } else if (Telephony.Mms.Intents.CONTENT_CHANGED_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "MMS content changed.");
                intent.putExtra("MMS Action", "MMS content changed.");
            }

            sendData(intent);
        }
    };

    ContentResolver contentResolver;

    private ContentObserver smsOutgoingObserver = new ContentObserver(new Handler()) {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onChange(boolean selfChange) {
            Intent i = new Intent();
            i.putExtra("Action", "SMS sent");
            Log.d(TAG, "SMS was sent with this device.");
            sendData(i);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    };

    private ContentObserver mmsOutgoingObserver = new ContentObserver(new Handler()) {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onChange(boolean selfChange) {
            Intent i = new Intent();
            i.putExtra("Action", "MMS sent");
            Log.d(TAG, "MMS was sent with this device.");
            sendData(i);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    };


    public void sendData(Intent intent) {
        sendData(getGson().toJsonTree(intent).getAsJsonObject());
    }

    @Override
    protected void onEnable() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED");
        filter.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
        getContext().registerReceiver(receiver, filter);

        contentResolver = getContext().getContentResolver();
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, smsOutgoingObserver);
        contentResolver.registerContentObserver(Uri.parse("content://mms"), true, mmsOutgoingObserver);
    }

    @Override
    protected void onDisable() {
        super.onStop();
        getContext().unregisterReceiver(receiver);

        contentResolver.unregisterContentObserver(smsOutgoingObserver);
        contentResolver.unregisterContentObserver(mmsOutgoingObserver);
    }


}
