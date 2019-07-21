package com.example.arbi.printngo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    double latitudeFromPrint=0;
    double longitudeFromPrint=0;
    double tmpLat;
    double tmpLong;
    String adresaNaziv = "";
    ProgressDialog pd;
    Marker destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle extras = getIntent().getExtras();

        if (extras != null){
            latitudeFromPrint = extras.getDouble("LATITUDE_ID");
            longitudeFromPrint = extras.getDouble("LONGITUDE_ID");
            adresaNaziv = extras.getString("NAZIV KOPIRNICE");
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //Menu
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.maps_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                showMapTypeSelectorDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
//test
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                destination = marker;
                LatLng origin = mCurrLocationMarker.getPosition();
                LatLng dest = destination.getPosition();
                String url = getDirectionsUrl(origin, dest);

                // Start downloading json data from Google Directions API:
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
                return false;
            }
        });

        // Custom info window for the marker (supporting multiline snippet)
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(MapsActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);
                // Title
                TextView title = new TextView(MapsActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());
                // Snippet
                TextView snippet = new TextView(MapsActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());
                // Building infowindow layout
                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    // Create Directions API URL from the origin and destination points:
    private String getDirectionsUrl(LatLng origin, LatLng dest){

        // Route origin:
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Route destination
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Mode of Transport => WALKING! (defaults to "driving"!)
        String transportMode = "mode=walking";

        // Sensor
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + transportMode + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Make sure that GPS is enabled on the device
        LocationManager mlocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!enabled) {
            showDialogGPS();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        if(latitudeFromPrint!=0){
            LatLng latLng1 = new LatLng(latitudeFromPrint, longitudeFromPrint);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng1);
            markerOptions.title(adresaNaziv);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.addMarker(markerOptions);
        }
        else {
            new JsonTask().execute("http://207.154.235.97/login/lista_kopirnica.php");
        }
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Vaša trenutna lokacija.");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    /**
     * Show a dialog to the user requesting that GPS be enabled
     */
    private void showDialogGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Uključite Lokaciju");
        builder.setMessage("Ova aplikacija koristi vašu lokaciju, želite li je uključiti? ");
        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton("Uključi", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(
                        new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("Prekini", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private class JsonTask extends AsyncTask<String, String, String> {
        //On pre execute show progress dialog with message "Učitavanje"
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MapsActivity.this);
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
            String tempAdresa;
            String naziv;
            String cijenaStandard;
            String cijenaBoja;
            String cijenaUvez_M;
            String cijenaUvez_T;
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            if (result != null) {
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
                        tempAdresa = e.getString("adresa");
                        naziv = (e.getString("naziv"));
                        cijenaStandard = ("Cijena standard: " + e.getString("cijenaStandard") + " kn");
                        cijenaBoja = ("Cijena Boja: " + e.getString("cijenaBoja") + " kn");
                        cijenaUvez_M = ("Cijena meki uvez: " + e.getString("cijenaUvez_M") + " kn");
                        cijenaUvez_T = ("Cijena tvrdi uvez: " + e.getString("cijenaUvez_T" + " kn"));
                        getLatLong(tempAdresa);
                        LatLng latLng = new LatLng(tmpLat, tmpLong);
                        drawMarker(latLng, naziv, cijenaStandard, cijenaBoja, cijenaUvez_M, cijenaUvez_T);
                    /*    mCurrLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(tmpLat, tmpLong))
                                                                                .title(data)
                                                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
                        markerList.add(mCurrLocationMarker);*/
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
           //     markerList.size();

            }


        }
    }

    public void getLatLong(String address) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<android.location.Address> lastLocationName =  geocoder.getFromLocationName(address, 1);
        tmpLat = lastLocationName.get(0).getLatitude();
        tmpLong = lastLocationName.get(0).getLongitude();
    }

    private void drawMarker (LatLng point, String naziv, String cijenaS, String cijenaB, String cijenaMU, String cijenaTU){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.title(naziv);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerOptions.snippet(cijenaS + "\n" + cijenaB + "\n" + cijenaMU + cijenaTU);
        mMap.addMarker(markerOptions);
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-UI thread
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch(Exception e) {
                //Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }


    // Download json data (containing route) from API url:
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();
            br.close();

        } catch(Exception e) {
            //Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    // Class containing List with drawing info, and walking data (distance and estimated duration)
    private class jsonParsedData
    {
        List<List<HashMap<String,String>>> routeToDraw;
        String[] walkingData;

        public jsonParsedData(List<List<HashMap<String,String>>> routeToDraw, String[] walkingData)
        {
            this.routeToDraw = routeToDraw;
            this.walkingData = walkingData;
        }

        public List<List<HashMap<String,String>>> get_routeToDraw(){
            return this.routeToDraw;
        }

        public String[] get_walkingData(){
            return this.walkingData;
        }
    }


    // A class to parse the Google Places in JSON format:
    private class ParserTask extends AsyncTask<String, Integer, jsonParsedData>
    {
        // Parsing the data in non-UI thread:
        @Override
        protected jsonParsedData doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            String[] walkData = new String[2];
            jsonParsedData allData = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
                walkData = parser.decodeWalkingDurationAndWalkingDistance(jObject);
                allData = new jsonParsedData(routes, walkData);
            } catch(Exception e) {
                // e.printStackTrace();
            }
            return allData;
        }

        // Executes in UI thread, after the parsing process in doInBackground method:
        @Override
        protected void onPostExecute(jsonParsedData completeResult) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
          //  MarkerOptions markerOptions = new MarkerOptions();

            List<List<HashMap<String, String>>> result = completeResult.get_routeToDraw();
            String[] infoWalking = completeResult.get_walkingData();

            // Traversing through all the routes
            for(int i=0; i < result.size(); i++)
            {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0; j < path.size(); j++)
                {
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(5);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);

            // Show walking info on the marker:
            destination.setSnippet("Distance: " + infoWalking[0] + "\n" +
                    "Duration: " + infoWalking[1]);
            destination.showInfoWindow();
        }
    }

    private static final CharSequence[] MAP_TYPE_ITEMS =
            {"Cestovna", "Hibridna", "Satelitska", "Terenska"};

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = "Odaberite Vrstu Mape";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = mMap.getMapType() - 1;

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 1:
                                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 2:
                                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                            case 3:
                                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            default:
                                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        dialog.dismiss();
                    }
                }
        );

        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }

}