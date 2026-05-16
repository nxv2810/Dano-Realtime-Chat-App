package com.dano.chatapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private ImageButton btnSend;
    private Toolbar toolbar;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    private String receiverId, receiverName;
    private String senderId;
    private String chatId;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("userName");
        senderId = FirebaseAuth.getInstance().getUid();

        if (senderId == null || receiverId == null) {
            finish();
            return;
        }

        String[] ids = {senderId, receiverId};
        Arrays.sort(ids);
        chatId = ids[0] + "_" + ids[1];

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupChat();
        readMessages();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(receiverName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerChat = findViewById(R.id.recycler_chat);
        editMessage = findViewById(R.id.edit_chat_message);
        btnSend = findViewById(R.id.btn_chat_send);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);

        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);
    }

    private void setupChat() {
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(msg);
                editMessage.setText("");
            }
        });
    }

    private void sendMessage(String messageText) {
        DatabaseReference chatRef = mDatabase.child("chats").child(chatId).child("messages");
        String messageId = chatRef.push().getKey();

        long timestamp = System.currentTimeMillis();
        ChatMessage chatMessage = new ChatMessage(senderId, messageText, timestamp, "");

        if (messageId != null) {
            chatRef.child(messageId).setValue(chatMessage);
            
            // Cập nhật thông tin chat gần đây cho cả 2 người
            Map<String, Object> chatListData = new HashMap<>();
            chatListData.put("id", receiverId);
            chatListData.put("lastMessage", messageText);
            chatListData.put("timestamp", timestamp);

            mDatabase.child("chatlist").child(senderId).child(receiverId).setValue(chatListData);

            Map<String, Object> chatListDataReceiver = new HashMap<>();
            chatListDataReceiver.put("id", senderId);
            chatListDataReceiver.put("lastMessage", messageText);
            chatListDataReceiver.put("timestamp", timestamp);

            mDatabase.child("chatlist").child(receiverId).child(senderId).setValue(chatListDataReceiver);
        }
    }

    private void readMessages() {
        mDatabase.child("chats").child(chatId).child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ChatMessage cm = ds.getValue(ChatMessage.class);
                            messageList.add(cm);
                        }
                        adapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            recyclerChat.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Không thể tải tin nhắn", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
