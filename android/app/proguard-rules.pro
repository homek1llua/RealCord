# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }

# Models (Firestore serialization)
-keep class com.discordclone.models.** { *; }

# Keep R
-keep class com.discordclone.R$* { *; }
