package com.discordclone.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discordclone.MainActivity;
import com.discordclone.R;
import com.discordclone.repository.UserRepository;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.PreferencesUtil;
import com.google.firebase.messaging.FirebaseMessaging;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerBtn;
    private ProgressBar progressBar;
    private UserRepository userRepo;
    private PreferencesUtil prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userRepo = new UserRepository();
        prefs = new PreferencesUtil(this);

        usernameInput = findViewById(R.id.register_username);
        emailInput = findViewById(R.id.register_email);
        passwordInput = findViewById(R.id.register_password);
        confirmPasswordInput = findViewById(R.id.register_confirm_password);
        registerBtn = findViewById(R.id.register_btn);
        progressBar = findViewById(R.id.register_progress);

        usernameInput.setFilters(new InputFilter[] {
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) &&
                        source.charAt(i) != '_' && source.charAt(i) != '.') {
                        return "";
                    }
                }
                return null;
            }
        });

        registerBtn.setOnClickListener(v -> register());
    }

    private void register() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() < 2 || username.length() > 32) {
            Toast.makeText(this, "Username must be 2-32 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        userRepo.register(email, password, username)
            .addOnCompleteListener(task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    prefs.saveUsername(username);
                    prefs.saveEmail(email);
                    saveFcmToken();
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Registration failed";
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
        registerBtn.setEnabled(!loading);
        usernameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
    }
}
