package com.rushana.mytaxi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView circleImageView;
    private EditText nameET, phoneET, carET;
    private ImageView closeBtn, saveBtn;
    private TextView imageChangeBtn;

    private String getType;
    private String checker = "";

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private Uri imageUri;
    private String myUrl = "";
    private StorageTask upLoadTask;
    private StorageReference storageProfileImageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        storageProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profiles Pictures");

        mAuth = FirebaseAuth.getInstance();
        getType = getIntent().getStringExtra("type");  //чтобы понимать с какой стр пришел user
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);

        circleImageView = (CircleImageView)findViewById(R.id.profile_image);
        nameET = (EditText) findViewById(R.id.name);
        phoneET = (EditText) findViewById(R.id.phone);

        carET = (EditText) findViewById(R.id.car_name);
        if (getType.equals("Drivers")){
            carET.setVisibility(View.VISIBLE);

        }

        closeBtn = (ImageView) findViewById(R.id.close_button);
        saveBtn = (ImageView) findViewById(R.id.save_button);
        imageChangeBtn = (TextView) findViewById(R.id.change_photo_btn);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getType.equals("Drivers")){
                    startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));

                }
                else {
                    startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checker.equals("Clicked")){

                    ValidateControllers();

                }
                else {

                    ValidateAndSaveOnlyInformation();

                }

            }

        });

        imageChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "Clicked";

                CropImage.activity().setAspectRatio(1,1).start(SettingsActivity.this);

            }
        });

        getUserInformation(); // загрузить информацию пользователя при входе


    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode==RESULT_OK && data != null){

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            circleImageView.setImageURI(imageUri);
        }
        else {
            if (getType.equals("Drivers")){

                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            }
            else {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

            }


            Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show();
        }
    }

    private void ValidateControllers(){

        if (TextUtils.isEmpty(nameET.getText().toString())){
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();

        }
        else if (TextUtils.isEmpty(phoneET.getText().toString())){
            Toast.makeText(this, "Заполните поле номер телефона", Toast.LENGTH_SHORT).show();

        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(carET.getText().toString())) {
            Toast.makeText(this, "Заполните поле марка машины", Toast.LENGTH_SHORT).show();

        }
        else if (checker.equals("clicked")){

            upLoadProfileImage();
        }
    }

    private void upLoadProfileImage() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Загрузка информации");
        progressDialog.setMessage("Пожалуйста, подождите");
        progressDialog.show();


        if (imageUri != null){

            final StorageReference fileRef = storageProfileImageRef.child(mAuth.getCurrentUser().getUid() + ".jpg"); //путь сохранения фото

            upLoadTask = fileRef.putFile(imageUri);

            upLoadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task <Uri> task) {
                    if (task.isSuccessful()){

                        Uri downLoadUrl = task.getResult();
                        myUrl= downLoadUrl.toString();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", mAuth.getCurrentUser().getUid());
                        userMap.put("name", nameET.getText().toString());
                        userMap.put("phone", phoneET.getText().toString());
                        userMap.put("image", myUrl);

                        if (getType.equals("Drivers")){
                            userMap.put("carname", carET.getText().toString());
                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();

                        if (getType.equals("Drivers")){

                            startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                        }
                        else {
                            startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

                        }
                    }
                }
            });
        }
        else {
            Toast.makeText(this,"Изображение не выбрано", Toast.LENGTH_SHORT).show();
        }
    }
    private void ValidateAndSaveOnlyInformation() {
        if (TextUtils.isEmpty(nameET.getText().toString())){
            Toast.makeText(this, "Заполните поле имя", Toast.LENGTH_SHORT).show();

        }
        else if (TextUtils.isEmpty(phoneET.getText().toString())){
            Toast.makeText(this, "Заполните поле номер телефона", Toast.LENGTH_SHORT).show();

        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(carET.getText().toString())) {
            Toast.makeText(this, "Заполните поле марка машины", Toast.LENGTH_SHORT).show();

        }
        else {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", nameET.getText().toString());
            userMap.put("phone", phoneET.getText().toString());

            if (getType.equals("Drivers")){
                userMap.put("carname", carET.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);


            if (getType.equals("Drivers")){

                startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
            }
            else {
                startActivity(new Intent(SettingsActivity.this, CustomersMapActivity.class));

            }

        }
    }
    private void getUserInformation() {
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    nameET.setText(name);
                    phoneET.setText(phone);


                    if (getType.equals("Drivers")) {
                        String carname = dataSnapshot.child("carname").getValue().toString();
                        carET.setText(carname);
                    }

                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(circleImageView);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}