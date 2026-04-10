package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Adapter.UserBooksAdapter;
import com.example.testbooks1.BadgeRules;
import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.CommunityBook;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class LibraryActivity extends AppCompatActivity {

    private static final String STREAK_ZONE_ID = "Asia/Manila";

    BottomNavigationView bottomNav;
    private Context c;

    private View currentlyReadingCard, currentlyReadingHeader, weeklyProgressCard;
    private MaterialButton btnAll, btnCurrentlyReadingTab;
    private ImageView ivCurrentBookCover;
    private TextView tvCurrentBookTitle, tvCurrentBookAuthor, tvCurrentBookDescription;
    private MaterialButton btnContinueReading;
    
    private RecyclerView rvWantToRead;
    private UserBooksAdapter wantToReadAdapter;
    private ArrayList<CommunityBook> wantToReadList;
    private TextView tvWantToReadEmpty;
    private View readShelfHeader;
    private RecyclerView rvRead;
    private UserBooksAdapter readAdapter;
    private ArrayList<CommunityBook> readList;
    private TextView tvReadEmpty;
    private TextView tvLibraryStreakBanner;
    private ValueEventListener libraryStatsListener;
    private View[] weeklyDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_library);
        c = this;
        
        initViews();
        initialize();
        loadLibraryData();
        listenLibraryStreak();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        currentlyReadingCard = findViewById(R.id.currentlyReadingCard);
        currentlyReadingHeader = findViewById(R.id.currentlyReadingHeader);

        ivCurrentBookCover = findViewById(R.id.ivCurrentBookCover);
        tvCurrentBookTitle = findViewById(R.id.tvCurrentBookTitle);
        tvCurrentBookAuthor = findViewById(R.id.tvCurrentBookAuthor);
        tvCurrentBookDescription = findViewById(R.id.tvCurrentBookDescription);
        btnContinueReading = findViewById(R.id.btnContinueReading);

        btnAll = findViewById(R.id.btn_all);
        btnCurrentlyReadingTab = findViewById(R.id.btn_currently_reading_tab);
        rvWantToRead = findViewById(R.id.rvWantToRead);
        tvWantToReadEmpty = findViewById(R.id.tvWantToReadEmpty);
        readShelfHeader = findViewById(R.id.readShelfHeader);
        rvRead = findViewById(R.id.rvRead);
        tvReadEmpty = findViewById(R.id.tvReadEmpty);
        tvLibraryStreakBanner = findViewById(R.id.tvReadingStreak);
        weeklyDots = new View[]{
                findViewById(R.id.weekly_dot_0),
                findViewById(R.id.weekly_dot_1),
                findViewById(R.id.weekly_dot_2),
                findViewById(R.id.weekly_dot_3),
                findViewById(R.id.weekly_dot_4),
                findViewById(R.id.weekly_dot_5),
                findViewById(R.id.weekly_dot_6),
        };

        btnAll.setOnClickListener(v -> updateTabUI(true));
        btnCurrentlyReadingTab.setOnClickListener(v -> updateTabUI(false));

        wantToReadList = new ArrayList<>();
        wantToReadAdapter = new UserBooksAdapter(c, wantToReadList, new HashSet<>(), null);
        rvWantToRead.setLayoutManager(new GridLayoutManager(this, 2));
        rvWantToRead.setAdapter(wantToReadAdapter);
        applyWantToReadEmptyState();

        readList = new ArrayList<>();
        readAdapter = new UserBooksAdapter(c, readList, new HashSet<>(), null);
        rvRead.setLayoutManager(new GridLayoutManager(this, 2));
        rvRead.setAdapter(readAdapter);
        applyReadEmptyState();
    }

    private void updateTabUI(boolean isAll) {
        View wantToReadHeader = findViewById(R.id.wantToReadHeader);
        if (isAll) {
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.navyBlue)));
            btnAll.setTextColor(getColor(R.color.white));
            btnCurrentlyReadingTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.white)));
            btnCurrentlyReadingTab.setTextColor(getColor(R.color.darkGray));

            currentlyReadingHeader.setVisibility(currentlyReadingCard.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
            wantToReadHeader.setVisibility(View.VISIBLE);
            readShelfHeader.setVisibility(View.VISIBLE);
            applyWantToReadEmptyState();
            applyReadEmptyState();
        } else {
            btnCurrentlyReadingTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.navyBlue)));
            btnCurrentlyReadingTab.setTextColor(getColor(R.color.white));
            btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.white)));
            btnAll.setTextColor(getColor(R.color.darkGray));

            rvWantToRead.setVisibility(View.GONE);
            wantToReadHeader.setVisibility(View.GONE);
            if (tvWantToReadEmpty != null) {
                tvWantToReadEmpty.setVisibility(View.GONE);
            }
            readShelfHeader.setVisibility(View.GONE);
            rvRead.setVisibility(View.GONE);
            if (tvReadEmpty != null) {
                tvReadEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void applyWantToReadEmptyState() {
        if (tvWantToReadEmpty == null || rvWantToRead == null || wantToReadList == null) {
            return;
        }
        boolean empty = wantToReadList.isEmpty();
        tvWantToReadEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvWantToRead.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void applyReadEmptyState() {
        if (tvReadEmpty == null || rvRead == null || readList == null) {
            return;
        }
        boolean empty = readList.isEmpty();
        tvReadEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvRead.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void loadLibraryData() {
        String uid = AuthManager.getUid();
        if (uid == null) return;

        DatabaseReference userBooksRef = FirebaseDatabase.getInstance().getReference("user_books").child(uid);

        userBooksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                wantToReadList.clear();
                readList.clear();
                boolean currentReadingFound = false;

                for (DataSnapshot bookSnapshot : snapshot.getChildren()) {
                    String status = bookSnapshot.child("status").getValue(String.class);
                    String title = bookSnapshot.child("title").getValue(String.class);
                    String author = bookSnapshot.child("author").getValue(String.class);
                    String imageUrl = bookSnapshot.child("imageUrl").getValue(String.class);
                    String description = bookSnapshot.child("description").getValue(String.class);
                    String category = bookSnapshot.child("category").getValue(String.class);
                    String bookId = bookSnapshot.getKey();

                    CommunityBook book = new CommunityBook(bookId, title, author, imageUrl, category, description, "");

                    if ("Currently Reading".equals(status) && !currentReadingFound) {
                        displayCurrentlyReading(book);
                        currentReadingFound = true;
                    } else if ("Want to Read".equals(status)) {
                        wantToReadList.add(book);
                    } else if ("Read".equals(status)) {
                        readList.add(book);
                    }
                }

                if (!currentReadingFound) {
                    currentlyReadingCard.setVisibility(View.GONE);
                    currentlyReadingHeader.setVisibility(View.GONE);
                } else {
                    currentlyReadingCard.setVisibility(View.VISIBLE);
                    currentlyReadingHeader.setVisibility(View.VISIBLE);
                }

                wantToReadAdapter.notifyDataSetChanged();
                applyWantToReadEmptyState();
                readAdapter.notifyDataSetChanged();
                applyReadEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayCurrentlyReading(CommunityBook book) {
        tvCurrentBookTitle.setText(book.title);
        tvCurrentBookAuthor.setText("by " + book.author);
        tvCurrentBookDescription.setText(book.description);

        if (c != null && !isFinishing()) {
            Glide.with(c)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.rounded_image_bg)
                    .into(ivCurrentBookCover);
        }

        btnContinueReading.setOnClickListener(v -> {
            Intent intent = new Intent(c, BookDetailActivity.class);
            intent.putExtra("bookId", book.bookId);
            intent.putExtra("title", book.title);
            intent.putExtra("author", book.author);
            intent.putExtra("image", book.imageUrl);
            intent.putExtra("description", book.description);
            intent.putExtra("category", book.category);
            c.startActivity(intent);
        });
    }

    public void initialize(){
        bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_library);

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
                //startActivity(new Intent(c, LibraryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(c, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void listenLibraryStreak() {
        String uid = AuthManager.getUid();
        if (uid == null || tvLibraryStreakBanner == null) {
            return;
        }
        DatabaseReference statsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("stats");
        libraryStatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long streak = BadgeRules.readStatLong(snapshot, "readingStreak");
                String lastStreakDate = snapshot.child("lastStreakDate").getValue(String.class);
                String today = streakDayKey();
                if (streak == 1L && today.equals(lastStreakDate)) {
                    if (enforceSingleDayWeeklyForStreakOne(uid, snapshot.child("weeklyActivity"))) {
                        return;
                    }
                }
                int n = streak > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, streak);
                tvLibraryStreakBanner.setText(getResources().getQuantityString(
                        R.plurals.library_reading_streak_banner, n, n));
                applyWeeklyDotsFromStats(snapshot.child("weeklyActivity"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        statsRef.addValueEventListener(libraryStatsListener);
    }

    @Override
    protected void onDestroy() {
        String uid = AuthManager.getUid();
        if (uid != null && libraryStatsListener != null) {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("stats")
                    .removeEventListener(libraryStatsListener);
            libraryStatsListener = null;
        }
        super.onDestroy();
    }

    private void applyWeeklyDotsFromStats(@NonNull DataSnapshot weeklyActivity) {
        if (weeklyDots == null) {
            return;
        }
        boolean allZero = true;
        for (int i = 0; i < 7; i++) {
            if (BadgeRules.readStatLong(weeklyActivity, String.valueOf(i)) > 0L) {
                allZero = false;
                break;
            }
        }
        if (!weeklyActivity.exists() || allZero) {
            for (View dot : weeklyDots) {
                if (dot != null) {
                    setDotAlphaTint(dot, R.color.lightGray, 0.28f);
                }
            }
            return;
        }
        for (int d = 0; d < weeklyDots.length; d++) {
            View dot = weeklyDots[d];
            if (dot == null) {
                continue;
            }
            long val = BadgeRules.readStatLong(weeklyActivity, String.valueOf(d));
            if (val > 0L) {
                setDotAlphaTint(dot, R.color.navyBlue, 0.88f);
            } else {
                setDotAlphaTint(dot, R.color.lightGray, 0.38f);
            }
        }
    }

    private void setDotAlphaTint(View dot, int colorResId, float alpha01) {
        int base = ContextCompat.getColor(this, colorResId);
        int argb = ColorUtils.setAlphaComponent(base, Math.round(255f * alpha01));
        dot.setBackgroundTintList(ColorStateList.valueOf(argb));
    }

    private static TimeZone streakTimeZone() {
        return TimeZone.getTimeZone(STREAK_ZONE_ID);
    }

    private static String streakDayKey() {
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayKey.setTimeZone(streakTimeZone());
        return dayKey.format(Calendar.getInstance().getTime());
    }

    private static int dayOfWeekIndexStreak() {
        Calendar cal = Calendar.getInstance(streakTimeZone(), Locale.US);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return (dow + 5) % 7;
    }

    private boolean enforceSingleDayWeeklyForStreakOne(String uid, @NonNull DataSnapshot weeklyActivitySnapshot) {
        int todayIdx = dayOfWeekIndexStreak();
        boolean needsFix = false;
        for (int i = 0; i < 7; i++) {
            long v = BadgeRules.readStatLong(weeklyActivitySnapshot, String.valueOf(i));
            if (i == todayIdx) {
                if (v <= 0L) {
                    needsFix = true;
                }
            } else if (v > 0L) {
                needsFix = true;
            }
        }
        if (!needsFix) {
            return false;
        }
        DatabaseReference waRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("stats")
                .child("weeklyActivity");
        for (int i = 0; i < 7; i++) {
            waRef.child(String.valueOf(i)).setValue(i == todayIdx ? 1L : 0L);
        }
        return true;
    }

}