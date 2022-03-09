package com.rushana.mytaxi;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker PickUpMarker;

    private Button LogoutDriverButton, SettingsDriverButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogoutDriverStatus = false;
    private DatabaseReference assignedCustomerRef, AssignedCustomerPositionRef;
    private String driverID, customerID = "";
    private ValueEventListener AssignedCustomerPositionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();

        LogoutDriverButton = (Button)findViewById(R.id.driver_logout_button);
        SettingsDriverButton = (Button)findViewById(R.id.driver_settings_button);

        SettingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });

        LogoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLogoutDriverStatus = true;
                mAuth.signOut();

                LogoutDriver();
                DisconnectDriver();

            }
        });

        getAssignedCustomerRequest();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        }

    private void getAssignedCustomerRequest() { //показать водителю назначенного клиента
        assignedCustomerRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID).child("CustomerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    customerID = dataSnapshot.getValue().toString();

                    getAssignedCustomerPosition();

                }
                else{
                    customerID = "";

                    if(PickUpMarker != null){
                        PickUpMarker.remove();
                    }

                    if (AssignedCustomerPositionListener!= null){
                        AssignedCustomerPositionRef.removeEventListener(AssignedCustomerPositionListener);
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerPosition() {
        AssignedCustomerPositionRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests") //передаем водителю положение клиента
                .child(customerID).child("l");

        AssignedCustomerPositionListener = AssignedCustomerPositionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    List<Object> customerPositionMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = Double.parseDouble(customerPositionMap.get(0).toString());
                    double LocationLng = Double.parseDouble(customerPositionMap.get(1).toString());



                    LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Забрать клиента тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(DriverLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000);
        locationRequest.setFastestInterval(100000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null){
            lastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());  //долгота и широта

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid(); //информация о пользователе, к-ый нах-ся в приложении
            DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //child - передача данных в папку где будет храниться, доступный водитель
            GeoFire geoFireAvailablity = new GeoFire(DriverAvalablityRef);


            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); // водитель на заказе
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);


            switch (customerID){

                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailablity.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailablity.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

            }
        }




    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();


    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!currentLogoutDriverStatus) {

            DisconnectDriver();

        }
    }

    private void DisconnectDriver() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid(); //информация о пользователе, к-ый нах-ся в приложении
        DatabaseReference DriverAvalablityRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //передача данных в папку где будет храниться

        GeoFire geoFire = new GeoFire(DriverAvalablityRef);
        geoFire.removeLocation(userID);

    }

    private void LogoutDriver() {
        Intent welcomeIntent = new Intent(DriverMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();

    }


    @Override
    public void onClick(View view) {
        currentLogoutDriverStatus = true;
        mAuth.signOut();

        LogoutDriver();
        DisconnectDriver();
    }
}