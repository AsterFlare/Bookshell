package com.example.testbooks1;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testbooks1.Adapter.CommunityListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CommunityActivity extends AppCompatActivity {
    RecyclerView rvCommunityLists;
    ArrayList<CommunityList> communityLists;
    CommunityListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize(){
        rvCommunityLists = findViewById(R.id.rvCommunityLists);
        communityLists = new ArrayList<>();
        adapter = new CommunityListAdapter(this, communityLists);

        rvCommunityLists.setLayoutManager(new LinearLayoutManager(this));
        rvCommunityLists.setAdapter(adapter);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("communityLists");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                communityLists.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot listSnapshot : userSnapshot.getChildren()) {
                        String listTitle = listSnapshot.getKey();
                        ArrayList<CommunityBook> books = new ArrayList<>();

                        for (DataSnapshot bookSnapshot : listSnapshot.getChildren()) {
                            CommunityBook book = bookSnapshot.getValue(CommunityBook.class);
                            if (book != null) {
                                if (book.listTitle == null || book.listTitle.isEmpty()) {
                                    book.listTitle = listTitle;
                                }
                                books.add(book);
                            }
                        }

                        if (!books.isEmpty()) {
                            communityLists.add(new CommunityList(listTitle, books));
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                Toast.makeText(CommunityActivity.this, "Loaded " + communityLists.size() + " lists", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CommunityActivity.this, "Failed to load lists", Toast.LENGTH_SHORT).show();
            }
        });

    }
}