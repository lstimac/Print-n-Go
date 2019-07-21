package com.example.arbi.printngo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.shockwave.pdfium.PdfiumCore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import in.gauriinfotech.commons.Commons;


import static android.view.View.INVISIBLE;


public class PrintActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {


    private GoogleApiClient mGoogleApiClient;
    final int ACTIVITY_CHOOSE_FILE = 1;
    private static final String TAG = "PrintActivity";
    public final static String FOLDER = Environment.getExternalStorageDirectory() + "/PDF";

    private String[] labels_postavke = {"label_postavke","label_interval_ispisa","label_postavke_uveza"};
    private String[] layout_postavke = {"layout_postavke","layout_interval_ispisa","layout_postavke_uveza"};
    public List<String> spinner_array = new ArrayList<String>();
    public List<String> cijena_array = new ArrayList<String>();
    private int cursor_postavke=0;
    ProgressDialog pd;
    AlertDialog sendDialog;

    String fileNamePath = "";
    String brojStranica = "";
    String fileName = "";
    String vrstaUveza = "";
    long kopije = 1;
    String bothSides;
    String inColor;
    String whatToPrint = "";
    String odabranaKopirnica = "";
    Bitmap fullscreenpicture;
    String copyShopAdress = "";
    String tmpText;

    EditText startPage;
    EditText endPage;

    String[] cijena_kopirnica;
    float cijena_uvez;
    float cijena_boja;
    float cijena_dio_printanja;

    TextView aboutPrinting;

    float ukupna_cijena;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        aboutPrinting = (TextView) findViewById(R.id.textViewShowData);
        final TextView pageCount = (TextView) findViewById(R.id.textViewBrojKopija);
        final TextView selectedPagesStart = (TextView) findViewById(R.id.textViewSelectedPagesStart);
        final TextView selectedPagesEnd = (TextView) findViewById(R.id.textViewSelectedPagesEnd);
        final TextView minus = (TextView) findViewById(R.id.textViewMinus);
        final TextView selectedShop = (TextView) findViewById(R.id.textViewSelectedShop);
        final CheckBox obostrano = (CheckBox) findViewById(R.id.checkBoxBothSides);
        final CheckBox uBoji = (CheckBox) findViewById(R.id.checkBoxInColor);
        final ImageView imageThumbnail = (ImageView) findViewById(R.id.imageViewShowPDF);
        final EditText brojKopija = (EditText) findViewById(R.id.editTextPaperCount);
        startPage = (EditText) findViewById(R.id.editTextStart);
        endPage = (EditText) findViewById(R.id.editTextEnd);
        final RadioGroup uvez = (RadioGroup) findViewById(R.id.radioGroupUvez);
        final RadioGroup ispis = (RadioGroup) findViewById(R.id.radioGroupIntervalIspisa);
        final RadioButton bezUveza = (RadioButton) findViewById(R.id.radioButtonBezUveza);
        final RadioButton mekiUvez = (RadioButton) findViewById(R.id.radioButtonMekiUvez);
        final RadioButton cijelispis = (RadioButton) findViewById(R.id.radioButtonPrintAll);
        final Spinner spinnerSelection = (Spinner) findViewById(R.id.spinnerPrintList);


        if(cursor_postavke==0){
            Button previous_button = (Button) findViewById(R.id.previous_postavke);
            previous_button.setVisibility(View.GONE);
        }

        //Set click listeners on buttons "Dalje" and "Natrag"
        findViewById(R.id.previous_postavke).setOnClickListener(this);
        findViewById(R.id.next_postavke).setOnClickListener(this);
        inColor = "crno-bijelo";

        //Execute async task for fetching list of copy shops
        new JsonTask().execute("http://207.154.235.97/login/lista_kopirnica.php");


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        //getInfo();

        imageThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImage();
            }
        });

        uBoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String trenutniText = (String) aboutPrinting.getText();
                if (uBoji.isChecked()) {
                    //aboutPrinting.setText(trenutniText + "\n\tU boji");
                    inColor = "u boji";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                }
                else {
                    trenutniText = trenutniText.replaceAll("\\n\\tU boji", "");
                   // aboutPrinting.setText(trenutniText);
                    inColor = "crno-bijelo";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                }
            }
        });

        obostrano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String trenutniText = (String) aboutPrinting.getText();
                if (obostrano.isChecked()) {
                    //aboutPrinting.setText(trenutniText + "\n\tObostrano");
                    bothSides = "obostrano";
                }
                else {
                    trenutniText = trenutniText.replaceAll("\\n\\tObostrano", "");
                    //aboutPrinting.setText(trenutniText);
                    bothSides = "jednostrano";
                }
            }
        });


        pageCount.setText("\tBroj kopija: " + kopije);
        brojKopija.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE){
                    pageCount.setText("\tBroj kopija: " + brojKopija.getText().toString());
                    InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    kopije = Long.parseLong(brojKopija.getText().toString());
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                    return true;
                }
                return false;
            }
        });

        uvez.check(bezUveza.getId());
        //aboutPrinting.setText("\n\tBez uveza");
        vrstaUveza = "bez uveza";
        uvez.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                String trenutniText = (String) aboutPrinting.getText();
                if (bezUveza.isChecked()){
                    trenutniText = trenutniText.replaceAll("\\n\\tMeki uvez", "");
                    trenutniText = trenutniText.replaceAll("\\n\\tTvrdi uvez", "");
                    //aboutPrinting.setText(trenutniText + "\n\tBez uveza");
                    vrstaUveza = "bez uveza";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                    Log.i(TAG,"Cijena: "+Float.toString(ukupna_cijena));



                }
                else if (mekiUvez.isChecked()){
                    trenutniText = trenutniText.replaceAll("\\n\\tBez uveza", "");
                    trenutniText = trenutniText.replaceAll("\\n\\tTvrdi uvez", "");
                    //aboutPrinting.setText(trenutniText + "\n\tMeki uvez");
                    vrstaUveza = "meki uvez";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                    //Log.i(TAG,"Cijena: "+Float.toString(ukupna_cijena));

                }
                else {
                    trenutniText = trenutniText.replaceAll("\\n\\tBez uveza", "");
                    trenutniText = trenutniText.replaceAll("\\n\\tMeki uvez", "");
                    //aboutPrinting.setText(trenutniText + "\n\tTvrdi uvez");
                    vrstaUveza = "tvrdi uvez";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                    Log.i(TAG,"Cijena: "+Float.toString(ukupna_cijena));
                }
            }
        });

        ispis.check(cijelispis.getId());
        selectedPagesStart.setText("\n\tIsprintaj sve");
        minus.setVisibility(INVISIBLE);
        startPage.setVisibility(INVISIBLE);
        endPage.setVisibility(INVISIBLE);
        selectedPagesEnd.setText("");
        whatToPrint = "isprintaj sve";
        ispis.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (cijelispis.isChecked()){
                    selectedPagesStart.setText("\n\tIsprintaj sve");
                    minus.setVisibility(INVISIBLE);
                    startPage.setVisibility(INVISIBLE);
                    endPage.setVisibility(INVISIBLE);
                    selectedPagesEnd.setText("");
                    whatToPrint = "isprintaj sve";
                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                }
                else {
                    minus.setVisibility(View.VISIBLE);
                    startPage.setVisibility(View.VISIBLE);
                    endPage.setVisibility(View.VISIBLE);
                    selectedPagesStart.setText("");
                    selectedPagesEnd.setText("");
                }
            }
        });

        startPage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                if (fileName != "") {
                    if (i == EditorInfo.IME_ACTION_DONE && Integer.parseInt(startPage.getText().toString()) >= 1 && Integer.parseInt(startPage.getText().toString()) <= Integer.parseInt(brojStranica) && fileName != "") {


                        String trenutniText = (String) selectedPagesEnd.getText();
                        selectedPagesStart.setText("\tPočetna: " + startPage.getText().toString());
                        InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                        whatToPrint = "Početna: " + startPage.getText().toString() + " " + minus.getText().toString() + trenutniText;
                        izracunajCijenu();
                        aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                        TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                        error_stranice.setVisibility(View.GONE);

                        return true;
                    } else {
                        TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                        error_stranice.setText("Postavljena početna stranica je neispravna.");
                        error_stranice.setTextColor(Color.RED);
                        error_stranice.setVisibility(View.VISIBLE);
                        return false;
                    }
                } else {
                    TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                    error_stranice.setText("Niste odabrali datoteku.");
                    error_stranice.setTextColor(Color.RED);
                    error_stranice.setVisibility(View.VISIBLE);

                }
                return false;
            }

        });

        endPage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (fileName != "") {
                    if (i == EditorInfo.IME_ACTION_DONE && Integer.parseInt(endPage.getText().toString()) <= Integer.parseInt(brojStranica) && Integer.parseInt(endPage.getText().toString()) >= Integer.parseInt(startPage.getText().toString())) {
                        String trenutniText = (String) selectedPagesStart.getText();
                        selectedPagesEnd.setText("Završna: " + endPage.getText().toString());
                        InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                        whatToPrint = trenutniText + minus.getText().toString() + " " + "Završna: " + endPage.getText().toString();
                        izracunajCijenu();
                        aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");

                        TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                        error_stranice.setVisibility(View.GONE);

                        return true;
                    } else {
                        TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                        error_stranice.setText("Postavljena završna stranica je neispravna.");
                        error_stranice.setTextColor(Color.RED);
                        error_stranice.setVisibility(View.VISIBLE);
                        return false;
                    }

                } else {
                    TextView error_stranice = (TextView) findViewById(R.id.Error_stranica);
                    error_stranice.setText("Niste odabrali datoteku.");
                    error_stranice.setTextColor(Color.RED);
                    error_stranice.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });

        spinnerSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItemText = (String) adapterView.getItemAtPosition(i);
                if(selectedItemText != "Odaberite kopirnicu..."){

                    odabranaKopirnica = selectedItemText.split("                              #")[1];


                    int c;

                    for(c=0;c<=cijena_array.size();c++){
                        Log.i(TAG,cijena_array.get(c).split(",")[0]+" ?= "+odabranaKopirnica);
                        if(cijena_array.get(c).split(",")[0].equals(odabranaKopirnica)){
                            cijena_kopirnica = cijena_array.get(c).split(",");
                            break;

                        }

                    }



                    Log.i(TAG,cijena_kopirnica[3]);
                    tmpText = selectedItemText.split("                              #")[0].substring(selectedItemText.indexOf(", ") + 1);
                    tmpText = tmpText.split("                              #")[0];
                    selectedShop.setText("\t" + selectedItemText.split("                              #")[0]);

                    copyShopAdress = tmpText;
                    tmpText=selectedItemText.split("                              #")[0];

                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                }
                else {
                    odabranaKopirnica = "Odaberite kopirnicu...";
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
/*When the button (. . . ) is pressed user can select file he wants to send for printing. It is allowed only to pick
 * .pdf, .csv, .txt, .xml, .docx files */
        Button openFile = (Button) findViewById(R.id.buttonSendFile);
        openFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent chooseFile;
                Intent intent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                String [] mimeTypes = {"text/csv", "application/pdf","text/plain","application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"};
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                intent = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);


            }
        });

    }
    /*The selected file's name is written in string. First page of selected PDF file is shown on image view, and pages of PDF file are counted */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        EditText editTextFile = (EditText) findViewById(R.id.editTextFilePath);
        // TextView textViewShowData = (TextView) findViewById(R.id.textViewShowData);
        switch(requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    String filePath = uri.toString();
                    File myFile = new File(filePath);
                    String displayName = null;
                    if (filePath.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = this.getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                String fullPath = Commons.getPath(uri, this);
                                fileNamePath = fullPath;
                            }

                        } finally {
                            cursor.close();
                        }
                    } else if (filePath.startsWith("file://")) {
                        displayName = myFile.getName();
                        String tmpfilepath = myFile.getAbsolutePath();
                        tmpfilepath = tmpfilepath.replaceAll("file:/", "");
                        fileNamePath = tmpfilepath;
                    }

                    editTextFile.setText(displayName, TextView.BufferType.EDITABLE);
                    //Log.i(TAG, uri.getPath());
                    //Generating image and counting page numbers of PDF file
                    generateImageFromPdf(uri);
                    editTextFile.setText(displayName, TextView.BufferType.EDITABLE);
                   // Log.i(TAG, uri.getPath());
                    fileName = displayName;

                    izracunajCijenu();
                    aboutPrinting.setText("Cijena: "+Float.toString(ukupna_cijena)+" kn");
                }
            }
        }

    }

    //PdfiumAndroid (https://github.com/barteksc/PdfiumAndroid)
    //https://github.com/barteksc/AndroidPdfViewer/issues/49
    //Generate image from first page, and count number of pages (only for PDF files)
    void generateImageFromPdf(Uri pdfUri) {
        ImageView showPdf = (ImageView) findViewById(R.id.imageViewShowPDF);
        TextView textViewBrojstranica = (TextView) findViewById(R.id.textViewBrojStranica);
        int pageNumber = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {
            //http://www.programcreek.com/java-api-examples/index.php?api=android.os.ParcelFileDescriptor
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(pdfUri, "r");
            com.shockwave.pdfium.PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, pageNumber);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
            fullscreenpicture = bmp;
            saveImage(bmp);
            showPdf.setImageBitmap(bmp);
            int pageCount = pdfiumCore.getPageCount(pdfDocument);
            brojStranica = Integer.toString(pageCount);
            textViewBrojstranica.setText("\tBroj stranica: " + brojStranica);
            pdfiumCore.closeDocument(pdfDocument); // important!
        } catch(Exception e) {
            //todo with exception
        }
    }

    private class JsonTask extends AsyncTask<String, String, String> {
        //On pre execute show progress dialog with message "Učitavanje"
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(PrintActivity.this);
            pd.setMessage("Učitavanje...");
            pd.setCancelable(false);
            pd.show();
        }
        //Make connection to server and fetch server response in background
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        //On post execute decode JSON array and put all entries into List of strings
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            if (result != null) {
                spinner_array.add("Odaberite kopirnicu...");
                // ...
                JSONArray json = null;
                try {
                    json = new JSONArray(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                for(int i=0;i<json.length();i++){
                    JSONObject e = null;
                    try {
                        e = json.getJSONObject(i);

                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }

                    try {
                        spinner_array.add(e.getString("naziv")+", "+ e.getString("adresa")+"                              #"+e.getString("idKopirnice"));
                        cijena_array.add(e.getString("idKopirnice")+","+e.getString("cijenaStandard")+","+e.getString("cijenaBoja")+","+e.getString("cijenaUvez_M")+","+e.getString("cijenaUvez_T"));
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }


                }
                //Put contents of List<Strings> inside a spinner through ArrayAdapter
                final Spinner spinner = (Spinner) findViewById(R.id.spinnerPrintList);

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(PrintActivity.this,   android.R.layout.simple_spinner_item, spinner_array);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerArrayAdapter);
            }

            odabranaKopirnica = "Odaberite kopirnicu...";
            Log.i(TAG,cijena_array.get(0));
        }
    }

    private void saveImage(Bitmap bmp) {
        FileOutputStream out = null;
        try {
            File folder = new File(FOLDER);
            if(!folder.exists())
                folder.mkdirs();
            File file = new File(folder, "PDF.png");
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (Exception e) {
            //todo with exception
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                //todo with exception
            }
        }
    }



    /*Icon send file to copy shop is set in the menu */

    private void getInfo(){

        SharedPreferences pref_print=this.getSharedPreferences("Login",0);

        TextView info = (TextView)findViewById(R.id.textViewShowData);

        info.setText("\n\tEMAIL= "+pref_print.getString("email", null)+"\n\tPASS= "+pref_print.getString("password", null)+
                "\n\tID= "+pref_print.getString("id", null)+"\n\tIME: "+pref_print.getString("ime", null)+"\n\tPREZIME= "+
                pref_print.getString("prezime", null)+"\n\tTEL= "+pref_print.getString("tel", null));

    }


    /*Icon send file to copy shop is set in the menu */
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.print_activity_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.print_logout:

                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();

                Intent intent = new Intent(this, LoginActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;

            case R.id.action_sendFile:
                if (fileName == "" || odabranaKopirnica == "Odaberite kopirnicu..."){
                    showToastFromDialog("Can't send without selecting file or shop");
                }
                else send_file();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void viewMap(View view) throws IOException {
        TextView tmt = (TextView) findViewById(R.id.textViewShowData);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<android.location.Address> lastLocationName =  geocoder.getFromLocationName(copyShopAdress, 1);
        double latitude = lastLocationName.get(0).getLatitude();
        double longitude = lastLocationName.get(0).getLongitude();
     //   tmt.setText(String.valueOf(latitude) + " " + String.valueOf(longitude));
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra("LATITUDE_ID", latitude);
        intent.putExtra("LONGITUDE_ID", longitude);
        intent.putExtra("NAZIV KOPIRNICE", tmpText);
        startActivity(intent);
   //  tmt.setText(copyShopAdress);
    }

    public void onClick(View v) {


        switch (v.getId()) {
            case R.id.previous_postavke:

                LinearLayout layout_animation = (LinearLayout)findViewById(R.id.master_layout);

//enable animation
                layout_animation.setLayoutTransition(null);

//disable animation



                int resID = getResources().getIdentifier(labels_postavke[cursor_postavke], "id", getPackageName());
                TextView label = (TextView)findViewById(resID);
                label.setVisibility(View.GONE);

                resID = getResources().getIdentifier(layout_postavke[cursor_postavke], "id", getPackageName());
                LinearLayout layout = (LinearLayout) findViewById(resID);
                layout.setVisibility(View.GONE);

                cursor_postavke=cursor_postavke-1;

                LayoutTransition layoutTransition = new LayoutTransition();
                layout_animation.setLayoutTransition(layoutTransition);

                resID = getResources().getIdentifier(labels_postavke[cursor_postavke], "id", getPackageName());
                label = (TextView)findViewById(resID);
                label.setVisibility(View.VISIBLE);

                resID = getResources().getIdentifier(layout_postavke[cursor_postavke], "id", getPackageName());
                layout = (LinearLayout) findViewById(resID);
                layout.setVisibility(View.VISIBLE);

                Button button_next = (Button)findViewById(R.id.next_postavke);
                Button button_previous = (Button)findViewById(R.id.previous_postavke);

                if(cursor_postavke==0){
                    button_previous.setVisibility(View.GONE);
                }else if(cursor_postavke==1 && button_next.getVisibility()==View.GONE){
                    button_next.setVisibility(View.VISIBLE);
                }


                break;
            case R.id.next_postavke:

                layout_animation = (LinearLayout)findViewById(R.id.master_layout);
                layout_animation.setLayoutTransition(null);

                resID = getResources().getIdentifier(labels_postavke[cursor_postavke], "id", getPackageName());
                label = (TextView)findViewById(resID);
                label.setVisibility(View.GONE);

                resID = getResources().getIdentifier(layout_postavke[cursor_postavke], "id", getPackageName());
                layout = (LinearLayout) findViewById(resID);
                layout.setVisibility(View.GONE);

                cursor_postavke=cursor_postavke+1;

                layoutTransition = new LayoutTransition();
                layout_animation.setLayoutTransition(layoutTransition);

                resID = getResources().getIdentifier(labels_postavke[cursor_postavke], "id", getPackageName());
                label = (TextView)findViewById(resID);
                label.setVisibility(View.VISIBLE);

                resID = getResources().getIdentifier(layout_postavke[cursor_postavke], "id", getPackageName());
                layout = (LinearLayout) findViewById(resID);
                layout.setVisibility(View.VISIBLE);
                button_next = (Button)findViewById(R.id.next_postavke);
                button_previous = (Button)findViewById(R.id.previous_postavke);

                if(cursor_postavke==2){
                    button_next.setVisibility(View.GONE);
                } else if(cursor_postavke==1 && button_next.getVisibility()==View.GONE){
                    button_next.setVisibility(View.VISIBLE);
                } else if(cursor_postavke==1 && button_previous.getVisibility()==View.GONE){
                    button_previous.setVisibility(View.VISIBLE);
                }




                break;
        }
    }
    public void showImage() {
        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(fullscreenpicture);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.show();
    }

    public void send_file(){

        izracunajCijenu();

        if (inColor == null ){
            inColor = "crno-bijelo";
        }
        if (bothSides == null){
            bothSides = "jednostrano";
        }
        dialog_for_sending("http://207.154.235.97/files/sendfile-baza.php",
                 "\n\nDatoteka: " + fileName + "\nKopirnica: " + tmpText.split(",")[0] + "\nBroj kopija: "
                         + kopije + "\nBroj stranica: " + brojStranica + "\nDio za ispis: "
                        + whatToPrint + "\nPostavke za ispis: " + inColor + ", " + bothSides + ", " + vrstaUveza+"\n\nCijena: "+ukupna_cijena+"kn",
                "Pošaljite datoteku za printanje",
                "Datoteka je uspješno poslana.",
                "Greška u slanju preko veze.");
    }


    private void showToastFromDialog(String message){
        Toast.makeText(this, message , Toast.LENGTH_SHORT).show();
    }

    private void izracunajCijenu(){

        try{

        if(odabranaKopirnica!="Odaberite kopirnicu..."){
            Log.i(TAG,startPage.getText().toString());
            Log.i(TAG,endPage.getText().toString());
            if(inColor.equals("u boji")){
                cijena_boja = Float.parseFloat(cijena_kopirnica[2]);
            }else {
                cijena_boja = Float.parseFloat(cijena_kopirnica[1]);
                Log.i(TAG, Float.toString(cijena_boja));
            }

        if(vrstaUveza!=null) {
            if (vrstaUveza.equals("bez uveza")) {
                cijena_uvez = 0;
            } else if (vrstaUveza.equals("meki uvez")) {
                cijena_uvez = Float.parseFloat(cijena_kopirnica[3]);
            } else if (vrstaUveza.equals("tvrdi uvez")) {
                cijena_uvez = Float.parseFloat(cijena_kopirnica[4]);
            }
        }


                if (whatToPrint.equals("isprintaj sve")) {

                    cijena_dio_printanja = Float.parseFloat(brojStranica);
                }else if (startPage.getText().toString()!="" && endPage.getText().toString()!="") {

                    if(Float.parseFloat(startPage.getText().toString())== Float.parseFloat(endPage.getText().toString())){

                        cijena_dio_printanja = 1;
                    }else if (Float.parseFloat(startPage.getText().toString()) < Float.parseFloat(endPage.getText().toString())) {
                        cijena_dio_printanja = Float.parseFloat(endPage.getText().toString()) - Float.parseFloat(startPage.getText().toString()) + 1;
                    }

                }

                ukupna_cijena = cijena_boja * kopije * cijena_dio_printanja + cijena_uvez;

                //Log.i(TAG, Float.toString(kopije));


        }

        }catch(Exception ex){ // handle your exception
 ex.printStackTrace();
        }

    }

    private void dialog_for_sending(final String urladdress,
                                    String dlgMessage, String dlgTitle,
                                    final String dlgResultOK, final String dlgResultNOTok){

        // Let's design dialog programatically:
        // It will contain title, text, editbox, and progressbar. And 2 buttons, of course.
        // Progress bar will be shown only when network operation lasts longer.
        final SharedPreferences pref_print=this.getSharedPreferences("Login",0);
        final String user = pref_print.getString("id", null) ;
        final LinearLayout myDialogLayout = new LinearLayout(this);
        myDialogLayout.setOrientation(LinearLayout.VERTICAL);
        final ProgressBar myBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        // myDialogLayout.addView(myBar); // only when network operation is performed

        final AlertDialog.Builder alertDlg = new AlertDialog.Builder(this);
        alertDlg.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDlg.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });


        sendDialog = alertDlg.create();
        sendDialog.setMessage(dlgMessage);
        sendDialog.setTitle(dlgTitle);
        sendDialog.setView(myDialogLayout);

        /*
        Add an OnShowListener to change the OnClickListener on the first time the alert is shown.
        Calling getButton() before the alert is shown will return null.
        Then use a regular View.OnClickListener for the button, which will not dismiss
        the AlertDialog after it has been called.
        */

        sendDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                final Button button = sendDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myDialogLayout.addView(myBar);
                        button.setEnabled(false);
                        sendDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);

                        SendTask mySendTask = new SendTask(
                                urladdress,
                                vrstaUveza,
                                odabranaKopirnica,
                                user,
                                kopije,
                                fileNamePath,
                                fileName,
                                inColor,
                                bothSides,
                                whatToPrint,
                                vrstaUveza,
                                brojStranica);
                        mySendTask.setNetworkOperationFinished(new SendTask.NetworkOperationFinished() {
                            @Override
                            public void onNetworkOperationFinished(String response) {
                                myBar.setVisibility(View.INVISIBLE);
                                sendDialog.cancel();
                                if (response!="") {
                                    showToastFromDialog(dlgResultOK);
                                    Intent intentService = new Intent(PrintActivity.this, NotificationService.class);
                                    startService(intentService);
                                } else {
                                    showToastFromDialog(dlgResultNOTok);
                                }
                            }
                        });

                        mySendTask.execute();
                    }
                });
            }
        });
        sendDialog.show();
    }

}

