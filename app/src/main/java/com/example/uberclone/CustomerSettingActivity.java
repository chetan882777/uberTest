package com.example.uberclone;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerSettingActivity extends AppCompatActivity {

    private EditText mEditTextName;
    private EditText mEditTextPhone;
    private Button mButtonConfirm;
    private Button mButtonCancel;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userId;

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

        finish();
    }
}
