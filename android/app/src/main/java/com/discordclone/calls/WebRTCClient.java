package com.discordclone.calls;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebRTCClient {
    private static final String TAG = "WebRTCClient";
    private static final String STUN_URL = "stun:stun.l.google.com:19302";

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private MediaStream localStream;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoCapturer videoCapturer;
    private EglBase eglBase;
    private Context context;
    private boolean isVideoEnabled = false;
    private boolean isMicEnabled = true;

    private PeerConnectionEvents events;

    public interface PeerConnectionEvents {
        void onIceCandidate(String sdpMid, int sdpMLineIndex, String sdp);
        void onSdpCreated(String type, String sdp);
        void onRemoteStreamAdded(MediaStream stream);
        void onConnectionStateChanged(String state);
        void onError(String error);
    }

    public WebRTCClient(Context context, PeerConnectionEvents events) {
        this.context = context;
        this.events = events;
        init();
    }

    private void init() {
        eglBase = EglBase.create();

        PeerConnectionFactory.InitializationOptions initOptions =
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setFieldTrials("")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
            eglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(
            eglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();

        createLocalStream();
    }

    private void createLocalStream() {
        localStream = factory.createLocalMediaStream("localStream");

        // Audio
        MediaConstraints audioConstraints = new MediaConstraints();
        audioSource = factory.createAudioSource(audioConstraints);
        AudioTrack audioTrack = factory.createAudioTrack("audio_track", audioSource);
        audioTrack.setEnabled(true);
        localStream.addTrack(audioTrack);
    }

    public VideoCapturer createVideoCapturer() {
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(context)) {
            enumerator = new Camera2Enumerator(context);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        String[] deviceNames = enumerator.getDeviceNames();
        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                VideoCapturer capturer = enumerator.createCapturer(name, null);
                if (capturer != null) return capturer;
            }
        }

        for (String name : deviceNames) {
            VideoCapturer capturer = enumerator.createCapturer(name, null);
            if (capturer != null) return capturer;
        }

        return null;
    }

    public void enableVideo(SurfaceViewRenderer localRenderer) {
        if (videoSource != null) return;

        videoCapturer = createVideoCapturer();
        if (videoCapturer == null) {
            Log.w(TAG, "No camera available");
            return;
        }

        SurfaceTextureHelper helper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = factory.createVideoSource(false);
        videoCapturer.initialize(helper, context, videoSource.getCapturerObserver());

        VideoTrack videoTrack = factory.createVideoTrack("video_track", videoSource);
        videoTrack.setEnabled(true);
        videoTrack.addSink(localRenderer);
        localStream.addTrack(videoTrack);

        videoCapturer.startCapture(1280, 720, 30);
        isVideoEnabled = true;
    }

    public void disableVideo() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
                videoCapturer = null;
            } catch (Exception e) {
                Log.w(TAG, "Error stopping video capture", e);
            }
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        isVideoEnabled = false;
    }

    public void toggleMic() {
        isMicEnabled = !isMicEnabled;
        if (localStream != null && localStream.audioTracks.size() > 0) {
            localStream.audioTracks.get(0).setEnabled(isMicEnabled);
        }
    }

    public boolean isMicEnabled() {
        return isMicEnabled;
    }

    public boolean isVideoEnabled() {
        return isVideoEnabled;
    }

    public void createPeerConnection(PeerConnectionEvents events, List<PeerConnection.IceServer> iceServers) {
        if (iceServers == null) {
            iceServers = new ArrayList<>();
            iceServers.add(PeerConnection.IceServer.builder(STUN_URL).createIceServer());
        }

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = factory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                if (events != null) {
                    events.onIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
                }
            }

            @Override
            public void onAddStream(MediaStream stream) {
                if (events != null) {
                    events.onRemoteStreamAdded(stream);
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                if (events != null) {
                    events.onConnectionStateChanged(state.toString());
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}

            @Override
            public void onRemoveStream(MediaStream stream) {}

            @Override
            public void onDataChannel(DataChannel channel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {}

            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {}

            @Override
            public void onAddTrack(org.webrtc.RtpReceiver receiver, MediaStream[] streams) {}
        });

        if (localStream != null) {
            peerConnection.addStream(localStream);
        }
    }

    public void createOffer() {
        if (peerConnection == null) return;

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                final String type = sdp.type.canonicalForm();
                final String description = sdp.description;
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sdp) {}

                    @Override
                    public void onSetSuccess() {
                        if (events != null) {
                            events.onSdpCreated(type, description);
                        }
                    }

                    @Override
                    public void onCreateFailure(String error) {
                        Log.e(TAG, "setLocalDescription error: " + error);
                    }

                    @Override
                    public void onSetFailure(String error) {
                        Log.e(TAG, "setLocalDescription error: " + error);
                    }
                }, sdp);
            }

            @Override
            public void onSetSuccess() {}

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "createOffer error: " + error);
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "createOffer error: " + error);
            }
        }, constraints);
    }

    public void createAnswer(Runnable onComplete) {
        if (peerConnection == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                final String type = sdp.type.canonicalForm();
                final String description = sdp.description;
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sdp) {}

                    @Override
                    public void onSetSuccess() {
                        if (events != null) {
                            events.onSdpCreated(type, description);
                        }
                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onCreateFailure(String error) {
                        Log.e(TAG, "setLocalDescription error: " + error);
                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onSetFailure(String error) {
                        Log.e(TAG, "setLocalDescription error: " + error);
                        if (onComplete != null) onComplete.run();
                    }
                }, sdp);
            }

            @Override
            public void onSetSuccess() {}

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "createAnswer error: " + error);
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "createAnswer error: " + error);
                if (onComplete != null) onComplete.run();
            }
        }, constraints);
    }

    public void setRemoteDescription(String type, String sdp, Runnable onSuccess) {
        if (peerConnection == null) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        SessionDescription.Type sdpType = "offer".equals(type) ?
            SessionDescription.Type.OFFER : SessionDescription.Type.ANSWER;
        SessionDescription sessionDescription = new SessionDescription(sdpType, sdp);
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {}

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "Remote description set successfully");
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onCreateFailure(String error) {
                Log.e(TAG, "setRemoteDescription error: " + error);
            }

            @Override
            public void onSetFailure(String error) {
                Log.e(TAG, "setRemoteDescription error: " + error);
            }
        }, sessionDescription);
    }

    public void addIceCandidate(String sdpMid, int sdpMLineIndex, String sdp) {
        if (peerConnection == null) return;
        IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
        peerConnection.addIceCandidate(candidate);
    }

    public SurfaceViewRenderer createRenderer(Context context) {
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(context);
        renderer.init(eglBase.getEglBaseContext(), null);
        renderer.setMirror(true);
        renderer.setZOrderMediaOverlay(true);
        return renderer;
    }

    public void attachRemoteRenderer(SurfaceViewRenderer renderer) {
        if (localStream != null && localStream.videoTracks.size() > 0) {
            localStream.videoTracks.get(0).addSink(renderer);
        }
    }

    public void dispose() {
        disableVideo();
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            ((CameraVideoCapturer) videoCapturer).switchCamera(null);
        }
    }
}
