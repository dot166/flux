package io.github.dot166.flux;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.navigation.NavigationView;
import com.prof18.rssparser.model.RssChannel;

import java.net.URI;
import java.util.Calendar;

import io.github.dot166.jlib.app.jActivity;
import io.github.dot166.jlib.rss.RSSAlarmScheduler;
import io.github.dot166.jlib.rss.RSSFragment;
import io.github.dot166.jlib.rss.RSSViewModel;
import io.github.dot166.jlib.time.ReminderItem;

public class MainActivity extends jActivity {

    private RSSViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] rssUrls = PreferenceManager.getDefaultSharedPreferences(this).getString("rssUrls", "").split(";");
        viewModel = new ViewModelProvider(this).get(RSSViewModel.class);
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
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment, new RSSFragment(menuItem.getItemId()))
                            .commit();
                } else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new PreferenceFragment()).commit();
                }
                drawerLayout.close();
                return true;
            }
        });
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ReminderItem reminderItem = new ReminderItem(cal.getTimeInMillis(), 1);
        new RSSAlarmScheduler(this).schedule(reminderItem);
    }

    public void rebuildMenu(NavigationView navigationView, String[] rssUrls, int requiredId) {
        navigationView.getMenu().clear();
        navigationView.setItemIconTintList(null);
        navigationView.getMenu().add(0, 0, 0, "All Feeds");
        navigationView.getMenu().findItem(0).setIcon(R.drawable.ic_launcher_foreground);
        for (int i = 0; i < rssUrls.length; i++) {
            String url = rssUrls[i];
            int itemId = i + 1;

            // Add placeholder first
            navigationView.getMenu().add(0, itemId, 0, "Loading...")
                    .setIcon(R.drawable.ic_launcher_foreground);

            // Observe channel updates
            viewModel.getChannel(url).observe(this, channel -> {
                if (channel != null && channel.getTitle() != null) {
                    navigationView.getMenu().findItem(itemId).setTitle(channel.getTitle());

                    URI uri = URI.create(url);
                    URI uriNoPath = uri.resolve("/");
                    if (uriNoPath.toString().contains("feeds.bbci.co.uk")) {
                        uriNoPath = URI.create("bbc.co.uk");
                    }

                    if (channel.getImage() != null) {
                        Glide.with(MainActivity.this)
                                .load(channel.getImage().getUrl())
                                .into(new CustomTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        LayerDrawable ld = new LayerDrawable(new Drawable[]{
                                                new ColorDrawable(obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurface}).getColor(0, 0)), resource
                                        });
                                        navigationView.getMenu().findItem(itemId).setIcon(ld);
                                    }
                                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                                });
                    } else {
                        Glide.with(MainActivity.this)
                                .load("https://www.google.com/s2/favicons?domain=" + uriNoPath.toString())
                                .into(new CustomTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        LayerDrawable ld = new LayerDrawable(new Drawable[]{
                                                new ColorDrawable(obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurface}).getColor(0, 0)), resource
                                        });
                                        navigationView.getMenu().findItem(itemId).setIcon(ld);
                                    }
                                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                                });
                    }
                }
            });

            // Kick off fetch
            viewModel.fetchFeedAsync(url);
        }
        navigationView.getMenu().add(0, Integer.MAX_VALUE, 0, getString(io.github.dot166.jlib.R.string.settings_name));
        navigationView.getMenu().findItem(Integer.MAX_VALUE).setIcon(io.github.dot166.jlib.R.mipmap.settings);
        navigationView.getMenu().findItem(requiredId).setChecked(true);
    }
}
