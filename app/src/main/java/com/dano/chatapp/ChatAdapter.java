package com.dano.chatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
            
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                sentHolder.imgSent.setVisibility(View.VISIBLE);
                // Tối ưu hóa Glide để tránh lag và glitch
                Glide.with(sentHolder.itemView.getContext())
                        .load(message.getImageUrl())
                        .override(600, 600)
                        .centerInside()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(sentHolder.imgSent);
            } else {
                sentHolder.imgSent.setVisibility(View.GONE);
            }

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

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                receivedHolder.imgReceived.setVisibility(View.VISIBLE);
                // Tối ưu hóa Glide để tránh lag và glitch
                Glide.with(receivedHolder.itemView.getContext())
                        .load(message.getImageUrl())
                        .override(600, 600)
                        .centerInside()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(receivedHolder.imgReceived);
            } else {
                receivedHolder.imgReceived.setVisibility(View.GONE);
            }

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

    @Override
    public int getItemCount() {
        return chatMessages != null ? chatMessages.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imgSent;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message_sent);
            textTime = itemView.findViewById(R.id.text_time_sent);
            imgSent = itemView.findViewById(R.id.img_sent);
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
