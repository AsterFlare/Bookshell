package com.example.testbooks1.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.CommunityBook;
import com.example.testbooks1.R;

import java.util.ArrayList;

public class CommunityBookAdapter extends RecyclerView.Adapter<CommunityBookAdapter.ViewHolder> {
    Context context;
    ArrayList<CommunityBook> books;

    public CommunityBookAdapter(Context context, ArrayList<CommunityBook> books) {
        this.context = context;
        this.books = books;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBook;
        TextView tvBookTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivBook);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityBook book = books.get(position);
        holder.tvBookTitle.setText(book.bookTitle);
        Glide.with(context).load(book.image).into(holder.ivBook);
    }

    @Override
    public int getItemCount() {
        return books.size();
    }
}