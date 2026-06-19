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
import com.discordclone.calls.CallActivity;
import com.discordclone.models.Message;
import com.discordclone.models.User;
import com.discordclone.repository.MessageRepository;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.GifService;
import com.discordclone.utils.NitroManager;
import com.discordclone.utils.PreferencesUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class DMChatActivity extends AppCompatActivity {
    public static String currentChannel = null;
    private TextView friendNameView;
    private ListView messagesList;
    private EditText messageInput;
    private Button sendBtn, gifBtn, callBtn, videoBtn;
    private ProgressBar loadingSpinner;
    private MessageRepository messageRepo;
    private ChatAdapter chatAdapter;
    private String friendId, friendName;
    private String dmChannelId;
    private String myUsername;
    private boolean isGodUser = false;
    private TextView typingIndicator;
    private Handler typingHandler = new Handler();
    private Runnable stopTypingRunnable;
    private boolean isTyping = false;
    private ListenerRegistration typingListener;
    private ListenerRegistration messagesListener;
    private boolean hasReadMessages = false;
    private View replyPreview;
    private TextView replyPreviewText;
    private Button replyPreviewCancel;
    private String replyingToId, replyingToSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");

        messageRepo = new MessageRepository();
        chatAdapter = new ChatAdapter();
        chatAdapter.setDmContext(this);
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

        friendNameView = findViewById(R.id.chat_title);
        typingIndicator = findViewById(R.id.typing_indicator);
        messagesList = findViewById(R.id.messages_list);
        messageInput = findViewById(R.id.message_input);
        loadingSpinner = findViewById(R.id.loading_spinner);
        sendBtn = findViewById(R.id.send_btn);
        gifBtn = findViewById(R.id.gif_btn);
        callBtn = findViewById(R.id.call_btn);
        videoBtn = findViewById(R.id.video_btn);
        replyPreview = findViewById(R.id.reply_preview);
        replyPreviewText = findViewById(R.id.reply_preview_text);
        replyPreviewCancel = findViewById(R.id.reply_preview_cancel);
        callBtn.setVisibility(View.VISIBLE);
        videoBtn.setVisibility(View.VISIBLE);
        replyPreviewCancel.setOnClickListener(v -> clearReply());

        friendNameView.setText(friendName);

        messagesList.setAdapter(chatAdapter);
        messagesList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        loadCurrentUser();
        setupDMChannel();

        sendBtn.setOnClickListener(v -> sendMessage());
        gifBtn.setOnClickListener(v -> pickGif());
        callBtn.setOnClickListener(v -> startCall("voice"));
        videoBtn.setOnClickListener(v -> startCall("video"));

        setupTypingDetection();
        watchFriendTyping();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentChannel = dmChannelId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentChannel = null;
        if (isTyping) {
            isTyping = false;
            FirebaseUtil.currentUserRef().update("typing." + dmChannelId, FieldValue.delete());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (typingListener != null) typingListener.remove();
        if (messagesListener != null) messagesListener.remove();
        typingHandler.removeCallbacks(stopTypingRunnable);
        if (isTyping) {
            FirebaseUtil.currentUserRef().update("typing." + dmChannelId, FieldValue.delete());
        }
    }

    private void setupTypingDetection() {
        stopTypingRunnable = () -> {
            if (isTyping) {
                isTyping = false;
                FirebaseUtil.currentUserRef().update("typing." + dmChannelId, FieldValue.delete());
            }
        };
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && !isTyping) {
                    isTyping = true;
                    FirebaseUtil.currentUserRef().update("typing." + dmChannelId, System.currentTimeMillis());
                }
                typingHandler.removeCallbacks(stopTypingRunnable);
                typingHandler.postDelayed(stopTypingRunnable, 2000);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void watchFriendTyping() {
        if (friendId == null) return;
        typingListener = FirebaseUtil.usersRef().document(friendId)
            .addSnapshotListener((doc, e) -> {
                if (e != null || doc == null || !doc.exists()) return;
                Object typingData = doc.get("typing");
                if (typingData instanceof java.util.Map) {
                    Object ts = ((java.util.Map<String, Object>) typingData).get(dmChannelId);
                    if (ts instanceof Number) {
                        long now = System.currentTimeMillis();
                        long lastTyping = ((Number) ts).longValue();
                        if (now - lastTyping < 3000) {
                            typingIndicator.setVisibility(View.VISIBLE);
                            return;
                        }
                    }
                }
                typingIndicator.setVisibility(View.GONE);
            });
    }

    private void markMessagesAsRead(List<Message> messages) {
        if (hasReadMessages || messages.isEmpty()) return;
        WriteBatch batch = FirebaseUtil.getDb().batch();
        boolean hasUpdates = false;
        for (Message msg : messages) {
            if (msg.getReadAt() == null && friendId.equals(msg.getSenderId()) && msg.getId() != null) {
                batch.update(FirebaseUtil.messagesRef().document(msg.getId()), "readAt", Timestamp.now());
                hasUpdates = true;
            }
        }
        if (hasUpdates) {
            hasReadMessages = true;
            batch.commit();
        }
    }

    private void loadCurrentUser() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) return;
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                myUsername = user.getUsername();
                isGodUser = NitroManager.canGrantNitro(myUsername);
            }
        });
    }

    private void setupDMChannel() {
        String myId = FirebaseUtil.getCurrentUid();
        dmChannelId = myId.compareTo(friendId) < 0 ?
            myId + "_" + friendId : friendId + "_" + myId;
        dmChannelId = "dm_" + dmChannelId;

        new PreferencesUtil(this).markChannelRead(dmChannelId);
        FirebaseUtil.resetUnreadCount(dmChannelId);
        currentChannel = dmChannelId;
        loadMessages();
    }

    private void loadMessages() {
        loadingSpinner.setVisibility(View.VISIBLE);
        messagesListener = FirebaseUtil.messagesForChannel(dmChannelId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w("DMChat", "Messages listener error", e);
                    return;
                }
                if (snapshot == null) return;

                loadingSpinner.setVisibility(View.GONE);
                List<Message> messages = messageRepo.parseMessages(snapshot);
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    messagesList.smoothScrollToPosition(messages.size() - 1);
                }
                markMessagesAsRead(messages);
            });
    }

    private void clearReply() {
        replyingToId = null;
        replyingToSender = null;
        replyPreview.setVisibility(View.GONE);
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        if (isGodUser && (content.startsWith("/nitro ") || content.startsWith("/nitros "))) {
            handleNitroCommand(content);
            return;
        }

        messageInput.setText("");
        String replyId = replyingToId;
        clearReply();
        messageRepo.sendMessage(dmChannelId, content, replyId)
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show();
                messageInput.setText(content);
            });
    }

    private void handleNitroCommand(String content) {
        NitroManager.NitroCommand cmd = NitroManager.parseCommand(content);
        if (cmd.error != null) {
            Toast.makeText(this, cmd.error, Toast.LENGTH_SHORT).show();
            messageInput.setText("");
            return;
        }

        if (cmd.isSelf) {
            String uid = FirebaseUtil.getCurrentUid();
            NitroManager.grantNitro(uid, cmd.durationMs, cmd.tier);
            messageInput.setText("");
            String tierDisplay = cmd.tier.equals("premium") ? " Premium" : "";
            Toast.makeText(this, tierDisplay + " Nitro granted to you for " + formatDuration(cmd.durationMs), Toast.LENGTH_SHORT).show();
            messageRepo.sendMessage(dmChannelId, "_" + tierDisplay + " Nitro activated for " + formatDuration(cmd.durationMs) + "_");
            return;
        }

        FirebaseUtil.usersByUsername(cmd.targetUsername).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                Toast.makeText(this, "User '" + cmd.targetUsername + "' not found", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            String targetUid = doc.getId();
            NitroManager.grantNitro(targetUid, cmd.durationMs, cmd.tier);
            messageInput.setText("");
            String dur = formatDuration(cmd.durationMs);
            String tierDisplay = cmd.tier.equals("premium") ? " Premium" : "";
            Toast.makeText(this, tierDisplay + " Nitro granted to " + cmd.targetUsername + " for " + dur, Toast.LENGTH_SHORT).show();
            messageRepo.sendMessage(dmChannelId, "_" + tierDisplay + " Nitro granted to " + cmd.targetUsername + " for " + dur + "_");
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to find user", Toast.LENGTH_SHORT).show();
        });
    }

    private String formatDuration(long ms) {
        long days = ms / (24 * 60 * 60 * 1000);
        if (days > 0) return days + " day" + (days > 1 ? "s" : "");
        long hours = ms / (60 * 60 * 1000);
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        long mins = ms / (60 * 1000);
        return mins + " minute" + (mins > 1 ? "s" : "");
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
                messageRepo.sendGif(dmChannelId, gif.gifUrl, replyId)
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

    private void startCall(String type) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("friendId", friendId);
        intent.putExtra("friendName", friendName);
        intent.putExtra("callType", type);
        intent.putExtra("isIncoming", false);
        startActivity(intent);
    }
}