package com.dano.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private ImageButton btnSend, btnAddImage;
    private Toolbar toolbar;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;

    private String receiverId, receiverName;
    private String senderId;
    private String chatId;

    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadImage(imageUri);
                }
            }
    );

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

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        mStorage = FirebaseStorage.getInstance();

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
        btnAddImage = findViewById(R.id.btn_add_image);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList, (message, position) -> {
            if (message.getSender().equals(senderId)) {
                showDeleteDialog(message);
            }
        });

        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);
    }

    private void showDeleteDialog(ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xóa tin nhắn");
        builder.setMessage("Bạn có chắc chắn muốn xóa tin nhắn này không?");
        builder.setPositiveButton("Xóa", (dialog, which) -> {
            deleteMessage(message);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteMessage(ChatMessage message) {
        if (message.getMessageId() != null) {
            mDatabase.child("chats").child(chatId).child("messages")
                    .child(message.getMessageId()).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(ChatActivity.this, "Đã xóa tin nhắn", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void setupChat() {
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(msg, null);
                editMessage.setText("");
            }
        });

        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });
    }

    private void uploadImage(Uri imageUri) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = mStorage.getReference().child("chat_images/" + chatId + "/" + fileName);

        Toast.makeText(this, "Đang gửi ảnh...", Toast.LENGTH_SHORT).show();

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    sendMessage("", uri.toString());
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage(String messageText, String imageUrl) {
        DatabaseReference chatRef = mDatabase.child("chats").child(chatId).child("messages");
        String messageId = chatRef.push().getKey();

        long timestamp = System.currentTimeMillis();
        ChatMessage chatMessage = new ChatMessage(senderId, messageText, imageUrl, timestamp, "");
        chatMessage.setMessageId(messageId);

        if (messageId != null) {
            chatRef.child(messageId).setValue(chatMessage);
            
            String displayMsg = TextUtils.isEmpty(messageText) ? "📷 Ảnh" : messageText;

            // Cập nhật thông tin chat gần đây cho cả 2 người
            Map<String, Object> chatListData = new HashMap<>();
            chatListData.put("id", receiverId);
            chatListData.put("lastMessage", displayMsg);
            chatListData.put("timestamp", timestamp);

            mDatabase.child("chatlist").child(senderId).child(receiverId).setValue(chatListData);

            Map<String, Object> chatListDataReceiver = new HashMap<>();
            chatListDataReceiver.put("id", senderId);
            chatListDataReceiver.put("lastMessage", displayMsg);
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
                            if (cm != null) {
                                cm.setMessageId(ds.getKey());
                                messageList.add(cm);
                            }
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
