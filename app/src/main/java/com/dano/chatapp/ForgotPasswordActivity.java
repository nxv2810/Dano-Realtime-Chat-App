package com.dano.chatapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private Button buttonResetPassword;
    private ImageView btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        editEmail = findViewById(R.id.edit_email);
        buttonResetPassword = findViewById(R.id.button_reset_password);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        buttonResetPassword.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            resetPassword(email);
        });
    }

    private void resetPassword(String email) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập Email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Vui lòng nhập đúng định dạng Email", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonResetPassword.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    buttonResetPassword.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Yêu cầu đã được gửi! Vui lòng kiểm tra Email của bạn.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
