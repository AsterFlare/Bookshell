package com.example.testbooks1.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.testbooks1.Model.Book;
import com.example.testbooks1.BookDetailActivity;
import com.example.testbooks1.R;

import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    Context context;
    private ArrayList<Book> list;

    public BookAdapter(Context context, ArrayList<Book> list) {
        this.context = context;
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBook;
        TextView tvTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBook = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new ViewHolder(view);
    }

//    public void onBindViewHolder(ViewHolder holder, int position) {
//        Book book = list.get(position);
//
//        holder.tvTitle.setText(book.getTitle());
//
//        Glide.with(context)
//                .load(book.getImageUrl())
//                .into(holder.ivBook);
//    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Book book = list.get(position);

        holder.tvTitle.setText(book.getTitle());

        Glide.with(context)
                .load(book.getImageUrl())
                .centerCrop()
                .into(holder.ivBook);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            intent.putExtra("title", book.getTitle());
            intent.putExtra("image", book.getImageUrl());
            intent.putExtra("author", book.getAuthor());
            intent.putExtra("description", book.getDescription());
            intent.putExtra("publisher", book.getPublisher());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}