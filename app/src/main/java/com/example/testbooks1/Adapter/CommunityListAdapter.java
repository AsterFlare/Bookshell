package com.example.testbooks1.Adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.CommunityList;
import com.example.testbooks1.R;

import java.util.ArrayList;

public class CommunityListAdapter extends RecyclerView.Adapter<CommunityListAdapter.ViewHolder> {
    Context context;
    ArrayList<CommunityList> lists;

    public CommunityListAdapter(Context context, ArrayList<CommunityList> lists) {
        this.context = context;
        this.lists = lists;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvListTitle;
        RecyclerView rvBooksInList;

        public ViewHolder(View itemView) {
            super(itemView);
            tvListTitle = itemView.findViewById(R.id.tvListTitle);
            rvBooksInList = itemView.findViewById(R.id.rvBooksInList);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityList list = lists.get(position);
        holder.tvListTitle.setText(list.listTitle);

        CommunityBookAdapter adapter = new CommunityBookAdapter(context, list.books);
        holder.rvBooksInList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.rvBooksInList.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }
}