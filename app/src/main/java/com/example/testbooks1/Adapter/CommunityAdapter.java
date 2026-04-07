package com.example.testbooks1.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.CommunityItem;
import com.example.testbooks1.R;
import com.example.testbooks1.ListDetailActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

    Context context;
    ArrayList<CommunityItem> items;

    public CommunityAdapter(Context context, ArrayList<CommunityItem> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgBook;
        TextView txtFullName, txtListTitle, txtListDescription;
        ImageView btnHeart;
        TextView tvHeartCount, tvCommentCount;

        public ViewHolder(View itemView) {
            super(itemView);
            imgBook = itemView.findViewById(R.id.imgBook);
            txtFullName = itemView.findViewById(R.id.txtUserFullName);
            //txtListTitle = itemView.findViewById(R.id.txtListTitle);
            txtListDescription = itemView.findViewById(R.id.txtListDescription);
            btnHeart = itemView.findViewById(R.id.btnHeart);
            tvHeartCount = itemView.findViewById(R.id.tvHeartCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    /*
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityItem item = items.get(position);
        String currentUserId = user.getUid();

        DatabaseReference listRef = db.child("communityLists")
                .child(item.userId)
                .child(item.listId);

        DatabaseReference reactRef = listRef.child("reactions").child(currentUserId);

        holder.txtFullName.setText(item.fullName);
        //holder.txtListTitle.setText(item.listTitle);
        holder.txtListDescription.setText(item.listDescription);
        Glide.with(context).load(item.firstBookImage).into(holder.imgBook);

        holder.itemView.setOnClickListener(v -> {
            // Open list detail activity
            Intent intent = new Intent(context, ListDetailActivity.class);
            intent.putExtra("userId", item.userId);
            intent.putExtra("listId", item.listId);
            intent.putExtra("listTitle", item.listTitle);
            intent.putExtra("listDescription", item.listDescription);
            intent.putParcelableArrayListExtra("books", item.books);
            context.startActivity(intent);
        });
    }\

     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityItem item = items.get(position);
        String currentUserId = user.getUid();

        DatabaseReference listRef = db.child("communityLists")
                .child(item.userId)
                .child(item.listId);

        DatabaseReference reactRef = listRef.child("reactions").child(currentUserId);

        holder.txtFullName.setText(item.fullName);
        // holder.txtListTitle.setText(item.listTitle);
        holder.txtListDescription.setText(item.listDescription);

        //Glide.with(context).load(item.firstBookImage).into(holder.imgBook);

        if (item.coverImage != null && !item.coverImage.isEmpty()) {
            try {
                byte[] decodedBytes = android.util.Base64.decode(item.coverImage, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imgBook.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Glide.with(context).load(item.firstBookImage).into(holder.imgBook);
            }
        } else {
            Glide.with(context).load(item.firstBookImage).into(holder.imgBook);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListDetailActivity.class);
            intent.putExtra("userId", item.userId);
            intent.putExtra("listId", item.listId);
            intent.putExtra("listTitle", item.listTitle);
            intent.putExtra("listDescription", item.listDescription);
            intent.putParcelableArrayListExtra("books", item.books);
            context.startActivity(intent);
        });

        listRef.child("reactionCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer count = snapshot.getValue(Integer.class);
                holder.tvHeartCount.setText(String.valueOf(count != null ? count : 0));
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        holder.tvCommentCount.setText(String.valueOf(item.commentCount));

        reactRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    holder.btnHeart.setImageResource(R.drawable.ic_heart_filled);
                } else {
                    holder.btnHeart.setImageResource(R.drawable.ic_community_heart);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        holder.btnHeart.setOnClickListener(v -> {
            reactRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        reactRef.removeValue();
                        listRef.child("reactionCount").setValue(ServerValue.increment(-1));
                    } else {
                        reactRef.setValue(true);
                        listRef.child("reactionCount").setValue(ServerValue.increment(1));
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}