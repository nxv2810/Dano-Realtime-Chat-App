package com.dano.chatapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private DatabaseReference messageRef;
    private ValueEventListener seenListener;

    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelection(uri);
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
        messageRef = mDatabase.child("chats").child(chatId).child("messages");

        initViews();
        setupChat();
        readMessages();
        seenMessage();
    }

    private void seenMessage() {
        seenListener = messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage chat = ds.getValue(ChatMessage.class);
                    if (chat != null && chat.getSender() != null && !chat.getSender().equals(senderId)) {
                        if (!chat.isSeen()) {
                            ds.getRef().child("seen").setValue(true);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(adapter);
    }

    private void showDeleteDialog(ChatMessage message) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tin nhắn")
                .setMessage("Bạn có muốn xóa tin nhắn này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (message.getMessageId() != null) {
                        messageRef.child(message.getMessageId()).removeValue();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupChat() {
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(msg, null);
                editMessage.setText("");
            }
        });

        btnAddImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void handleImageSelection(Uri imageUri) {
        Toast.makeText(this, "Đang gửi ảnh...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

                if (bitmap == null) return;

                // Nén ảnh xuống mức 250px (Siêu nhẹ) để lưu trực tiếp vào Database miễn phí
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratio = (float) width / 250;
                if (width > 250) {
                    width = 250;
                    height = (int) (height / ratio);
                }
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // Nén 50% chất lượng
                byte[] bytes = baos.toByteArray();
                
                String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);
                
                runOnUiThread(() -> {
                    sendMessage("", base64Image);
                });
                
            } catch (Exception e) {
                Log.e("DANO_CHAT", "Lỗi xử lý ảnh: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendMessage(String messageText, String imageUrl) {
        String messageId = messageRef.push().getKey();
        long timestamp = System.currentTimeMillis();
        
        ChatMessage chatMessage = new ChatMessage(senderId, messageText, imageUrl, timestamp, "");
        chatMessage.setMessageId(messageId);
        chatMessage.setSeen(false);

        if (messageId != null) {
            messageRef.child(messageId).setValue(chatMessage)
                .addOnSuccessListener(aVoid -> {
                    String lastMsg = TextUtils.isEmpty(messageText) ? "📷 Ảnh" : messageText;
                    updateChatList(lastMsg, timestamp);
                })
                .addOnFailureListener(e -> {
                    Log.e("DANO_CHAT", "Gửi thất bại: " + e.getMessage());
                    Toast.makeText(ChatActivity.this, "Gửi thất bại (Ảnh quá lớn?)", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void updateChatList(String lastMsg, long timestamp) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", receiverId);
        map.put("lastMessage", lastMsg);
        map.put("timestamp", timestamp);
        mDatabase.child("chatlist").child(senderId).child(receiverId).setValue(map);

        Map<String, Object> mapRev = new HashMap<>();
        mapRev.put("id", senderId);
        mapRev.put("lastMessage", lastMsg);
        mapRev.put("timestamp", timestamp);
        mDatabase.child("chatlist").child(receiverId).child(senderId).setValue(mapRev);
    }

    private void readMessages() {
        messageRef.addValueEventListener(new ValueEventListener() {
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (messageRef != null && seenListener != null) {
            messageRef.removeEventListener(seenListener);
        }
    }
}
