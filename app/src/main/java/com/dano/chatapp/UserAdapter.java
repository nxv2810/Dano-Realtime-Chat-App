package com.dano.chatapp;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;
    private OnAddFriendClickListener addFriendListener;
    private Map<String, String> userStates = new HashMap<>(); // uid -> state (none, sent, received, friends)

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnAddFriendClickListener {
        void onAddFriendClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener, OnAddFriendClickListener addFriendListener) {
        this.userList = userList;
        this.listener = listener;
        this.addFriendListener = addFriendListener;
    }

    public void setUserStates(Map<String, String> states) {
        this.userStates = states;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());

        displayAvatar(holder.imgProfile, user.getProfileImage());

        String state = userStates.getOrDefault(user.getUid(), "none");
        
        switch (state) {
            case "friends":
                holder.btnAddFriend.setText(R.string.friends_label);
                holder.btnAddFriend.setEnabled(false);
                holder.btnAddFriend.setAlpha(0.5f);
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onUserClick(user);
                });
                break;
            case "sent":
                holder.btnAddFriend.setText(R.string.request_sent);
                holder.btnAddFriend.setEnabled(false);
                holder.btnAddFriend.setAlpha(0.7f);
                holder.itemView.setOnClickListener(null);
                break;
            case "received":
                holder.btnAddFriend.setText(R.string.accept_friend);
                holder.btnAddFriend.setEnabled(true);
                holder.btnAddFriend.setAlpha(1.0f);
                holder.itemView.setOnClickListener(null);
                break;
            default: // none
                holder.btnAddFriend.setText(R.string.add_friend);
                holder.btnAddFriend.setEnabled(true);
                holder.btnAddFriend.setAlpha(1.0f);
                holder.itemView.setOnClickListener(null);
                break;
        }

        holder.btnAddFriend.setOnClickListener(v -> {
            if (addFriendListener != null) {
                addFriendListener.onAddFriendClick(user);
            }
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
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView textName, textEmail;
        Button btnAddFriend;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_user_profile);
            textName = itemView.findViewById(R.id.text_user_name);
            textEmail = itemView.findViewById(R.id.text_user_email);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}
