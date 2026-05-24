package com.dano.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private ShapeableImageView imageProfile;
    private TextView textProfileName, textProfileStatus;
    private TextView textMessageCount, textContactCount;
    private LinearLayout itemEditProfile;
    private View btnLogout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference mRootRef;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance(STORAGE_BUCKET_URL);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
            getActivity().finish();
            return view;
        }

        mRootRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        mDatabase = mRootRef.child("users").child(currentUser.getUid());

        initViews(view);
        loadUserInfo();
        loadStats(currentUser.getUid());
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        imageProfile = view.findViewById(R.id.image_profile);
        textProfileName = view.findViewById(R.id.text_profile_name);
        textProfileStatus = view.findViewById(R.id.text_profile_status);
        textMessageCount = view.findViewById(R.id.text_message_count);
        textContactCount = view.findViewById(R.id.text_contact_count);
        itemEditProfile = view.findViewById(R.id.item_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void loadUserInfo() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    currentUserModel = snapshot.getValue(User.class);
                    if (currentUserModel != null) {
                        textProfileName.setText(currentUserModel.getName());
                        if (currentUserModel.getStatus() != null && !currentUserModel.getStatus().isEmpty()) {
                            textProfileStatus.setText(currentUserModel.getStatus());
                        } else {
                            textProfileStatus.setText(getString(R.string.no_status));
                        }
                        
                        if (currentUserModel.getProfileImage() != null && !currentUserModel.getProfileImage().isEmpty()) {
                            Glide.with(ProfileFragment.this)
                                    .load(currentUserModel.getProfileImage())
                                    .placeholder(R.drawable.ic_person)
                                    .into(imageProfile);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadStats(String uid) {
        mRootRef.child("friends").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    long count = snapshot.getChildrenCount();
                    textContactCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mRootRef.child("chatlist").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    long count = snapshot.getChildrenCount();
                    textMessageCount.setText(String.valueOf(count));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
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
            if (task.isSuccessful() && isAdded()) {
                Toast.makeText(getContext(), getString(R.string.update_profile_success), Toast.LENGTH_SHORT).show();
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
                        if (isAdded()) {
                            mDatabase.child("profileImage").setValue(uri.toString());
                            Toast.makeText(getContext(), getString(R.string.update_avatar_success), Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
