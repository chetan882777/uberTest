package com.example.uberclone;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingActivity extends AppCompatActivity {

    private EditText mEditTextName;
    private EditText mEditTextPhone;
    private Button mButtonConfirm;
    private Button mButtonCancel;
    private ImageView mImageViewProfile;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userId;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setting);

        mEditTextName = findViewById(R.id.editText_customer_setting_name);
        mEditTextPhone = findViewById(R.id.editText_customer_setting_phone);

        mButtonConfirm = findViewById(R.id.button_customer_setting_save);
        mButtonCancel = findViewById(R.id.button_customer_setting_cancel);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userId);

        getUserInfo();

        mButtonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });

        mImageViewProfile = findViewById(R.id.imageView_customer_setting_profile);
        mImageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("image/*");
                startActivityForResult(intent , 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();
            mImageViewProfile.setImageURI(imageUri);
        }
    }

    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.containsKey("name")){
                        String name = map.get("name").toString();
                        mEditTextName.setText(name);
                    }
                    if(map.containsKey("phone")){
                        String phone = map.get("phone").toString();
                        mEditTextPhone.setText(phone);
                    }
                    if(map.containsKey("profileImageUrl")){
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl)
                                .into(mImageViewProfile);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void saveUserInformation() {
        Map userInfo = new HashMap();
        userInfo.put("name" , mEditTextName.getText().toString());
        userInfo.put("phone" , mEditTextPhone.getText().toString());
        mCustomerDatabase.updateChildren(userInfo);

        if( imageUri != null){
            final StorageReference filePath = FirebaseStorage.getInstance()
                    .getReference()
                    .child("profile_images")
                    .child(userId);

            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver() , imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG , 20 , baos);

            byte[] data = baos.toByteArray();

            final UploadTask uploadTask  = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUrl = uri;

                            Map<String, Object> map = new HashMap<>();

                            map.put("profileImageUrl" , downloadUrl);
                            mCustomerDatabase.updateChildren(map);

                            finish();
                            return;
                        }
                    });
                }
            });
            finish();
        }
    }
}
