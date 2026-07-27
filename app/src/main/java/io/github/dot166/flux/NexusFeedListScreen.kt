package io.github.dot166.flux

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.nononsenseapps.feeder.archmodel.FeedItemStyle
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.ui.MainActivity
import com.nononsenseapps.feeder.ui.compose.components.safeSemantics
import com.nononsenseapps.feeder.ui.compose.feed.FeedItemState
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.ui.compose.feed.FeedScreenType
import com.nononsenseapps.feeder.ui.compose.feed.PlainTooltipBox
import com.nononsenseapps.feeder.ui.compose.feed.isSavedArticles
import com.nononsenseapps.feeder.ui.compose.feed.rememberLazyListState
import com.nononsenseapps.feeder.ui.compose.feedarticle.FeedScreenViewState
import com.nononsenseapps.feeder.ui.compose.feedarticle.FeedViewModel
import com.nononsenseapps.feeder.ui.compose.feedarticle.TranslatedFeedCards
import com.nononsenseapps.feeder.ui.compose.modifiers.trackVisibility
import com.nononsenseapps.feeder.ui.compose.pullrefresh.PullRefreshIndicator
import com.nononsenseapps.feeder.ui.compose.pullrefresh.pullRefresh
import com.nononsenseapps.feeder.ui.compose.pullrefresh.rememberPullRefreshState
import com.nononsenseapps.feeder.ui.compose.text.annotatedStringResource
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.utils.addMargin
import com.nononsenseapps.feeder.ui.compose.utils.isCompactDevice
import com.nononsenseapps.feeder.util.ActivityLauncher
import com.nononsenseapps.feeder.util.DEEP_LINK_BASE_URI
import com.nononsenseapps.feeder.util.logDebug
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.compose.LocalDI
import org.kodein.di.instance
import java.time.Instant

