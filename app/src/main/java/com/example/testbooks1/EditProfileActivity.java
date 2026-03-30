package com.example.testbooks1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvCancel = findViewById(R.id.tvCancel);
        tvCancel.setOnClickListener(v -> finish());

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            // Logic to save profile changes would go here
            finish();
        });
    }
}
