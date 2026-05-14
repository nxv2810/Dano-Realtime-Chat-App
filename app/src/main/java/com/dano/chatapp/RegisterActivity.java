package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword, editConfirmPassword;
    private CheckBox checkboxTerms;
    private Button buttonRegister;
    private TextView textLogin;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Ánh xạ View
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        checkboxTerms = findViewById(R.id.checkbox_terms);
        buttonRegister = findViewById(R.id.button_register);
        textLogin = findViewById(R.id.text_login);

        buttonRegister.setOnClickListener(v -> registerUser());

        textLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra trống
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra độ dài mật khẩu (theo hình là ít nhất 8 ký tự)
        if (password.length() < 8) {
            Toast.makeText(this, "Mật khẩu quá ngắn. Cần ít nhất 8 ký tự.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Kiểm tra điều khoản
        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý với Điều khoản dịch vụ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tiến hành đăng ký với Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(userId, name, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email) {
        // Tạo đối tượng Map để lưu vào Database
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("profileImage", ""); // Mặc định để trống

        mDatabase.child("users").child(userId).setValue(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
