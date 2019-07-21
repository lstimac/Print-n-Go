package com.example.arbi.printngo;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

/**
 * Created by Sandi on 9.3.2016..
 */
public class SendTask extends AsyncTask<String, String, String> {

    String urlAddress;
    String uvez;
    String kopirnica;
    String user;
    long brojKopija;
    String file;
    String fileName;
    String uBoji;
    String obostrano;
    String dioPrintanja;
    String vrstaUveza;
    String brojStranica;

    private HttpURLConnection conn;
    public static final int CONNECTION_TIMEOUT = 15 * 1000;
    private NetworkOperationFinished myNetworkOpeartionListener;
    String finalResponse="";

    // Constructor
    public SendTask(String url, String uvez, String kopirnica,
                    String user, long brojKopija, String file, String fileName, String uBoji, String obostrano, String dioPrintanja, String vrstaUveza, String brojstranica) {

        if(dioPrintanja!="isprintaj sve"){
            dioPrintanja=dioPrintanja.split("-")[0].split(":")[1]+"-"+dioPrintanja.split("-")[1].split(": ")[1];
        }
        this.urlAddress = url;
        this.uvez = uvez;
        this.kopirnica = kopirnica;
        this.user = user;
        this.brojKopija = brojKopija;
        this.file = file;
        this.fileName = fileName;
        this.uBoji = uBoji;
        this.obostrano = obostrano;
        this.dioPrintanja = dioPrintanja;
        System.out.print(this.dioPrintanja+"\n");
        this.vrstaUveza = vrstaUveza;
        this.brojStranica = brojstranica;
    }


    public interface NetworkOperationFinished {
        void onNetworkOperationFinished(String response);
    }

    public void setNetworkOperationFinished(NetworkOperationFinished inputListener){
        this.myNetworkOpeartionListener = inputListener;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // do stuff before posting data
    }


    @Override
    protected String doInBackground(String... strings) {
        try {
            postData_okhttp();
            //postData();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(String lenghtOfFile) {
        // do stuff after posting data
        System.out.println("Sent from Android...");

        // tell parent activity that network operation finished!
        if (myNetworkOpeartionListener != null)
            myNetworkOpeartionListener.onNetworkOperationFinished(finalResponse);
    }

    // Method that sends data (in background)
    private void postData_okhttp() {
        finalResponse="";

        try {
            final MediaType MEDIA_TYPE_PDF = MediaType.parse("text/plain");
            final OkHttpClient client = new OkHttpClient();


            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("applicationid", "PrintnGo")
                    .addFormDataPart("uvez", uvez)
                    .addFormDataPart("kopirnica", kopirnica)
                    .addFormDataPart("user", user)
                    .addFormDataPart("uBoji", uBoji)
                    .addFormDataPart("obostrano", obostrano)
                    .addFormDataPart("dioPrintanja", dioPrintanja)
                    .addFormDataPart("vrstaUveza", vrstaUveza)
                    .addFormDataPart("brojKopija", Long.toString(brojKopija))
                    .addFormDataPart("file", fileName,
                            RequestBody.create(MEDIA_TYPE_PDF,
                                    new File(file)))
                    .build();

            Request request = new Request.Builder()
                    .url(urlAddress)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                finalResponse="";
                return;
            }

            String feedback = response.body().string();

            System.out.println("Server Response: " + feedback.toString());
            Thread.sleep(1000);

            if (feedback.toString().equals("200"))
            {
                finalResponse=feedback.toString();
                System.out.print(kopirnica+"\n");


            }


        } catch (Exception ex) {
            finalResponse="";
            ex.printStackTrace();
        }
    }

}