private const val LOG_TAG = "FEEDER_FEEDSCREEN_MINUSONE"

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
) {
    val viewState: FeedScreenViewState by viewModel.viewState.collectAsState()
    val translatedFeedCards by viewModel.translatedFeedCards.collectAsState()
    val pagedFeedItems = viewModel.feedListItemsForMinusOne.collectAsLazyPagingItems()
    val ctx = LocalContext.current

    val activityLauncher: ActivityLauncher by LocalDI.current.instance()

    // Each feed gets its own scroll state. Persists across device rotations, but is cleared when
    // switching feeds
    val feedListState =
        key(viewState.currentFeedOrTag) {
            pagedFeedItems.rememberLazyListState()
        }

    val feedGridState =
        key(viewState.currentFeedOrTag) {
            rememberLazyStaggeredGridState()
        }

    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()


    FeedScreen(
        viewState = viewState,
        onRefreshVisible = {
            viewModel.requestImmediateSyncOfMinusOne()
        },
        markAsUnread = { itemId, unread ->
            if (unread) {
                viewModel.markAsUnread(itemId)
            } else {
                viewModel.markAsRead(itemId)
            }
        },
        onOpenFeedItem = { itemId ->
            viewModel.openArticle(
                itemId = itemId,
                openInBrowser = { articleLink ->
                    activityLauncher.openLinkInBrowser(articleLink)
                },
                openInCustomTab = { articleLink ->
                    activityLauncher.openLinkInCustomTab(articleLink, toolbarColor)
                },
                navigateToArticle = {
                    activityLauncher.startActivity(false, Intent(
                        Intent.ACTION_VIEW,
                        "$DEEP_LINK_BASE_URI/article/${itemId}".toUri(),
                        ctx,
                        MainActivity::class.java,
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
            )
        },
        feedGridState = feedGridState,
        feedListState = feedListState,
        pagedFeedItems = pagedFeedItems,
        translatedFeedCards = translatedFeedCards,
        onTranslateFeedCard = viewModel::translateFeedCardIfNeeded,
        onOpenApp = {
            activityLauncher.startActivity(false, Intent(
                ctx,
                MainActivity::class.java,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewState: FeedScreenViewState,
    onRefreshVisible: () -> Unit,
    markAsUnread: (Long, Boolean) -> Unit,
    onOpenFeedItem: (Long) -> Unit,
    feedGridState: LazyStaggeredGridState,
    feedListState: LazyListState,
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    translatedFeedCards: TranslatedFeedCards,
    onTranslateFeedCard: (FeedListItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit,
) {
    FeedScreen(
        viewState = viewState,
        onRefreshVisible = onRefreshVisible,
        modifier = modifier,
        onOpenApp = onOpenApp,
        { innerModifier ->
            val screenType =
                when (isCompactDevice()) {
                    true -> FeedScreenType.FeedList
                    false -> FeedScreenType.FeedGrid
                }

            when (screenType) {
                FeedScreenType.FeedGrid ->
                    FeedGridContent(
                        viewState = viewState,
                        markAsUnread = markAsUnread,
                        onItemClick = onOpenFeedItem,
                        gridState = feedGridState,
                        pagedFeedItems = pagedFeedItems,
                        translatedFeedCards = translatedFeedCards,
                        onTranslateFeedCard = onTranslateFeedCard,
                        modifier = innerModifier,
                        onOpenApp = onOpenApp,
                    )

                FeedScreenType.FeedList ->
                    FeedListContent(
                        viewState = viewState,
                        markAsUnread = markAsUnread,
                        onItemClick = onOpenFeedItem,
                        listState = feedListState,
                        pagedFeedItems = pagedFeedItems,
                        translatedFeedCards = translatedFeedCards,
                        onTranslateFeedCard = onTranslateFeedCard,
                        modifier = innerModifier,
                        onOpenApp = onOpenApp,
                    )
            }
        },
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun FeedScreen(
    viewState: FeedScreenViewState,
    onRefreshVisible: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    var manuallyTriggeredRefresh by rememberSaveable {
        mutableStateOf(false)
    }
    val isRefreshing =
        remember(manuallyTriggeredRefresh, viewState.currentlySyncing) {
            (manuallyTriggeredRefresh || viewState.currentlySyncing)
        }

    LaunchedEffect(viewState.currentlySyncing) {
        if (manuallyTriggeredRefresh && viewState.currentlySyncing) {
            // A sync has happened so can safely set this to false now
            manuallyTriggeredRefresh = false
        }
    }

    LaunchedEffect(manuallyTriggeredRefresh) {
        // In the event that pulling doesn't trigger a refresh. Say if no feeds are present
        // or all feeds are so recent that no sync is triggered - or an error happens in sync
        // THEN we need to manually disable this variable so we don't get an infinite spinner
        if (manuallyTriggeredRefresh) {
            delay(5_000L)
            manuallyTriggeredRefresh = false
        }
    }

    val pullRefreshState =
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                manuallyTriggeredRefresh = true
                onRefreshVisible()
            },
        )

    Scaffold(
        floatingActionButton = {
            PlainTooltipBox(tooltip = { Text(stringResource(R.string.open_in_app)) }) {
                FloatingActionButton(
                    onClick = {
                        onOpenApp()
                    },
                    modifier =
                        Modifier
                            .navigationBarsPadding(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.open_in_app),
                    )
                }
            }
        },
        modifier =
            modifier
                // The order is important! PullToRefresh MUST come first
                .pullRefresh(state = pullRefreshState)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.statusBars,
        contentColor = Color.Transparent,
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .padding(padding),
        ) {
            content(
                Modifier,
            )

            PullRefreshIndicator(
                isRefreshing,
                pullRefreshState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedGridContent(
    viewState: FeedScreenViewState,
    markAsUnread: (Long, Boolean) -> Unit,
    onItemClick: (Long) -> Unit,
    gridState: LazyStaggeredGridState,
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    translatedFeedCards: TranslatedFeedCards,
    onTranslateFeedCard: (FeedListItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = !viewState.haveVisibleFeedItems,
        ) {
            // Keeping the Box behind so the scrollability doesn't override clickable
            // Separate box because scrollable will ignore max size.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            )
            NothingToRead(
                modifier = Modifier,
                onOpenApp = onOpenApp,
            )
        }

        val arrangement =
            when (viewState.feedItemStyle) {
                FeedItemStyle.CARD -> Arrangement.spacedBy(LocalDimens.current.gutter)
                FeedItemStyle.COMPACT_CARD -> Arrangement.spacedBy(LocalDimens.current.gutter)
                FeedItemStyle.COMPACT -> Arrangement.spacedBy(LocalDimens.current.gutter)
                FeedItemStyle.SUPER_COMPACT -> Arrangement.spacedBy(LocalDimens.current.gutter)
            }

        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = viewState.haveVisibleFeedItems,
        ) {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(LocalDimens.current.feedScreenColumns),
                contentPadding =
                    if (viewState.isBottomBarVisible) {
                        PaddingValues(0.dp)
                    } else {
                        WindowInsets.navigationBars
                            .only(
                                WindowInsetsSides.Bottom,
                            ).addMargin(LocalDimens.current.margin)
                            .asPaddingValues()
                    },
                verticalItemSpacing = LocalDimens.current.gutter,
                horizontalArrangement = arrangement,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = pagedFeedItems.itemCount,
                    key = pagedFeedItems.itemKey { it.id },
                    contentType = pagedFeedItems.itemContentType { it.contentType(viewState.feedItemStyle) },
                ) { itemIndex ->
                    val loadedItem = pagedFeedItems[itemIndex] ?: PLACEHOLDER_ITEM
                    val previewItem = translatedFeedCards.merge(loadedItem)

                    val itemCoroutineScope = rememberCoroutineScope()
                    var itemWasVisible by remember(previewItem.id) { mutableStateOf(false) }

                    LaunchedEffect(loadedItem.id, loadedItem.title, loadedItem.snippet, translatedFeedCards.generation, onTranslateFeedCard) {
                        onTranslateFeedCard(loadedItem)
                    }

                    // Gets executed when only unread items are being shown
                    // Marks items which have been visible as read when they scroll off screen
                    DisposableEffect(previewItem.id, markAsUnread) {
                        onDispose {
                            if (itemWasVisible) {
                                coroutineScope.launch {
                                    logDebug(LOG_TAG, "Marking ${previewItem.id} as read")
                                    markAsUnread(previewItem.id, false)
                                }
                            }
                        }
                    }

                    NonSwipeableFeedItemPreview(
                        item = previewItem,
                        showThumbnail = viewState.showThumbnails,
                        feedItemStyle = viewState.feedItemStyle,
                        bookmarkIndicator = !viewState.currentFeedOrTag.isSavedArticles,
                        maxLines = viewState.maxLines,
                        showOnlyTitle = viewState.showOnlyTitle,
                        showReadingTime = viewState.showReadingTime,
                        onItemClick = {
                            onItemClick(previewItem.id)
                        },
                        modifier =
                            if (viewState.markAsReadOnScroll && previewItem.unread) {
                                Modifier
                                    .trackVisibility(0.9f) { info ->
                                        if (info.isAboveThreshold) {
                                            // Using itemCoroutineScope because that scope gets destroyed when item scrolls off screen
                                            // So implicitly, if user is scrolling very fast, the coroutine will be cancelled
                                            // before marking as read
                                            itemCoroutineScope.launch {
                                                delay(REQUIRED_VISIBLE_TIME_FOR_MARK_AS_READ)
                                                if (viewState.filter.unread) {
                                                    logDebug(LOG_TAG, "Item $itemIndex marking as wasVisible")
                                                    itemWasVisible = true
                                                    // Marks as read in disposable effect
                                                } else {
                                                    logDebug(LOG_TAG, "Item $itemIndex marking as read")
                                                    markAsUnread(previewItem.id, false)
                                                }
                                            }
                                        }
                                    }
                                    .then(
                                        // Disable item animations during refresh to prevent scroll position issues
                                        if (!viewState.currentlySyncing) {
                                            Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                        } else {
                                            Modifier
                                        },
                                    )
                            } else {
                                Modifier.then(
                                    // Disable item animations during refresh to prevent scroll position issues
                                    if (!viewState.currentlySyncing) {
                                        Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    } else {
                                        Modifier
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun FeedListContent(
    viewState: FeedScreenViewState,
    markAsUnread: (Long, Boolean) -> Unit,
    onItemClick: (Long) -> Unit,
    listState: LazyListState,
    pagedFeedItems: LazyPagingItems<FeedListItem>,
    translatedFeedCards: TranslatedFeedCards,
    onTranslateFeedCard: (FeedListItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = !viewState.haveVisibleFeedItems,
        ) {
            // Keeping the Box behind so the scrollability doesn't override clickable
            // Separate box because scrollable will ignore max size.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            )
            NothingToRead(
                modifier = Modifier,
                onOpenApp = onOpenApp,
            )
        }

        val arrangement =
            when (viewState.feedItemStyle) {
                FeedItemStyle.CARD -> Arrangement.spacedBy(LocalDimens.current.margin)
                FeedItemStyle.COMPACT_CARD -> Arrangement.spacedBy(LocalDimens.current.margin)
                FeedItemStyle.COMPACT -> Arrangement.spacedBy(0.dp)
                FeedItemStyle.SUPER_COMPACT -> Arrangement.spacedBy(0.dp)
            }

        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = viewState.haveVisibleFeedItems,
        ) {
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = arrangement,
                contentPadding =
                    if (viewState.isBottomBarVisible) {
                        PaddingValues(0.dp)
                    } else {
                        WindowInsets.navigationBars
                            .only(
                                WindowInsetsSides.Bottom,
                            ).run {
                                when (viewState.feedItemStyle) {
                                    FeedItemStyle.CARD -> addMargin(horizontal = LocalDimens.current.margin)
                                    FeedItemStyle.COMPACT_CARD -> addMargin(horizontal = LocalDimens.current.margin)
                                    // No margin since dividers
                                    FeedItemStyle.COMPACT, FeedItemStyle.SUPER_COMPACT -> this
                                }
                            }.asPaddingValues()
                    },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeSemantics {
                            testTag = "feed_list"
                            collectionInfo = CollectionInfo(pagedFeedItems.itemCount, 1)
                        },
            ) {
                /*
                This is a trick to make the list stay at item 0 when updates come in IF it is
                scrolled to the top.
                 */
                item(
                    key = "SpacerScrollTrick",
                    contentType = "SpacerScrollTrick",
                ) {
                    Spacer(modifier = Modifier.fillMaxWidth())
                }
                items(
                    count = pagedFeedItems.itemCount,
                    key = pagedFeedItems.itemKey { it.id },
                    contentType = pagedFeedItems.itemContentType { it.contentType(viewState.feedItemStyle) },
                ) { itemIndex ->
                    val loadedItem = pagedFeedItems[itemIndex] ?: PLACEHOLDER_ITEM
                    val previewItem = translatedFeedCards.merge(loadedItem)

                    val itemCoroutineScope = rememberCoroutineScope()
                    var itemWasVisible by remember(previewItem.id) { mutableStateOf(false) }

                    LaunchedEffect(loadedItem.id, loadedItem.title, loadedItem.snippet, translatedFeedCards.generation, onTranslateFeedCard) {
                        onTranslateFeedCard(loadedItem)
                    }

                    // Gets executed when only unread items are being shown
                    // Marks items which have been visible as read when they scroll off screen
                    DisposableEffect(previewItem.id, markAsUnread) {
                        onDispose {
                            if (itemWasVisible) {
                                coroutineScope.launch {
                                    logDebug(LOG_TAG, "Marking ${previewItem.id} as read")
                                    markAsUnread(previewItem.id, false)
                                }
                            }
                        }
                    }

                    NonSwipeableFeedItemPreview(
                        item = previewItem,
                        showThumbnail = viewState.showThumbnails,
                        feedItemStyle = viewState.feedItemStyle,
                        bookmarkIndicator = !viewState.currentFeedOrTag.isSavedArticles,
                        maxLines = viewState.maxLines,
                        showOnlyTitle = viewState.showOnlyTitle,
                        showReadingTime = viewState.showReadingTime,
                        onItemClick = {
                            onItemClick(previewItem.id)
                        },
                        modifier =
                            Modifier
                                .then(
                                    // Disable item animations during refresh to prevent scroll position issues
                                    if (!viewState.currentlySyncing) {
                                        Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    } else {
                                        Modifier
                                    },
                                ).safeSemantics {
                                    collectionItemInfo =
                                        CollectionItemInfo(
                                            rowIndex = itemIndex,
                                            rowSpan = 1,
                                            columnIndex = 1,
                                            columnSpan = 1,
                                        )
                                }.let { modifier ->
                                    if (viewState.markAsReadOnScroll && previewItem.unread) {
                                        modifier.trackVisibility(0.9f) { info ->
                                            if (info.isAboveThreshold) {
                                                // Using itemCoroutineScope because that scope gets destroyed when item scrolls off screen
                                                // So implicitly, if user is scrolling very fast, the coroutine will be cancelled
                                                // before marking as read
                                                itemCoroutineScope.launch {
                                                    delay(REQUIRED_VISIBLE_TIME_FOR_MARK_AS_READ)
                                                    if (viewState.filter.unread) {
                                                        logDebug(LOG_TAG, "Item $itemIndex marking as wasVisible")
                                                        itemWasVisible = true
                                                        // Marks as read in disposable effect
                                                    } else {
                                                        logDebug(LOG_TAG, "Item $itemIndex marking as read")
                                                        markAsUnread(previewItem.id, false)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        modifier
                                    }
                                },
                    )

                    if (viewState.feedItemStyle != FeedItemStyle.CARD &&
                        viewState.feedItemStyle != FeedItemStyle.COMPACT_CARD
                    ) {
                        if (itemIndex < pagedFeedItems.itemCount - 1) {
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .height(1.dp)
                                        .fillMaxWidth(),
                            )
                        }
                    }
                }
                /*
                This item is provide padding for the FAB
                 */
                if (viewState.showFab && !viewState.isBottomBarVisible) {
                    item(
                        key = "SpacerForFab",
                        contentType = "SpacerForFab",
                    ) {
                        Spacer(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height((56 + 16).dp),
                        )
                    }
                }
            }
        }
    }
}

private const val REQUIRED_VISIBLE_TIME_FOR_MARK_AS_READ = 500L

private val PLACEHOLDER_ITEM =
    FeedListItem(
        id = ID_UNSET,
        title = "",
        snippet = "",
        feedTitle = "",
        unread = true,
        pubDate = "",
        image = null,
        link = null,
        bookmarked = false,
        feedImageUrl = null,
        primarySortTime = Instant.EPOCH,
        rawPubDate = null,
        wordCount = 0,
    )

@Composable
fun NonSwipeableFeedItemPreview(
    item: FeedListItem,
    showThumbnail: Boolean,
    feedItemStyle: FeedItemStyle,
    bookmarkIndicator: Boolean,
    maxLines: Int,
    showOnlyTitle: Boolean,
    showReadingTime: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val unreadLabel = stringResource(R.string.unread_adjective)
    val alreadyReadLabel = stringResource(R.string.read_adjective)
    val readStatusLabel by remember(item.unread) {
        derivedStateOf {
            if (item.unread) {
                unreadLabel
            } else {
                alreadyReadLabel
            }
        }
    }

    val dimens = LocalDimens.current

    Box(
        modifier =
            modifier
                .width(dimens.maxContentWidth)
                .clip(
                    shape =
                        when (feedItemStyle) {
                            FeedItemStyle.COMPACT, FeedItemStyle.SUPER_COMPACT -> RectangleShape
                            else -> MaterialTheme.shapes.medium
                        },
                )
                .combinedClickable(
                    onClick = onItemClick,
                )
                .safeSemantics {
                    stateDescription = readStatusLabel
                },
    ) {
        when (feedItemStyle) {
            FeedItemStyle.CARD -> {
                FeedItemCard(
                    item = item,
                    showThumbnail = showThumbnail,
                    bookmarkIndicator = bookmarkIndicator,
                    maxLines = maxLines,
                    showOnlyTitle = showOnlyTitle,
                    showReadingTime = showReadingTime
                )
            }

            FeedItemStyle.COMPACT_CARD -> {
                FeedItemCompactCard(
                    state =
                        FeedItemState(
                            item = item,
                            showThumbnail = showThumbnail,
                            dropDownMenuExpanded = false, // cannot be bothered removing, just send false, as this is not supported
                            bookmarkIndicator = bookmarkIndicator,
                            maxLines = maxLines,
                            showReadingTime = showReadingTime,
                        ),
                )
            }

            FeedItemStyle.COMPACT -> {
                FeedItemCompact(
                    item = item,
                    showThumbnail = showThumbnail,
                    bookmarkIndicator = bookmarkIndicator,
                    maxLines = maxLines,
                    showOnlyTitle = showOnlyTitle,
                    showReadingTime = showReadingTime,
                    imageWidth = 64.dp,
                )
            }

            FeedItemStyle.SUPER_COMPACT -> {
                FeedItemSuperCompact(
                    item = item,
                    bookmarkIndicator = bookmarkIndicator,
                    maxLines = maxLines,
                    showOnlyTitle = showOnlyTitle,
                    showReadingTime = showReadingTime,
                )
            }
        }
    }
}

@Composable
fun NothingToRead(
    modifier: Modifier = Modifier,
    onOpenApp: () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .padding(horizontal = LocalDimens.current.margin)
                .fillMaxHeight()
                .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.empty_feed_top),
                style =
                    MaterialTheme.typography.headlineMedium.merge(
                        TextStyle(fontWeight = FontWeight.Light),
                    ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .heightIn(min = TextFieldDefaults.MinHeight)
                        .fillMaxWidth()
                        .clickable {
                            onOpenApp()
                        },
            ) {
                Text(
                    text = annotatedStringResource(id = R.string.empty_feed_open_minus_one),
                    style =
                        MaterialTheme.typography.headlineMedium.merge(
                            TextStyle(fontWeight = FontWeight.Light),
                        ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

