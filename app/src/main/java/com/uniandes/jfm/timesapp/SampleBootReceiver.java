package com.uniandes.jfm.timesapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class SampleBootReceiver extends BroadcastReceiver {
    //Constantes
    private final static int MINUTOSALARMA = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent intentP = new Intent(context, Medidor.class);
            PendingIntent sender = PendingIntent.getService(context, 0,
                    intentP, 0);

            // We want the alarm to go off 30 seconds from now.
            long firstTime = SystemClock.elapsedRealtime();
            firstTime += 10 * 1000;

            // Schedule the alarm!
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                    MINUTOSALARMA * 60 * 1000, sender);

            //Auto apagado
            //Alarma auto apagado
            intentP = new Intent(context, AlarmReceiver.class);
            sender = PendingIntent.getBroadcast(context, 0,
                    intentP, 0);

            // We want the alarm to go off 30 seconds from now.
            Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());

            cal.set(Calendar.DATE,MainActivity.DIA);  //1-31
            cal.set(Calendar.MONTH,MainActivity.MES);  //first month is 0!!! January is zero!!!
            cal.set(Calendar.YEAR,2015);//year...

            cal.set(Calendar.HOUR_OF_DAY, MainActivity.HORA);  //HOUR
            cal.set(Calendar.MINUTE, MainActivity.MINUTO);       //MIN
            cal.set(Calendar.SECOND, 0);       //SEC

            // Schedule the alarm!
            am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
        }
    }
}

