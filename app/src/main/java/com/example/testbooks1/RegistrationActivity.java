package com.example.testbooks1;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testbooks1.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, fNameET, lNameET;
    private Button registerButton;
    private TextView alreadyRegisteredTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        initialize();
    }

    public void initialize(){
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        fNameET = findViewById(R.id.fNameET);
        lNameET = findViewById(R.id.lNameET);

        registerButton = findViewById(R.id.registerBtn);
        alreadyRegisteredTextView = findViewById(R.id.alreadyRegistered);
        mAuth = FirebaseAuth.getInstance();


        registerButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String fName = fNameET.getText().toString();
            String lName = lNameET.getText().toString();


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegistrationActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Implement registration logic
                //Toast.makeText(RegistrationActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        //bonus
                        FirebaseUser user = mAuth.getInstance().getCurrentUser();
                        String uid = user.getUid();
                        //user.sendEmailVerification();

                        if (user != null) {
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

                            User newUser = new User(fName, lName, email);
                            ref.child(uid).setValue(newUser).addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    user.sendEmailVerification();
                                    Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));

                                } else {
                                    Toast.makeText(RegistrationActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        //Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        //startActivity(new Intent(RegistrationActivity.this, WelcomeActivity.class));
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Registration failed" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        alreadyRegisteredTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}