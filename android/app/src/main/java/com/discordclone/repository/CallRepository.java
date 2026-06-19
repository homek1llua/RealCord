package com.discordclone.repository;

import com.discordclone.models.CallOffer;
import com.discordclone.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CallRepository {

    public Task<String> initiateCall(String calleeId, String callerName, String type) {
        String callerId = FirebaseUtil.getCurrentUid();
        CallOffer offer = new CallOffer(callerId, callerName, calleeId, type);

        final com.google.firebase.firestore.DocumentReference docRef = FirebaseUtil.callsRef().document();
        final String callId = docRef.getId();
        offer.setId(callId);

        return docRef.set(offer)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return callId;
                }
                throw task.getException();
            });
    }

    public Task<Void> acceptCall(String callId) {
        return FirebaseUtil.callsRef().document(callId)
            .update("status", CallOffer.STATUS_ACCEPTED);
    }

    public Task<Void> rejectCall(String callId) {
        return FirebaseUtil.callsRef().document(callId)
            .update("status", CallOffer.STATUS_REJECTED);
    }

    public Task<Void> endCall(String callId) {
        return FirebaseUtil.callsRef().document(callId)
            .update("status", CallOffer.STATUS_ENDED);
    }

    public Task<Void> updateSdp(String callId, String sdp) {
        return FirebaseUtil.callsRef().document(callId)
            .update("sdp", sdp);
    }

    public Task<DocumentSnapshot> getCall(String callId) {
        return FirebaseUtil.callsRef().document(callId).get();
    }

    public Task<QuerySnapshot> getIncomingCalls(String userId) {
        return FirebaseUtil.callsRef()
            .whereEqualTo("calleeId", userId)
            .whereEqualTo("status", CallOffer.STATUS_RINGING)
            .get();
    }

    public Task<QuerySnapshot> getActiveCall(String userId1, String userId2) {
        return FirebaseUtil.callsRef()
            .whereEqualTo("callerId", userId1)
            .whereEqualTo("calleeId", userId2)
            .whereIn("status", new ArrayList<String>() {{
                add(CallOffer.STATUS_RINGING);
                add(CallOffer.STATUS_ACCEPTED);
            }})
            .get();
    }

    public CallOffer parseCall(DocumentSnapshot doc) {
        if (doc.exists()) {
            CallOffer call = doc.toObject(CallOffer.class);
            if (call != null) {
                call.setId(doc.getId());
            }
            return call;
        }
        return null;
    }

    public List<CallOffer> parseCalls(QuerySnapshot snapshot) {
        List<CallOffer> calls = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                CallOffer call = doc.toObject(CallOffer.class);
                if (call != null) {
                    call.setId(doc.getId());
                    calls.add(call);
                }
            }
        }
        return calls;
    }
}
