package com.dano.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navChat, navContacts, navProfile;
    private ImageView imgChat, imgContacts, imgProfile;
    private TextView txtChat, txtContacts, txtProfile;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupNavigation();

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new ChatFragment(), "chat");
        }
    }

    private void initViews() {
        navChat = findViewById(R.id.nav_chat);
        navContacts = findViewById(R.id.nav_contacts);
        navProfile = findViewById(R.id.nav_profile);

        imgChat = findViewById(R.id.img_nav_chat);
        imgContacts = findViewById(R.id.img_nav_contacts);
        imgProfile = findViewById(R.id.img_nav_profile);

        txtChat = findViewById(R.id.text_nav_chat);
        txtContacts = findViewById(R.id.text_nav_contacts);
        txtProfile = findViewById(R.id.text_nav_profile);
    }

    private void setupNavigation() {
        navChat.setOnClickListener(v -> {
            loadFragment(new ChatFragment(), "chat");
            updateNavUI("chat");
        });

        navContacts.setOnClickListener(v -> {
            loadFragment(new ContactsFragment(), "contacts");
            updateNavUI("contacts");
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment(), "profile");
            updateNavUI("profile");
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        // Check if fragment is already active
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getTag() != null && currentFragment.getTag().equals(tag)) {
            return;
        }

        ft.replace(R.id.fragment_container, fragment, tag);
        ft.commit();
    }

    private void updateNavUI(String activeTab) {
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_grey);

        // Reset all
        imgChat.setColorFilter(inactiveColor);
        txtChat.setTextColor(inactiveColor);
        navChat.setBackground(null);

        imgContacts.setColorFilter(inactiveColor);
        txtContacts.setTextColor(inactiveColor);
        navContacts.setBackground(null);

        imgProfile.setColorFilter(inactiveColor);
        txtProfile.setTextColor(inactiveColor);
        navProfile.setBackground(null);

        // Set active
        switch (activeTab) {
            case "chat":
                imgChat.setColorFilter(activeColor);
                txtChat.setTextColor(activeColor);
                navChat.setBackgroundColor(0x1A007AFF); // Light blue tint
                break;
            case "contacts":
                imgContacts.setColorFilter(activeColor);
                txtContacts.setTextColor(activeColor);
                navContacts.setBackgroundColor(0x1A007AFF);
                break;
            case "profile":
                imgProfile.setColorFilter(activeColor);
                txtProfile.setTextColor(activeColor);
                navProfile.setBackgroundColor(0x1A007AFF);
                break;
        }
    }
}
