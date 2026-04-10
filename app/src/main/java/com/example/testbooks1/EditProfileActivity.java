package com.example.testbooks1;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etBio;
    private ImageView ivProfileImage;
    private ImageButton btnEditProfileImage;
    private Button btnSave;
    private TextView tvEditProfileName;

    private DatabaseReference mDatabase;
    private String userId;
    private Uri imageUri;
    @Nullable
    private Uri cameraOutputUri;
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && cameraOutputUri != null) {
                    imageUri = cameraOutputUri;
                    ivProfileImage.setImageURI(imageUri);
                    revokeUriPermission(cameraOutputUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            });

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

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etBio = findViewById(R.id.etBio);
        ivProfileImage = findViewById(R.id.profileImage);
        btnEditProfileImage = findViewById(R.id.btnEditProfileImage);
        btnSave = findViewById(R.id.btnSave);
        TextView tvCancel = findViewById(R.id.tvCancel);
        tvEditProfileName = findViewById(R.id.tvEditProfileName);

        findViewById(R.id.toolbarBack).setOnClickListener(v -> finish());

        loadUserData();

        btnEditProfileImage.setOnClickListener(v -> {
            if (btnSave.isEnabled()) {
                openCamera();
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
                    tvEditProfileName.setText(getString(R.string.profile_name));
                    tvEditProfileName.setVisibility(View.VISIBLE);
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                    ivProfileImage.setVisibility(View.VISIBLE);
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
                String defaultBio = getString(R.string.bio_default_text);
                if (bio == null || bio.trim().isEmpty() || defaultBio.equals(bio.trim())) {
                    etBio.setText("");
                } else {
                    etBio.setText(bio);
                }
                String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                tvEditProfileName.setText(fullName.isEmpty() ? getString(R.string.profile_name) : fullName);
                tvEditProfileName.setVisibility(View.VISIBLE);

                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(EditProfileActivity.this).load(imageUrl).into(ivProfileImage);
                } else {
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                }
                ivProfileImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, R.string.toast_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("profile_capture_" + userId + "_", ".jpg", getCacheDir());
            cameraOutputUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            List<ResolveInfo> cameraApps = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : cameraApps) {
                grantUriPermission(ri.activityInfo.packageName, cameraOutputUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveChanges() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String defaultBio = getString(R.string.bio_default_text);
        if (bio.isEmpty() || bio.equals(defaultBio)) {
            bio = "";
        }

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
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
        ivProfileImage.setEnabled(!saving);
        btnEditProfileImage.setEnabled(!saving);
        btnSave.setText(saving ? R.string.edit_saving : R.string.action_save_changes);
    }

    @Nullable
    private static String imageUrlFromCloudinaryResult(@Nullable Map<?, ?> resultData) {
        if (resultData == null) {
            return null;
        }
        for (String key : new String[]{"secure_url", "url"}) {
            Object v = resultData.get(key);
            if (v != null) {
                String s = v.toString();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    private void uploadImageThenSave(String firstName, String lastName, String bio) {
        if (imageUri == null) {
            Toast.makeText(this, R.string.toast_image_read_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uploadUri = copyImageToCacheForUpload(imageUri);
        if (uploadUri == null) {
            Toast.makeText(this, R.string.toast_image_read_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        try {
            MediaManager.get().upload(uploadUri)
                    .unsigned("profile_preset")
                    .option("public_id", userId + "_profile_pic_" + System.currentTimeMillis())
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = imageUrlFromCloudinaryResult(resultData);
                            runOnUiThread(() -> {
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    saveToFirebase(firstName, lastName, bio, imageUrl);
                                } else {
                                    setSaving(false);
                                    Toast.makeText(EditProfileActivity.this, R.string.toast_upload_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                setSaving(false);
                                String msg = error != null && error.getDescription() != null
                                        ? error.getDescription()
                                        : getString(R.string.toast_upload_failed);
                                if (error != null) {
                                    msg = "[" + error.getCode() + "] " + msg;
                                }
                                Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    })
                    .dispatch();
        } catch (IllegalStateException e) {
            setSaving(false);
            Toast.makeText(this, R.string.toast_upload_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private Uri copyImageToCacheForUpload(@NonNull Uri sourceUri) {
        File out = new File(getCacheDir(), "profile_upload_" + userId.hashCode() + ".jpg");
        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                fos.write(buf, 0, n);
            }
        
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
        } catch (IOException e) {
            return null;
        }
    }

    private void saveToFirebase(String firstName, String lastName, String bio, @Nullable String newImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("bio", bio);
        if (newImageUrl != null) {
            updates.put("profileImageUrl", withCacheBuster(newImageUrl));
        }

        setSaving(true);
        mDatabase.child("users").child(userId).updateChildren(updates).addOnCompleteListener(task -> {
            setSaving(false);
            if (!task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, R.string.toast_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(EditProfileActivity.this, R.string.toast_profile_updated, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditProfileActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @NonNull
    private static String withCacheBuster(@NonNull String url) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "t=" + System.currentTimeMillis();
    }
}
