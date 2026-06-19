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

public class CreateServerActivity extends AppCompatActivity {
    private EditText nameInput;
    private Button createBtn;
    private ProgressBar progressBar;
    private ServerRepository serverRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_server);

        serverRepo = new ServerRepository();

        nameInput = findViewById(R.id.server_name_input);
        createBtn = findViewById(R.id.create_server_btn);
        progressBar = findViewById(R.id.create_server_progress);

        createBtn.setOnClickListener(v -> createServer());
    }

    private void createServer() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a server name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.length() > 100) {
            Toast.makeText(this, "Server name too long", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        createBtn.setEnabled(false);

        serverRepo.createServer(name)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                createBtn.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Server created!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Failed to create server";
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });
    }
}
