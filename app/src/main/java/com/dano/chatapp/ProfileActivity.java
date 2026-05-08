package com.dano.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imageProfile;
    private EditText editDisplayName;
    private Button buttonChooseImage, buttonSaveProfile;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageProfile.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        imageProfile = findViewById(R.id.image_profile);
        editDisplayName = findViewById(R.id.edit_display_name);
        buttonChooseImage = findViewById(R.id.button_choose_image);
        buttonSaveProfile = findViewById(R.id.button_save_profile);
        progressBar = findViewById(R.id.progress_bar);

        if (user != null) {
            editDisplayName.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(imageProfile);
            }
        }

        buttonChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String displayName = editDisplayName.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveProfile.setEnabled(false);

        if (selectedImageUri != null) {
            // Upload ảnh lên Firebase Storage
            StorageReference storageRef = mStorage.getReference().child("avatars/" + user.getUid() + ".jpg");
            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfile(displayName, uri);
                    }))
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        buttonSaveProfile.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateProfile(displayName, user.getPhotoUrl());
        }
    }

    private void updateProfile(String displayName, Uri photoUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSaveProfile.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
