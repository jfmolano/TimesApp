package com.uniandes.jfm.timesapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
    //- - - - - - - - - - - - - - Constantes - - - - - - - - - - - - - - - -
    //Tiempo alarma
    public final static int MINUTOSALARMA = 1;
    //Fecha auto apagado
    public final static int DIA = 8;
    public final static int MES = Calendar.SEPTEMBER;
    public final static int HORA = 12;
    public final static int MINUTO = 00;
    //Parametros medicion de ruido
    public final static int ITERACIONES_RUIDO = 1;
    public final static int T_INTERVALO_RUIDO = 1000;
    //Medicion de APs
    public final static int NUM_AP = 3;
    //URL del servidor
    public final static String URLSERV = "http://timesapptesis.mooo.com/api/marcas";

    //Botones de aplicacion
    private Button btnStart;
    private Button btnStop;
    private Button btnActualizarCod;
    private Button btnPrueba;
    private EditText codigoEntrada;
    private TextView vistaCodigo;

    //Preferencias
    private SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Preferencias
        sharedPref = getApplicationContext().getSharedPreferences(
                "com.jfm.appMT.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);

        //Interfaz
        setContentView(R.layout.activity_main);

        //Codigo
        codigoEntrada = (EditText) findViewById(R.id.codigoEdText);

        //Codigo vista
        vistaCodigo = (TextView) findViewById(R.id.textoCodigo);

        //Listener
        addListenerOnButton();
    }

    //Listener button
    public void addListenerOnButton() {
        btnStop = (Button) findViewById(R.id.btnDisplay);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnActualizarCod = (Button) findViewById(R.id.btnActualizarCod);
        btnPrueba = (Button) findViewById(R.id.btnPrueba);

        //Boton Prueba
        btnPrueba.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        metodoPrueba();
                    }
                });
            }

        });

        //Boton stop
        btnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(getBaseContext(), Medidor.class);
                        PendingIntent sender = PendingIntent.getService(getBaseContext(), 0,
                                intent, 0);
                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        am.cancel(sender);

                        //UNBOOT
                        ComponentName receiver = new ComponentName(getBaseContext(), SampleBootReceiver.class);
                        PackageManager pm = getBaseContext().getPackageManager();

                        pm.setComponentEnabledSetting(receiver,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);

                        Toast.makeText(getBaseContext(), "Medicion detenida", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });

        //Boton start
        btnStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(getBaseContext(), Medidor.class);
                        PendingIntent sender = PendingIntent.getService(getBaseContext(), 0,
                                intent, 0);

                        // We want the alarm to go off 30 seconds from now.
                        long firstTime = SystemClock.elapsedRealtime();
                        firstTime += 1 * 1000;

                        // Schedule the alarm!
                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime,
                                MINUTOSALARMA * 60 * 1000, sender);

                        //Alarma auto apagado
                        intent = new Intent(getBaseContext(), AlarmReceiver.class);
                        sender = PendingIntent.getBroadcast(getBaseContext(), 0,
                                intent, 0);

                        // We want the alarm to go off 30 seconds from now.
                        Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());

                        cal.set(Calendar.DATE,DIA);  //1-31
                        cal.set(Calendar.MONTH,MES);  //first month is 0!!! January is zero!!!
                        cal.set(Calendar.YEAR,2015);//year...

                        cal.set(Calendar.HOUR_OF_DAY, HORA);  //HOUR
                        cal.set(Calendar.MINUTE, MINUTO);       //MIN
                        cal.set(Calendar.SECOND, 0);       //SEC

                        // Schedule the alarm!
                        am = (AlarmManager) getSystemService(ALARM_SERVICE);
                        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

                        //BOOT
                        ComponentName receiver = new ComponentName(getBaseContext(), SampleBootReceiver.class);
                        PackageManager pm = getBaseContext().getPackageManager();

                        pm.setComponentEnabledSetting(receiver,
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);

                        Toast.makeText(getBaseContext(), "Medicion establecida", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });

        //Boton actualizar
        btnActualizarCod.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        //
                        String codigo = codigoEntrada.getText().toString();
                        //
                        SharedPreferences.Editor editor = sharedPref.edit();
                        boolean hayCodInicial = !sharedPref.getString("CodigoUni", "No Codigo").equals("No Codigo");
                        if(!hayCodInicial) {
                            editor.putString("CodigoUni", codigo);
                            editor.commit();
                        }
                        //
                        //
                        String codStored = sharedPref.getString("CodigoUni", "No Codigo");
                        //
                        vistaCodigo.setText("Codigo: " + codStored);
                        Toast.makeText(getBaseContext(), "Codigo actualizado", Toast.LENGTH_LONG).show();
                    }
                });
            }

        });
    }

    private void metodoPrueba() {
        Toast.makeText(getBaseContext(), "Boton prueba", Toast.LENGTH_LONG).show();
        String resp = "";
        List<ApplicationInfo> appsInfo = getApplicationContext().getPackageManager()
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        Iterator<ApplicationInfo> it = appsInfo.iterator();
        while(it.hasNext())
        {
            ApplicationInfo apInfo = it.next();
            String procName = apInfo.processName;
            int uid = apInfo.uid;
            System.out.print("Nombre de proceso: ");
            System.out.println(procName);
            System.out.print("uid: ");
            System.out.println(uid);
            System.out.print("Consumo de red bytes (TX): ");
            long txBytes = TrafficStats.getUidTxBytes(uid)/(SystemClock.elapsedRealtime()/(1000*60*60));
            System.out.println(txBytes);
            System.out.print("Consumo de red bytes (RX): ");
            long rxBytes = TrafficStats.getUidRxBytes(uid) / (SystemClock.elapsedRealtime() / (1000 * 60 * 60));
            System.out.println(rxBytes);
            resp = resp + procName + ";" + txBytes + ";" + rxBytes + ";;";
        }
        long mStartRX = TrafficStats.getTotalRxBytes();
        Toast.makeText(getBaseContext(), "mStartRX "+mStartRX, Toast.LENGTH_LONG).show();
    }
}