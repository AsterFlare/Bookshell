package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvProfileBio, tvBooksCount, tvBadgesCount, tvStreakDays, tvViewAllBadges;
    private TextView tvProfileLevel;
    private TextView tvCurrentlyReadingEmpty;
    private ImageView ivProfileImage;
    private ImageButton menuButton;
    private BottomNavigationView bottomNav;
    private RecyclerView recyclerCurrentlyReading;
    private CurrentlyReadingAdapter currentlyReadingAdapter;
    private RecyclerView recyclerProfileBadges;
    private ProfileBadgeAdapter profileBadgeAdapter;
    private final ArrayList<BadgeRules.BadgeRow> profileBadgeRows = new ArrayList<>();

    private Context context;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        context = this;

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bottomNav = findViewById(R.id.bottomNavigationView);
        tvFullName = findViewById(R.id.fullName);
        tvProfileBio = findViewById(R.id.profileBio);
        tvBooksCount = findViewById(R.id.booksCount);
        tvBadgesCount = findViewById(R.id.badgesCount);
        tvStreakDays = findViewById(R.id.streakDays);
        tvViewAllBadges = findViewById(R.id.viewAllBadges);
        tvProfileLevel = findViewById(R.id.profileBadge);
        ivProfileImage = findViewById(R.id.profileImage);
        menuButton = findViewById(R.id.menuButton);
        tvCurrentlyReadingEmpty = findViewById(R.id.tvCurrentlyReadingEmpty);
        recyclerCurrentlyReading = findViewById(R.id.recyclerCurrentlyReading);

        recyclerCurrentlyReading.setLayoutManager(new LinearLayoutManager(this));
        currentlyReadingAdapter = new CurrentlyReadingAdapter(new ArrayList<>(), context, bookId -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });
        recyclerCurrentlyReading.setAdapter(currentlyReadingAdapter);

        recyclerProfileBadges = findViewById(R.id.recyclerProfileBadges);
        recyclerProfileBadges.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        profileBadgeAdapter = new ProfileBadgeAdapter(profileBadgeRows);
        recyclerProfileBadges.setAdapter(profileBadgeAdapter);

        bottomNav.setSelectedItemId(R.id.nav_profile);

        setupNavigation();
        listenUserProfile();
        listenUserStats();
        listenCurrentlyReading();
        setupClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(context, MainActivity.class));
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(context, LibraryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void listenUserProfile() {
        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                String fn = firstName != null ? firstName : "";
                String ln = lastName != null ? lastName : "";
                tvFullName.setText(getString(R.string.profile_name_format, fn, ln).trim());
                tvProfileBio.setText(bio != null ? bio : "");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).into(ivProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, R.string.toast_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenUserStats() {
        mDatabase.child("users").child(userId).child("stats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvBooksCount.setText("0");
                    tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format, 0, BadgeRules.TOTAL_BADGES));
                    tvStreakDays.setText(getString(R.string.streak_days_format, 0));
                    tvProfileLevel.setText(BadgeRules.levelNameForUnlockedCount(context, 0));
                    profileBadgeRows.clear();
                    profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, 0, 0, 0));
                    profileBadgeAdapter.notifyDataSetChanged();
                    return;
                }
                long completed = BadgeRules.readStatLong(snapshot, "completed");
                long reviews = BadgeRules.readStatLong(snapshot, "reviews");
                long readingLists = BadgeRules.readStatLong(snapshot, "readingLists");
                long streak = BadgeRules.readStatLong(snapshot, "readingStreak");

                tvBooksCount.setText(String.valueOf(completed));
                int unlocked = BadgeRules.countUnlocked(completed, reviews, readingLists);
                tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format, unlocked, BadgeRules.TOTAL_BADGES));
                tvStreakDays.setText(getString(R.string.streak_days_format, (int) streak));
                tvProfileLevel.setText(BadgeRules.levelNameForUnlockedCount(context, unlocked));
                profileBadgeRows.clear();
                profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, completed, reviews, readingLists));
                profileBadgeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, R.string.toast_stats_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenCurrentlyReading() {
        mDatabase.child("user_books").child(userId)
                .orderByChild("status")
                .equalTo("Currently Reading")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<CurrentlyReadingBook> list = new ArrayList<>();
                        for (DataSnapshot bookSnap : snapshot.getChildren()) {
                            CurrentlyReadingBook row = new CurrentlyReadingBook();
                            row.bookId = bookSnap.getKey();
                            row.title = bookSnap.child("title").getValue(String.class);
                            row.author = bookSnap.child("author").getValue(String.class);
                            row.coverUrl = bookSnap.child("coverUrl").getValue(String.class);
                            list.add(row);
                        }
                        currentlyReadingAdapter.setBooks(list);
                        boolean empty = list.isEmpty();
                        tvCurrentlyReadingEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        recyclerCurrentlyReading.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, R.string.toast_currently_reading_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        tvViewAllBadges.setOnClickListener(v -> startActivity(new Intent(context, BadgesActivity.class)));
        menuButton.setOnClickListener(v -> startActivity(new Intent(context, EditProfileActivity.class)));
        ivProfileImage.setOnClickListener(v -> startActivity(new Intent(context, EditProfileActivity.class)));
    }

    private static class CurrentlyReadingBook {
        String bookId;
        String title;
        String author;
        String coverUrl;
    }

    private static class CurrentlyReadingAdapter extends RecyclerView.Adapter<CurrentlyReadingAdapter.BookVH> {

        interface OnBookClickListener {
            void onBookClick(String bookId);
        }

        private final List<CurrentlyReadingBook> books;
        private final Context context;
        private final OnBookClickListener listener;

        CurrentlyReadingAdapter(List<CurrentlyReadingBook> books, Context context, OnBookClickListener listener) {
            this.books = books;
            this.context = context;
            this.listener = listener;
        }

        void setBooks(List<CurrentlyReadingBook> newBooks) {
            books.clear();
            books.addAll(newBooks);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BookVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_currently_reading, parent, false);
            return new BookVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BookVH holder, int position) {
            CurrentlyReadingBook b = books.get(position);
            holder.tvTitle.setText(b.title != null ? b.title : "");
            holder.tvAuthor.setText(b.author != null ? b.author : "");

            if (b.coverUrl != null && !b.coverUrl.isEmpty()) {
                Glide.with(context).load(b.coverUrl).into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.sample_book);
            }

            View.OnClickListener openDetail = v -> {
                if (b.bookId != null) {
                    listener.onBookClick(b.bookId);
                }
            };
            holder.card.setOnClickListener(openDetail);
            holder.tvContinue.setOnClickListener(openDetail);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        static class BookVH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ImageView ivCover;
            final TextView tvTitle;
            final TextView tvAuthor;
            final TextView tvContinue;

            BookVH(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
                ivCover = itemView.findViewById(R.id.bookCover);
                tvTitle = itemView.findViewById(R.id.bookTitle);
                tvAuthor = itemView.findViewById(R.id.bookAuthor);
                tvContinue = itemView.findViewById(R.id.btnContinue);
            }
        }
    }

    private static class ProfileBadgeAdapter extends RecyclerView.Adapter<ProfileBadgeAdapter.BadgeVH> {

        private final List<BadgeRules.BadgeRow> rows;

        ProfileBadgeAdapter(List<BadgeRules.BadgeRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public BadgeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge_profile, parent, false);
            return new BadgeVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BadgeVH holder, int position) {
            BadgeRules.BadgeRow row = rows.get(position);
            holder.tvName.setText(row.name);

            int tint = row.unlocked ? row.accentColorRes : R.color.badge_icon_locked;
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.ivIcon.getContext(), tint)));

            if (row.unlocked) {
                holder.itemView.setAlpha(1f);
                holder.tvStatus.setText(R.string.badge_status_unlocked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            } else {
                holder.itemView.setAlpha(0.55f);
                holder.tvStatus.setText(R.string.badge_status_locked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class BadgeVH extends RecyclerView.ViewHolder {
            final ShapeableImageView ivIcon;
            final TextView tvName;
            final TextView tvStatus;

            BadgeVH(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivBadgeIcon);
                tvName = itemView.findViewById(R.id.tvBadgeName);
                tvStatus = itemView.findViewById(R.id.tvBadgeStatus);
            }
        }
    }
}
