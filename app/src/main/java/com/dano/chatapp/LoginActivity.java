package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button buttonLogin;
    private TextView textRegister, textForgotPassword;
    private ImageView imageShowPassword;
    private boolean isPasswordVisible = false;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://chatapp-20a5f5b5-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference().child("users");

        // Kiểm tra nếu đã đăng nhập thì vào thẳng MainActivity
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        buttonLogin = findViewById(R.id.button_login);
        textRegister = findViewById(R.id.text_register);
        textForgotPassword = findViewById(R.id.text_forgot_password);
        imageShowPassword = findViewById(R.id.image_show_password);

        buttonLogin.setOnClickListener(v -> loginUser());

        textRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        textForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        imageShowPassword.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ẩn mật khẩu
            editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imageShowPassword.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            // Hiện mật khẩu
            editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imageShowPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Hoặc một icon khác đại diện cho việc ẩn
        }
        isPasswordVisible = !isPasswordVisible;
        // Đưa con trỏ về cuối văn bản
        editPassword.setSelection(editPassword.getText().length());
    }

    private void loginUser() {
        String input = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra nếu input là Email
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            performLogin(input, password);
        } else {
            // Nếu không phải email, giả định là số điện thoại
            loginWithPhone(input, password);
        }
    }

    private void loginWithPhone(String phone, String password) {
        // Tìm email tương ứng với số điện thoại trong Database
        Query query = mDatabase.orderByChild("phone").equalTo(phone);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            performLogin(email, password);
                            return;
                        }
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
