package com.learning.valentinstrungareanu.userlocationtracking;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    LocationListener locationListener;
    Location currentLocation;
    PolylineOptions polylineOptions  = new PolylineOptions();
    private static final int MIN_DISPLACEMENT = 10;
    private static final int LOCATION_REQUEST_CODE = 101;
    public static final String TAG = MapsActivity.class.getSimpleName();

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Instantiating the GoogleApiClient
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    public void onStart() {
        super.onStart();
        // Initiating the connection
        googleApiClient.connect();
    }

    public void onStop() {
        super.onStop();
        // Disconnecting the connection
        googleApiClient.disconnect();

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
             ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
            return;
        }
        //Fetching the last known location using the FusedLocationProviderApi
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
       // Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @SuppressLint({"RestrictedApi", "MissingPermission"})
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = location;
                            // Add a marker in user's current location and move the camera
                            LatLng initialCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
                            Log.d(TAG, initialCoordinates.toString());

                            polylineOptions.add(new LatLng[]{initialCoordinates});

                            //MarkerOptions are used to create a new Marker.You can specify location, title etc with MarkerOptions
                            MarkerOptions markerOptions = new MarkerOptions().position(new
                                    LatLng(location.getLatitude(), location.getLongitude())).title("You are Here");

                            mMap.addMarker(markerOptions);
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(initialCoordinates));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                            //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                            locationListener=new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {

                                    Toast.makeText(MapsActivity.this,location.getLatitude()+","+location.getLongitude(),Toast.LENGTH_SHORT).show();
                                    LatLng moveCoordinates= new LatLng(location.getLatitude(), location.getLongitude());
                                    polylineOptions.add(new LatLng[]{moveCoordinates});
                                    mMap.addPolyline(polylineOptions);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                                    MarkerOptions markerOptions = new MarkerOptions().position(new
                                            LatLng(location.getLatitude(), location.getLongitude())).title("You are Here");
                                    mMap.addMarker(markerOptions);


                                }
                            };

                            mLocationRequest = new LocationRequest();

                            mLocationRequest.setSmallestDisplacement(MIN_DISPLACEMENT);
                            mLocationRequest.setInterval(10000);
                            mLocationRequest.setFastestInterval(5000);
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, locationListener);

                        }
                        else{
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    //Permission Granted
                } else
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                break;

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    //Callback invoked if the GoogleApiClient conection has failed
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
