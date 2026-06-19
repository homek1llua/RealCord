package com.discordclone;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.discordclone.auth.LoginActivity;
import com.discordclone.models.User;
import com.discordclone.repository.UserRepository;
import com.discordclone.utils.AvatarGenerator;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.PreferencesUtil;
import com.google.firebase.Timestamp;

import java.io.File;

public class SettingsFragment extends Fragment {
    private ImageView avatarView;
    private TextView usernameView, emailView;
    private EditText bioInput, avatarTextInput;
    private Switch notificationSwitch;
    private Button logoutBtn, saveBtn;
    private Button statusOnline, statusIdle, statusDnd, statusInvisible;
    private Button fontBold, fontItalic, fontSerif, fontMono;
    private LinearLayout usernameColorSection, usernameColorSection2, usernameFontSection;
    private LinearLayout usernameGradientSection, chatColorSection, chatColorSection2, chatGradientSection, profileBgSection;
    private EditText pbgEmojiInput, pbgOpacityInput, pbgBannerUrlInput;
    private String selectedProfileBackgroundColor;
    private PreferencesUtil prefs;
    private UserRepository userRepo;
    private Uri selectedAvatarUri;
    private String selectedColor = "#5865F2";
    private String selectedUsernameColor = "#7983F5";
    private String selectedUsernameFont = "bold";
    private String selectedUsernameGradientStart = "#FF6B6B";
    private String selectedUsernameGradientEnd = "#FFD43B";
    private String selectedChatColor = "#FFFFFF";
    private String selectedChatGradientStart = "#FF6B6B";
    private String selectedChatGradientEnd = "#FFD43B";
    private String currentStatus = User.STATUS_ONLINE;
    private boolean hasNitro = false;
    private boolean hasPremium = false;
    private boolean isGodUser = false;
    private Button updateBtn;
    private static final int PICK_IMAGE = 100;
    private static final int PICK_APK = 200;
    private String pendingVersionName;
    private int pendingVersionCode;
    private String pendingChangelog;

