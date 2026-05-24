package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private EditText editSearch;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> fullUserList; // Danh sách gốc để lọc
    private DatabaseReference mDatabase, mFriendsDatabase;
    private String currentUserId;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Firebase Setup
        currentUserId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("users");
        mFriendsDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("friends");

        // View Binding
        recyclerUsers = findViewById(R.id.recycler_users);
        editSearch = findViewById(R.id.edit_search_users);
        
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        fullUserList = new ArrayList<>();
        
        adapter = new UserAdapter(userList, user -> {
            // Khi nhấn vào item -> Mở chat
            Intent intent = new Intent(UsersActivity.this, ChatActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("userName", user.getName());
            startActivity(intent);
        }, user -> {
            // Khi nhấn nút "Thêm bạn"
            addFriend(user);
        });
        recyclerUsers.setAdapter(adapter);

        fetchUsers();

        // Logic Tìm kiếm
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void addFriend(User user) {
        if (currentUserId == null || user.getUid() == null) return;

        // Lưu quan hệ bạn bè 2 chiều
        mFriendsDatabase.child(currentUserId).child(user.getUid()).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mFriendsDatabase.child(user.getUid()).child(currentUserId).setValue(true);
                        Toast.makeText(UsersActivity.this, "Đã thêm " + user.getName() + " vào danh sách bạn bè", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UsersActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullUserList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getUid() != null && !user.getUid().equals(currentUserId)) {
                        fullUserList.add(user);
                    }
                }
                // Ban đầu hiển thị tất cả
                userList.clear();
                userList.addAll(fullUserList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UsersActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchUsers(String query) {
        userList.clear();
        if (query.isEmpty()) {
            userList.addAll(fullUserList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : fullUserList) {
                // Tìm kiếm theo tên, email hoặc số điện thoại
                boolean matchesName = user.getName() != null && user.getName().toLowerCase().contains(lowerCaseQuery);
                boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseQuery);
                boolean matchesPhone = user.getPhone() != null && user.getPhone().contains(query);

                if (matchesName || matchesEmail || matchesPhone) {
                    userList.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
