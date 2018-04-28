package com.learning.valentinstrungareanu.userlocationtracking;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class LocationViewModel extends ViewModel {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    LocationListener locationListener;
    Location currentLocation;
    PolylineOptions polylineOptions  = new PolylineOptions();

    private MutableLiveData<List<Location>> locations;
    public LiveData<List<Location>> getLocations() {
        if (locations == null) {
            locations = new MutableLiveData<List<Location>>();
            loadUsers();
        }
        return locations;
    }

    private void loadUsers() {
        // Do an asynchronous operation to fetch users.
    }
}