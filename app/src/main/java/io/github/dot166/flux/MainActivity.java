package io.github.dot166.flux;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.navigation.NavigationView;
import com.prof18.rssparser.model.RssChannel;

import java.net.URI;

import io.github.dot166.jlib.app.jActivity;
import io.github.dot166.jlib.app.jConfigActivity;
import io.github.dot166.jlib.rss.RSSFragment;
import io.github.dot166.jlib.rss.RSSViewModel;

public class MainActivity extends jActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] rssUrls = PreferenceManager.getDefaultSharedPreferences(this).getString("rssUrls", "").split(";");
        setContentView(R.layout.activity_main);
        Toolbar topAppBar = findViewById(R.id.actionbar);
        setSupportActionBar(topAppBar);
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        rebuildMenu(navigationView, rssUrls, 0);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new RSSFragment(0)).commit();
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // Handle menu item selected
                for (int i = 0; i < navigationView.getMenu().size(); i++) {
                    navigationView.getMenu().getItem(i).setChecked(false);
                }
                menuItem.setChecked(true);
                if (menuItem.getItemId() != Integer.MAX_VALUE) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new RSSFragment(menuItem.getItemId())).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment, (jConfigActivity.jLIBSettingsFragment) new PreferenceFragment()).commit();
                }
                drawerLayout.close();
                return true;
            }
        });
    }

    public void rebuildMenu(NavigationView navigationView, String[] rssUrls, int requiredId) {
        navigationView.getMenu().clear();
        navigationView.setItemIconTintList(null);
        navigationView.getMenu().add(0, 0, 0, "All Feeds");
        navigationView.getMenu().findItem(0).setIcon(R.drawable.ic_launcher_foreground);
        for (int i = 0; i < rssUrls.length; i++) {
            RssChannel channel = (new RSSViewModel()).fetchFeedWithoutViewModel(rssUrls[i], this);
            navigationView.getMenu().add(0, i+1, 0, channel.getTitle());
            URI uri = URI.create(rssUrls[i]);

            URI uriNoPath = uri.resolve("/");
            int finalI = i;
            Glide.with(MainActivity.this)
                    .load("https://www.google.com/s2/favicons?domain=" + uriNoPath.toString())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            LayerDrawable ld = new LayerDrawable(new Drawable[]{new ColorDrawable(obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurface}).getColor(0, 0)), resource});
                            navigationView.getMenu().findItem(finalI+1).setIcon(ld);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
            if (navigationView.getMenu().findItem(i+1).getIcon() == null) {
                navigationView.getMenu().findItem(finalI+1).setIcon(R.drawable.ic_launcher_foreground);
            }
        }
        navigationView.getMenu().add(0, Integer.MAX_VALUE, 0, getString(io.github.dot166.jlib.R.string.settings_name));
        navigationView.getMenu().findItem(Integer.MAX_VALUE).setIcon(io.github.dot166.jlib.R.mipmap.settings);
        navigationView.getMenu().findItem(requiredId).setChecked(true);
    }
}
