package com.example.testbooks1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etBio;
    private TextView tvEmail;
    private ImageView ivProfileImage;
    private FloatingActionButton fabEditImage;
    private Button btnSave;
    private TextView tvCancel;

    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private String userId;
    private Uri imageUri;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etBio = findViewById(R.id.etBio);
        tvEmail = findViewById(R.id.tvEmail);
        ivProfileImage = findViewById(R.id.profileImage);
        fabEditImage = findViewById(R.id.fabEditImage);
        btnSave = findViewById(R.id.btnSave);
        tvCancel = findViewById(R.id.tvCancel);

        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserData();

        fabEditImage.setOnClickListener(v -> {
            if (btnSave.isEnabled()) {
                openGallery();
            }
        });
        btnSave.setOnClickListener(v -> saveChanges());
        tvCancel.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                String first = snapshot.child("firstName").getValue(String.class);
                String last = snapshot.child("lastName").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);

                if (first != null) {
                    etFirstName.setText(first);
                }
                if (last != null) {
                    etLastName.setText(last);
                }
                if (bio != null) {
                    etBio.setText(bio);
                }

                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(EditProfileActivity.this).load(imageUrl).into(ivProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, R.string.toast_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfileImage.setImageURI(imageUri);
        }
    }

    private void saveChanges() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(bio)) {
            Toast.makeText(this, R.string.toast_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageThenSave(firstName, lastName, bio);
        } else {
            saveToFirebase(firstName, lastName, bio, null);
        }
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        fabEditImage.setEnabled(!saving);
        btnSave.setText(saving ? R.string.edit_saving : R.string.action_save_changes);
    }

    private void uploadImageThenSave(String firstName, String lastName, String bio) {
        setSaving(true);
        StorageReference fileRef = mStorageRef.child(userId + ".jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                        saveToFirebase(firstName, lastName, bio, uri.toString())
                ).addOnFailureListener(e -> {
                    setSaving(false);
                    Toast.makeText(EditProfileActivity.this, R.string.toast_update_failed, Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            setSaving(false);
            Toast.makeText(EditProfileActivity.this, R.string.toast_upload_failed, Toast.LENGTH_SHORT).show();
        });
    }

    private void saveToFirebase(String firstName, String lastName, String bio, @Nullable String newImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("bio", bio);
        if (newImageUrl != null) {
            updates.put("profileImageUrl", newImageUrl);
        }

        setSaving(true);
        mDatabase.child("users").child(userId).updateChildren(updates).addOnCompleteListener(task -> {
            setSaving(false);
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, R.string.toast_profile_updated, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, R.string.toast_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
