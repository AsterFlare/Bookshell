package com.example.testbooks1;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
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

public class BadgesActivity extends AppCompatActivity {

    private TextView tvLevelName;
    private TextView tvBadgesEarned;
    private ProgressBar progressBadges;
    private RecyclerView recyclerBadges;
    private BadgesAdapter adapter;

    private DatabaseReference mDatabase;
    private String userId;

    private final ArrayList<BadgeRules.BadgeRow> badgeRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvLevelName = findViewById(R.id.tvLevelName);
        tvBadgesEarned = findViewById(R.id.tvBadgesEarned);
        progressBadges = findViewById(R.id.progressBadges);
        recyclerBadges = findViewById(R.id.recyclerBadges);

        recyclerBadges.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BadgesAdapter(badgeRows);
        recyclerBadges.setAdapter(adapter);

        progressBadges.setMax(BadgeRules.TOTAL_BADGES);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        listenToStats();
    }

    private void listenToStats() {
        mDatabase.child("users").child(userId).child("stats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long completed = BadgeRules.readStatLong(snapshot, "completed");
                long reviews = BadgeRules.readStatLong(snapshot, "reviews");
                long readingLists = BadgeRules.readStatLong(snapshot, "readingLists");

                int unlockedCount = BadgeRules.countUnlocked(completed, reviews, readingLists);

                tvLevelName.setText(BadgeRules.levelNameForUnlockedCount(BadgesActivity.this, unlockedCount));
                tvBadgesEarned.setText(getString(R.string.badges_earned_status_format, unlockedCount, BadgeRules.TOTAL_BADGES));
                progressBadges.setProgress(unlockedCount);

                badgeRows.clear();
                badgeRows.addAll(BadgeRules.badgeRowsFromStats(BadgesActivity.this, completed, reviews, readingLists));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BadgesActivity.this, R.string.toast_error_badges, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.VH> {

        private final List<BadgeRules.BadgeRow> rows;

        BadgesAdapter(List<BadgeRules.BadgeRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge_grid, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BadgeRules.BadgeRow row = rows.get(position);
            float density = holder.itemView.getResources().getDisplayMetrics().density;

            holder.tvName.setText(row.name);

            int tint = row.unlocked ? row.accentColorRes : R.color.badge_icon_locked;
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.ivIcon.getContext(), tint)));

            if (row.unlocked) {
                holder.card.setAlpha(1f);
                holder.card.setStrokeWidth(0);
                holder.card.setCardElevation(4f * density);
                holder.ivCheck.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(R.string.badge_status_unlocked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            } else {
                holder.card.setAlpha(0.72f);
                holder.card.setStrokeWidth(Math.round(1f * density));
                holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_card_stroke));
                holder.card.setCardElevation(1f * density);
                holder.ivCheck.setVisibility(View.GONE);
                holder.tvStatus.setText(R.string.badge_status_locked);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ShapeableImageView ivIcon;
            final ImageView ivCheck;
            final TextView tvName;
            final TextView tvStatus;

            VH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.badgeCard);
                ivIcon = itemView.findViewById(R.id.ivBadgeIcon);
                ivCheck = itemView.findViewById(R.id.ivBadgeCheck);
                tvName = itemView.findViewById(R.id.tvBadgeName);
                tvStatus = itemView.findViewById(R.id.tvBadgeStatus);
            }
        }
    }
}
