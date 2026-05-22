package com.dano.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView imageProfile, imgToolbarAvatar;
    private TextView textProfileName, textProfileStatus;
    private LinearLayout itemEditProfile;
    private View btnLogout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;
    private Uri selectedImageUri;
    private User currentUserModel;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String STORAGE_BUCKET_URL = "gs://chatapp-20a5f5b5.firebasestorage.app";

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageProfile.setImageURI(selectedImageUri);
                    uploadImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        // FIX: Cấu hình URL cho Storage để tránh lỗi "Object does not exist"
        mStorage = FirebaseStorage.getInstance(STORAGE_BUCKET_URL);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("users").child(currentUser.getUid());

        initViews();
        loadUserInfo();
        setupListeners();
    }

    private void initViews() {
        imageProfile = findViewById(R.id.image_profile);
        imgToolbarAvatar = findViewById(R.id.img_toolbar_avatar);
        textProfileName = findViewById(R.id.text_profile_name);
        textProfileStatus = findViewById(R.id.text_profile_status);
        itemEditProfile = findViewById(R.id.item_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserInfo() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserModel = snapshot.getValue(User.class);
                    if (currentUserModel != null) {
                        textProfileName.setText(currentUserModel.getName());
                        if (currentUserModel.getStatus() != null && !currentUserModel.getStatus().isEmpty()) {
                            textProfileStatus.setText(currentUserModel.getStatus());
                        } else {
                            textProfileStatus.setText(getString(R.string.no_status));
                        }
                        
                        if (currentUserModel.getProfileImage() != null && !currentUserModel.getProfileImage().isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(currentUserModel.getProfileImage())
                                    .placeholder(R.drawable.ic_person)
                                    .into(imageProfile);
                            Glide.with(ProfileActivity.this)
                                    .load(currentUserModel.getProfileImage())
                                    .placeholder(R.drawable.ic_person)
                                    .into(imgToolbarAvatar);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupListeners() {
        imageProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        itemEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_name);
        EditText editStatus = view.findViewById(R.id.edit_status);
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        TextView btnSave = view.findViewById(R.id.btn_save);

        if (currentUserModel != null) {
            editName.setText(currentUserModel.getName());
            editStatus.setText(currentUserModel.getStatus());
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String status = editStatus.getText().toString().trim();

            if (name.isEmpty()) {
                editName.setError(getString(R.string.name_cannot_be_empty));
                return;
            }

            updateProfileInfo(name, status);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateProfileInfo(String name, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("status", status);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, getString(R.string.update_profile_success), Toast.LENGTH_SHORT).show();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(ProfileActivity.this, getString(R.string.update_error, error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImage() {
        if (selectedImageUri != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            StorageReference storageRef = mStorage.getReference().child("avatars/" + user.getUid() + ".jpg");
            
            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        mDatabase.child("profileImage").setValue(uri.toString());
                        Toast.makeText(ProfileActivity.this, getString(R.string.update_avatar_success), Toast.LENGTH_SHORT).show();
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
