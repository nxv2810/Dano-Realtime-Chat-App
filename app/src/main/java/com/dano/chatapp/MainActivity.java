package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private RecyclerView recyclerActiveUsers, recyclerRecentChats;
    private RecentChatAdapter recentChatAdapter;
    private HorizontalUserAdapter activeUserAdapter;
    
    private List<RecentChat> recentChatList;
    private List<User> activeUserList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = mAuth.getUid();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initViews();
        loadUserData();
        loadActiveUsers();
        loadRecentChats();
    }

    private void initViews() {
        imgProfile = findViewById(R.id.img_main_profile);
        recyclerActiveUsers = findViewById(R.id.recycler_active_users);
        recyclerRecentChats = findViewById(R.id.recycler_recent_chats);

        // Active Users setup
        activeUserList = new ArrayList<>();
        activeUserAdapter = new HorizontalUserAdapter(activeUserList, user -> {
            openChat(user.getUid(), user.getName());
        });
        recyclerActiveUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerActiveUsers.setAdapter(activeUserAdapter);

        // Recent Chats setup
        recentChatList = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(
                recentChatList, 
                chat -> openChat(chat.getUserId(), chat.getName()),
                chat -> showDeleteChatDialog(chat)
        );
        recyclerRecentChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentChats.setAdapter(recentChatAdapter);

        findViewById(R.id.fab_new_chat).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });
        
        imgProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        findViewById(R.id.img_menu).setOnClickListener(v -> {
            // Hiển thị menu nhanh nếu cần
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteChatDialog(RecentChat chat) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa cuộc trò chuyện")
                .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện với " + chat.getName() + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteChat(chat))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteChat(RecentChat chat) {
        mDatabase.child("chatlist").child(currentUserId).child(chat.getUserId())
                .removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        mDatabase.child("users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getProfileImage() != null) {
                    Glide.with(MainActivity.this)
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.ic_person)
                            .into(imgProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadActiveUsers() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeUserList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        activeUserList.add(user);
                    }
                }
                activeUserAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRecentChats() {
        mDatabase.child("chatlist").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentChatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.child("id").getValue(String.class);
                    String lastMsg = ds.child("lastMessage").getValue(String.class);
                    Long timestamp = ds.child("timestamp").getValue(Long.class);
                    
                    if (userId != null) {
                        RecentChat recent = new RecentChat();
                        recent.setUserId(userId);
                        recent.setLastMessage(lastMsg != null ? lastMsg : "");
                        recent.setTimestamp(timestamp != null ? timestamp : 0);
                        recentChatList.add(recent);
                    }
                }
                fetchRecentChatDetails();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchRecentChatDetails() {
        if (recentChatList.isEmpty()) {
            recentChatAdapter.notifyDataSetChanged();
            return;
        }

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (RecentChat chat : recentChatList) {
                    DataSnapshot userDs = snapshot.child(chat.getUserId());
                    if (userDs.exists()) {
                        chat.setName(userDs.child("name").getValue(String.class));
                        chat.setProfileImage(userDs.child("profileImage").getValue(String.class));
                    }
                }
                
                // Sắp xếp theo thời gian mới nhất
                Collections.sort(recentChatList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                recentChatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openChat(String userId, String userName) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        startActivity(intent);
    }
}
