package com.learning.valentinstrungareanu.userlocationtracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;
    private LocationCallback mLocationCallback;
    private Boolean locationOn;
    private Switch locationSwitch;
    private PolylineOptions polylineOptions = new PolylineOptions();
    private static final float MIN_DISPLACEMENT = 10;
    private static final int LOCATION_REQUEST_CODE = 101;
    private static final String TAG = MapsActivity.class.getSimpleName();


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

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data

                }
            }
        };
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
                if (isChecked){
                    Toast.makeText(MapsActivity.this, "ON",Toast.LENGTH_SHORT).show();
                    locationOn = true;
                }
                else {
                    Toast.makeText(MapsActivity.this, "Off",Toast.LENGTH_SHORT).show();
                    locationOn = false;
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

        //enable My location layer, dot and button (listeners on the map)
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            LatLng initialCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
                            Log.d(TAG, initialCoordinates.toString());

                            polylineOptions.add(new LatLng[]{initialCoordinates});
                            // Add a marker in user's current location and move the camera
                            MarkerOptions markerOptions = new MarkerOptions().position(initialCoordinates).title("This is You");
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(initialCoordinates));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                            mMap.addMarker(markerOptions);
                            //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                            //creating the location update request
                            createLocationRequest();

                            //Toast.makeText(MapsActivity.this, initialCoordinates.toString(), Toast.LENGTH_SHORT).show();

                            mLocationListener = new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {

                                    Toast.makeText(MapsActivity.this, "Moving to: "+location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
                                    LatLng moveCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
                                    polylineOptions.add(new LatLng[]{moveCoordinates});
                                    mMap.addPolyline(polylineOptions);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                                    MarkerOptions markerOptions = new MarkerOptions()
                                            .position(new LatLng(location.getLatitude(), location.getLongitude())).title("You are Here");
                                    mMap.addMarker(markerOptions);

                                }
                            };

                            //turn on location update sendouts
                           // startLocationUpdates();
                            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, mLocationListener);

                        } else {
                            Toast.makeText(MapsActivity.this, "Please make sure your location service is activated and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

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
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}
