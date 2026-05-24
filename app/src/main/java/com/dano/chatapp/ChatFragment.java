package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class ChatFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getUid();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initViews(view);
        loadUserData();
        loadActiveUsers();
        loadRecentChats();

        return view;
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.img_main_profile_fragment);
        recyclerActiveUsers = view.findViewById(R.id.recycler_active_users);
        recyclerRecentChats = view.findViewById(R.id.recycler_recent_chats);

        activeUserList = new ArrayList<>();
        activeUserAdapter = new HorizontalUserAdapter(activeUserList, user -> {
            if (user.getUid() != null) {
                openChat(user.getUid(), user.getName());
            }
        });
        recyclerActiveUsers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerActiveUsers.setAdapter(activeUserAdapter);

        recentChatList = new ArrayList<>();
        recentChatAdapter = new RecentChatAdapter(recentChatList, 
            chat -> {
                if (chat != null && chat.getUserId() != null) {
                    openChat(chat.getUserId(), chat.getName());
                }
            },
            chat -> showDeleteChatDialog(chat));
        recyclerRecentChats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRecentChats.setAdapter(recentChatAdapter);

        view.findViewById(R.id.img_menu).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Menu", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        if (currentUserId == null) return;
        mDatabase.child("users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && isAdded()) {
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        Glide.with(ChatFragment.this)
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
                if (user != null && user.getUid() != null && isAdded()) {
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
            if (isAdded()) recentChatAdapter.notifyDataSetChanged();
            return;
        }
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
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
        new AlertDialog.Builder(requireContext())
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
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        startActivity(intent);
    }
}
