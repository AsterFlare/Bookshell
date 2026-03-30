package com.example.testbooks1;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.ReviewAdapter;
import com.example.testbooks1.Model.Review;
import com.example.testbooks1.Model.UserBook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BookDetailActivity extends AppCompatActivity {
    ImageView ivBook;
    TextView tvTitle, tvAuthor, tvDescription;
    EditText etListTitle;
    Button btnAddToCommunity;
    DatabaseReference communityRef;
    RatingBar ratingBar;
    EditText etReview;
    Button btnSubmitReview;
    RecyclerView rvReviews;

    String bookId;
    DatabaseReference reviewRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize(){
        ivBook = findViewById(R.id.ivDetailImage);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        etListTitle = findViewById(R.id.etListTitle);
        btnAddToCommunity = findViewById(R.id.btnAddToCommunity);
        Button btnWantToRead = findViewById(R.id.btnWantToRead);
        Button btnCurrentlyReading = findViewById(R.id.btnCurrentlyReading);
        Button btnCompleted = findViewById(R.id.btnCompleted);

        communityRef = FirebaseDatabase.getInstance().getReference("communityLists");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        rvReviews = findViewById(R.id.rvReviews);

        bookId = getIntent().getStringExtra("bookId");

        reviewRef = FirebaseDatabase.getInstance()
                .getReference("reviews")
                .child(bookId);

        String title = getIntent().getStringExtra("title");
        String image = getIntent().getStringExtra("image");
        String author = getIntent().getStringExtra("author");
        String description = getIntent().getStringExtra("description");

        tvTitle.setText(title);
        tvAuthor.setText(author != null ? author : "Unknown");
        //tvDescription.setText(description != null ? description : "No description available");

        Glide.with(this)
                .load(image)
                .into(ivBook);

        if (user != null) {
            String uid = user.getUid();
        }

        btnAddToCommunity.setOnClickListener(v -> {
            if (user == null) return;

            String listTitle = etListTitle.getText().toString().trim();
            if (listTitle.isEmpty()) {
                Toast.makeText(this, "Please enter a list title", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = System.currentTimeMillis();

            // Path: communityLists/userId/listTitle/push()
            DatabaseReference listRef = FirebaseDatabase.getInstance()
                    .getReference("communityLists")
                    .child(user.getUid())
                    .child(listTitle);

            // Book entry
            String bookId = listRef.push().getKey(); // unique book key
            CommunityBook cb = new CommunityBook(
                    user.getUid(),
                    tvTitle.getText().toString(),
                    listTitle,
                    image,
                    timestamp
            );

            listRef.child(bookId).setValue(cb).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Added to community list", Toast.LENGTH_SHORT).show();
                    etListTitle.setText(""); // clear input
                } else {
                    Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnWantToRead.setOnClickListener(v -> addBookToUserBooks("Want to Read"));
        btnCurrentlyReading.setOnClickListener(v -> addBookToUserBooks("Currently Reading"));
        btnCompleted.setOnClickListener(v -> addBookToUserBooks("Completed"));

        btnSubmitReview.setOnClickListener(v -> {
            //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String comment = etReview.getText().toString().trim();
            int rating = (int) ratingBar.getRating();

            if (comment.isEmpty() || rating == 0) {
                Toast.makeText(this, "Please add rating and review", Toast.LENGTH_SHORT).show();
                return;
            }

            String reviewId = reviewRef.push().getKey();

            String userName = user.getEmail(); // or fetch from users node later

            Review review = new Review(
                    user.getUid(),
                    userName,
                    rating,
                    comment,
                    System.currentTimeMillis()
            );

            reviewRef.child(reviewId).setValue(review).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Review added!", Toast.LENGTH_SHORT).show();
                    etReview.setText("");
                    ratingBar.setRating(0);
                }
            });
        });

        ArrayList<Review> reviewList = new ArrayList<>();
        ReviewAdapter adapter = new ReviewAdapter(this, reviewList);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);

        reviewRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reviewList.clear();
                Toast.makeText(BookDetailActivity.this,
                        "Reviews found: " + snapshot.getChildrenCount(), Toast.LENGTH_SHORT).show();
                reviewList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review r = ds.getValue(Review.class);

                    if (r != null) {
                        reviewList.add(r);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(BookDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addBookToUserBooks(String status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        long timestamp = System.currentTimeMillis();

        DatabaseReference userBooksRef = FirebaseDatabase.getInstance()
                .getReference("user_books")
                .child(uid);

        String bookId = getIntent().getStringExtra("bookId");
        //String bookId = userBooksRef.push().getKey();

        UserBook userBook = new UserBook(
                uid,
                tvTitle.getText().toString(),
                status,
                Glide.with(this).toString(), // image URL from intent
                timestamp
        );

        userBooksRef.child(bookId).setValue(userBook).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Book added to " + status, Toast.LENGTH_SHORT).show();

                // Update stats in /users
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("stats");

                if (status.equals("Completed")) {

                    userRef.child("completed").get().addOnSuccessListener(snapshot -> {
                        int completed = 0;

                        if (snapshot.getValue() != null) {
                            completed = snapshot.getValue(Integer.class);
                        }

                        userRef.child("completed").setValue(completed + 1);
                    });

                } else if (status.equals("Currently Reading")) {

                    userRef.child("reading").get().addOnSuccessListener(snapshot -> {
                        int reading = 0;

                        if (snapshot.getValue() != null) {
                            reading = snapshot.getValue(Integer.class);
                        }

                        userRef.child("reading").setValue(reading + 1);
                    });
                }

            } else {
                Toast.makeText(this, "Failed to add book", Toast.LENGTH_SHORT).show();
            }
        });
    }
}