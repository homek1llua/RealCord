package com.discordclone.calls;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.discordclone.R;
import com.discordclone.models.CallOffer;
import com.discordclone.repository.CallRepository;
import com.discordclone.utils.FirebaseUtil;
import com.google.firebase.firestore.ListenerRegistration;

import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallActivity extends AppCompatActivity implements WebRTCClient.PeerConnectionEvents {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView callerNameView, callStatusView;
    private Button muteBtn, speakerBtn, cameraBtn, switchCameraBtn, endCallBtn, answerBtn;
    private RelativeLayout localVideoContainer, remoteVideoContainer;
    private SurfaceViewRenderer localRenderer, remoteRenderer;

    private WebRTCClient webRTC;
    private CallRepository callRepo;
    private String callId, friendId, friendName, callType;
    private boolean isIncoming, isSpeakerOn = true, isCallActive = false;
    private ListenerRegistration callListener;
    private Handler timeoutHandler = new Handler();
    private Set<String> processedIceKeys = new HashSet<>();
    private List<String[]> pendingIceCandidates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        callId = getIntent().getStringExtra("callId");
        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");
        callType = getIntent().getStringExtra("callType");
        isIncoming = getIntent().getBooleanExtra("isIncoming", false);
        callRepo = new CallRepository();

        callerNameView = findViewById(R.id.call_caller_name);
        callStatusView = findViewById(R.id.call_status);
        muteBtn = findViewById(R.id.call_mute_btn);
        speakerBtn = findViewById(R.id.call_speaker_btn);
        cameraBtn = findViewById(R.id.call_camera_btn);
        switchCameraBtn = findViewById(R.id.call_switch_camera_btn);
        endCallBtn = findViewById(R.id.call_end_btn);
        answerBtn = findViewById(R.id.call_answer_btn);
        localVideoContainer = findViewById(R.id.local_video_container);
        remoteVideoContainer = findViewById(R.id.remote_video_container);

        callerNameView.setText(friendName != null ? friendName : "Unknown");

        if (isIncoming) {
            answerBtn.setVisibility(View.VISIBLE);
            callStatusView.setText("Incoming " + callType + " call...");
        } else {
            answerBtn.setVisibility(View.GONE);
            callStatusView.setText("Calling " + (friendName != null ? friendName : "Unknown") + "...");
        }

        if (!"video".equals(callType)) {
            cameraBtn.setVisibility(View.GONE);
            switchCameraBtn.setVisibility(View.GONE);
        }

        checkPermissions();

        muteBtn.setOnClickListener(v -> {
            if (webRTC != null) {
                webRTC.toggleMic();
                muteBtn.setSelected(!webRTC.isMicEnabled());
                muteBtn.setText(webRTC.isMicEnabled() ? "Mute" : "Unmuted");
            }
        });

        speakerBtn.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (isSpeakerOn) {
                audioManager.setSpeakerphoneOn(true);
                speakerBtn.setText("Speaker");
            } else {
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                speakerBtn.setText("Earpiece");
            }
            speakerBtn.setSelected(isSpeakerOn);
        });

        cameraBtn.setOnClickListener(v -> {
            if (webRTC == null) return;
            if (webRTC.isVideoEnabled()) {
                webRTC.disableVideo();
                localRenderer.setVisibility(View.GONE);
                cameraBtn.setText("Camera On");
            } else {
                localRenderer.setVisibility(View.VISIBLE);
                webRTC.enableVideo(localRenderer);
                cameraBtn.setText("Camera Off");
            }
            cameraBtn.setSelected(webRTC.isVideoEnabled());
        });

        switchCameraBtn.setOnClickListener(v -> {
            if (webRTC != null) webRTC.switchCamera();
        });

        endCallBtn.setOnClickListener(v -> endCall());
        answerBtn.setOnClickListener(v -> answerCall());
    }

    private void checkPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }
        if ("video".equals(callType) && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (!needed.isEmpty()) {
            String[] perms = new String[needed.size()];
            needed.toArray(perms);
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST_CODE);
        } else {
            initWebRTC();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) allGranted = false;
            }
            if (allGranted) {
                initWebRTC();
            } else {
                Toast.makeText(this, "Permissions required for calls", Toast.LENGTH_LONG).show();
                if ("video".equals(callType) && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    callType = "voice";
                    cameraBtn.setVisibility(View.GONE);
                    switchCameraBtn.setVisibility(View.GONE);
                }
                initWebRTC();
            }
        }
    }

    private void initWebRTC() {
        webRTC = new WebRTCClient(this, this);

        // Create local renderer
        localRenderer = webRTC.createRenderer(this);
        localVideoContainer.addView(localRenderer);

        // Create remote renderer
        remoteRenderer = webRTC.createRenderer(this);
        remoteVideoContainer.addView(remoteRenderer);

        // Enable video if call type is video
        if ("video".equals(callType)) {
            webRTC.enableVideo(localRenderer);
        }

        // Initiate call or listen for incoming
        if (isIncoming) {
            listenForCallUpdates();
        } else {
            initiateCall();
        }
    }

    private void initiateCall() {
        String myName = FirebaseUtil.getAuth().getCurrentUser() != null ?
            FirebaseUtil.getAuth().getCurrentUser().getDisplayName() : "Me";
        if (myName == null) myName = "Me";

        callRepo.initiateCall(friendId, myName, callType)
            .addOnSuccessListener(id -> {
                callId = id;
                webRTC.createPeerConnection(this, null);
                flushPendingIceCandidates();
                webRTC.createOffer();
                listenForCallUpdates();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to start call", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void flushPendingIceCandidates() {
        for (String[] parts : pendingIceCandidates) {
            if (webRTC != null && parts.length == 3) {
                try {
                    int mline = Integer.parseInt(parts[1]);
                    webRTC.addIceCandidate(parts[0], mline, parts[2]);
                } catch (NumberFormatException ignored) {}
            }
        }
        pendingIceCandidates.clear();
    }

    private void answerCall() {
        if (callId == null) return;

        answerBtn.setVisibility(View.GONE);
        callStatusView.setText("Connecting...");

        webRTC.createPeerConnection(this, null);
        flushPendingIceCandidates();

        Runnable onAnswerCreated = () -> {
            callRepo.acceptCall(callId);
            isCallActive = true;
        };

        callRepo.getCall(callId).addOnSuccessListener(doc -> {
            CallOffer call = doc.toObject(CallOffer.class);
            if (call != null && call.getSdp() != null && !call.getSdp().isEmpty()) {
                webRTC.setRemoteDescription("offer", call.getSdp(), () -> {
                    webRTC.createAnswer(onAnswerCreated);
                });
            } else {
                webRTC.createAnswer(onAnswerCreated);
            }
        }).addOnFailureListener(e -> {
            webRTC.createAnswer(onAnswerCreated);
        });
    }

    private void listenForCallUpdates() {
        if (callId == null) return;

        callListener = FirebaseUtil.getDb().collection("calls").document(callId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null || !snapshot.exists()) {
                    finish();
                    return;
                }

                CallOffer call;
                try {
                    call = snapshot.toObject(CallOffer.class);
                } catch (Exception ex) {
                    Log.w("CallActivity", "Failed to parse call", ex);
                    return;
                }
                if (call == null) return;

                String status = call.getStatus();

                if (status != null) {
                    switch (status) {
                        case CallOffer.STATUS_ACCEPTED:
                            isCallActive = true;
                            callStatusView.setText("Connected");
                            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            audioManager.setSpeakerphoneOn(true);
                            break;

                        case CallOffer.STATUS_REJECTED:
                            Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show();
                            finish();
                            return;

                        case CallOffer.STATUS_ENDED:
                            Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
                            finish();
                            return;

                        case CallOffer.STATUS_MISSED:
                            Toast.makeText(this, "Missed call", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                    }
                }

                // Handle SDP exchange
                // Outgoing side (caller) receives answer from "answerSdp" field
                if (!isIncoming && call.getAnswerSdp() != null && !call.getAnswerSdp().isEmpty() && webRTC != null) {
                    webRTC.setRemoteDescription("answer", call.getAnswerSdp(), null);
                }

                // Exchange ICE candidates
                if (snapshot.contains("iceCandidates")) {
                    Object iceData = snapshot.get("iceCandidates");
                    if (iceData instanceof Map) {
                        Map<String, Object> iceCandidates = (Map<String, Object>) iceData;
                        for (Map.Entry<String, Object> entry : iceCandidates.entrySet()) {
                            if (!processedIceKeys.contains(entry.getKey()) && entry.getValue() != null) {
                                String candStr = entry.getValue().toString();
                                String[] parts = candStr.split("\\|", 3);
                                if (parts.length == 3) {
                                    if (webRTC != null) {
                                        try {
                                            int mline = Integer.parseInt(parts[1]);
                                            webRTC.addIceCandidate(parts[0], mline, parts[2]);
                                        } catch (NumberFormatException ignored) {}
                                    } else {
                                        pendingIceCandidates.add(parts);
                                    }
                                }
                                processedIceKeys.add(entry.getKey());
                            }
                        }
                    }
                }
            });
    }

    @Override
    public void onIceCandidate(String sdpMid, int sdpMLineIndex, String sdp) {
        // Store ICE candidate in Firestore
        if (callId != null) {
            String key = "ice_" + sdpMLineIndex + "_" + System.currentTimeMillis();
            FirebaseUtil.getDb().collection("calls").document(callId)
                .update("iceCandidates." + key, sdpMid + "|" + sdpMLineIndex + "|" + sdp);
        }
    }

    @Override
    public void onSdpCreated(String type, String sdp) {
        if (callId != null) {
            // Caller stores offer in "sdp", callee stores answer in "answerSdp"
            String field = isIncoming ? "answerSdp" : "sdp";
            FirebaseUtil.getDb().collection("calls").document(callId)
                .update(field, sdp);
        }
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream) {
        runOnUiThread(() -> {
            if (stream.videoTracks.size() > 0) {
                VideoTrack videoTrack = stream.videoTracks.get(0);
                videoTrack.addSink(remoteRenderer);
                remoteVideoContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onConnectionStateChanged(String state) {
        runOnUiThread(() -> {
            callStatusView.setText("Connection: " + state);
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Call error: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    private void endCall() {
        if (callId != null) {
            callRepo.endCall(callId);
        }
        cleanup();
        finish();
    }

    private void cleanup() {
        if (callListener != null) callListener.remove();
        if (webRTC != null) {
            webRTC.dispose();
            webRTC = null;
        }
        if (localRenderer != null) {
            localRenderer.release();
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        endCall();
    }
}
