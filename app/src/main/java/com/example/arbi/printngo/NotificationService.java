package com.example.arbi.printngo;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by Luka on 11.6.2017..
 */

public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    public String response = "";
    public static final int CONNECTION_TIMEOUT=10000;
    public static final int READ_TIMEOUT=15000;
    HttpURLConnection conn;
    URL url = null;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        //checkForPrintStatus();



        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    //Log.i(TAG,"Ziv sam");
                    checkForPrintStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }


    @Override
    public void onDestroy() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    public void checkForPrintStatus(){

        while (response.split(",")[0]!="Gotovo") {


            SharedPreferences pref_print = this.getSharedPreferences("Login", 0);
            String user_id = pref_print.getString("id", null);


            try {

                // URL address where php file is
                url = new URL("http://207.154.235.97/files/checkPrintStatus.php");

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                // Log.e(TAG,e.toString());
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("user_id", user_id);
                String query = builder.build().getEncodedQuery();
                Log.i(TAG, user_id);
                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                //Log.e(TAG,e1.toString());
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    Log.i(TAG, "HTTP OK");
                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method

                    response = result.toString();
                    Log.i(TAG,response.split(",")[0]);

                } else {

                    System.out.print("Neuspjeh!");
                }

            } catch (IOException e) {
                e.printStackTrace();
                // Log.e(TAG,e.toString());
            } finally {
                conn.disconnect();
            }


            if (response.split(",")[0].equals("Gotovo")) {

                break;
            }

            SystemClock.sleep(7000);

        }

        Intent intentNotifikacija = new Intent();
        Notification noti = new Notification.Builder(this)
                .setTicker("Ticker title")
                .setContentTitle("Ispis je završen!")
                .setContentText("Ispis vaše datoteke "+response.split(",")[1].replace("files/","")+" je završen")
                .setSmallIcon(R.drawable.notifikacija_check)
                .setContentIntent(PendingIntent.getActivity(this, 0, intentNotifikacija, 0)).getNotification();

        noti.flags |= Notification.FLAG_AUTO_CANCEL;
        noti.flags |= Notification.COLOR_DEFAULT;
        noti.flags |= Notification.DEFAULT_SOUND;
        noti.flags |= Notification.DEFAULT_VIBRATE;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        int inc = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
        nm.notify(inc, noti);

        this.stopSelf();

    }
}





