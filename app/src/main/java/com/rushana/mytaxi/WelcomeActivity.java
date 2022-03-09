package com.rushana.mytaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WelcomeActivity extends AppCompatActivity {

    Button driverBtn, customerBtn;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initialize(); //вывели инициализацию в отдельный метод



        driverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent driverIntent = new Intent(WelcomeActivity.this, DriverRegLoginActivity.class);
                startActivity(driverIntent);

            }
        });

        customerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent customerIntent = new Intent(WelcomeActivity.this, CustomerRegLoginActivity.class);
                startActivity(customerIntent);


                }


        });

        if (currentUser!= null){ // пользователь уже авторизовывался в прграмме
            openMap();
        }

    }

    private void openMap() {
        databaseReference.child("Customers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(mAuth.getCurrentUser().getUid()).exists()){

                    startActivity(new Intent(WelcomeActivity.this, CustomersMapActivity.class));
                }
                else {

                    startActivity(new Intent(WelcomeActivity.this, DriverMapActivity.class));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initialize() {
        driverBtn = (Button) findViewById(R.id.driverBtn);
        customerBtn = (Button) findViewById(R.id.customerBtn);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
    }
}