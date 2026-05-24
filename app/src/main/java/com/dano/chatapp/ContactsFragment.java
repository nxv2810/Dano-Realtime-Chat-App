package com.dano.chatapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private EditText editSearch;
    private View btnSearch;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> allGlobalUsers;
    private DatabaseReference mUserDatabase, mFriendsDatabase, mRequestDatabase;
    private String currentUserId;
    private String currentSearchQuery = "";
    
    private Map<String, String> userStates = new HashMap<>();
    private Map<String, String> friendStates = new HashMap<>();
    private Map<String, String> requestStates = new HashMap<>();

    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        currentUserId = FirebaseAuth.getInstance().getUid();
        mUserDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("users");
        mFriendsDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("friends");
        mRequestDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("friend_requests");

        recyclerUsers = view.findViewById(R.id.recycler_users);
        editSearch = view.findViewById(R.id.edit_search_users);
        btnSearch = view.findViewById(R.id.img_search_button);
        
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        allGlobalUsers = new ArrayList<>();
        
        adapter = new UserAdapter(userList, user -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("userName", user.getName());
            startActivity(intent);
        }, this::handleFriendAction);
        
        recyclerUsers.setAdapter(adapter);

        fetchAllUsers();
        listenToRelationships();

        // Xử lý khi nhấn nút tìm kiếm (hình mũi tên/gửi)
        btnSearch.setOnClickListener(v -> performSearch());

        // Xử lý khi nhấn Search trên bàn phím
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Vẫn giữ TextWatcher để tự động quay về danh sách bạn bè khi xóa trắng ô tìm kiếm
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    currentSearchQuery = "";
                    updateDisplayList("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void performSearch() {
        currentSearchQuery = editSearch.getText().toString().trim();
        updateDisplayList(currentSearchQuery);
        
        // Ẩn bàn phím sau khi tìm kiếm
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        }
        
        if (currentSearchQuery.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tên để tìm kiếm", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenToRelationships() {
        if (currentUserId == null) return;

        mFriendsDatabase.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendStates.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    friendStates.put(ds.getKey(), "friends");
                }
                combineAndRefresh();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRequestDatabase.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestStates.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    requestStates.put(ds.getKey(), ds.getValue(String.class));
                }
                combineAndRefresh();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void combineAndRefresh() {
        userStates.clear();
        userStates.putAll(requestStates);
        userStates.putAll(friendStates);
        
        if (adapter != null) {
            adapter.setUserStates(new HashMap<>(userStates));
        }
        updateDisplayList(currentSearchQuery);
    }

    private void handleFriendAction(User user) {
        if (currentUserId == null || user.getUid() == null) return;

        String currentState = userStates.getOrDefault(user.getUid(), "none");

        if ("none".equals(currentState)) {
            sendFriendRequest(user);
        } else if ("received".equals(currentState)) {
            acceptFriendRequest(user);
        }
    }

    private void sendFriendRequest(User user) {
        mRequestDatabase.child(currentUserId).child(user.getUid()).setValue("sent")
                .addOnSuccessListener(aVoid -> {
                    mRequestDatabase.child(user.getUid()).child(currentUserId).setValue("received")
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), getString(R.string.friend_request_sent), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void acceptFriendRequest(User user) {
        mFriendsDatabase.child(currentUserId).child(user.getUid()).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    mFriendsDatabase.child(user.getUid()).child(currentUserId).setValue(true)
                            .addOnSuccessListener(aVoid1 -> {
                                mRequestDatabase.child(currentUserId).child(user.getUid()).removeValue();
                                mRequestDatabase.child(user.getUid()).child(currentUserId).removeValue();
                                String msg = getString(R.string.now_friends, user.getName());
                                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void fetchAllUsers() {
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allGlobalUsers.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getUid() != null && !user.getUid().equals(currentUserId)) {
                        allGlobalUsers.add(user);
                    }
                }
                updateDisplayList(currentSearchQuery);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDisplayList(String query) {
        userList.clear();
        if (query.isEmpty()) {
            for (User user : allGlobalUsers) {
                if (userStates.containsKey(user.getUid())) {
                    userList.add(user);
                }
            }
        } else {
            String lowerQuery = query.toLowerCase();
            for (User user : allGlobalUsers) {
                if (user.getName().toLowerCase().contains(lowerQuery) ||
                    user.getEmail().toLowerCase().contains(lowerQuery)) {
                    userList.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
