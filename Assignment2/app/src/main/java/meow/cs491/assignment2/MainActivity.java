package meow.cs491.assignment2;

import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Button btnSearch;
    EditText etSearch;
    Circle radiusForDestination = null;
    Marker destinationPin = null;
    LatLng userLocation;
    boolean isRouting = true;
    LocationManager locationManager;
    String locationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        setUpMapIfNeeded();
        btnSearch = (Button) findViewById(R.id.btnSearch);
        etSearch = (EditText)findViewById(R.id.etSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If the users location is null, a Toast should be shown that corrects them.
                if (userLocation == null){
                    Toast.makeText(getApplicationContext(), "Could not find your location. Please try again", Toast.LENGTH_SHORT).show();
                }else{
                    String URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + userLocation.latitude + "," + userLocation.longitude + "&name=" + etSearch.getText().toString() + "&rankby=distance&key=" + getResources().getString(R.string.server_key);
                    Log.d("DEBUG", URL);
                    new HTTPTask().execute(URL);
                }

                //Hide the Keyboard when the user does a search
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

            }

        });

        //If the user clicks the search button on the keyboard, a search should be executed.
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    btnSearch.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Once the destination is reached, the radius around the destination should be removed, but the pin should still be there.
     * ALso, a TOast should be shown to notify the user that they are within 200m of their destination.
     */
    private void destinationReached() {
        radiusForDestination.remove();
        Toast.makeText(getApplicationContext(), "You are within 200m of your destination. Have fun!", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Set up the GoogleMap
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Enable GoogleMaps location functionality
        mMap.setMyLocationEnabled(true);

        //Center the map to Baltimore.
        CameraUpdate center=
                CameraUpdateFactory.newLatLng(new LatLng(39.2929,
                        -76.6702));
        CameraUpdate zoom= CameraUpdateFactory.zoomTo(10);

        //Animate the camera to move the starting location
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

    }

    /**
     * Sets up the Map fragment is needed.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {

            // Try to obtain the map from the SupportMapFragment.
            MapFragment mapFrag = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
            mapFrag.getMapAsync(this);

            //initialize the location manager
            this.initializeLocationManager();

        }
    }

    /**
     * Initializes the Location manager by determiming which provider should be used while initializing the location variable.
     */
    private void initializeLocationManager() {

        //get the location manager
        this.locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);


        //define the location manager criteria
        Criteria criteria = new Criteria();

        //Finds best location for the specific phone
        this.locationProvider = locationManager.getBestProvider(criteria, false);

        Location location = locationManager.getLastKnownLocation(locationProvider);

        //initialize the location
        if(location != null) {
            onLocationChanged(location);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        //Update the users location and update the route in case the user is routing.
        userLocation = new LatLng(location.getLatitude(),location.getLongitude());
        updateRoute(location);
        Log.d("DEBUG", "New location is is  " + location.getLatitude() + " " + location.getLongitude());

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private class HTTPTask extends AsyncTask<String, String, String>{
        String JSON = "", name = "";
        LatLng location = null;
        @Override
        protected String doInBackground(String... params) {
            //Do an HTTP request for the URL and wait for a JSON response.
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(params[0]);
            try{
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                //200 = OK = Successful response
                if(statusCode == 200){
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    //Read the JSON using a StringBuilder
                    while((line = reader.readLine()) != null){
                        builder.append(line);
                    }
                    JSON = builder.toString();
                } else {
                    Log.e(MainActivity.class.toString(), "Failed to get JSON object");
                }
            }catch(ClientProtocolException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
            return builder.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {

                //Create a JSON object so we can grab specific values from the JSON reasponce from the GOogle Places server
                JSONObject jsonObject = new JSONObject(JSON);

                //Grab all of the needed items from the JSON Array
                JSONObject jsonLocation = jsonObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                location = new LatLng(jsonLocation.getDouble("lat"),jsonLocation.getDouble("lng"));
                name = jsonObject.getJSONArray("results").getJSONObject(0).getString("name");
                isRouting = true;

                //After determining the location and name of the location being searched, a pin will be places on the map with a destination radius
                createPin(location, name);


                Log.i(MainActivity.class.getName(),
                        "\nLatitude is " + jsonLocation.getString("lat") +
                                "\nLongitude is " + jsonLocation.getString("lng") +
                                "\nName is " + jsonObject.getJSONArray("results").getJSONObject(0).getString("name")
                        );

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("Success");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.locationManager.requestLocationUpdates(this.locationProvider, 400, 1, this);
    }

    /**
     * Creates a MArker and draws a radius circle around the location that the user is routing to.
     * @param location Location of destination
     * @param name Name of destination
     */
    private void createPin(LatLng location, String name) {
        if(radiusForDestination != null)
            radiusForDestination.remove();
        if(destinationPin != null)
            destinationPin.remove();

        //Draw radius circle around destination
        radiusForDestination = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.latitude, location.longitude))
                .radius(200).strokeColor(Color.RED));

        //Display marker with name at destination location
        destinationPin = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(name));

        //If the user has a location, update the route to see where they are in the route
        if(userLocation != null)
        {
            Location loc = new Location("");
            loc.setLatitude(userLocation.latitude);
            loc.setLongitude(userLocation.longitude);
            updateRoute(loc);
        }
    }

    /**
     *  Update the route to check if the user is within 200 meter of their destination.
     *  If they are within 200 meters, a Toast needs to be displayed
     *  IF they are not within 200 meters, continue routing.
     * @param location Users location
     */
    private void updateRoute(Location location) {
        if(destinationPin != null && isRouting){

            Location destinationLocation = new Location("");
            destinationLocation.setLatitude(destinationPin.getPosition().latitude);
            destinationLocation.setLongitude(destinationPin.getPosition().longitude);

            //If they are within 200 meters, a Toast needs to be displayed since the destination has been reached
            if(destinationLocation.distanceTo(location) - 200 <= 0){
                destinationReached();
                isRouting = false;
            }
        }
    }

}