    private static final String[] COLORS = {
        "#5865F2", "#ED4245", "#23A559", "#F0B232",
        "#9B59B6", "#1ABC9C", "#E67E22", "#3498DB",
        "#E91E63", "#00BCD4", "#FF5722", "#795548"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = new PreferencesUtil(getActivity());
        userRepo = new UserRepository();

        avatarView = view.findViewById(R.id.settings_avatar);
        usernameView = view.findViewById(R.id.settings_username);
        emailView = view.findViewById(R.id.settings_email);
        bioInput = view.findViewById(R.id.settings_bio);
        avatarTextInput = view.findViewById(R.id.settings_avatar_text);
        notificationSwitch = view.findViewById(R.id.settings_notifications);
        logoutBtn = view.findViewById(R.id.settings_logout);
        saveBtn = view.findViewById(R.id.settings_save);
        updateBtn = view.findViewById(R.id.settings_update);

        statusOnline = view.findViewById(R.id.status_online);
        statusIdle = view.findViewById(R.id.status_idle);
        statusDnd = view.findViewById(R.id.status_dnd);
        statusInvisible = view.findViewById(R.id.status_invisible);

        fontBold = view.findViewById(R.id.font_bold);
        fontItalic = view.findViewById(R.id.font_italic);
        fontSerif = view.findViewById(R.id.font_serif);
        fontMono = view.findViewById(R.id.font_mono);

        usernameColorSection = view.findViewById(R.id.username_color_section);
        usernameColorSection2 = view.findViewById(R.id.username_color_section_2);
        usernameFontSection = view.findViewById(R.id.username_font_section);
        usernameGradientSection = view.findViewById(R.id.username_gradient_section);
        chatColorSection = view.findViewById(R.id.chat_color_section);
        chatColorSection2 = view.findViewById(R.id.chat_color_section_2);
        chatGradientSection = view.findViewById(R.id.chat_gradient_section);
        profileBgSection = view.findViewById(R.id.profile_bg_section);
        pbgEmojiInput = view.findViewById(R.id.pbg_emoji);
        pbgOpacityInput = view.findViewById(R.id.pbg_opacity);
        pbgBannerUrlInput = view.findViewById(R.id.pbg_banner_url);

        statusOnline.setOnClickListener(v -> setStatus(User.STATUS_ONLINE));
        statusIdle.setOnClickListener(v -> setStatus(User.STATUS_IDLE));
        statusDnd.setOnClickListener(v -> setStatus(User.STATUS_DND));
        statusInvisible.setOnClickListener(v -> setStatus(User.STATUS_OFFLINE));

        fontBold.setOnClickListener(v -> selectFont("bold"));
        fontItalic.setOnClickListener(v -> selectFont("italic"));
        fontSerif.setOnClickListener(v -> selectFont("serif"));
        fontMono.setOnClickListener(v -> selectFont("monospace"));

        setupColorPickers(view);
        loadProfile();

        avatarView.setOnClickListener(v -> pickImage());
        avatarTextInput.setOnEditorActionListener((v, actionId, event) -> {
            updateAvatarPreview();
            return false;
        });
        avatarTextInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String text = s.toString().trim();
                if (text.startsWith("http")) {
                    Glide.with(SettingsFragment.this).load(text).circleCrop().into(avatarView);
                } else {
                    updateAvatarPreview();
                }
            }
        });

        saveBtn.setOnClickListener(v -> saveProfile());
        logoutBtn.setOnClickListener(v -> logout());
        updateBtn.setOnClickListener(v -> handleUpdate());

        Button gradStart = view.findViewById(R.id.uname_grad_start);
        Button gradEnd = view.findViewById(R.id.uname_grad_end);
        gradStart.setOnClickListener(v -> {
            if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
            cycleGradStart();
        });
        gradEnd.setOnClickListener(v -> {
            if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
            cycleGradEnd();
        });

        Button cgStart = view.findViewById(R.id.chat_grad_start);
        Button cgEnd = view.findViewById(R.id.chat_grad_end);
        cgStart.setOnClickListener(v -> {
            if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
            cycleChatGradStart();
        });
        cgEnd.setOnClickListener(v -> {
            if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
            cycleChatGradEnd();
        });

        return view;
    }

    private static final String[] GRADIENT_COLORS = {
        "#FF6B6B", "#FFA94D", "#FFD43B", "#69DB7C", "#4DABF7", "#9775FA", "#F783AC", "#20C997"
    };
    private int gradStartIdx = 0, gradEndIdx = 2;
    private int chatGradStartIdx = 0, chatGradEndIdx = 2;

    private void cycleGradStart() {
        gradStartIdx = (gradStartIdx + 1) % GRADIENT_COLORS.length;
        selectedUsernameGradientStart = GRADIENT_COLORS[gradStartIdx];
        Button btn = getView() != null ? getView().findViewById(R.id.uname_grad_start) : null;
        if (btn != null) btn.setBackgroundColor(android.graphics.Color.parseColor(selectedUsernameGradientStart));
    }

    private void cycleGradEnd() {
        gradEndIdx = (gradEndIdx + 1) % GRADIENT_COLORS.length;
        selectedUsernameGradientEnd = GRADIENT_COLORS[gradEndIdx];
        Button btn = getView() != null ? getView().findViewById(R.id.uname_grad_end) : null;
        if (btn != null) btn.setBackgroundColor(android.graphics.Color.parseColor(selectedUsernameGradientEnd));
    }

    private void cycleChatGradStart() {
        chatGradStartIdx = (chatGradStartIdx + 1) % GRADIENT_COLORS.length;
        selectedChatGradientStart = GRADIENT_COLORS[chatGradStartIdx];
        Button btn = getView() != null ? getView().findViewById(R.id.chat_grad_start) : null;
        if (btn != null) btn.setBackgroundColor(android.graphics.Color.parseColor(selectedChatGradientStart));
    }

    private void cycleChatGradEnd() {
        chatGradEndIdx = (chatGradEndIdx + 1) % GRADIENT_COLORS.length;
        selectedChatGradientEnd = GRADIENT_COLORS[chatGradEndIdx];
        Button btn = getView() != null ? getView().findViewById(R.id.chat_grad_end) : null;
        if (btn != null) btn.setBackgroundColor(android.graphics.Color.parseColor(selectedChatGradientEnd));
    }

    private void setupColorPickers(View view) {
        int[] avatarColorIds = {
            R.id.color_1, R.id.color_2, R.id.color_3, R.id.color_4,
            R.id.color_5, R.id.color_6, R.id.color_7, R.id.color_8,
            R.id.color_9, R.id.color_10, R.id.color_11, R.id.color_12
        };
        for (int i = 0; i < avatarColorIds.length && i < COLORS.length; i++) {
            Button btn = view.findViewById(avatarColorIds[i]);
            final String c = COLORS[i];
            btn.setBackgroundColor(android.graphics.Color.parseColor(c));
            btn.setOnClickListener(v -> {
                selectedColor = c;
                updateAvatarPreview();
            });
        }

        // Username colors
        int[] unameColorIds = {
            R.id.uname_color_1, R.id.uname_color_2, R.id.uname_color_3, R.id.uname_color_4,
            R.id.uname_color_5, R.id.uname_color_6, R.id.uname_color_7, R.id.uname_color_8
        };
        for (int i = 0; i < unameColorIds.length && i < COLORS.length; i++) {
            Button btn = view.findViewById(unameColorIds[i]);
            final String c = COLORS[i];
            btn.setBackgroundColor(android.graphics.Color.parseColor(c));
            btn.setOnClickListener(v -> {
                if (hasNitro) {
                    selectedUsernameColor = c;
                    previewUsernameStyle();
                } else {
                    Toast.makeText(getActivity(), "Requires Nitro", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Chat colors
        int[] chatColorIds = {
            R.id.chat_color_1, R.id.chat_color_2, R.id.chat_color_3, R.id.chat_color_4,
            R.id.chat_color_5, R.id.chat_color_6, R.id.chat_color_7, R.id.chat_color_8
        };
        for (int i = 0; i < chatColorIds.length && i < COLORS.length; i++) {
            Button btn = view.findViewById(chatColorIds[i]);
            final String c = COLORS[i];
            btn.setBackgroundColor(android.graphics.Color.parseColor(c));
            final int idx = i;
            btn.setOnClickListener(v -> {
                if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
                selectedChatColor = c;
                if (idx == 0) selectedChatColor = null; // first = none (reset to white)
                Toast.makeText(getActivity(), "Chat color set", Toast.LENGTH_SHORT).show();
            });
        }

        // Profile bg colors
        int[] pbgColorIds = {
            R.id.pbg_color_1, R.id.pbg_color_2, R.id.pbg_color_3, R.id.pbg_color_4
        };
        for (int i = 0; i < pbgColorIds.length; i++) {
            Button btn = view.findViewById(pbgColorIds[i]);
            final String c = COLORS[i];
            btn.setBackgroundColor(android.graphics.Color.parseColor(c));
            btn.setOnClickListener(v -> {
                if (!hasPremium) { Toast.makeText(getActivity(), "Requires Premium Nitro", Toast.LENGTH_SHORT).show(); return; }
                selectedProfileBackgroundColor = c;
                view.findViewById(R.id.pbg_color_1).setAlpha(1f);
                view.findViewById(R.id.pbg_color_2).setAlpha(1f);
                view.findViewById(R.id.pbg_color_3).setAlpha(1f);
                view.findViewById(R.id.pbg_color_4).setAlpha(1f);
                btn.setAlpha(0.5f);
            });
        }
    }

    private void selectFont(String font) {
        if (!hasNitro) {
            Toast.makeText(getActivity(), "Requires Nitro", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedUsernameFont = font;
        previewUsernameStyle();
        highlightFontButton();
    }

    private void highlightFontButton() {
        fontBold.setAlpha(selectedUsernameFont.equals("bold") ? 1f : 0.4f);
        fontItalic.setAlpha(selectedUsernameFont.equals("italic") ? 1f : 0.4f);
        fontSerif.setAlpha(selectedUsernameFont.equals("serif") ? 1f : 0.4f);
        fontMono.setAlpha(selectedUsernameFont.equals("monospace") ? 1f : 0.4f);
    }

    private void previewUsernameStyle() {
        try {
            int color = android.graphics.Color.parseColor(selectedUsernameColor);
            usernameView.setTextColor(color);
        } catch (Exception ignored) {}
        int tf = Typeface.NORMAL;
        switch (selectedUsernameFont) {
            case "bold": tf = Typeface.BOLD; break;
            case "italic": tf = Typeface.ITALIC; break;
            case "serif": tf = Typeface.BOLD_ITALIC; break;
        }
        usernameView.setTypeface(null, tf);
    }

    private void setStatus(String status) {
        currentStatus = status;
        FirebaseUtil.updateUserStatus(status);
        highlightStatusButton();
    }

    private void highlightStatusButton() {
        statusOnline.setAlpha(currentStatus.equals(User.STATUS_ONLINE) ? 1f : 0.5f);
        statusIdle.setAlpha(currentStatus.equals(User.STATUS_IDLE) ? 1f : 0.5f);
        statusDnd.setAlpha(currentStatus.equals(User.STATUS_DND) ? 1f : 0.5f);
        statusInvisible.setAlpha(currentStatus.equals(User.STATUS_OFFLINE) ? 1f : 0.5f);
    }

    private void loadProfile() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) return;

        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                usernameView.setText(user.getUsername());
                emailView.setText(user.getEmail());
                if (user.getBio() != null) bioInput.setText(user.getBio());
                if (user.getAvatarColor() != null) selectedColor = user.getAvatarColor();
                if (user.getAvatarText() != null) avatarTextInput.setText(user.getAvatarText());

                currentStatus = user.getStatus() != null ? user.getStatus() : User.STATUS_ONLINE;
                highlightStatusButton();

                hasNitro = user.hasNitro();
                hasPremium = user.hasPremium();
                isGodUser = "it".equals(user.getUsername());

                if (user.getUsernameColor() != null) selectedUsernameColor = user.getUsernameColor();
                if (user.getUsernameFont() != null) selectedUsernameFont = user.getUsernameFont();
                if (user.getUsernameGradientStart() != null) selectedUsernameGradientStart = user.getUsernameGradientStart();
                if (user.getUsernameGradientEnd() != null) selectedUsernameGradientEnd = user.getUsernameGradientEnd();
                if (user.getChatColor() != null) selectedChatColor = user.getChatColor();
                if (user.getChatGradientStart() != null) selectedChatGradientStart = user.getChatGradientStart();
                if (user.getChatGradientEnd() != null) selectedChatGradientEnd = user.getChatGradientEnd();
                if (user.getProfileBackgroundColor() != null) selectedProfileBackgroundColor = user.getProfileBackgroundColor();
                if (user.getProfileBackgroundEmoji() != null) pbgEmojiInput.setText(user.getProfileBackgroundEmoji());
                pbgOpacityInput.setText(String.valueOf(user.getProfileBackgroundEmojiOpacity()));
                if (user.getProfileBannerUrl() != null) pbgBannerUrlInput.setText(user.getProfileBannerUrl());

                updateBtn.setText(isGodUser ? "Upload Update" : "Check for Updates");

                float normalAlpha = hasNitro ? 1f : 0.35f;
                usernameColorSection.setAlpha(normalAlpha);
                usernameColorSection2.setAlpha(normalAlpha);
                usernameFontSection.setAlpha(normalAlpha);

                float premiumAlpha = hasPremium ? 1f : 0.35f;
                usernameGradientSection.setAlpha(premiumAlpha);
                chatColorSection.setAlpha(premiumAlpha);
                chatColorSection2.setAlpha(premiumAlpha);
                chatGradientSection.setAlpha(premiumAlpha);
                profileBgSection.setAlpha(premiumAlpha);

                if (hasNitro) {
                    previewUsernameStyle();
                }
                highlightFontButton();

                Button gradStart = getView() != null ? getView().findViewById(R.id.uname_grad_start) : null;
                Button gradEnd = getView() != null ? getView().findViewById(R.id.uname_grad_end) : null;
                if (gradStart != null) gradStart.setBackgroundColor(android.graphics.Color.parseColor(selectedUsernameGradientStart));
                if (gradEnd != null) gradEnd.setBackgroundColor(android.graphics.Color.parseColor(selectedUsernameGradientEnd));

                Button cgStart = getView() != null ? getView().findViewById(R.id.chat_grad_start) : null;
                Button cgEnd = getView() != null ? getView().findViewById(R.id.chat_grad_end) : null;
                cgStart.setBackgroundColor(android.graphics.Color.parseColor(selectedChatGradientStart));
                cgEnd.setBackgroundColor(android.graphics.Color.parseColor(selectedChatGradientEnd));

                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(this).load(user.getAvatarUrl()).circleCrop().into(avatarView);
                } else {
                    updateAvatarPreview();
                }
            }
        });
    }

    private void updateAvatarPreview() {
        String text = avatarTextInput.getText().toString().trim();
        if (text.isEmpty()) {
            text = usernameView.getText().toString();
        }
        Bitmap bmp = AvatarGenerator.generate(text, selectedColor, 160);
        avatarView.setImageBitmap(bmp);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void saveProfile() {
        String bio = bioInput.getText().toString().trim();
        String avatarText = avatarTextInput.getText().toString().trim();

        // If avatar text is a URL, use it as avatar URL directly (supports GIF avatars)
        if (avatarText.startsWith("http")) {
            userRepo.setAvatarUrl(avatarText);
            avatarText = "";
        }

        String pbgEmoji = pbgEmojiInput.getText().toString().trim();
        String pbgOpStr = pbgOpacityInput.getText().toString().trim();
        double pbgOpacity = 0.12;
        try { pbgOpacity = Double.parseDouble(pbgOpStr); } catch (Exception ignored) {}
        if (pbgOpacity < 0.05) pbgOpacity = 0.05;
        if (pbgOpacity > 0.5) pbgOpacity = 0.5;

        String bannerUrl = pbgBannerUrlInput.getText().toString().trim();
        if (bannerUrl.isEmpty()) bannerUrl = null;

        if (hasPremium) {
            userRepo.updateFullProfile(bio, selectedColor, avatarText,
                selectedUsernameColor, selectedUsernameFont,
                selectedUsernameGradientStart, selectedUsernameGradientEnd,
                selectedChatColor, selectedChatGradientStart, selectedChatGradientEnd,
                selectedProfileBackgroundColor, pbgEmoji.isEmpty() ? null : pbgEmoji, pbgOpacity,
                bannerUrl);
        } else if (hasNitro) {
            userRepo.updateProfile(bio, selectedColor, avatarText, selectedUsernameColor, selectedUsernameFont);
        } else {
            userRepo.updateProfile(bio, selectedColor, avatarText, null, null);
        }

        if (selectedAvatarUri != null) {
            userRepo.uploadAvatar(selectedAvatarUri)
                .addOnSuccessListener(task -> {
                    String uid = FirebaseUtil.getCurrentUid();
                    FirebaseUtil.avatarsRef().child(uid + ".jpg").getDownloadUrl()
                        .addOnSuccessListener(url -> {
                            userRepo.setAvatarUrl(url.toString());
                            prefs.saveAvatarUrl(url.toString());
                            Toast.makeText(getActivity(), "Profile updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Profile saved (avatar requires Firebase Blaze plan)", Toast.LENGTH_LONG).show();
                        });
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null && e.getMessage().contains("Blaze") ?
                        "Avatar upload requires Firebase Blaze plan" : "Failed to upload avatar";
                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                });
        } else {
            Toast.makeText(getActivity(), "Profile updated", Toast.LENGTH_SHORT).show();
        }

        prefs.setNotificationsEnabled(notificationSwitch.isChecked());
    }

    private void logout() {
        FirebaseUtil.updateUserStatus(User.STATUS_OFFLINE);
        FirebaseUtil.getAuth().signOut();
        prefs.clearAll();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }

    // ─── In-app Update System ───

    private void handleUpdate() {
        if (isGodUser) {
            showUploadDialog();
        } else {
            checkForUpdates();
        }
    }

    private long getCurrentVersionCode() {
        try {
            return getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void checkForUpdates() {
        FirebaseUtil.updatesRef().get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(getActivity(), "No updates available", Toast.LENGTH_SHORT).show();
                return;
            }
            long remoteVersion = doc.getLong("versionCode") != null ? doc.getLong("versionCode") : 0;
            long currentVersion = getCurrentVersionCode();
            String versionName = doc.getString("versionName");
            String changelog = doc.getString("changelog");
            String downloadUrl = doc.getString("downloadUrl");

            if (remoteVersion <= currentVersion) {
                Toast.makeText(getActivity(), "You're up to date!", Toast.LENGTH_SHORT).show();
                return;
            }

            new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setTitle("Update v" + (versionName != null ? versionName : remoteVersion))
                .setMessage(changelog != null ? changelog : "New version available")
                .setPositiveButton("Download", (d, w) -> downloadAndInstall(downloadUrl))
                .setNegativeButton("Later", null)
                .show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getActivity(), "Failed to check updates", Toast.LENGTH_SHORT).show();
        });
    }

    private void downloadAndInstall(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Toast.makeText(getActivity(), "No download URL", Toast.LENGTH_SHORT).show();
            return;
        }

        File apkFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        if (apkFile.exists()) apkFile.delete();

        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("Downloading update");
        request.setDestinationUri(Uri.fromFile(apkFile));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);

        Toast.makeText(getActivity(), "Download started", Toast.LENGTH_SHORT).show();

        // Install when download completes — use a BroadcastReceiver approach
        new android.app.DownloadManager.Query();
        getActivity().registerReceiver(new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id > 0 && apkFile.exists()) {
                    installApk(apkFile);
                }
                getActivity().unregisterReceiver(this);
            }
        }, new android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void installApk(File apkFile) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(getActivity(),
                getActivity().getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Uri apkUri = Uri.fromFile(apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void showUploadDialog() {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_upload_update, null);
        EditText versionNameInput = dialogView.findViewById(R.id.update_version_name);
        EditText versionCodeInput = dialogView.findViewById(R.id.update_version_code);
        EditText changelogInput = dialogView.findViewById(R.id.update_changelog);

        versionCodeInput.setText(String.valueOf(getCurrentVersionCode() + 1));

        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
            .setTitle("Upload Update")
            .setView(dialogView)
            .setPositiveButton("Pick APK & Upload", (d, w) -> {
                String vName = versionNameInput.getText().toString().trim();
                String vCodeStr = versionCodeInput.getText().toString().trim();
                String changelog = changelogInput.getText().toString().trim();

                if (vName.isEmpty() || vCodeStr.isEmpty()) {
                    Toast.makeText(getActivity(), "Version name & code required", Toast.LENGTH_SHORT).show();
                    return;
                }

                int vCode;
                try { vCode = Integer.parseInt(vCodeStr); } catch (Exception e) {
                    Toast.makeText(getActivity(), "Invalid version code", Toast.LENGTH_SHORT).show();
                    return;
                }

                pendingVersionName = vName;
                pendingVersionCode = vCode;
                pendingChangelog = changelog;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/octet-stream");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/vnd.android.package-archive", "application/octet-stream"});
                startActivityForResult(intent, PICK_APK);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void uploadUpdate(int versionCode, String versionName, String changelog, Uri apkUri) {
        Toast.makeText(getActivity(), "Uploading...", Toast.LENGTH_SHORT).show();

        // Copy content URI to temp file for reliable upload
        File tempFile = new File(getActivity().getCacheDir(), "upload_" + versionCode + ".apk");
        try {
            java.io.InputStream in = getActivity().getContentResolver().openInputStream(apkUri);
            java.io.OutputStream out = new java.io.FileOutputStream(tempFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            in.close();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Failed to read APK file", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uploadUri = Uri.fromFile(tempFile);
        String fileName = "discordclone_v" + versionCode + ".apk";
        FirebaseUtil.getStorage().getReference().child("updates").child(fileName)
            .putFile(uploadUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return FirebaseUtil.getStorage().getReference().child("updates").child(fileName).getDownloadUrl();
            })
            .addOnSuccessListener(downloadUrl -> {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("versionCode", versionCode);
                data.put("versionName", versionName);
                data.put("downloadUrl", downloadUrl.toString());
                data.put("changelog", changelog);
                data.put("uploadedAt", Timestamp.now());
                data.put("uploadedBy", FirebaseUtil.getCurrentUid());

                FirebaseUtil.updatesRef().set(data)
                    .addOnSuccessListener(v -> Toast.makeText(getActivity(), "Update published!", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to save metadata", Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e -> {
                String msg = e.getMessage() != null ? e.getMessage() : "Upload failed";
                if (msg.contains("Blaze")) msg = "APK upload requires Firebase Blaze plan";
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_APK && resultCode == getActivity().RESULT_OK && data != null) {
            Uri apkUri = data.getData();
            if (apkUri != null && pendingVersionName != null) {
                uploadUpdate(pendingVersionCode, pendingVersionName, pendingChangelog, apkUri);
                pendingVersionName = null;
            } else if (apkUri != null) {
                uploadApkFromUri(apkUri);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == getActivity().RESULT_OK && data != null) {
            selectedAvatarUri = data.getData();
            avatarView.setImageURI(selectedAvatarUri);
        }
    }

    private void uploadApkFromUri(Uri apkUri) {
        Toast.makeText(getActivity(), "Uploading...", Toast.LENGTH_SHORT).show();
        String fileName = "discordclone_manual.apk";
        FirebaseUtil.getStorage().getReference().child("updates").child(fileName)
            .putFile(apkUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return FirebaseUtil.getStorage().getReference().child("updates").child(fileName).getDownloadUrl();
            })
            .addOnSuccessListener(downloadUrl -> {
                FirebaseUtil.updatesRef().update("downloadUrl", downloadUrl.toString(),
                    "uploadedAt", Timestamp.now(), "uploadedBy", FirebaseUtil.getCurrentUid());
                Toast.makeText(getActivity(), "APK uploaded!", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                String msg = e.getMessage() != null ? e.getMessage() : "Upload failed";
                if (msg.contains("Blaze")) msg = "APK upload requires Firebase Blaze plan";
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            });
    }
}