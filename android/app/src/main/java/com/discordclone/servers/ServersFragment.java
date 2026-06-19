package com.discordclone.servers;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.discordclone.R;
import com.discordclone.models.Server;
import com.discordclone.repository.ServerRepository;
import com.discordclone.utils.AvatarGenerator;
import com.discordclone.utils.FirebaseUtil;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ServersFragment extends Fragment {
    private ListView serversList;
    private TextView emptyView;
    private Button createBtn, joinBtn;
    private ServerRepository serverRepo;
    private ListenerRegistration serverListener;
    private List<Server> serverList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_servers, container, false);

        serverRepo = new ServerRepository();

        serversList = view.findViewById(R.id.servers_list);
        emptyView = view.findViewById(R.id.servers_empty);
        createBtn = view.findViewById(R.id.create_server_btn);
        joinBtn = view.findViewById(R.id.join_server_btn);

        createBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateServerActivity.class));
        });

        joinBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), JoinServerActivity.class));
        });

        serversList.setAdapter(new ServerListAdapter());

        loadServers();

        return view;
    }

    private void loadServers() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) return;

        serverListener = FirebaseUtil.serversForUser(uid)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;

                serverList = serverRepo.parseServers(snapshot);

                if (serverList.isEmpty()) {
                    serversList.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                serversList.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                ((ServerListAdapter) serversList.getAdapter()).notifyDataSetChanged();
            });
    }

    private class ServerListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return serverList.size(); }

        @Override
        public Server getItem(int position) { return serverList.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_server, parent, false);
            }

            Server server = getItem(position);
            String name = server.getName() != null ? server.getName() : "Server";
            String firstLetter = name.substring(0, 1).toUpperCase();

            TextView iconView = convertView.findViewById(R.id.server_icon);
            iconView.setText(firstLetter);
            String color = AvatarGenerator.getColorForUser(server.getId());
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(android.graphics.Color.parseColor(color));
            iconView.setBackground(drawable);

            ((TextView) convertView.findViewById(R.id.server_name_text)).setText(name);
            int memberCount = server.getMemberIds() != null ? server.getMemberIds().size() : 0;
            ((TextView) convertView.findViewById(R.id.server_members_text)).setText(memberCount + " members");

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ServerActivity.class);
                intent.putExtra("serverId", server.getId());
                intent.putExtra("serverName", name);
                startActivity(intent);
            });

            return convertView;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serverListener != null) serverListener.remove();
    }
}