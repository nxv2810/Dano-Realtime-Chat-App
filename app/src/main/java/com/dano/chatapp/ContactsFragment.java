package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class ContactsFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private EditText editSearch;
    private UserAdapter adapter;
    private List<User> userList;
    private List<User> fullUserList;
    private DatabaseReference mDatabase, mFriendsDatabase;
    private String currentUserId;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        currentUserId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("users");
        mFriendsDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("friends");

        recyclerUsers = view.findViewById(R.id.recycler_users);
        editSearch = view.findViewById(R.id.edit_search_users);
        
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        fullUserList = new ArrayList<>();
        
        adapter = new UserAdapter(userList, user -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("userName", user.getName());
            startActivity(intent);
        }, this::addFriend);
        
        recyclerUsers.setAdapter(adapter);

        fetchUsers();

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

        return view;
    }

    private void addFriend(User user) {
        if (currentUserId == null || user.getUid() == null) return;

        mFriendsDatabase.child(currentUserId).child(user.getUid()).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mFriendsDatabase.child(user.getUid()).child(currentUserId).setValue(true);
                        Toast.makeText(getContext(), "Đã thêm " + user.getName() + " vào danh sách bạn bè", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUsers() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                fullUserList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getUid() != null && !user.getUid().equals(currentUserId)) {
                        fullUserList.add(user);
                    }
                }
                userList.clear();
                userList.addAll(fullUserList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void searchUsers(String query) {
        userList.clear();
        if (query.isEmpty()) {
            userList.addAll(fullUserList);
        } else {
            for (User user : fullUserList) {
                if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                    userList.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
