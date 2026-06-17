package io.github.dot166.flux

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.settingslib.spa.framework.theme.SettingsTheme
import io.github.dot166.jlib.RSSFeed
import io.github.dot166.jlib.app.SettingsLibComposeDialog
import io.github.dot166.jlib.app.jActivity

class RSSUrlsPreferenceActivity: jActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsTheme {
                val repo = Repository.getInstance(this)
                var refreshing by remember { mutableStateOf(true) }
                val list = repo.getFeeds()
                var showAddDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var text by remember { mutableStateOf("") }
                var checked by remember { mutableStateOf(false) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.rss_feed_urls))
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        showAddDialog = true
                                        text = ""
                                        checked = false
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
                                items(list) {
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
                                                        showAddDialog = true
                                                        text = it.url
                                                        checked = it.hiddenFromAll
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            stringResource(R.string.edit_feed)
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        showDeleteDialog = true
                                                        text = it.url
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
                when {
                    showAddDialog -> {
                        SettingsLibComposeDialog(onDismissRequest = {
                            showAddDialog = false
                            text = ""
                            checked = false
                        }, onPositivePress = {
                            val url = text
                            val hidden = checked
                            var index = list.size
                            if (list.containsUrl(url)) {
                                index = list.indexOfUrl(url)
                                list.removeUrl(url)
                            }
                            list.addUrl(url, hidden, index)
                            repo.saveFeeds(list)
                            refreshing = true
                            refreshing = false
                            showAddDialog = false
                            text = ""
                            checked = false
                        }, positiveContent = {
                            Text(stringResource(android.R.string.ok))
                        }, onNegativePress = {
                            showAddDialog = false
                            text = ""
                            checked = false
                        }, negativeContent = {
                            Text(stringResource(android.R.string.cancel))
                        }, properties = DialogProperties()) {
                            TextField(text, {
                                text = it
                            })
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                ) {
                                Text(
                                    text = stringResource(R.string.exclude_from_all_feeds_view),
                                    modifier = Modifier.padding(16.dp),
                                    )
                                Switch(checked, {
                                    checked = it
                                })
                            }
                        }
                    }
                    showDeleteDialog -> {
                        SettingsLibComposeDialog(onDismissRequest = {
                            showAddDialog = false
                            text = ""
                        }, onPositivePress = {
                            list.removeUrl(text)
                            repo.saveFeeds(list)
                            refreshing = true
                            refreshing = false
                            showDeleteDialog = false
                            text = ""
                        }, positiveContent = {
                            Text(stringResource(R.string.yes))
                        }, onNegativePress = {
                            showDeleteDialog = false
                            text = ""
                        }, negativeContent = {
                            Text(stringResource(R.string.no))
                        }, properties = DialogProperties()) {
                            Text(stringResource(R.string.are_you_sure_you_want_to_delete, text))
                        }
                    }
                }
                refreshing = false
            }
        }
    }

    private fun MutableList<RSSFeed>.addUrl(url: String, hidden: Boolean, index: Int) {
        add(index, RSSFeed(false, url, null, hidden)) // can be null as this list gets dumped to JSON and reset with the actual feeds, is never used by the RSS part
    }

    private fun MutableList<RSSFeed>.containsUrl(url: String): Boolean {
        return find { it.url == url } != null
    }

    private fun MutableList<RSSFeed>.indexOfUrl(url: String): Int {
        return indexOf(find { it.url == url })
    }

    private fun MutableList<RSSFeed>.removeUrl(url: String) {
        remove(find { it.url == url })
    }

}
