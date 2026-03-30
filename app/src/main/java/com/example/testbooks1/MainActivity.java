package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.testbooks1.Adapter.BookAdapter;
import com.example.testbooks1.Model.Book;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvBooks;
    private BookAdapter adapter;
    private List<Book> bookList;
    private Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void initialize(){
        etSearch = findViewById(R.id.etSearch);
        rvBooks = findViewById(R.id.rvBooks);
        bookList = new ArrayList<>();

        rvBooks.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BookAdapter(c, (ArrayList<Book>) bookList);
        rvBooks.setAdapter(adapter);
        callBooks("bestseller");
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                //startActivity(new Intent(c, HomeActivity.class));
                return true;
            } else if (id == R.id.nav_community) {
                startActivity(new Intent(c, CommunityActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(c, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    public void callBooks(String queryInput) {

        String query = queryInput.trim().replace(" ", "+");
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query + "&key=AIzaSyAycxqRNFLfOCxktkf3cDcWChAc0Cfvk4Y";

        RequestQueue r = Volley.newRequestQueue(c);

        JsonObjectRequest json = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        bookList.clear();
                        if (!response.has("items")) return;

                        JSONArray items = response.getJSONArray("items");

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject bookObj = items.getJSONObject(i);
                            JSONObject volumeInfo = bookObj.getJSONObject("volumeInfo");

                            String id = bookObj.getString("id");
                            String title = volumeInfo.getString("title");
                            String author = "Unknown";
                            if (volumeInfo.has("authors")) {
                                JSONArray authorsArray = volumeInfo.getJSONArray("authors");
                                author = authorsArray.getString(0);
                            }
                            String description = volumeInfo.optString("description", "No description available");
                            String publisher = volumeInfo.optString("publisher", "Unknown");

                            String imageUrl = "";
                            if (volumeInfo.has("imageLinks")) {
                                imageUrl = volumeInfo
                                        .getJSONObject("imageLinks")
                                        .optString("thumbnail", "");
                                if (imageUrl.startsWith("http://")) {
                                    imageUrl = imageUrl.replace("http://", "https://");
                                }
                            }
                            bookList.add(new Book(id, title, imageUrl, author, description, publisher));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(c, error.toString(), Toast.LENGTH_SHORT).show()
        );
        r.add(json);
    }
}