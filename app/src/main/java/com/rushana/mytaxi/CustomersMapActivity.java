package com.rushana.mytaxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        com.google.android.gms.location.LocationListener   {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;

    private Button customerLogoutButton, settingsButton;
    private Button callTaxiButton;
    private String customerID;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference CustomerDatabaseRef;
    private DatabaseReference DriversAvailableRef;
    private DatabaseReference DriversLocationRef;
    private LatLng CustomerPosition;
    private int radius = 1;
    private Boolean driverFound = false, requestTape = false;
    private String driverFoundID;
    private DatabaseReference DriverRef;

    private ValueEventListener DriverLocationsReflistener;
    private ImageView callDriver;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView driverPhoto;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mAuth = FirebaseAuth.getInstance();
        settingsButton = (Button) findViewById(R.id.customer_settings_button);
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available"); //доступный водитель
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working"); //водитель в работе

        txtName = (TextView)findViewById(R.id.driver_name);
        txtPhone = (TextView)findViewById(R.id.driver_phone_number);
        txtCarName = (TextView)findViewById(R.id.driver_car);
        driverPhoto = (CircleImageView)findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.rel_1);

        callDriver = (ImageView)findViewById(R.id.call_to_driver);

        relativeLayout.setVisibility(View.INVISIBLE);


        customerLogoutButton = (Button)findViewById(R.id.customer_logout_button);
        callTaxiButton = (Button)findViewById(R.id.customer_order_button);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });


        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                LogautCustomer();
            }
        });

        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(requestTape){
                    requestTape = false;
                    geoQuery.removeAllListeners();
                    DriversLocationRef.removeEventListener(DriverLocationsReflistener);

                    if(driverFound != null){
                        DriverRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");
                        DriverRef.removeValue();
                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerID);

                    if(PickUpMarker != null){

                        PickUpMarker.remove();
                    }
                    if(driverMarker != null){

                        driverMarker.remove();
                    }

                    callTaxiButton.setText("Вызвать такси");
                }
                else {
                    requestTape = true;

                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    CustomerPosition = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiButton.setText("Поиск водителя...");
                    getNearbyDriver();

                }



            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
    public void onClick(View view) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
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
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());  //долгота и широта

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

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
    }

    private void LogautCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();


    }

    private void getNearbyDriver() {
        GeoFire geoFire = new GeoFire(DriversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location){
                if (!driverFound && requestTape);
                {

                    driverFound = true;
                    driverFoundID = key; //нашли водителя

                    DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID); //закрепили водителя за клиентом
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriverRef.updateChildren(driverMap);  //обновили данные
                    
                    GetDriverLocation();
                    

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
                if (!driverFound){

                    radius = radius + 1;
                    getNearbyDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GetDriverLocation() {
        DriverLocationsReflistener = DriversLocationRef.child(driverFoundID).child("l").
                addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && requestTape){

                            List<Object> driverLocationsMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            callTaxiButton.setText("Водитель найден");

                            getDriverInformation();
                            relativeLayout.setVisibility(View.VISIBLE);

                            if (driverLocationsMap.get(0) != null){

                                LocationLat = Double.parseDouble(driverLocationsMap.get(0).toString());
                            }

                            if (driverLocationsMap.get(1) != null){

                                LocationLng = Double.parseDouble(driverLocationsMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);

                            if (driverMarker != null){

                                driverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(DriverLatLng.latitude);
                            location1.setLongitude(DriverLatLng.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(CustomerPosition.latitude);
                            location2.setLongitude(CustomerPosition.longitude);

                            float Distance = location1.distanceTo(location2); //расстояние до такси
                            if (Distance< 100){
                                callTaxiButton.setText("Ваше такси подъезжает");

                            }
                            else {
                                callTaxiButton.setText("Расстояние до такси" + String.valueOf(Distance));
                            }


                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                    .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void getDriverInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String carname = dataSnapshot.child("carname").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCarName.setText(carname);

                    callDriver.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int permissionCheck = ContextCompat.checkSelfPermission(CustomersMapActivity.this, Manifest.permission.CALL_PHONE);

                            if (permissionCheck != PackageManager.PERMISSION_GRANTED){
                                ActivityCompat.requestPermissions(
                                        CustomersMapActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 123
                                );
                            }
                            else {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + phone));
                                startActivity(intent);
                            }

                        }
                    });


                     if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(driverPhoto);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}