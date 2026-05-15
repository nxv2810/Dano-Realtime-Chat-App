package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private UserAdapter adapter;
    private List<User> chatUserList;
    private List<String> chatIds;

    private FirebaseAuth mAuth;
    private DatabaseReference chatListRef;

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

        String currentUserId = mAuth.getUid();
        chatListRef = FirebaseDatabase.getInstance().getReference("chatlist").child(currentUserId);

        recyclerMessages = findViewById(R.id.recycler_messages);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));

        chatUserList = new ArrayList<>();
        chatIds = new ArrayList<>();
        
        adapter = new UserAdapter(chatUserList, user -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("userId", user.getUid());
            intent.putExtra("userName", user.getName());
            startActivity(intent);
        });
        recyclerMessages.setAdapter(adapter);

        FloatingActionButton fabNewChat = findViewById(R.id.fab_new_chat);
        fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        });

        loadChatList();
    }

    private void loadChatList() {
        chatListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.child("id").getValue(String.class);
                    if (id != null) {
                        chatIds.add(id);
                    }
                }
                fetchUserDetails();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void fetchUserDetails() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatUserList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        for (String id : chatIds) {
                            if (user.getUid().equals(id)) {
                                chatUserList.add(user);
                                break;
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.menu_profile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
