package com.discordclone.servers;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.discordclone.R;
import com.discordclone.repository.ServerRepository;

public class JoinServerActivity extends AppCompatActivity {
    private EditText inviteInput;
    private Button joinBtn;
    private ProgressBar progressBar;
    private ServerRepository serverRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_server);

        serverRepo = new ServerRepository();

        inviteInput = findViewById(R.id.invite_code_input);
        joinBtn = findViewById(R.id.join_server_btn);
        progressBar = findViewById(R.id.join_server_progress);

        joinBtn.setOnClickListener(v -> joinServer());
    }

    private void joinServer() {
        String code = inviteInput.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter an invite code", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        joinBtn.setEnabled(false);

        serverRepo.joinServer(code)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                joinBtn.setEnabled(true);
                if (task.isSuccessful() && task.getResult() != null) {
                    Toast.makeText(this, "Joined server!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Invalid invite code", Toast.LENGTH_LONG).show();
                }
            });
    }
}
