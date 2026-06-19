package com.discordclone.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.discordclone.R;
import com.discordclone.models.Message;
import com.discordclone.models.Server;
import com.discordclone.models.User;
import com.discordclone.repository.MessageRepository;
import com.discordclone.repository.ServerRepository;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.GifService;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChannelChatActivity extends AppCompatActivity {
    private TextView channelNameView;
    private ListView messagesList;
    private EditText messageInput;
    private Button sendBtn, gifBtn, mentionBtn;
    private ProgressBar loadingSpinner;
    private View replyPreview;
    private TextView replyPreviewText;
    private Button replyPreviewCancel;
    private String replyingToId, replyingToSender;
    private MessageRepository messageRepo;
    private ServerRepository serverRepo;
    private ChatAdapter chatAdapter;
    private String channelId, channelName, serverName, serverId;
    private List<String> mentionUserIds = new ArrayList<>();
    private List<String> mentionNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        channelId = getIntent().getStringExtra("channelId");
        channelName = getIntent().getStringExtra("channelName");
        serverName = getIntent().getStringExtra("serverName");
        serverId = getIntent().getStringExtra("serverId");

        messageRepo = new MessageRepository();
        serverRepo = new ServerRepository();
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnReplyListener((messageId, senderName) -> {
            replyingToId = messageId;
            replyingToSender = senderName;
            replyPreviewText.setText("Replying to " + senderName);
            replyPreview.setVisibility(View.VISIBLE);
            messageInput.requestFocus();
        });
        chatAdapter.setOnDeleteListener(messageId -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete message?")
                .setPositiveButton("Delete", (d, w) -> messageRepo.deleteMessage(messageId))
                .setNegativeButton("Cancel", null)
                .show();
        });

        channelNameView = findViewById(R.id.chat_title);
        messagesList = findViewById(R.id.messages_list);
        messageInput = findViewById(R.id.message_input);
        loadingSpinner = findViewById(R.id.loading_spinner);
        sendBtn = findViewById(R.id.send_btn);
        gifBtn = findViewById(R.id.gif_btn);
        mentionBtn = findViewById(R.id.mention_btn);
        replyPreview = findViewById(R.id.reply_preview);
        replyPreviewText = findViewById(R.id.reply_preview_text);
        replyPreviewCancel = findViewById(R.id.reply_preview_cancel);
        replyPreviewCancel.setOnClickListener(v -> {
            replyingToId = null;
            replyingToSender = null;
            replyPreview.setVisibility(View.GONE);
        });

        String title = serverName != null ? serverName + " / " + channelName : channelName;
        channelNameView.setText(title);

        messagesList.setAdapter(chatAdapter);
        messagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        sendBtn.setOnClickListener(v -> sendMessage());
        gifBtn.setOnClickListener(v -> pickGif());

        if (serverId != null) {
            mentionBtn.setVisibility(View.VISIBLE);
            loadServerMembers();
        }

        mentionBtn.setOnClickListener(v -> showMentionPicker());

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && s.charAt(s.length() - 1) == '@') {
                    showMentionPicker();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadMessages();
    }

    private void loadServerMembers() {
        serverRepo.getServer(serverId).addOnSuccessListener(doc -> {
            Server server = doc.toObject(Server.class);
            if (server != null && server.getMemberIds() != null) {
                mentionUserIds.clear();
                mentionNames.clear();
                for (String uid : server.getMemberIds()) {
                    fetchMemberName(uid);
                }
            }
        });
    }

    private void fetchMemberName(String uid) {
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && user.getUsername() != null) {
                mentionUserIds.add(uid);
                mentionNames.add(user.getUsername());
            }
        });
    }

    private void showMentionPicker() {
        if (mentionNames.isEmpty()) {
            Toast.makeText(this, "No members to mention", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = mentionNames.toArray(new String[0]);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Mention someone");
        builder.setItems(names, (dialog, which) -> {
            String mention = "@" + names[which] + " ";
            int pos = messageInput.getSelectionStart();
            String current = messageInput.getText().toString();
            String result;
            if (pos > 0 && current.charAt(pos - 1) == '@') {
                result = current.substring(0, pos - 1) + mention + current.substring(pos);
                messageInput.setText(result);
                messageInput.setSelection(pos - 1 + mention.length());
            } else {
                result = current.substring(0, pos) + mention + current.substring(pos);
                messageInput.setText(result);
                messageInput.setSelection(pos + mention.length());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadMessages() {
        loadingSpinner.setVisibility(View.VISIBLE);
        FirebaseUtil.messagesForChannel(channelId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w("ChannelChat", "Messages listener error", e);
                    return;
                }
                if (snapshot == null) return;

                loadingSpinner.setVisibility(View.GONE);
                List<Message> messages = messageRepo.parseMessages(snapshot);
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    messagesList.smoothScrollToPosition(messages.size() - 1);
                }
            });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        List<String> mentionedIds = new ArrayList<>();
        for (int i = 0; i < mentionNames.size(); i++) {
            if (content.contains("@" + mentionNames.get(i))) {
                mentionedIds.add(mentionUserIds.get(i));
            }
        }

        messageInput.setText("");
        String replyId = replyingToId;
        replyingToId = null;
        replyingToSender = null;
        replyPreview.setVisibility(View.GONE);
        messageRepo.sendMessage(channelId, content, mentionedIds, replyId)
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
                messageInput.setText(content);
            });
    }

    private void pickGif() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_gif_search, null);
        EditText searchInput = dialogView.findViewById(R.id.gif_search_input);
        GridView grid = dialogView.findViewById(R.id.gif_grid);
        TextView emptyView = dialogView.findViewById(R.id.gif_empty);

        final GifGridAdapter adapter = new GifGridAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            GifService.GifResult gif = adapter.getItem(position);
            if (gif != null) {
                String replyId = replyingToId;
                replyingToId = null;
                replyPreview.setVisibility(View.GONE);
                messageRepo.sendGif(channelId, gif.gifUrl, replyId)
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to send GIF", Toast.LENGTH_SHORT).show());
            }
        });

        Handler searchHandler = new Handler();
        Runnable searchRunnable = new Runnable() {
            @Override
            public void run() {
                String q = searchInput.getText().toString().trim();
                GifService.search(q, new GifService.GifCallback() {
                    @Override
                    public void onResult(List<GifService.GifResult> gifs) {
                        if (!gifs.isEmpty()) {
                            grid.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                            adapter.setGifs(gifs);
                        } else {
                            grid.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("No GIFs found");
                        }
                    }
                    @Override
                    public void onError(String error) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Search failed");
                    }
                });
            }
        };

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        new AlertDialog.Builder(this)
            .setTitle("Send GIF")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .show();

        GifService.search("", new GifService.GifCallback() {
            @Override
            public void onResult(List<GifService.GifResult> gifs) {
                if (!gifs.isEmpty()) {
                    grid.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    adapter.setGifs(gifs);
                }
            }
            @Override
            public void onError(String error) {}
        });
    }
}