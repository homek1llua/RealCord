package com.discordclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.discordclone.auth.LoginActivity;
import com.discordclone.friends.FriendsFragment;
import com.discordclone.servers.ServersFragment;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.PreferencesUtil;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!FirebaseUtil.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        FirebaseUtil.updateUserStatus(com.discordclone.models.User.STATUS_ONLINE);
        FirebaseUtil.updateLastSeen();

        viewPager = findViewById(R.id.main_viewpager);
        tabLayout = findViewById(R.id.main_tabs);

        requestNotificationPermission();
        setupViewPager();
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FriendsFragment(), "Friends");
        adapter.addFragment(new ServersFragment(), "Servers");
        adapter.addFragment(new SettingsFragment(), "Settings");
        viewPager.setAdapter(adapter);
    }

    private void setupTabIcons() {
        if (tabLayout.getTabCount() >= 3) {
            tabLayout.getTabAt(0).setIcon(android.R.drawable.ic_menu_myplaces);
            tabLayout.getTabAt(1).setIcon(android.R.drawable.ic_menu_directions);
            tabLayout.getTabAt(2).setIcon(android.R.drawable.ic_menu_sort_by_size);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUtil.updateUserStatus(com.discordclone.models.User.STATUS_OFFLINE);
    }
}
