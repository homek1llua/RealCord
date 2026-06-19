const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.onMessageCreated = functions.firestore
    .document('messages/{messageId}')
    .onCreate(async (snap, context) => {
        const msg = snap.data();
        const channelId = msg.channelId;
        const senderId = msg.senderId;
        const senderName = msg.senderName || 'Someone';
        const content = msg.content || '';
        const imageUrl = msg.imageUrl || '';

        // Only send push for DM channels
        if (!channelId || !channelId.startsWith('dm_')) return null;

        // Don't notify yourself
        if (!senderId) return null;

        // Figure out recipient
        const ids = channelId.substring(3);
        const parts = ids.split('_');
        if (parts.length !== 2) return null;

        const recipientId = parts[0] === senderId ? parts[1] : parts[0];

        // Get recipient's FCM token
        const userDoc = await admin.firestore().collection('users').doc(recipientId).get();
        if (!userDoc.exists) return null;

        const userData = userDoc.data();
        const fcmToken = userData && userData.fcmToken;
        if (!fcmToken) return null;

        const body = content ? (content.length > 100 ? content.substring(0, 100) + '…' : content) : (imageUrl ? 'Sent an image' : 'Sent a message');

        const payload = {
            token: fcmToken,
            data: {
                type: 'message',
                title: senderName,
                body: body,
                channelId: channelId,
                senderId: senderId
            }
        };

        try {
            await admin.messaging().send(payload);
            console.log('Push sent to', recipientId, 'via channel', channelId);
        } catch (err) {
            console.error('Failed to send push:', err);
        }

        return null;
    });
