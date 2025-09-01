package io.github.dot166.flux;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.dot166.jlib.app.jConfigActivity;

public class PreferenceFragment extends jConfigActivity.jLIBSettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

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
