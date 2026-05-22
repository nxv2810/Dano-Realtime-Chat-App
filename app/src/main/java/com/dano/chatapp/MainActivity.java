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
    
    private List<User> recentChatList;
    private List<User> activeUserList; // Sửa lại kiểu dữ liệu nếu cần, đảm bảo đồng nhất

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
        // loadRecentChats(); // Tạm thời comment nếu chưa ổn định hoặc để fix sau
    }

    private void initViews() {
        imgProfile = findViewById(R.id.img_main_profile);
        recyclerActiveUsers = findViewById(R.id.recycler_active_users);
        recyclerRecentChats = findViewById(R.id.recycler_recent_chats);

        // Active Users setup
        activeUserList = new ArrayList<>();
        activeUserAdapter = new HorizontalUserAdapter(activeUserList, user -> {
            if (user.getUid() != null) {
                openChat(user.getUid(), user.getName());
            }
        });
        recyclerActiveUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerActiveUsers.setAdapter(activeUserAdapter);

        // Recent Chats setup - Giữ nguyên nhưng thêm check null trong adapter nếu cần
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
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeUserList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    // FIX: Thêm kiểm tra user.getUid() != null để tránh crash
                    if (user != null && user.getUid() != null && !user.getUid().equals(currentUserId)) {
                        activeUserList.add(user);
                    }
                }
                activeUserAdapter.notifyDataSetChanged();
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
