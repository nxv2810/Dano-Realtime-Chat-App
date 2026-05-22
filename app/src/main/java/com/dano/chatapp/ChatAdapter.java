package com.dano.chatapp;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<ChatMessage> chatMessages;
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }

    public ChatAdapter(List<ChatMessage> chatMessages, OnMessageLongClickListener longClickListener) {
        this.chatMessages = chatMessages;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        if (message != null && message.getSender() != null && message.getSender().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (message == null) return;

        String time = "";
        try {
            time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.getTimestamp()));
        } catch (Exception e) {
            time = "00:00";
        }

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            sentHolder.textTime.setText(time);
            
            // Trạng thái tin nhắn
            if (message.isSeen()) {
                sentHolder.imgStatus.setImageResource(R.drawable.ic_check); 
                sentHolder.imgStatus.setColorFilter(ContextCompat.getColor(sentHolder.itemView.getContext(), R.color.primary));
            } else {
                sentHolder.imgStatus.setImageResource(R.drawable.ic_check);
                sentHolder.imgStatus.setColorFilter(ContextCompat.getColor(sentHolder.itemView.getContext(), R.color.neutral));
            }

            displayImage(sentHolder.imgSent, message.getImageUrl());

            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                sentHolder.textMessage.setVisibility(View.VISIBLE);
                sentHolder.textMessage.setText(message.getMessage());
            } else {
                sentHolder.textMessage.setVisibility(View.GONE);
            }

            sentHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message, position);
                }
                return true;
            });
            
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            receivedHolder.textTime.setText(time);

            displayImage(receivedHolder.imgReceived, message.getImageUrl());

            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                receivedHolder.textMessage.setVisibility(View.VISIBLE);
                receivedHolder.textMessage.setText(message.getMessage());
            } else {
                receivedHolder.textMessage.setVisibility(View.GONE);
            }

            receivedHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message, position);
                }
                return true;
            });
        }
    }

    // Hàm xử lý hiển thị ảnh (Hỗ trợ cả URL và Base64)
    private void displayImage(ImageView imageView, String imageSource) {
        if (imageSource != null && !imageSource.isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            
            if (imageSource.startsWith("http")) {
                // Nếu là URL (Firebase Storage cũ)
                Glide.with(imageView.getContext())
                        .load(imageSource)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            } else {
                // Nếu là chuỗi Base64 (Cách mới không tốn phí)
                try {
                    byte[] imageBytes = Base64.decode(imageSource, Base64.DEFAULT);
                    Glide.with(imageView.getContext())
                            .asBitmap()
                            .load(imageBytes)
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // Không cần cache vì đã có trong DB
                            .into(imageView);
                } catch (Exception e) {
                    imageView.setVisibility(View.GONE);
                }
            }
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imgSent, imgStatus;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message_sent);
            textTime = itemView.findViewById(R.id.text_time_sent);
            imgSent = itemView.findViewById(R.id.img_sent);
            imgStatus = itemView.findViewById(R.id.img_status);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imgReceived;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message_received);
            textTime = itemView.findViewById(R.id.text_time_received);
            imgReceived = itemView.findViewById(R.id.img_received);
        }
    }
}
