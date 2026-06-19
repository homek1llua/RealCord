package com.discordclone.friends;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discordclone.R;
import com.discordclone.models.User;
import com.discordclone.repository.FriendRepository;
import com.discordclone.repository.UserRepository;
import com.discordclone.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddFriendActivity extends AppCompatActivity {
    private EditText searchInput;
    private Button searchBtn;
    private ListView resultsList;
    private UserRepository userRepo;
    private FriendRepository friendRepo;
    private List<Map<String, String>> searchResults = new ArrayList<>();
    private List<String> resultUids = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        userRepo = new UserRepository();
        friendRepo = new FriendRepository();

        searchInput = findViewById(R.id.search_username);
        searchBtn = findViewById(R.id.search_btn);
        resultsList = findViewById(R.id.search_results);

        searchBtn.setOnClickListener(v -> search());
    }

    private void search() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepo.searchUsers(query).addOnSuccessListener(snapshot -> {
            List<User> users = userRepo.parseUsers(snapshot);
            searchResults.clear();
            resultUids.clear();

            String currentUid = FirebaseUtil.getCurrentUid();

            for (User user : users) {
                if (user.getUid().equals(currentUid)) continue;

                Map<String, String> item = new HashMap<>();
                item.put("name", user.getUsername());
                item.put("status", user.getStatus() != null ? user.getStatus() : "offline");
                searchResults.add(item);
                resultUids.add(user.getUid());
            }

            if (searchResults.isEmpty()) {
                Map<String, String> empty = new HashMap<>();
                empty.put("name", "No users found");
                empty.put("status", "");
                searchResults.add(empty);
            }

            SimpleAdapter adapter = new SimpleAdapter(
                this, searchResults,
                android.R.layout.simple_list_item_2,
                new String[]{"name", "status"},
                new int[]{android.R.id.text1, android.R.id.text2}
            );

            resultsList.setAdapter(adapter);
            resultsList.setOnItemClickListener((parent, view, position, id) -> {
                if (position < resultUids.size()) {
                    String friendUid = resultUids.get(position);
                    friendRepo.sendFriendRequest(friendUid)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Friend request sent!",
                                Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        });
                }
            });
        });
    }
}
