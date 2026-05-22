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

        // Active Users setup (Friends)
        activeUserList = new ArrayList<>();
        activeUserAdapter = new HorizontalUserAdapter(activeUserList, user -> {
            if (user.getUid() != null) {
                openChat(user.getUid(), user.getName());
            }
        });
        recyclerActiveUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerActiveUsers.setAdapter(activeUserAdapter);

        // Recent Chats setup
        recentChatList = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(recentChatList, 
            chat -> {
                if (chat != null && chat.getUserId() != null) {
                    openChat(chat.getUserId(), chat.getName());
                }
            },
            chat -> showDeleteChatDialog(chat));
        recyclerRecentChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentChats.setAdapter(recentChatAdapter);

        // Bottom Navigation Events
        findViewById(R.id.nav_chat).setOnClickListener(v -> {
            // Đã ở màn hình trò chuyện
            recyclerRecentChats.smoothScrollToPosition(0);
        });

        findViewById(R.id.nav_contacts).setOnClickListener(v -> {
            // Mở màn hình Danh bạ (Tìm kiếm người dùng)
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Other Click Events
        findViewById(R.id.fab_new_chat).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        });
        
        imgProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        findViewById(R.id.img_menu).setOnClickListener(v -> {
            Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (currentUserId == null) return;
        mDatabase.child("users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && !isFinishing()) {
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.ic_person)
                                .into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadActiveUsers() {
        if (currentUserId == null) return;
        mDatabase.child("friends").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeUserList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String friendId = ds.getKey();
                        if (friendId != null) {
                            fetchFriendInfo(friendId);
                        }
                    }
                } else {
                    activeUserAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchFriendInfo(String friendId) {
        mDatabase.child("users").child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getUid() != null) {
                    boolean exists = false;
                    for (User u : activeUserList) {
                        if (u.getUid().equals(user.getUid())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        activeUserList.add(user);
                        activeUserAdapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRecentChats() {
        if (currentUserId == null) return;
        mDatabase.child("chatlist").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentChatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    RecentChat chat = ds.getValue(RecentChat.class);
                    if (chat != null) {
                        if (chat.getUserId() == null) {
                            chat.setUserId(ds.getKey());
                        }
                        recentChatList.add(chat);
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
                    if (chat.getUserId() != null) {
                        DataSnapshot userDs = snapshot.child(chat.getUserId());
                        if (userDs.exists()) {
                            chat.setName(userDs.child("name").getValue(String.class));
                            chat.setProfileImage(userDs.child("profileImage").getValue(String.class));
                        }
                    }
                }
                Collections.sort(recentChatList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                recentChatAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDeleteChatDialog(RecentChat chat) {
        if (chat == null || chat.getUserId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xóa cuộc trò chuyện")
                .setMessage("Bạn có chắc chắn muốn xóa tin nhắn với " + (chat.getName() != null ? chat.getName() : "người này") + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mDatabase.child("chatlist").child(currentUserId).child(chat.getUserId()).removeValue();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openChat(String userId, String userName) {
        if (userId == null) return;
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        startActivity(intent);
    }
}
