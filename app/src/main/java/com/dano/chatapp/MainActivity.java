package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private Button buttonSend;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

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

        databaseReference = FirebaseDatabase.getInstance().getReference("messages");

        recyclerMessages = findViewById(R.id.recycler_messages);
        editMessage = findViewById(R.id.edit_message);
        buttonSend = findViewById(R.id.button_send);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        buttonSend.setOnClickListener(v -> sendMessage());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    messageList.add(message);
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMessage() {
        String messageText = editMessage.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();
        
        if (!TextUtils.isEmpty(messageText) && user != null) {
            String messageId = databaseReference.push().getKey();
            
            String senderName = user.getDisplayName();
            if (TextUtils.isEmpty(senderName)) {
                senderName = user.getEmail();
            }
            
            String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
            
            ChatMessage message = new ChatMessage(senderName, messageText, System.currentTimeMillis(), photoUrl);
            
            if (messageId != null) {
                databaseReference.child(messageId).setValue(message);
            }
            editMessage.setText("");
        }
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
