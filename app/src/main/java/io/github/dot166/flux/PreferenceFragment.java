package io.github.dot166.flux;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.dot166.jlib.app.jConfigActivity;

public class PreferenceFragment extends jConfigActivity.jLIBSettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public int preferenceXML() {
        return R.xml.config;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (Objects.requireNonNull(key).equals("rssUrls")) {
            ((MainActivity) requireActivity()).rebuildMenu(requireActivity().findViewById(R.id.nav_view), sharedPreferences.getString("rssUrls", "").split(";"), Integer.MAX_VALUE);
        }
    }
}
