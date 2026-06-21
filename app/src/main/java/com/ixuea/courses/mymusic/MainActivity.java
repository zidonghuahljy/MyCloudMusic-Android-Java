package com.ixuea.courses.mymusic;

import android.os.Bundle;
import android.widget.TextView;

import com.ixuea.courses.mymusic.activity.BaseCommonActivity;
import com.ixuea.courses.mymusic.activity.MineActivity;
import com.ixuea.courses.mymusic.activity.PlayerActivity;
import com.ixuea.courses.mymusic.activity.PlaylistActivity;
import com.ixuea.courses.mymusic.activity.SearchActivity;

public class MainActivity extends BaseCommonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        TextView search = findViewById(R.id.tv_shortcut_search);
        TextView playlist = findViewById(R.id.tv_shortcut_playlist);
        TextView player = findViewById(R.id.tv_shortcut_player);
        TextView mine = findViewById(R.id.tv_shortcut_mine);

        search.setOnClickListener(v -> startActivity(SearchActivity.class));
        playlist.setOnClickListener(v -> startActivity(PlaylistActivity.class));
        player.setOnClickListener(v -> startActivity(PlayerActivity.class));
        mine.setOnClickListener(v -> startActivity(MineActivity.class));
    }
}
