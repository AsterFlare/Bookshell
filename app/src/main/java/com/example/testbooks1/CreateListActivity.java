package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.UserBooksAdapter;
import com.example.testbooks1.BadgeMilestoneHelper;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.CommunityBook;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CreateListActivity extends AppCompatActivity {
    private static final String TAG = "CreateListActivity";

    EditText etTitle, etDescription;
    RecyclerView rvUserBooks;
    ArrayList<CommunityBook> selectedBooks, userBooks;
    UserBooksAdapter userBooksAdapter;
    HashSet<String> selectedBookIds;
    MaterialButton btnShare;
    DatabaseReference communityRef;
    //FirebaseUser user;
    ImageView btnBack, ivCoverImage;
    Uri coverImageUri;
    String coverImageBase64;
    BottomNavigationView bottomNav;
    private static final int PICK_IMAGE_REQUEST = 101;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_list);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initialize() {
        etTitle = findViewById(R.id.title);
        etDescription = findViewById(R.id.description);
        rvUserBooks = findViewById(R.id.rvUserBooks);
        btnShare = findViewById(R.id.addToCommunity);
        btnBack = findViewById(R.id.btnBack);
        ivCoverImage = findViewById(R.id.ivCoverImage);
        ivCoverImage.setOnClickListener(v -> selectCoverImage());

        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                MainActivity.openHome(c);
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(c, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                startActivity(new Intent(c, CommunityActivity.class));
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(c, LibraryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(c, ProfileActivity.class));
                return true;
            }
            return false;
        });

        selectedBooks = new ArrayList<>();
        userBooks = new ArrayList<>();
        selectedBookIds = new HashSet<>();

        userBooksAdapter = new UserBooksAdapter(this, userBooks, selectedBookIds, (book, isSelected) -> {
            if (isSelected) selectedBooks.add(book);
            else removeBookFromSelected(book.bookId);
            userBooksAdapter.notifyItemRangeChanged(0, userBooks.size());
        });

        rvUserBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvUserBooks.setAdapter(userBooksAdapter);
        rvUserBooks.setNestedScrollingEnabled(true);

        //user = FirebaseAuth.getInstance().getCurrentUser();
        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");

        loadUserBooks();

        btnShare.setOnClickListener(v -> saveCommunityList());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserBooks() {
        String uid = AuthManager.getUid();
        if (uid == null) {
            Toast.makeText(this, R.string.toast_sign_in_for_community, Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);

        userBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int previousSize = userBooks.size();
                userBooks.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CommunityBook book = new CommunityBook();
                    book.bookId = ds.getKey();
                    book.title = ds.child("title").getValue(String.class);
                    book.author = ds.child("author").getValue(String.class);
                    book.imageUrl = ds.child("imageUrl").getValue(String.class);
                    book.category = ds.child("category").getValue(String.class);
                    book.description = ds.child("description").getValue(String.class);
                    if (book.title != null) userBooks.add(book);
                }
                if (previousSize > 0) {
                    userBooksAdapter.notifyItemRangeRemoved(0, previousSize);
                }
                if (!userBooks.isEmpty()) {
                    userBooksAdapter.notifyItemRangeInserted(0, userBooks.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveCommunityList() {
        String uid = AuthManager.getUid();
        if (uid == null) {
            Toast.makeText(this, R.string.toast_sign_in_for_community, Toast.LENGTH_LONG).show();
            return;
        }
        btnShare.setEnabled(false);
        if (selectedBooks.size() < 2) {
            Toast.makeText(this, "Please select at least 2 books.", Toast.LENGTH_SHORT).show();
            btnShare.setEnabled(true);
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title.", Toast.LENGTH_SHORT).show();
            btnShare.setEnabled(true);
            return;
        } if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description.", Toast.LENGTH_SHORT).show();
            btnShare.setEnabled(true);
            return;
        }

        String listId = communityRef.child(uid).push().getKey();
        if (listId == null) {
            Toast.makeText(this, "Unable to create list ID.", Toast.LENGTH_SHORT).show();
            btnShare.setEnabled(true);
            return;
        }
        DatabaseReference listRef = communityRef.child(uid).child(listId);

        Map<String, Object> booksMap = new HashMap<>();
        for (CommunityBook book : selectedBooks) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("bookId", book.bookId);
            map.put("title", book.title);
            map.put("author", book.author);
            map.put("imageUrl", book.imageUrl);
            map.put("category", book.category);
            map.put("description", book.description);
            booksMap.put(book.bookId, map);
        }
        Map<String, Object> listData = new HashMap<>();
        listData.put("title", title);
        listData.put("description", description);
        listData.put("timestamp", System.currentTimeMillis());
        listData.put("reactionCount", 0);
        listData.put("reactions", new HashMap<String, Object>());
        listData.put("books", booksMap);
        if (coverImageBase64 != null) {
            listData.put("coverImage", coverImageBase64);
        }

        listRef.setValue(listData).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                btnShare.setEnabled(true);
                Toast.makeText(c, R.string.toast_share_list_failed, Toast.LENGTH_LONG).show();
                return;
            }
            publishStatsAndFinish(uid);
        });
    }

    private void publishStatsAndFinish(String uid) {
        DatabaseReference statsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("stats")
                .child("readingLists");
        statsRef.setValue(ServerValue.increment(1)).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                btnShare.setEnabled(true);
                Toast.makeText(c, R.string.toast_share_list_failed, Toast.LENGTH_LONG).show();
                return;
            }
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats").get()
                    .addOnCompleteListener(t2 -> {
                        if (isFinishing()) {
                            return;
                        }
                        Runnable done = () -> {
                            if (isFinishing()) {
                                return;
                            }
                            Toast.makeText(c, R.string.toast_list_shared_success, Toast.LENGTH_SHORT).show();
                            finish();
                        };
                        if (t2.isSuccessful() && t2.getResult() != null) {
                            BadgeMilestoneHelper.runAfterStatsCelebrations(
                                    CreateListActivity.this, getApplicationContext(), uid, t2.getResult(), done);
                        } else {
                            done.run();
                        }
                    });
        });
    }

    private void removeBookFromSelected(String bookId) {
        for (int i = 0; i < selectedBooks.size(); i++) {
            if (selectedBooks.get(i).bookId.equals(bookId)) {
                selectedBooks.remove(i);
                return;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void selectCoverImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            coverImageUri = data.getData();
            ivCoverImage.setImageURI(coverImageUri);
            findViewById(R.id.tvCoverHint).setVisibility(View.GONE);
            convertToBase64(coverImageUri);
        }
    }

    private void convertToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            Bitmap bitmap = Bitmap.createScaledBitmap(original, 600, 400, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length > 200_000) {
                Toast.makeText(this, "Image too large, please choose a smaller one", Toast.LENGTH_SHORT).show();
                return;
            }
            coverImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Failed converting selected image", e);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }
}