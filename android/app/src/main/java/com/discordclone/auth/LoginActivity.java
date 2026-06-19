package com.discordclone.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discordclone.MainActivity;
import com.discordclone.R;
import com.discordclone.repository.UserRepository;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.PreferencesUtil;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginBtn;
    private TextView registerLink;
    private ProgressBar progressBar;
    private UserRepository userRepo;
    private PreferencesUtil prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userRepo = new UserRepository();
        prefs = new PreferencesUtil(this);

        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.login_register_link);
        progressBar = findViewById(R.id.login_progress);

        if (FirebaseUtil.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        loginBtn.setOnClickListener(v -> login());
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        userRepo.login(email, password)
            .addOnCompleteListener(task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    prefs.saveEmail(email);
                    prefs.saveUsername(task.getResult().getUser().getDisplayName() != null ?
                        task.getResult().getUser().getDisplayName() : "");
                    saveFcmToken();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Login failed";
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> {
                String uid = FirebaseUtil.getCurrentUid();
                if (uid != null && token != null) {
                    FirebaseUtil.usersRef().document(uid).update("fcmToken", token);
                }
            });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginBtn.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }
}
