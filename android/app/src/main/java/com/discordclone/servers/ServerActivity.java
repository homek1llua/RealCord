package com.discordclone.servers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.discordclone.R;
import com.discordclone.chat.ChannelChatActivity;
import com.discordclone.models.Channel;
import com.discordclone.models.Server;
import com.discordclone.repository.ServerRepository;
import com.discordclone.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerActivity extends AppCompatActivity {
    private TextView serverNameView, serverIconView;
    private ListView channelsList;
    private Button createChannelBtn;
    private ServerRepository serverRepo;
    private String serverId, serverName;
    private ListenerRegistration channelListener;
    private List<Map<String, String>> channelData = new ArrayList<>();
    private List<String> channelIds = new ArrayList<>();
    private List<String> channelTypes = new ArrayList<>();
    private boolean isOwner = false;
    private TextView inviteCodeText;
    private Button shareInviteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        serverRepo = new ServerRepository();
        serverId = getIntent().getStringExtra("serverId");
        serverName = getIntent().getStringExtra("serverName");

        serverNameView = findViewById(R.id.server_name);
        serverIconView = findViewById(R.id.server_icon);
        channelsList = findViewById(R.id.channels_list);
        createChannelBtn = findViewById(R.id.create_channel_btn);
        inviteCodeText = findViewById(R.id.invite_code_text);
        shareInviteBtn = findViewById(R.id.share_invite_btn);

        serverNameView.setText(serverName);
        String firstLetter = serverName != null && serverName.length() > 0 ?
            serverName.substring(0, 1).toUpperCase() : "S";
        serverIconView.setText(firstLetter);
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconBg.setColor(android.graphics.Color.parseColor(
            com.discordclone.utils.AvatarGenerator.getColorForUser(serverId)));
        serverIconView.setBackground(iconBg);

        checkOwnership();
        loadChannels();
        loadInviteCode();

        createChannelBtn.setOnClickListener(v -> showCreateChannelDialog());

        shareInviteBtn.setOnClickListener(v -> shareInviteCode());
    }

    private void loadInviteCode() {
        serverRepo.getServer(serverId).addOnSuccessListener(doc -> {
            Server server = doc.toObject(Server.class);
            if (server != null && server.getInviteCode() != null) {
                inviteCodeText.setText(server.getInviteCode());
            }
        });
    }

    private void shareInviteCode() {
        String code = inviteCodeText.getText().toString();
        if (code.isEmpty() || code.equals("------")) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my server on Discord Clone! Invite code: " + code);
        startActivity(Intent.createChooser(shareIntent, "Share invite code"));
    }

    private void checkOwnership() {
        serverRepo.getServer(serverId).addOnSuccessListener(doc -> {
            Server server = doc.toObject(Server.class);
            if (server != null) {
                String ownerId = server.getOwnerId();
                isOwner = ownerId != null && ownerId.equals(FirebaseUtil.getCurrentUid());
                createChannelBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showCreateChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Channel");

        View view = getLayoutInflater().inflate(R.layout.dialog_create_channel, null);
        EditText nameInput = view.findViewById(R.id.channel_name_input);
        Button textBtn = view.findViewById(R.id.channel_type_text);
        Button voiceBtn = view.findViewById(R.id.channel_type_voice);
        final String[] selectedType = {Channel.TYPE_TEXT};

        textBtn.setOnClickListener(v -> {
            selectedType[0] = Channel.TYPE_TEXT;
            textBtn.setAlpha(1f);
            voiceBtn.setAlpha(0.5f);
        });
        voiceBtn.setOnClickListener(v -> {
            selectedType[0] = Channel.TYPE_VOICE;
            voiceBtn.setAlpha(1f);
            textBtn.setAlpha(0.5f);
        });

        builder.setView(view);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            if (!name.isEmpty()) {
                serverRepo.createChannel(serverId, name, selectedType[0])
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create channel", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteChannelDialog(int position) {
        if (!isOwner) return;
        String channelName = channelData.get(position).get("name");
        String channelId = channelIds.get(position);

        new AlertDialog.Builder(this)
            .setTitle("Delete Channel")
            .setMessage("Delete " + channelName + "?")
            .setPositiveButton("Delete", (dialog, which) ->
                serverRepo.deleteChannel(channelId)
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete channel", Toast.LENGTH_SHORT).show()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadChannels() {
        channelListener = FirebaseUtil.channelsForServer(serverId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;

                List<Channel> channels = serverRepo.parseChannels(snapshot);
                channelData.clear();
                channelIds.clear();
                channelTypes.clear();

                for (Channel channel : channels) {
                    Map<String, String> item = new HashMap<>();
                    String prefix = channel.getType().equals(Channel.TYPE_VOICE) ? "\uD83C\uDFA4 " : "# ";
                    item.put("name", prefix + channel.getName());
                    channelData.add(item);
                    channelIds.add(channel.getId());
                    channelTypes.add(channel.getType());
                }

                SimpleAdapter adapter = new SimpleAdapter(
                    this, channelData,
                    android.R.layout.simple_list_item_1,
                    new String[]{"name"},
                    new int[]{android.R.id.text1}
                );

                channelsList.setAdapter(adapter);
                channelsList.setOnItemClickListener((parent, view, position, id) -> {
                    String channelId = channelIds.get(position);
                    String channelName = channelData.get(position).get("name");
                    String type = channelTypes.get(position);

                    if (Channel.TYPE_TEXT.equals(type)) {
                        Intent intent = new Intent(this, ChannelChatActivity.class);
                        intent.putExtra("channelId", channelId);
                        intent.putExtra("channelName", channelName);
                        intent.putExtra("serverName", serverName);
                        intent.putExtra("serverId", serverId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this,
                            "Voice channels coming soon", Toast.LENGTH_SHORT).show();
                    }
                });

                if (isOwner) {
                    channelsList.setOnItemLongClickListener((parent, view, position, id) -> {
                        showDeleteChannelDialog(position);
                        return true;
                    });
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (channelListener != null) channelListener.remove();
    }
}
