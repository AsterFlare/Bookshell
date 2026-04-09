package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvProfileBio, tvBooksCount, tvBadgesCount, tvStreakDays, tvViewAllBadges;
    private TextView tvProfileLevel;
    private TextView tvCurrentlyReadingEmpty;
    private Button btnDailyCheckIn;
    private ImageButton btnSettings;
    private ImageView ivProfileImage;
    private ImageButton btnEditProfileImage;
    private View[] streakBars;
    private int streakBarMaxHeightPx;
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
        btnEditProfileImage = findViewById(R.id.btnEditProfileImage);
        btnSettings = findViewById(R.id.btnSettings);
        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        streakBars = new View[]{
                findViewById(R.id.streakBar0),
                findViewById(R.id.streakBar1),
                findViewById(R.id.streakBar2),
                findViewById(R.id.streakBar3),
                findViewById(R.id.streakBar4)
        };
        streakBarMaxHeightPx = (int) (80 * getResources().getDisplayMetrics().density + 0.5f);
        tvCurrentlyReadingEmpty = findViewById(R.id.tvCurrentlyReadingEmpty);
        recyclerCurrentlyReading = findViewById(R.id.recyclerCurrentlyReading);

        recyclerCurrentlyReading.setLayoutManager(new LinearLayoutManager(this));
        currentlyReadingAdapter = new CurrentlyReadingAdapter(new ArrayList<>(), context, book -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.bookId);
            intent.putExtra("title", book.title);
            intent.putExtra("image", book.coverUrl);
            intent.putExtra("author", book.author);
            intent.putExtra("description", book.description);
            intent.putExtra("category", book.category);
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
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(context, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                startActivity(new Intent(context, CommunityActivity.class));
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
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                    ivProfileImage.setVisibility(View.VISIBLE);
                    tvFullName.setVisibility(View.VISIBLE);
                    tvProfileBio.setVisibility(View.VISIBLE);
                    tvProfileLevel.setVisibility(View.VISIBLE);
                    return;
                }
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                String bio = snapshot.child("bio").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                String fn = firstName != null ? firstName : "";
                String ln = lastName != null ? lastName : "";
                tvFullName.setText(getString(R.string.profile_name_format, fn, ln).trim());
                if (bio == null || bio.trim().isEmpty()) {
                    tvProfileBio.setText(R.string.bio_default_text);
                } else {
                    tvProfileBio.setText(bio);
                }
                tvFullName.setVisibility(View.VISIBLE);
                tvProfileBio.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).into(ivProfileImage);
                } else {
                    ivProfileImage.setImageResource(R.drawable.default_pfp);
                }
                ivProfileImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ivProfileImage.setImageResource(R.drawable.default_pfp);
                ivProfileImage.setVisibility(View.VISIBLE);
                tvFullName.setVisibility(View.VISIBLE);
                tvProfileBio.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);
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
                    tvBooksCount.setVisibility(View.VISIBLE);
                    tvBadgesCount.setVisibility(View.VISIBLE);
                    tvProfileLevel.setVisibility(View.VISIBLE);
                    profileBadgeRows.clear();
                    profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, 0, 0, 0));
                    profileBadgeAdapter.notifyDataSetChanged();
                    applyStreakBars(0L);
                    return;
                }
                long completed = BadgeRules.readStatLong(snapshot, "completed");
                long reviews = BadgeRules.readStatLong(snapshot, "reviews");
                long readingLists = BadgeRules.readStatLong(snapshot, "readingLists");
                long streak = BadgeRules.readStatLong(snapshot, "readingStreak");
                String lastStreakDate = snapshot.child("lastStreakDate").getValue(String.class);
                String today = pstDayKey();
                int dayGap = dayGapPst(lastStreakDate, today);

                if (dayGap >= 2 && streak != 0L) {
                    DatabaseReference statsRef = mDatabase.child("users").child(userId).child("stats");
                    statsRef.child("readingStreak").setValue(0L);
                    for (int i = 0; i < 5; i++) {
                        statsRef.child("weeklyActivity").child(String.valueOf(i)).setValue(0L);
                    }
                    streak = 0L;
                }

                if (normalizeWeeklyActivityIfNeeded(streak, lastStreakDate, today, snapshot.child("weeklyActivity"))) {
                    return;
                }

                boolean alreadyCheckedInToday = today.equals(lastStreakDate);
                btnDailyCheckIn.setEnabled(!alreadyCheckedInToday);
                btnDailyCheckIn.setText(alreadyCheckedInToday
                        ? R.string.action_checked_in_today
                        : R.string.action_daily_check_in);

                tvBooksCount.setText(String.valueOf(completed));
                int unlocked = BadgeRules.countUnlocked(completed, reviews, readingLists);
                tvBadgesCount.setText(getString(R.string.badges_unlocked_count_format, unlocked, BadgeRules.TOTAL_BADGES));
                tvStreakDays.setText(getString(R.string.streak_days_format, (int) streak));
                tvProfileLevel.setText(BadgeRules.levelNameForUnlockedCount(context, unlocked));
                tvBooksCount.setVisibility(View.VISIBLE);
                tvBadgesCount.setVisibility(View.VISIBLE);
                tvProfileLevel.setVisibility(View.VISIBLE);
                profileBadgeRows.clear();
                profileBadgeRows.addAll(BadgeRules.badgeRowsFromStats(context, completed, reviews, readingLists));
                profileBadgeAdapter.notifyDataSetChanged();

                applyStreakBars(streak);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, R.string.toast_stats_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean normalizeWeeklyActivityIfNeeded(long streak,
                                                    @Nullable String lastStreakDate,
                                                    @NonNull String today,
                                                    @NonNull DataSnapshot weeklyActivitySnapshot) {
        long[] bars = new long[5];
        long maxValue = 0L;
        int maxIndex = -1;
        boolean hasAny = false;
        for (int i = 0; i < 5; i++) {
            bars[i] = BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(i));
            if (bars[i] > 0L) {
                hasAny = true;
                if (bars[i] > maxValue) {
                    maxValue = bars[i];
                    maxIndex = i;
                }
            }
        }

        if (streak <= 0L) {
            if (!hasAny) {
                return false;
            }
            DatabaseReference waRef = mDatabase.child("users").child(userId).child("stats").child("weeklyActivity");
            for (int i = 0; i < 5; i++) {
                waRef.child(String.valueOf(i)).setValue(0L);
            }
            return true;
        }

        if (!today.equals(lastStreakDate)) {
            return false;
        }

        int expectedIndex = (int) Math.max(0, Math.min(4, streak - 1L));
        if (maxIndex == -1 || maxIndex == expectedIndex) {
            return false;
        }

        DatabaseReference waRef = mDatabase.child("users").child(userId).child("stats").child("weeklyActivity");
        for (int i = 0; i < 5; i++) {
            waRef.child(String.valueOf(i)).setValue(i == expectedIndex ? Math.max(25L, maxValue) : 0L);
        }
        return true;
    }

    private void applyStreakBars(long streakDays) {
        if (streakBars == null) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            long daysInSegment = Math.max(0L, Math.min(7L, streakDays - (i * 7L)));
            long pct = Math.round((daysInSegment * 100.0) / 7.0);
            int h = (int) Math.max(8, streakBarMaxHeightPx * pct / 100.0);
            ViewGroup.LayoutParams lp = streakBars[i].getLayoutParams();
            lp.height = h;
            streakBars[i].setLayoutParams(lp);
        }
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
                            row.description = bookSnap.child("description").getValue(String.class);
                            row.category = bookSnap.child("category").getValue(String.class);
                            row.coverUrl = bookSnap.child("imageUrl").getValue(String.class);
                            if (row.coverUrl == null || row.coverUrl.isEmpty()) {
                                row.coverUrl = bookSnap.child("coverUrl").getValue(String.class);
                            }
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
        btnEditProfileImage.setOnClickListener(v -> startActivity(new Intent(context, EditProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(context, SettingsActivity.class)));
        btnDailyCheckIn.setOnClickListener(v -> applyDailyCheckIn());
        ivProfileImage.setOnClickListener(v -> {
            // Edit entry is the pencil button in the top bar.
        });
    }

    private void applyDailyCheckIn() {
        DatabaseReference statsRef = mDatabase.child("users").child(userId).child("stats");
        statsRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String today = pstDayKey();
                String last = currentData.child("lastStreakDate").getValue(String.class);
                if (today.equals(last)) {
                    return Transaction.success(currentData);
                }

                long currentStreak = readMutableLong(currentData.child("readingStreak"));
                int gap = dayGapPst(last, today);
                long nextStreak = (gap == 1) ? (currentStreak + 1L) : 1L;

                currentData.child("readingStreak").setValue(nextStreak);
                currentData.child("lastStreakDate").setValue(today);

                int bar = (int) Math.max(0, Math.min(4, nextStreak - 1));
                if (nextStreak == 1L) {
                    for (int i = 0; i < 5; i++) {
                        currentData.child("weeklyActivity").child(String.valueOf(i)).setValue(0L);
                    }
                }
                long prev = readMutableLong(currentData.child("weeklyActivity").child(String.valueOf(bar)));
                currentData.child("weeklyActivity").child(String.valueOf(bar))
                        .setValue(Math.min(100L, prev + 25L));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(context, R.string.toast_check_in_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                String today = pstDayKey();
                String last = currentData != null ? currentData.child("lastStreakDate").getValue(String.class) : null;
                if (today.equals(last)) {
                    Toast.makeText(context, R.string.toast_checked_in_today, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static long readMutableLong(@Nullable MutableData node) {
        if (node == null) {
            return 0L;
        }
        Object value = node.getValue();
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return Math.round((Double) value);
        }
        return 0L;
    }

    private static String pstDayKey() {
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayKey.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        return dayKey.format(Calendar.getInstance().getTime());
    }

    private static int dayGapPst(@Nullable String lastDay, @NonNull String todayDay) {
        if (lastDay == null || lastDay.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
            fmt.setLenient(false);
            fmt.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            long lastMs = fmt.parse(lastDay).getTime();
            long todayMs = fmt.parse(todayDay).getTime();
            long days = (todayMs - lastMs) / (24L * 60L * 60L * 1000L);
            return (int) days;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private static class CurrentlyReadingBook {
        String bookId;
        String title;
        String author;
        String description;
        String category;
        String coverUrl;
    }

    private static class CurrentlyReadingAdapter extends RecyclerView.Adapter<CurrentlyReadingAdapter.BookVH> {

        interface OnBookClickListener {
            void onBookClick(CurrentlyReadingBook book);
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
                    listener.onBookClick(b);
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
            final View tvContinue;

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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
            float density = parent.getContext().getResources().getDisplayMetrics().density;
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    (int) (118 * density),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int hMargin = (int) (6 * density);
            lp.setMargins(hMargin, 0, hMargin, 0);
            v.setLayoutParams(lp);
            return new BadgeVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BadgeVH holder, int position) {
            BadgeRules.BadgeRow row = rows.get(position);
            holder.tvName.setText(row.name);
            holder.ivCheck.setVisibility(View.GONE);

            holder.ivIcon.setImageTintList(null);
            holder.ivIcon.setBackgroundTintList(null);
            holder.ivIcon.setImageResource(BadgeRules.badgeDrawableRes(position, row.unlocked));

            if (row.unlocked) {
                holder.itemView.setAlpha(1f);
                holder.tvStatus.setText(R.string.badge_status_unlocked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            } else {
                holder.itemView.setAlpha(1f);
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
            final ImageView ivCheck;
            final TextView tvName;
            final TextView tvStatus;

            BadgeVH(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivBadgeIcon);
                ivCheck = itemView.findViewById(R.id.ivBadgeCheck);
                tvName = itemView.findViewById(R.id.tvBadgeName);
                tvStatus = itemView.findViewById(R.id.tvBadgeStatus);
            }
        }
    }
}
