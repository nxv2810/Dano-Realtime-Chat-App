package com.dano.chatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messageList;

    public MessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.textSender.setText(message.getSender());
        holder.textMessage.setText(message.getMessage());

        if (message.getSenderPhotoUrl() != null && !message.getSenderPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(message.getSenderPhotoUrl())
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .into(holder.imageProfile);
        } else {
            holder.imageProfile.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textSender, textMessage;
        ImageView imageProfile;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.text_sender);
            textMessage = itemView.findViewById(R.id.text_message);
            imageProfile = itemView.findViewById(R.id.image_message_profile);
        }
    }
}
