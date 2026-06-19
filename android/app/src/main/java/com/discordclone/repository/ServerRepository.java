package com.discordclone.repository;

import android.util.Log;

import com.discordclone.models.Channel;
import com.discordclone.models.Server;
import com.discordclone.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ServerRepository {
    private static final String TAG = "ServerRepository";

    public Task<String> createServer(String name) {
        String ownerId = FirebaseUtil.getCurrentUid();
        Server server = new Server(name, ownerId);
        server.setMemberIds(new ArrayList<>(Arrays.asList(ownerId)));
        server.setInviteCode(generateInviteCode());

        final com.google.firebase.firestore.DocumentReference docRef = FirebaseUtil.serversRef().document();
        final String serverId = docRef.getId();
        server.setId(serverId);

        return docRef.set(server)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    createDefaultChannels(serverId);
                    return serverId;
                }
                throw task.getException();
            });
    }

    private void createDefaultChannels(String serverId) {
        Channel general = new Channel(serverId, "general", Channel.TYPE_TEXT);
        general.setPosition(0);
        FirebaseUtil.channelsRef().document().set(general);

        Channel voice = new Channel(serverId, "Voice", Channel.TYPE_VOICE);
        voice.setPosition(1);
        FirebaseUtil.channelsRef().document().set(voice);
    }

    public Task<String> joinServer(String inviteCode) {
        String userId = FirebaseUtil.getCurrentUid();
        return FirebaseUtil.serversRef()
            .whereEqualTo("inviteCode", inviteCode)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                    Server server = doc.toObject(Server.class);
                    if (server != null) {
                        List<String> members = server.getMemberIds();
                        if (members == null) members = new ArrayList<>();
                        if (!members.contains(userId)) {
                            members.add(userId);
                            FirebaseUtil.serversRef().document(doc.getId())
                                .update("memberIds", members);
                        }
                        return doc.getId();
                    }
                }
                return null;
            });
    }

    public Task<QuerySnapshot> getMyServers() {
        String userId = FirebaseUtil.getCurrentUid();
        return FirebaseUtil.serversForUser(userId).get();
    }

    public Task<DocumentSnapshot> getServer(String serverId) {
        return FirebaseUtil.serversRef().document(serverId).get();
    }

    public Task<Void> deleteServer(String serverId) {
        return FirebaseUtil.serversRef().document(serverId).delete();
    }

    // Channel operations
    public Task<String> createChannel(String serverId, String name, String type) {
        Channel channel = new Channel(serverId, name, type);
        final com.google.firebase.firestore.DocumentReference docRef = FirebaseUtil.channelsRef().document();
        final String channelId = docRef.getId();
        channel.setId(channelId);
        return docRef.set(channel)
            .continueWith(task -> {
                if (task.isSuccessful()) return channelId;
                throw task.getException();
            });
    }

    public Task<Void> deleteChannel(String channelId) {
        return FirebaseUtil.channelsRef().document(channelId).delete();
    }

    public Task<QuerySnapshot> getChannels(String serverId) {
        return FirebaseUtil.channelsForServer(serverId).get();
    }

    public Task<DocumentSnapshot> getChannel(String channelId) {
        return FirebaseUtil.channelsRef().document(channelId).get();
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Task<Void> regenerateInviteCode(String serverId) {
        String newCode = generateInviteCode();
        return FirebaseUtil.serversRef().document(serverId)
            .update("inviteCode", newCode);
    }

    public List<Server> parseServers(QuerySnapshot snapshot) {
        List<Server> servers = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Server server = doc.toObject(Server.class);
                if (server != null) {
                    server.setId(doc.getId());
                    servers.add(server);
                }
            }
        }
        return servers;
    }

    public List<Channel> parseChannels(QuerySnapshot snapshot) {
        List<Channel> channels = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Channel channel = doc.toObject(Channel.class);
                if (channel != null) {
                    channel.setId(doc.getId());
                    channels.add(channel);
                }
            }
        }
        return channels;
    }
}
