package com.uniandes.jfm.timesapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import android.content.Context;
import android.location.LocationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Medidor extends Service {
    //Constantes

    //WIFI
    private WifiInfo wifiInfo;
    private DhcpInfo dhcpInfo;

    //WIFI2
    private List<ScanResult> scanResults;
    private boolean listoWifiScan;
    private WifiManager mWifiManager;
    private int valMasPotente;

    //RUIDO
    private MediaRecorder mRecorder = null;

    //LUZ
    DataCollection mDataCollection = null;

    //MUSICA
    private String artista;
    private String pista;
    private String apps;
    private String enReproduccion;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println(" - - - - - - - - - - - start del servicio - - - - - - - - - - -");
        //Toast.makeText(getBaseContext(), "Network update", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            public void run() {
                medicion();
                //Metodo de prueba
                //medicionTest();
            }
        }).start();
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    //Inicializacion servicio
    @Override
    public void onCreate() {
        System.out.println(" - - - - - - - - - - - Inicia el servicio - - - - - - - - - - -");

        //WiFi
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        //Luz
        mDataCollection = new DataCollection(this);

        //Musica
        artista = "";
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        iF.addAction("com.android.music.metachanged");
        //Nuevas lineas
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("com.sec.android.app.music.metachanged");
        iF.addAction("com.nullsoft.winamp.metachanged");
        iF.addAction("com.amazon.mp3.metachanged");
        iF.addAction("com.miui.player.metachanged");
        iF.addAction("com.real.IMP.metachanged");
        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.rdio.android.metachanged");
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.andrew.apollo.metachanged");
        registerReceiver(mReceiver, iF);

        //Redes 2
        iF.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(new BroadcastReceiver() {

            @Override

            public void onReceive(Context context, Intent intent) {
                mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                mWifiManager.getScanResults();
                scanResults = mWifiManager.getScanResults();
                listoWifiScan = true;
            }
        }
                , iF);
        listoWifiScan = false;
        valMasPotente = 0;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    //Dar Wifi mas poderoso
    public ScanResult darMasPoderoso(){
        ScanResult resp = null;
        mWifiManager.startScan();
        while(!listoWifiScan){}
        listoWifiScan = false;
        Iterator<ScanResult> it = scanResults.iterator();
        int maximo = Integer.MIN_VALUE;
        while(it.hasNext())
        {
            ScanResult sR = it.next();
            if(sR.SSID.equals("SENECA"))
            {
                int potencia = sR.level;
                if(potencia > maximo)
                {
                    maximo = potencia;
                    resp = sR;
                }
            }/*
            System.out.println("SSID: "+sR.SSID);
            System.out.println("BSSID: "+sR.BSSID);
            System.out.println("Nivel: "+sR.level);*/
        }
        if(resp!=null) {
            valMasPotente = resp.level;
        }
        else
        {
            valMasPotente = 0;
        }
        return resp;
    }

    //Dar Wifi mas poderoso
    public AccessPoint[] darMasPoderosos(){
        AccessPoint[] apList = new AccessPoint[MainActivity.NUM_AP];
        for(int i = 0; i < apList.length ; i++)
        {
            apList[i] = new AccessPoint("NoSeneca",-1);
        }
        ScanResult resp = null;
        mWifiManager.startScan();
        while(!listoWifiScan){}
        listoWifiScan = false;
        Iterator<ScanResult> it = scanResults.iterator();
        ArrayList<AccessPoint> lista = new ArrayList<AccessPoint>();
        while(it.hasNext())
        {
            ScanResult sr = it.next();
            if(sr.SSID.equals("SENECA"))
            {
                AccessPoint ap = new AccessPoint(sr.BSSID,sr.level);
                lista.add(ap);
            }
        }
        Collections.sort(lista);
        for(int i = 0; i < lista.size() && i < apList.length ; i++)
        {
            apList[i] = lista.get(lista.size()-i-1);
        }
        return apList;
    }

    public void medicionTest()
    {

    }

    public void medicion()
    {
        //Prende Wifi
        boolean wifiEnabled = mWifiManager.isWifiEnabled();
        if(!wifiEnabled)
        {
            mWifiManager.setWifiEnabled(true);
        }
        //SALON - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        String salon = "Salon";

        //HORA - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss z");
        String hora = sdf.format(cal.getTime());

        //RUIDO - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        double ruido = darRuido();
        double roundOff = Math.round(ruido * 100.0) / 100.0;
        String ruidoStr = ""+roundOff;

        //APPS ABIERTAS - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        ActivityManager actvityManager = (ActivityManager)
                this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningTaskInfo> packageNameList = actvityManager.getRunningTasks(1);
        Iterator<ActivityManager.RunningTaskInfo> it = packageNameList.iterator();
        apps = "";
        while(it.hasNext())
        {
            ActivityManager.RunningTaskInfo task = it.next();
            apps = apps+";"+task.topActivity.getPackageName();
        }

        //MUSICA - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        enReproduccion = "1";
        if(!manager.isMusicActive())
        {
            enReproduccion = "0";
        }
        if(enReproduccion == null)
        {
            enReproduccion = "NOpista";
        }
        if(pista == null)
        {
            pista = "noPista";
        }

        //LUZ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        float luz = mDataCollection.darLuzActual();
        String luzStr = luz + "";

        //REDES - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        //Manejadores
        wifiInfo = mWifiManager.getConnectionInfo();
        dhcpInfo = mWifiManager.getDhcpInfo();
        //WIFI INFO
        int ip = wifiInfo.getIpAddress();
        String mac = wifiInfo.getMacAddress();
        int netId = wifiInfo.getNetworkId();
        String macAP = wifiInfo.getBSSID();
        String ipAddress = Formatter.formatIpAddress(ip);
        String netIdString = Formatter.formatIpAddress(netId);
        //DHCP INFO
        int dns1 = dhcpInfo.dns1;
        int dns2 = dhcpInfo.dns2;
        int contents = dhcpInfo.describeContents();
        int gateway = dhcpInfo.gateway;
        int ipdhcp = dhcpInfo.ipAddress;
        int leaseduration = dhcpInfo.leaseDuration;
        int netmask = dhcpInfo.netmask;
        int servaddress = dhcpInfo.serverAddress;
        //BSSID mas poderoso
        //String scanRes = darBSSIDMasPoderoso();
        //Dar BSSID mas poderosos
        AccessPoint[] masPoderosos = darMasPoderosos();

        //USO DE APPS - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        //CODIGO - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("com.jfm.appMT.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
        String codigo = sharedPref.getString("CodigoUni","No codigo");

        //Apagar Wifi
        if(!wifiEnabled)
        {
            mWifiManager.setWifiEnabled(false);
        }

        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - POST - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        makeHTTPPOSTRequest(codigo,salon,ipAddress,intToIp(gateway),intToIp(netmask),masPoderosos,hora,ruidoStr,luzStr);
    }

    //Funcion para pasar numeros raros a direcciones IP STRINGs
    public String intToIp(int IpAddress) {
        return Formatter.formatIpAddress(IpAddress);
    }

    String darBSSIDMasPoderoso()
    {
        ScanResult scRes = darMasPoderoso();
        String scanRes = "No SENECA";
        if(scRes!=null)
        {
            scanRes = scRes.BSSID;
        }
        return scanRes;
    }

    public void makeHTTPPOSTRequest(String codigo,String salon, String ip,String ipAP, String netmask, AccessPoint[] macAP, String hora, String ruidoString, String luzString) {
        try {
            String urlPost = MainActivity.URLSERV;
            HttpClient c = new DefaultHttpClient();
            HttpPost p = new HttpPost(urlPost);
            p.addHeader("content-type", "application/json");
            //JSON de PRUEBA
            /*String jsonPost1 = "{\"codigo\":\"201116404\"," +
                    "\"tiempo\":\"11:04:00 15/07/2015\"," +
                    "\"lugar\":\"ML009\"," +
                    "\"ip\":\"157.253.0.3\"," +
                    "\"ipaccesspoint\":\"157.253.0.1\"," +
                    "\"ruido\":\"1\"," +
                    "\"luz\":\"2\"," +
                    "\"musica\":\"JBalvin\"," +
                    "\"temperatura\":\"20\"," +
                    "\"humedad\":\"30\"," +
                    "\"grupo\":\"201113844\"," +
                    "\"infoAdd\":\"-\"}";*/
            String jsonPost = "{\"codigo\":\""+codigo+"\"," +
                    "\"tiempo\":\""+hora+"\"," +
                    "\"mac1\":\""+macAP[0].macadd+"\"," +
                    "\"mac1Pot\":\""+macAP[0].potencia+"\"," +
                    "\"mac2\":\""+macAP[1].macadd+"\"," +
                    "\"mac2Pot\":\""+macAP[1].potencia+"\"," +
                    "\"mac3\":\""+macAP[2].macadd+"\"," +
                    "\"mac3Pot\":\""+macAP[2].potencia+"\"," +
                    "\"ip\":\""+ip+"\"," +
                    "\"ipaccesspoint\":\""+ipAP+"\"," +
                    "\"ruido\":\""+ruidoString+"\"," +
                    "\"luz\":\""+luzString+"\"," +
                    "\"pista\":\""+pista+"\"," +
                    "\"artista\":\""+artista+"\"," +
                    "\"enRep\":\""+enReproduccion+"\"," +
                    "\"appRun\":\""+apps+"\"," +
                    "\"infoAdd\":\""+darUsoAPPs()+"\"}";
            //IMPRIMIR PETICIONES REST
            //System.out.println("- - - - - - - JSON POST1 - - - - - - - -");
            //System.out.println(jsonPost1);
            //System.out.println("- - - - - - - JSON POST2 - - - - - - - -");
            //System.out.println(jsonPost);
            p.setEntity(new StringEntity(jsonPost));
            HttpResponse r = c.execute(p);

            BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }

        }
        catch(ParseException e) {
            System.out.println(e);
            System.out.println(" - - - - - - - - - - - Error de conexion - - - - - - - - - - -");
        }
        catch(IOException e) {
            System.out.println(e);
            System.out.println(" - - - - - - - - - - - Error de conexion - - - - - - - - - - -");
        }
    }

    //FUNCIONES RUIDOMETRO

    public void start() {
        try{
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.prepare();
                mRecorder.start();
            }
        }catch(Exception e){}

    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 0;

    }

    //Metodo para dar ruido promedio
    public double darRuido()
    {
        try {
            start();
            double ruido = 0;
            double ampMax;
            ampMax = getAmplitude();
            for (int i = 0; i < MainActivity.ITERACIONES_RUIDO; i++) {
                Thread.sleep(MainActivity.T_INTERVALO_RUIDO);
                ampMax = getAmplitude();
                ruido = (ruido*i+ampMax)/(i+1);
            }
            stop();
            return ruido;
        } catch(Exception e)
        {
            stop();
            return -1;
        }
    }

    //onReceive musica
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            artista = intent.getStringExtra("artist");
            pista = intent.getStringExtra("track");
        }
    };

    private boolean appDistractiva(String param)
    {
        boolean resp = false;
        if(param.contains("facebook"))
        {
            resp = true;
        }
        else if(param.contains("whatsapp"))
        {
            resp = true;
        }
        else if(param.contains("snapchat"))
        {
            resp = true;
        }
        else if(param.contains("shazam"))
        {
            resp = true;
        }
        else if(param.contains("twitter"))
        {
            resp = true;
        }
        else if(param.contains("spotify"))
        {
            resp = true;
        }
        else if(param.contains("instagram"))
        {
            resp = true;
        }
        else if(param.contains("vine"))
        {
            resp = true;
        }
        else if(param.contains("candy"))
        {
            resp = true;
        }
        else if(param.contains("facebook"))
        {
            resp = true;
        }
        else if(param.contains("facebook"))
        {
            resp = true;
        }

        return resp;
    }

    private String darUsoAPPs()
    {
        String resp = "";
        List<ApplicationInfo> appsInfo = getApplicationContext().getPackageManager()
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        Iterator<ApplicationInfo> it = appsInfo.iterator();
        while(it.hasNext())
        {
            ApplicationInfo apInfo = it.next();
            String procName = apInfo.processName;
            if(appDistractiva(procName)) {
                int uid = apInfo.uid;
                System.out.print("Nombre de proceso: ");
                System.out.println(procName);
                System.out.print("uid: ");
                System.out.println(uid);
                System.out.print("Consumo de red bytes (TX): ");
                long txBytes = TrafficStats.getUidTxBytes(uid) / ((SystemClock.elapsedRealtime() / (1000 * 60 * 60))+1);
                System.out.println(txBytes);
                System.out.print("Consumo de red bytes (RX): ");
                long rxBytes = TrafficStats.getUidRxBytes(uid) / ((SystemClock.elapsedRealtime() / (1000 * 60 * 60))+1);
                System.out.println(rxBytes);
                resp = resp + procName + "$$" + txBytes + "$$" + rxBytes + "&&";
            }
        }
        return resp;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

//CODIGO PARA IMPRIMIR INFO WIFI
/*
        System.out.println("- - - - - IP - - - - -");
        System.out.println(ipAddress);
        System.out.println("- - - - - MAC - - - - -");
        System.out.println(mac);
        System.out.println("- - - - - ID de Net - - - - -");
        System.out.println(netIdString);
        System.out.println("- - - - - dns1 - - - - -");
        System.out.println( intToIp(dns1));
        System.out.println("- - - - - dns2 - - - - -");
        System.out.println( intToIp(dns2));
        System.out.println("- - - - - gateway - - - - -");
        System.out.println( intToIp(gateway));
        System.out.println("- - - - - ipdhcp - - - - -");
        System.out.println( intToIp(ipdhcp));
        System.out.println("- - - - - leaseduration - - - - -");
        System.out.println(leaseduration);
        System.out.println("- - - - - netmask - - - - -");
        System.out.println( intToIp(netmask));
        System.out.println("- - - - - servaddress - - - - -");
        System.out.println(intToIp(servaddress));*/
