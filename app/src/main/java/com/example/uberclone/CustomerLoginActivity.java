package com.example.uberclone;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {

    public final CustomerLoginActivity CONTEXT = CustomerLoginActivity.this;
    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mFirebaseAuthStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mFirebaseAuth = FirebaseAuth.getInstance();

        mFirebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = mFirebaseAuth.getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(CONTEXT, MapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mLogin = findViewById(R.id.button_customer_login);
        mRegistration = findViewById(R.id.button_customer_registration);

        mEmail = findViewById(R.id.editText_customer_email);
        mPassword = findViewById(R.id.editText_customer_password);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                mFirebaseAuth.createUserWithEmailAndPassword(email , password)
                        .addOnCompleteListener(CONTEXT,
                                new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {

                                        if(!task.isSuccessful()){
                                            Toast.makeText(CONTEXT,
                                                    "Registration Failed" , Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            String user_id = mFirebaseAuth.getUid();
                                            DatabaseReference current_user_db = FirebaseDatabase
                                                    .getInstance()
                                                    .getReference()
                                                    .child("Users")
                                                    .child("Customers")
                                                    .child(user_id);
                                            current_user_db.setValue(true);
                                        }
                                    }
                                });
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                mFirebaseAuth
                        .signInWithEmailAndPassword(email , password)
                        .addOnCompleteListener(CONTEXT, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(!task.isSuccessful()){
                                    Toast.makeText(CONTEXT,
                                            "LogIn Failed" , Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mFirebaseAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirebaseAuth.addAuthStateListener(mFirebaseAuthStateListener);
    }

}
