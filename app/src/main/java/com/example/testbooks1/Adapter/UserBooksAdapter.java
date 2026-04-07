package com.example.testbooks1.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.CommunityBook;
import com.example.testbooks1.R;

import java.util.ArrayList;
import java.util.HashSet;

public class UserBooksAdapter extends RecyclerView.Adapter<UserBooksAdapter.ViewHolder> {

    Context context;
    ArrayList<CommunityBook> books;
    HashSet<String> selectedBookIds;
    OnBookSelectListener listener;

    public UserBooksAdapter(Context context,
                            ArrayList<CommunityBook> books,
                            HashSet<String> selectedBookIds,
                            OnBookSelectListener listener) {
        this.context = context;
        this.books = books;
        this.selectedBookIds = selectedBookIds;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityBook book = books.get(position);

        holder.tvTitle.setText(book.title != null ? book.title : book.bookTitle);
        holder.tvAuthor.setText(book.author);
        Glide.with(context).load(book.image).into(holder.ivBook);

        holder.checkIcon.setVisibility(selectedBookIds.contains(book.bookId) ? View.VISIBLE : View.GONE);

        /*
        holder.itemView.setOnClickListener(v -> {
            boolean isSelected = selectedBookIds.contains(book.bookId);

            if (isSelected) {
                selectedBookIds.remove(book.bookId);
                if (listener != null) listener.onBookSelected(book, false);
            } else {
                selectedBookIds.add(book.bookId);
                if (listener != null) listener.onBookSelected(book, true);
            }
            notifyItemChanged(position);
        });
         */
        holder.itemView.setOnClickListener(v -> {
            boolean isSelected = selectedBookIds.contains(book.bookId);

            // Prevent deselecting first book
            if (book == books.get(0) && isSelected) {
                //Toast.makeText(context, "First book must always be selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isSelected) {
                selectedBookIds.remove(book.bookId);
                if (listener != null) listener.onBookSelected(book, false);
            } else {
                selectedBookIds.add(book.bookId);
                if (listener != null) listener.onBookSelected(book, true);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBook, checkIcon;
        TextView tvTitle, tvAuthor;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivBook);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }

    public interface OnBookSelectListener {
        void onBookSelected(CommunityBook book, boolean isSelected);
    }
}