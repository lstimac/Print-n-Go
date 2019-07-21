package com.example.arbi.printngo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Timer;
import java.util.TimerTask;

public class RegisterActivity extends AppCompatActivity {

    public static final int CONNECTION_TIMEOUT=10000;
    public static final int READ_TIMEOUT=15000;

    private Timer timer = new Timer();
    private final long DELAY = 850; // in ms

    public static final String TAG="RegisterActivity";

    private String stanje_reg="";
    private String stanje_greske="OK";
    private Boolean email_isValid=true;

    EditText email_provjera;
    EditText pass;
    EditText pass_confirm;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        pass_confirm = (EditText)findViewById(R.id.registracija_pass_confirm);

        //Event handler for validating password input
        pass_confirm.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                final Handler handler = new Handler();
                Timer timer_pass = new Timer();
                TimerTask validate_pass_task = new TimerTask() {
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {
                                validateInputPass(pass_confirm);
                            }

                        });


                    }
                };
                timer_pass.schedule(validate_pass_task, DELAY);

            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(timer != null)
                    timer.cancel();

            }
        });


        //Event handler for checking email availability
        email_provjera = (EditText) findViewById(R.id.registracija_user);
        email_provjera.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                final Handler handler = new Handler();
                Timer timer_provjera_email = new Timer();
                TimerTask provjera_email_task = new TimerTask() {
                    public void run() {
                        handler.post(new Runnable() {
                            public void run() {

                                if(isValidEmail(email_provjera.getText().toString())) {
                                    provjeraDostupnosti();
                                    if(!email_isValid) {
                                        email_isValid=true;
                                        TextView error_user = (TextView)findViewById(R.id.error_user);
                                        error_user.setVisibility(View.GONE);
                                    }
                                }else{
                                    if(email_isValid) {
                                        email_isValid=false;
                                        TextView error_user = (TextView)findViewById(R.id.error_user);
                                        error_user.setTextColor(Color.RED);
                                        error_user.setText("Email nije ispravan.");
                                        error_user.setVisibility(View.VISIBLE);

                                    }
                                }
                            }

                        });


                    }
                };
                timer_provjera_email.schedule(provjera_email_task, 1500);

            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(timer != null)
                    timer.cancel();

            }
        });



        /*  pass_confirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (!hasFocus) {
                    validateInput(v);
                }
            }
        }); */



    }

    private void validateInputPass(View v){

        pass = (EditText)findViewById(R.id.registracija_pass);
        TextView error_pass = (TextView)findViewById(R.id.error_lozinka);
        error_pass.setTextColor(Color.RED);

        if (pass_confirm.getText().toString().equals(pass.getText().toString())){
            error_pass.setVisibility(View.GONE);
            error_pass.setText("");
            stanje_greske="OK";

        }else {
            error_pass.setVisibility(View.VISIBLE);
            error_pass.setText("Lozinke se ne poklapaju.");
            stanje_greske="lozinke se ne poklapaju.";
        }

    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private void provjeraDostupnosti(){
        TextView email = (TextView)findViewById(R.id.registracija_user);
        final String emailSend = email.getText().toString();
        stanje_reg="provjera";
        new AsyncRegister().execute(emailSend);


    }


    public void registracija(View arg0) {

        if(!email_isValid) return;

        TextView email = (TextView)findViewById(R.id.registracija_user);
        TextView password = (TextView)findViewById(R.id.registracija_pass);
        TextView ime = (TextView)findViewById(R.id.registracija_ime);
        TextView prezime = (TextView)findViewById(R.id.registracija_prezime);
        TextView tel = (TextView)findViewById(R.id.registracija_tel);

        final String emailSend = email.getText().toString();
        final String passwordSend = password.getText().toString();
        final String imeSend = ime.getText().toString();
        final String prezimeSend = prezime.getText().toString();
        final String telSend = tel.getText().toString();

        if(emailSend.equals("") || passwordSend.equals("") || imeSend.equals("") || prezimeSend.equals("") || telSend.equals("")){
            stanje_greske="postoje neispunjena polja.";
        }

        if (stanje_greske.equals("OK")) {
            // Initialize  AsyncLogin() class
            stanje_reg = "spremi";
            new AsyncRegister().execute(emailSend, passwordSend, imeSend, prezimeSend, telSend);
        }else if(stanje_greske.equals("postoje neispunjena polja.")){

            TextView error_registracija = (TextView)findViewById(R.id.error_registracija);
            error_registracija.setTextColor(Color.RED);
            error_registracija.setText("Registracija neuspješna, postoje neispunjena polja.");
            error_registracija.setVisibility(View.VISIBLE);
        }

    }

    //Async task for checking email availability or inserting data into Korisnik depending on the flag "stanje_reg"
    private class AsyncRegister extends AsyncTask<String, String, String> {
        ProgressDialog pdLoading = new ProgressDialog(RegisterActivity.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tUčitavanje...");
            pdLoading.setCancelable(false);
            if(stanje_reg.equals("spremi")) {
                pdLoading.show();
            }

        }
        @Override
        protected String doInBackground(String... params) {
            try {

                // Enter URL address where your php file resides
                if (stanje_reg.equals("spremi")) {
                    url = new URL("http://207.154.235.97/login/register.inc.php");
                }else if(stanje_reg.equals("provjera")){
                    url = new URL("http://207.154.235.97/login/provjera_dostupnosti.php");

                }

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                // Log.e(TAG,e.toString());
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                Uri.Builder builder = null;
                if (stanje_reg.equals("spremi")){
                    builder = new Uri.Builder()
                            .appendQueryParameter("email", params[0])
                            .appendQueryParameter("password", params[1])
                            .appendQueryParameter("ime", params[2])
                            .appendQueryParameter("prezime", params[3])
                            .appendQueryParameter("tel", params[4]);

                }else if(stanje_reg.equals("provjera")){
                    builder = new Uri.Builder()
                            .appendQueryParameter("email", params[0]);

                }

                String query = builder.build().getEncodedQuery();

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
                return "exception";
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
                    Log.i(TAG, result.toString());
                    return(result.toString());


                }else{

                    return("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                // Log.e(TAG,e.toString());
                return "exception";
            } finally {
                conn.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread

            pdLoading.dismiss();

            String response = result;

            Log.i(TAG, response);

            //Show error for email already taken
            if(response.equalsIgnoreCase("true"))
            {
                if(stanje_reg.equals("provjera")){
                    TextView error_user = (TextView)findViewById(R.id.error_user);
                    error_user.setTextColor(Color.RED);
                    error_user.setText("Email se već koristi.");
                    error_user.setVisibility(View.VISIBLE);
                    stanje_greske="email je već zauzet.";


                    //Inform user registration has been successful
                }else {

                    TextView error_registracija = (TextView)findViewById(R.id.error_registracija);
                    error_registracija.setVisibility(View.GONE);
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    Toast toast = Toast.makeText(getApplicationContext(),"Registracija uspješna!",Toast.LENGTH_SHORT);
                    toast.show();
                    RegisterActivity.this.finish();
                }

            }else if (result.equalsIgnoreCase("false")){

                if(stanje_reg.equals("provjera")){
                    TextView error_user = (TextView)findViewById(R.id.error_user);
                    error_user.setVisibility(View.GONE);
                    stanje_greske="OK";

                }

            } else if (result.equalsIgnoreCase("exception") || result.equalsIgnoreCase("unsuccessful")) {

                TextView error_registracija = (TextView)findViewById(R.id.error_registracija);
                error_registracija.setTextColor(Color.RED);
                error_registracija.setText("Greška u povezivanju sa serverom, pokušajte ponovno.");
                error_registracija.setVisibility(View.VISIBLE);
                //Log.i(TAG, "Greška u povezivanju sa serverom, pokušajte ponovno.");


            }
        }

    }

}
