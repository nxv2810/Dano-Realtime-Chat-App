package com.dano.chatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentChatAdapter extends RecyclerView.Adapter<RecentChatAdapter.ViewHolder> {

    private List<RecentChat> recentChatList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecentChat recentChat);
    }

    public RecentChatAdapter(List<RecentChat> recentChatList, OnItemClickListener listener) {
        this.recentChatList = recentChatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentChat chat = recentChatList.get(position);
        holder.textName.setText(chat.getName());
        holder.textLastMsg.setText(chat.getLastMessage());
        
        // Format timestamp
        if (chat.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.textTime.setText(sdf.format(new Date(chat.getTimestamp())));
        }

        if (chat.getUnreadCount() > 0) {
            holder.textUnread.setVisibility(View.VISIBLE);
            holder.textUnread.setText(String.valueOf(chat.getUnreadCount()));
        } else {
            holder.textUnread.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
                .load(chat.getProfileImage())
                .placeholder(R.drawable.ic_person)
                .into(holder.imgProfile);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(chat));
    }

    @Override
    public int getItemCount() {
        return recentChatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgProfile;
        TextView textName, textLastMsg, textTime, textUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_recent_profile);
            textName = itemView.findViewById(R.id.text_recent_name);
            textLastMsg = itemView.findViewById(R.id.text_recent_last_msg);
            textTime = itemView.findViewById(R.id.text_recent_time);
            textUnread = itemView.findViewById(R.id.text_recent_unread);
        }
    }
}
