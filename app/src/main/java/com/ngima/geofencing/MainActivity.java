package com.ngima.geofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<Status> {

    private static final String TAG = "MainActivity";
    private static final String REQUEST_GEOFENCE = "REQUEST_GEOFENCE";

    private static final int REQUEST_PERMISSION = 123;
    private static final int DEFAULT_GEOFENCE_CIRCLE_RADIUS = 3;

    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 1000;

    private LatLng officeLatLng = new LatLng(27.700622, 85.342523);

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mGoogleMap;
    private Marker mCurrLocationMarker;
    private Location mLastLocation;
    //    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;

    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.office_location_map))
                .getMapAsync(this);

        mGoogleApiClient = newGoogleApiClient();
        mGeofencingClient = LocationServices.getGeofencingClient(this);
//        mGeofenceList = new ArrayList<>();
//        mGeofenceList.add(newGeofence());
    }

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasAllPermissions()) {
            requestPermissions();
            return;
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (!hasAllPermissions()) {
            finish();
        }
    }

    //    OnMapReadyCallback...
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mCurrLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(officeLatLng));
        mGoogleMap.addCircle(new CircleOptions()
                .center(officeLatLng)
                .radius(DEFAULT_GEOFENCE_CIRCLE_RADIUS)
                .strokeColor(Color.RED)
                .fillColor(ContextCompat.getColor(this, R.color.circleFill)));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: ");
    }

    //    GoogleApiClient.ConnectionCallbacks...
    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: ");

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000); // Update location every second
        mLocationRequest.setFastestInterval(1000); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        try {
            mGeofencingClient.addGeofences(newGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            toast("geofences added successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast("failed to add geofences");
                        }
                    });
        } catch (SecurityException e) {

            if (!hasAllPermissions())
                requestPermissions();
        } catch (NullPointerException e) {

        }
//        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, newGeofencingRequest(), getGeofencePendingIntent())
//                .setResultCallback(this);
    }

    //    GoogleApiClient.OnConnectionFailedListener...
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: ");
    }

    //    LocationListener...
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosi);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(mGoogleMap.getCameraPosition().zoom));
    }

    @Override
    public void onResult(@NonNull Status status) {

        Log.d(TAG, "onResult: status" + status);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
    }

    private boolean hasAllPermissions() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private GoogleApiClient newGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private Geofence newGeofence() {
        return new Geofence.Builder()
                .setRequestId(REQUEST_GEOFENCE)
                .setCircularRegion(officeLatLng.latitude, officeLatLng.longitude,
                        DEFAULT_GEOFENCE_CIRCLE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT).build();
    }

    private GeofencingRequest newGeofencingRequest() {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//                .addGeofences(mGeofenceList)
                .addGeofence(newGeofence())
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void toast(String message) {

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
