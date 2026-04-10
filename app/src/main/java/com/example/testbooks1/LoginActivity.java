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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.User;
import com.example.testbooks1.Model.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button loginBtn;
    TextView tvNotRegistered;
    FirebaseAuth mAuth;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        c = this;
        applySafeAreaPaddingOnce();
        initialize();
    }

    private void applySafeAreaPaddingOnce() {
        View main = findViewById(R.id.main);
        final int baseLeft = main.getPaddingLeft();
        final int baseTop = main.getPaddingTop();
        final int baseRight = main.getPaddingRight();
        final int baseBottom = main.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            int insetTypes = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime();
            Insets combined = insets.getInsets(insetTypes);
            v.setPadding(
                    baseLeft + combined.left,
                    baseTop + combined.top,
                    baseRight + combined.right,
                    baseBottom + combined.bottom);
            return insets;
        });
    }

    public void initialize(){
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.loginBtn);
        tvNotRegistered = findViewById(R.id.tvNotRegistered);
        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(c, "Please enter your email.", Toast.LENGTH_SHORT).show();
                return;
            } if (password.isEmpty()) {
                Toast.makeText(c, "Please enter your password.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String uid = AuthManager.getUid();
                                if (uid == null) {
                                    Toast.makeText(c, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                DatabaseReference userRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(uid);

                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        User user = snapshot.getValue(User.class);
                                        if (user != null) {
                                            UserManager.setUser(user);
                                        }
                                        Toast.makeText(c, R.string.toast_login_success, Toast.LENGTH_SHORT).show();
                                        MainActivity.openHome(c);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(c, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                Exception ex = task.getException();
                                if (ex instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(c, "No account found with this email.", Toast.LENGTH_SHORT).show();
                                } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(c, "Incorrect credentials.", Toast.LENGTH_SHORT).show();
                                } else if (ex instanceof FirebaseAuthException) {
                                    Toast.makeText(c, ex.getMessage() != null ? ex.getMessage() : "", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(c, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        tvNotRegistered.setOnClickListener(v -> startActivity(new Intent(c, RegistrationActivity.class)));
    }
}
