package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView notRegisteredTextView;
    private FirebaseAuth mAuth;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initialize();
        c=this;
    }

    public void initialize(){
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);

        loginButton = findViewById(R.id.loginBtn);
        notRegisteredTextView = findViewById(R.id.notRegistered);
        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(c, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(MainActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(c, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(c, MainActivity.class));
                    } else {
                        Toast.makeText(c, "Login failed" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        notRegisteredTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(c, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }
}
