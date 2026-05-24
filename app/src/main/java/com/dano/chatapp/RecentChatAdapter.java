package com.dano.chatapp;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentChatAdapter extends RecyclerView.Adapter<RecentChatAdapter.ViewHolder> {

    private List<RecentChat> recentChatList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(RecentChat recentChat);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(RecentChat recentChat);
    }

    public RecentChatAdapter(List<RecentChat> recentChatList, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.recentChatList = recentChatList;
        this.listener = listener;
        this.longClickListener = longClickListener;
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

        displayAvatar(holder.imgProfile, chat.getProfileImage());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(chat));
        
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(chat);
            }
            return true;
        });
    }

    private void displayAvatar(ImageView imageView, String imageSource) {
        if (imageSource != null && !imageSource.isEmpty()) {
            if (imageSource.startsWith("http")) {
                // URL cũ
                Glide.with(imageView.getContext())
                        .load(imageSource)
                        .placeholder(R.drawable.ic_person)
                        .into(imageView);
            } else {
                // Base64 mới
                try {
                    byte[] imageBytes = Base64.decode(imageSource, Base64.DEFAULT);
                    Glide.with(imageView.getContext())
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(R.drawable.ic_person)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageView);
                } catch (Exception e) {
                    imageView.setImageResource(R.drawable.ic_person);
                }
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person);
        }
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
