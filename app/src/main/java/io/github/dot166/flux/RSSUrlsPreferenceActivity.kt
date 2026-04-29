package io.github.dot166.flux

import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import io.github.dot166.jlib.app.jActivity

class RSSUrlsPreferenceActivity: jActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.getInstance(this)
        setContent {
            AppTheme {
                val viewModel: RSSViewModel = viewModel()
                val refreshing = viewModel.state.collectAsState().value.refreshing
                val list = viewModel.state.collectAsState().value.listOfData
                val editableList = list.toMutableUrlList()
                val editableHiddenList = list.toMutableHiddenList()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.rss_feed_urls))
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        val alertDialog = SettingsLibAlertDialogBuilder(this@RSSUrlsPreferenceActivity)
                                        alertDialog.setTitle(getString(R.string.rssfeed))

                                        val layout = LinearLayout(this@RSSUrlsPreferenceActivity)
                                        val input = EditText(this@RSSUrlsPreferenceActivity)
                                        val lp = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT
                                        )
                                        input.layoutParams = lp
                                        layout.addView(input)
                                        val layoutSwitch =
                                            FrameLayout(this@RSSUrlsPreferenceActivity)
                                        val excludeSwitchText =
                                            TextView(this@RSSUrlsPreferenceActivity)
                                        excludeSwitchText.setText(R.string.exclude_from_all_feeds_view)
                                        layoutSwitch.addView(excludeSwitchText)
                                        val excludeSwitch = SwitchCompat(this@RSSUrlsPreferenceActivity)
                                        layoutSwitch.addView(excludeSwitch)
                                        layout.addView(layoutSwitch)
                                        alertDialog.setView(layout)

                                        alertDialog.setPositiveButton(android.R.string.ok) { _, _ ->
                                            val url = input.text.toString()
                                            val hidden = excludeSwitch.isChecked
                                            editableList.add(url)
                                            if (hidden) {
                                                editableHiddenList.add(url)
                                            }
                                            PreferenceManager.getDefaultSharedPreferences(this@RSSUrlsPreferenceActivity).edit {
                                                putString("RssUrls", editableList.toRSSString())
                                                putString(
                                                    "ExcludedRssUrls",
                                                    editableHiddenList.toRSSString()
                                                )
                                            }
                                            viewModel.refreshAll(this@RSSUrlsPreferenceActivity)
                                        }

                                        alertDialog.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                            dialog.cancel()
                                        }

                                        alertDialog.show()
                                    }
                                ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_feed)) }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        onBackPressedDispatcher.onBackPressed()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            }
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding),
                    ) {
                        if (refreshing) {
                            Text(text = stringResource(R.string.loading))
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(list.toSortedList()) {
                                    ListItem({
                                        ElevatedCard(
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = 6.dp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(5.dp, 500.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = it.url, modifier = Modifier.widthIn(5.dp, 280.dp))
                                                Row {
                                                    IconButton(onClick = {
                                                        val alertDialog =
                                                            SettingsLibAlertDialogBuilder(this@RSSUrlsPreferenceActivity)
                                                        alertDialog.setTitle(getString(R.string.rssfeed))

                                                        val layout = LinearLayout(this@RSSUrlsPreferenceActivity)
                                                        layout.orientation = VERTICAL
                                                        val input =
                                                            EditText(this@RSSUrlsPreferenceActivity)
                                                        val lp = LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.MATCH_PARENT
                                                        )
                                                        input.layoutParams = lp
                                                        input.setText(it.url)
                                                        layout.addView(input)
                                                        val layoutSwitch =
                                                            LinearLayout(this@RSSUrlsPreferenceActivity)
                                                        val excludeSwitchText =
                                                            TextView(this@RSSUrlsPreferenceActivity)
                                                        excludeSwitchText.setText(R.string.exclude_from_all_feeds_view)
                                                        layoutSwitch.addView(excludeSwitchText)
                                                        val excludeSwitch = SwitchCompat(this@RSSUrlsPreferenceActivity)
                                                        excludeSwitch.isChecked = it.hiddenFromAll
                                                        layoutSwitch.addView(excludeSwitch)
                                                        layout.addView(layoutSwitch)
                                                        alertDialog.setView(layout)

                                                        alertDialog.setPositiveButton(android.R.string.ok) { _, _ ->
                                                            val url = input.text.toString()
                                                            val hidden = excludeSwitch.isChecked
                                                            editableList.remove(it.url)
                                                            editableList.add(url)
                                                            if (editableHiddenList.contains(it.url)) {
                                                                editableHiddenList.remove(it.url)
                                                            }
                                                            if (hidden) {
                                                                editableHiddenList.add(url)
                                                            }
                                                            PreferenceManager.getDefaultSharedPreferences(
                                                                this@RSSUrlsPreferenceActivity
                                                            ).edit {
                                                                putString(
                                                                    "RssUrls",
                                                                    editableList.toRSSString()
                                                                )
                                                                putString(
                                                                    "ExcludedRssUrls",
                                                                    editableHiddenList.toRSSString()
                                                                )
                                                            }
                                                            viewModel.refreshAll(this@RSSUrlsPreferenceActivity)
                                                        }

                                                        alertDialog.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                                            dialog.cancel()
                                                        }

                                                        alertDialog.show()
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            stringResource(R.string.edit_feed)
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        val alertDialog =
                                                            SettingsLibAlertDialogBuilder(this@RSSUrlsPreferenceActivity)
                                                        alertDialog.setTitle(getString(R.string.rssfeed))
                                                        alertDialog.setMessage(
                                                            getString(
                                                                R.string.are_you_sure_you_want_to_delete,
                                                                it.url
                                                            )
                                                        )

                                                        alertDialog.setPositiveButton(getString(R.string.yes)) { _, _ ->
                                                            editableList.remove(it.url)
                                                            if (editableHiddenList.contains(it.url)) {
                                                                editableHiddenList.remove(it.url)
                                                            }
                                                            PreferenceManager.getDefaultSharedPreferences(
                                                                this@RSSUrlsPreferenceActivity
                                                            ).edit {
                                                                putString(
                                                                    "RssUrls",
                                                                    editableList.toRSSString()
                                                                )
                                                                putString(
                                                                    "ExcludedRssUrls",
                                                                    editableHiddenList.toRSSString()
                                                                )
                                                            }
                                                            viewModel.refreshAll(this@RSSUrlsPreferenceActivity)
                                                        }

                                                        alertDialog.setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                                            dialog.cancel()
                                                        }

                                                        alertDialog.show()
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            stringResource(R.string.delete_feed)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun MutableMap<Int, RSSFeed?>.toSortedList(): List<RSSFeed> {
        val list = mutableListOf<RSSFeed>()
        for (i in 0 until size) {
            val feed = get(i)
            if (feed != null && !feed.url.isBlank()) {
                list.add(feed)
            }
        }
        return list
    }

    private fun MutableMap<Int, RSSFeed?>.toMutableUrlList(): MutableList<String> {
        val list = mutableListOf<String>()
        for (i in 0 until size) {
            val feed = get(i)
            if (feed != null && !feed.url.isBlank()) {
                list.add(feed.url)
            }
        }
        return list
    }

    private fun MutableList<String>.toRSSString(): String {
        val sb = StringBuilder()
        for (i in 0 until size) {
            sb.append(get(i))
            if (i+1 < size) {
                sb.append(";")
            }
        }
        return sb.toString()
    }
}

private fun MutableMap<Int, RSSFeed?>.toMutableHiddenList(): MutableList<String> {
    val list = mutableListOf<String>()
    for (i in 0 until size) {
        val feed = get(i)
        if (feed != null && !feed.url.isBlank() && feed.hiddenFromAll) {
            list.add(feed.url)
        }
    }
    return list
}
