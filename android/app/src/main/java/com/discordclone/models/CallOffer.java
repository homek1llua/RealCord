package com.discordclone.models;

import com.google.firebase.Timestamp;

public class CallOffer {
    private String id;
    private String callerId;
    private String callerName;
    private String calleeId;
    private String channelId;
    private String type;
    private String status;
    private String sdp;
    private String answerSdp;
    private Timestamp createdAt;

    public static final String TYPE_VOICE = "voice";
    public static final String TYPE_VIDEO = "video";

    public static final String STATUS_RINGING = "ringing";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ENDED = "ended";
    public static final String STATUS_MISSED = "missed";

    public CallOffer() {}

    public CallOffer(String callerId, String callerName, String calleeId, String type) {
        this.callerId = callerId;
        this.callerName = callerName;
        this.calleeId = calleeId;
        this.type = type;
        this.status = STATUS_RINGING;
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }

    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }

    public String getCalleeId() { return calleeId; }
    public void setCalleeId(String calleeId) { this.calleeId = calleeId; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public String getAnswerSdp() { return answerSdp; }
    public void setAnswerSdp(String answerSdp) { this.answerSdp = answerSdp; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
