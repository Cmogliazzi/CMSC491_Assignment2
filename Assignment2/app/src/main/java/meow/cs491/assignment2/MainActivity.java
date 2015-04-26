package meow.cs491.assignment2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
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

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Button btnSearch;
    EditText etSearch;
    Circle radiusForDestination = null;
    Marker destinationPin = null;
    LatLng userLocation;
    boolean isRouting = true;

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

                isRouting = true;

                //If the users location is null, a Toast should be shown that corrects them.
                if (userLocation == null){
                    Toast.makeText(getApplicationContext(), "Could not find your location. Please try again", Toast.LENGTH_SHORT).show();
                }else{
                    String URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + userLocation.latitude + "," + userLocation.longitude + "&name=" + etSearch.getText().toString() + "&rankby=distance&key=" + getResources().getString(R.string.server_key);
                    Log.d("DEBUG", URL + " IS THE URL YOU ARE USING");
                    new HTTPTask().execute(URL);

                }

                //Hide the Keyboard
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

            }

        });

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




    private final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            userLocation = new LatLng(location.getLatitude(),location.getLongitude());

            updateRoute(location);
        }



        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void updateRoute(Location location) {
        if(destinationPin != null && isRouting){
            isRouting = false;
            Location destinationLocation = new Location("");
            destinationLocation.setLatitude(destinationPin.getPosition().latitude);
            destinationLocation.setLongitude(destinationPin.getPosition().longitude);
            if(location.distanceTo(destinationLocation) - 200 <= 0){
                destinationReached();
            }
        }
    }

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
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);

        CameraUpdate center=
                CameraUpdateFactory.newLatLng(new LatLng(39.2929,
                        -76.6702));
        CameraUpdate zoom= CameraUpdateFactory.zoomTo(10);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        long timer = 0;
        float distance = 0;
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timer,
                distance, mLocationListener);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.

            MapFragment mapFrag = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
            mapFrag.getMapAsync(this);

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

    private class HTTPTask extends AsyncTask<String, String, String>{
        String JSON = "", name = "";
        LatLng location = null;
        @Override
        protected String doInBackground(String... params) {
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(params[0]);
            try{
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if(statusCode == 200){
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
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

                JSONObject jsonObject = new JSONObject(JSON);

                JSONObject jsonLocation = jsonObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                location = new LatLng(jsonLocation.getDouble("lat"),jsonLocation.getDouble("lng"));
                name = jsonObject.getJSONArray("results").getJSONObject(0).getString("name");

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

    private void createPin(LatLng location, String name) {


        if(radiusForDestination != null)
            radiusForDestination.remove();
        if(destinationPin != null)
            destinationPin.remove();

        radiusForDestination = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.latitude, location.longitude))
                .radius(200).strokeColor(Color.RED));

        destinationPin = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(name));

        if(userLocation != null)
        {
            Location loc = new Location("");
            loc.setLatitude(userLocation.latitude);
            loc.setLongitude(userLocation.longitude);
            updateRoute(loc);
        }
    }


}
