package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPhone, editPassword, editConfirmPassword;
    private CheckBox checkboxTerms;
    private Button buttonRegister;
    private TextView textLogin, textPasswordError;
    private ImageView imageShowPassword, imageShowConfirmPassword;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Regex: Ít nhất 8 ký tự, 1 chữ cái, 1 số và 1 ký tự đặc biệt
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        // Ánh xạ View
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        checkboxTerms = findViewById(R.id.checkbox_terms);
        buttonRegister = findViewById(R.id.button_register);
        textLogin = findViewById(R.id.text_login);
        textPasswordError = findViewById(R.id.text_password_error);
        imageShowPassword = findViewById(R.id.image_show_password);
        imageShowConfirmPassword = findViewById(R.id.image_show_confirm_password);

        buttonRegister.setOnClickListener(v -> registerUser());

        textLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        imageShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        imageShowConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        // Ẩn cảnh báo khi người dùng bắt đầu nhập lại mật khẩu
        editPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textPasswordError.getVisibility() == View.VISIBLE) {
                    textPasswordError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imageShowPassword.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imageShowPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        isPasswordVisible = !isPasswordVisible;
        editPassword.setSelection(editPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            editConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imageShowConfirmPassword.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            editConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imageShowConfirmPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
        editConfirmPassword.setSelection(editConfirmPassword.getText().length());
    }

    private boolean isValidPassword(String password) {
        return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra trống
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra độ mạnh mật khẩu
        if (!isValidPassword(password)) {
            textPasswordError.setVisibility(View.VISIBLE); // Hiện cảnh báo đỏ
            editPassword.requestFocus();
            return;
        } else {
            textPasswordError.setVisibility(View.GONE);
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
                        saveUserToDatabase(userId, name, email, phone);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String email, String phone) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("profileImage", "");

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
