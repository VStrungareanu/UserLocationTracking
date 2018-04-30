package com.learning.valentinstrungareanu.userlocationtracking;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private static final float MIN_DISPLACEMENT = 10;
    private static final int LOCATION_REQUEST_CODE = 101;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Boolean locationOn = false;
    private Switch locationSwitch;
    private ArrayList<LatLng> journey = new ArrayList<>();
    private LatLng initialLocation;
    private LatLng latestLocation;
    private PolylineOptions polylineOptions = new PolylineOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        //create our viewmodel
        // LocationViewModel model = ViewModelProviders.of(this).get(LocationViewModel.class);

        //Instantiating the GoogleApiClient
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (savedInstanceState != null) {
            locationOn = savedInstanceState.getBoolean("switchState");
            journey = savedInstanceState.getParcelableArrayList("journey");

        }


    }

    public void onStart() {
        super.onStart();
        // Initiating the connection
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationOn) {
            startLocationUpdates();
        }
    }

    public void onStop() {
        super.onStop();
        // Disconnecting the connection
        googleApiClient.disconnect();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("switchState", locationOn);
        outState.putParcelableArrayList("journey", journey);
    }

    /**
     * we use this method to restore the switch button state on config changes
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        locationSwitch.setChecked(locationOn);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_options, menu);
        /**
         * get the switch menu item view and apply a change listener on it
         * so we can track if the user wants location tracking enabled/disabled
         */

        locationSwitch = menu.findItem(R.id.location_switch)
                .getActionView().findViewById(R.id.location_switch);

        locationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    if (mapExists()) {
                        mMap.clear();
                    }

                    Toast.makeText(MapsActivity.this, "ON", Toast.LENGTH_SHORT).show();
                    locationOn = true;
                    startLocationUpdates();
                } else {
                    Toast.makeText(MapsActivity.this, "Off", Toast.LENGTH_SHORT).show();
                    locationOn = false;
                    if (mLocationCallback != null) {
                        stopLocationUpdates();
                    }
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.location_switch:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        //check if permission was granted otherwise programatically request it
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

            return;
        }

        //Fetching the last known location using the FusedLocationProviderApi
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {

                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            findInitialLocation(location);
                            //creating the location update request
                            createLocationRequest();
                            //turn on location update sendouts
                            if (locationOn) {
                                startLocationUpdates();
                            } else if (mLocationCallback != null) {
                                stopLocationUpdates();
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "Please make sure your location service is activated and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean mapExists() {
        if (mMap != null) {
            return true;
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void findInitialLocation(Location location) {

        //enable My location layer, dot and button (listeners on the map)
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        LatLng initialCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d(TAG, "initial location " + initialCoordinates.toString());

        if (locationOn) {
            // journey.add(initialCoordinates);
        }
        // Move the camera to initial location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(initialCoordinates));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

    }

    //Callback invoked once the GoogleApiClient is connected successfully
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PERMISSION_GRANTED) {
            if (mapExists()) {
                //Fetching the last known location using the FusedLocationProviderApi
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    findInitialLocation(location);
                                } else {
                                    Toast.makeText(MapsActivity.this, "Please make sure your location service is activated and try again.", Toast.LENGTH_LONG).show();
                                }
                            }

                        });
            }
        }
    }

    //Callback invoked if the GoogleApiClient conection has failed
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        // Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }


    /**
     * Method used to create the location request by establishing the granularity of location updates
     * and accuracy
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setSmallestDisplacement(MIN_DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        Log.d(TAG, "Sending location updates");
        // Create the location request to start receiving updates
        createLocationRequest();

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        //Fetching the last known location using the FusedLocationProviderApi
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //check if the map object is ready to use
                if (mapExists()) {
                    //doing the map updates based on the new location
                    onChangedLocation(locationResult.getLastLocation());
                }

            }
        };
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Log.d(TAG, "Stop location updates");
        drawUserPath(null, latestLocation, locationOn);
    }

    /**
     * Method used to do the map tracking changes when location is detected to be changed
     *
     * @param location
     */
    private void onChangedLocation(Location location) {

        Toast.makeText(MapsActivity.this, "Moving to: " + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
        LatLng moveCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
        if (locationOn) {
            journey.add(moveCoordinates);
        }
        latestLocation = journey.get(journey.size() - 1);
        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        drawUserPath(journey, latestLocation, locationOn);
    }

    /**
     * @param journey        list of all coordinate points recorded since the location was turned on
     * @param latestLocation last coordinates before the location was turned off
     * @param locationOn     flag that marks the location tracking state
     * @drawUserPath method is used to trace the journey the user has taken until the location was turned off
     */
    private void drawUserPath(ArrayList<LatLng> journey, LatLng latestLocation, boolean locationOn) {
        MarkerOptions markerOptions = new MarkerOptions();
        if (locationOn) {
            if (journey != null) {
                polylineOptions.addAll(journey);
                if (polylineOptions != null) {
                    mMap.addPolyline(polylineOptions);
                }

                if (journey.size() > 1) {
                    markerOptions.position(journey.get(0)).title(getString(R.string.map_marker_start));
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                    mMap.addMarker(markerOptions);
                }
            }
        } else {
            markerOptions.position(latestLocation).title(getString(R.string.map_marker_end));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.addMarker(markerOptions);
        }

    }
}
