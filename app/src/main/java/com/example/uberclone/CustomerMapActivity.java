package com.example.uberclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private Location mLocation;

    private GoogleApiClient mGoogleApiClient;
    private Button customerLogoutBtn;
    private Button callUberBtn;

    private LatLng pickupLocation;

    private Marker pickupMarker;

    private boolean requestBol = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapCustomer);
        mapFragment.getMapAsync(this);

        customerLogoutBtn = findViewById(R.id.button_customer_logout);
        customerLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this , MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        callUberBtn = findViewById(R.id.button_call_uber);
        callUberBtn.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                if(requestBol){

                    requestBol = false;

                    geoQuery.removeAllListeners();

                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFoundId != null) {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                                .getReference()
                                .child("Users")
                                .child("Drivers")
                                .child(driverFoundId);
                        driverRef.setValue(true);

                        driverFoundId = null;

                    }
                    driverFound = false;
                    radius = 1;
                    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                    GeoFire geoFire = new GeoFire(ref);

                    geoFire.removeLocation(user_id);

                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }

                    callUberBtn.setText("Call Uber");
                }else {
                    requestBol = true;

                    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                    GeoFire geoFire = new GeoFire(ref);

                    geoFire.setLocation(user_id, new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()));

                    pickupLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

                    pickupMarker = mMap.addMarker(new MarkerOptions()
                                    .position(pickupLocation)
                                    .title("Pickup Here")
                                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker)));

                    callUberBtn.setText("Getting your driver ...");

                    getClosestDriver();
                }
            }
        });
    }

    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundId;

    private GeoQuery geoQuery;
    private void getClosestDriver() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(reference);

        geoQuery = geoFire.queryAtLocation(
                new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);

        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol){
                    driverFound = true;
                    driverFoundId = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance()
                            .getReference()
                            .child("Users")
                            .child("Drivers")
                            .child(driverFoundId);

                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    HashMap map = new HashMap();
                    map.put("customerRideId" , customerId);

                    driverRef.updateChildren(map);

                    getDriverLocation();
                    callUberBtn.setText("Looking for driverLocation ..");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("driversWorking")
                .child(driverFoundId)
                .child("l");

        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLag = 0;

                    callUberBtn.setText("Driver Found");
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLag = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLang = new LatLng(locationLat, locationLag);

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLang.latitude);
                    loc2.setLongitude(driverLatLang.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if (distance < 100) {
                        callUberBtn.setText("Driver is here");
                    } else {
                        callUberBtn.setText("Driver Found: " + String.valueOf(distance));
                    }
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions()
                            .position(driverLatLang)
                            .title("your Driver")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_taxi)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
