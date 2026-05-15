package com.dano.chatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class HorizontalUserAdapter extends RecyclerView.Adapter<HorizontalUserAdapter.ViewHolder> {

    private List<User> userList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public HorizontalUserAdapter(List<User> userList, OnItemClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textName.setText(user.getName());

        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImage())
                .placeholder(R.drawable.ic_person)
                .into(holder.imgAvatar);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
        
        // Cần thêm trạng thái online trong User model nếu muốn hiển thị chính xác
        holder.viewOnline.setVisibility(View.VISIBLE); 
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgAvatar;
        TextView textName;
        View viewOnline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_user_avatar);
            textName = itemView.findViewById(R.id.text_user_name_short);
            viewOnline = itemView.findViewById(R.id.view_online_indicator);
        }
    }
}
