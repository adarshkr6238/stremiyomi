package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.database.InstalledExtension
import com.example.data.database.MediaBookmark
import com.example.data.database.WatchHistory
import com.example.data.model.*
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavigationContainer(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf("home") }
    var selectedMediaForDetails by remember { mutableStateOf<UnifiedMedia?>(null) }
    var currentActiveStreamUrl by remember { mutableStateOf<String?>(null) }
    var currentActiveMangaChapterId by remember { mutableStateOf<String?>(null) }

    // Navigation back handlers list
    if (currentActiveStreamUrl != null) {
        BackHandler { currentActiveStreamUrl = null }
        Box(modifier = Modifier.fillMaxSize()) {
            VideoPlayerScreen(
                streamUrl = currentActiveStreamUrl!!,
                media = selectedMediaForDetails!!,
                viewModel = viewModel,
                onClose = { currentActiveStreamUrl = null }
            )
        }
    } else if (currentActiveMangaChapterId != null) {
        BackHandler { currentActiveMangaChapterId = null }
        Box(modifier = Modifier.fillMaxSize()) {
            MangaReaderScreen(
                chapterId = currentActiveMangaChapterId!!,
                manga = selectedMediaForDetails!!,
                viewModel = viewModel,
                onClose = { currentActiveMangaChapterId = null }
            )
        }
    } else if (selectedMediaForDetails != null) {
        BackHandler { selectedMediaForDetails = null }
        Box(modifier = Modifier.fillMaxSize()) {
            MediaDetailsScreen(
                media = selectedMediaForDetails!!,
                viewModel = viewModel,
                onBack = { selectedMediaForDetails = null },
                onPlayStream = { streamUrl ->
                    currentActiveStreamUrl = streamUrl
                },
                onReadChapter = { chapterId ->
                    currentActiveMangaChapterId = chapterId
                }
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.testTag("app_bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == "home",
                        onClick = { currentTab = "home" },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_tab_explore")
                    )
                    NavigationBarItem(
                        selected = currentTab == "search",
                        onClick = { currentTab = "search" },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        modifier = Modifier.testTag("nav_tab_search")
                    )
                    NavigationBarItem(
                        selected = currentTab == "extensions",
                        onClick = { currentTab = "extensions" },
                        icon = { Icon(Icons.Filled.Extension, contentDescription = "Extensions") },
                        label = { Text("Extensions") },
                        modifier = Modifier.testTag("nav_tab_extensions")
                    )
                    NavigationBarItem(
                        selected = currentTab == "library",
                        onClick = { currentTab = "library" },
                        icon = { Icon(Icons.Filled.FolderCopy, contentDescription = "Library") },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_tab_library")
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentTab) {
                    "home" -> HomeScreen(viewModel, onMediaSelected = {
                        viewModel.selectMedia(it)
                        selectedMediaForDetails = it
                    })
                    "search" -> SearchScreen(viewModel, onMediaSelected = {
                        viewModel.selectMedia(it)
                        selectedMediaForDetails = it
                    })
                    "extensions" -> ExtensionsScreen(viewModel)
                    "library" -> LibraryScreen(viewModel, onMediaSelected = {
                        viewModel.selectMedia(it)
                        selectedMediaForDetails = it
                    })
                }
            }
        }
    }
}

// 1. HomeScreen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediaViewModel,
    onMediaSelected: (UnifiedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.homeMovies.collectAsState()
    val anime by viewModel.homeAnime.collectAsState()
    val manga by viewModel.homeManga.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // App header banner
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Premium Stremio x Aniyomi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Omni Media Ecosystem",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isHomeLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Hot Stremio Cinemeta Movies
            MediaSectionRow(
                title = "Popular Channels (Stremio Movies)",
                icon = Icons.Filled.Movie,
                mediaList = movies,
                onMediaSelected = onMediaSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Popular anime list from Aniyomi Core Pahe
            MediaSectionRow(
                title = "Anime Broadcasts (Aniyomi Core)",
                icon = Icons.Filled.Tv,
                mediaList = anime,
                onMediaSelected = onMediaSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Popular manga translation directories
            MediaSectionRow(
                title = "Manga Read Catalogs (MangaDex)",
                icon = Icons.Filled.Book,
                mediaList = manga,
                onMediaSelected = onMediaSelected
            )
        }
    }
}

@Composable
fun MediaSectionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    mediaList: List<UnifiedMedia>,
    onMediaSelected: (UnifiedMedia) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No media loaded. Check internet or extensions.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mediaList) { media ->
                    MediaPosterCard(media = media, onClick = { onMediaSelected(media) })
                }
            }
        }
    }
}

@Composable
fun MediaPosterCard(
    media: UnifiedMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .clickable(onClick = onClick)
            .testTag("media_card_${media.id.hashCode()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Rating label overlay
                if (media.rating.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = media.rating,
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Media Type indicator icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = media.type.name,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = media.title,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// 2. SearchScreen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MediaViewModel,
    onMediaSelected: (UnifiedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Global Federated Crawl",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Omni Search Index",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchAll(it)
            },
            placeholder = { Text("Search Movies, Anime, Manga...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.searchAll("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.YoutubeSearchedFor,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter search queries to scan Stremio & Aniyomi catalogs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { media ->
                    MediaPosterCard(media = media, onClick = { onMediaSelected(media) })
                }
            }
        }
    }
}

// 3. ExtensionsScreen Composable (Stremio & Aniyomi)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val extensions by viewModel.extensions.collectAsState()
    var isAddDialogShowing by remember { mutableStateOf(false) }

    // Dialog Input states
    var extName by remember { mutableStateOf("") }
    var extType by remember { mutableStateOf("stremio") }
    var extUrl by remember { mutableStateOf("") }
    var extDesc by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogShowing = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_extension_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Extension")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Federation Registries",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Addons & Ext Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(extensions) { ext ->
                    ExtensionCard(ext = ext, onRemove = {
                        viewModel.removeExtension(ext)
                    })
                }
            }
        }
    }

    if (isAddDialogShowing) {
        AlertDialog(
            onDismissRequest = { isAddDialogShowing = false },
            title = { Text("Assemble Custom Crawler") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = extName,
                        onValueChange = { extName = it },
                        label = { Text("Addon/Extension Name") },
                        modifier = Modifier.fillMaxWidth().testTag("ext_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = extType == "stremio",
                            onClick = { extType = "stremio" },
                            label = { Text("Stremio Addon") },
                            modifier = Modifier.testTag("chip_stremio")
                        )
                        FilterChip(
                            selected = extType == "anime",
                            onClick = { extType = "anime" },
                            label = { Text("Aniyomi Anime") },
                            modifier = Modifier.testTag("chip_anime")
                        )
                        FilterChip(
                            selected = extType == "manga",
                            onClick = { extType = "manga" },
                            label = { Text("Aniyomi Manga") },
                            modifier = Modifier.testTag("chip_manga")
                        )
                    }

                    OutlinedTextField(
                        value = extUrl,
                        onValueChange = { extUrl = it },
                        label = { Text("Manifest/API URL path") },
                        placeholder = { Text("https://example.com/manifest.json") },
                        modifier = Modifier.fillMaxWidth().testTag("ext_url_input")
                    )

                    OutlinedTextField(
                        value = extDesc,
                        onValueChange = { extDesc = it },
                        label = { Text("Crawler Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (extName.isNotEmpty() && extUrl.isNotEmpty()) {
                            viewModel.addNewExtension(extName, extType, extUrl, extDesc)
                            isAddDialogShowing = false
                            extName = ""
                            extUrl = ""
                            extDesc = ""
                        }
                    },
                    modifier = Modifier.testTag("dialog_submit")
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddDialogShowing = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper functions removed

@Composable
fun ExtensionCard(
    ext: InstalledExtension,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ext.iconUrl,
                contentDescription = ext.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ext.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (ext.type) {
                                    "stremio" -> Color(0xFF673AB7)
                                    "manga" -> Color(0xFF00bcd4)
                                    else -> Color(0xFF4CAF50)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ext.type.uppercase(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ext.description.ifEmpty { ext.baseUrl },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!ext.isBuiltIn) {
                IconButton(onClick = onRemove, modifier = Modifier.testTag("remove_ext_${ext.id}")) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove Extension",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Native Source",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// 4. LibraryScreen Composable (Bookmarks + History)
@Composable
fun LibraryScreen(
    viewModel: MediaViewModel,
    onMediaSelected: (UnifiedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.watchHistory.collectAsState()
    var selectedSection by remember { mutableStateOf("bookmarks") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Archives & Records",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Personal Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Center Switch Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedSection = "bookmarks" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSection == "bookmarks") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f).testTag("tab_toggle_bookmarks")
            ) {
                Icon(Icons.Filled.Bookmark, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bookmarks (${bookmarks.size})", color = if (selectedSection == "bookmarks") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { selectedSection = "history" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSection == "history") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f).testTag("tab_toggle_history")
            ) {
                Icon(Icons.Filled.History, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("History (${history.size})", color = if (selectedSection == "history") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSection == "bookmarks") {
            if (bookmarks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No bookmarks yet. Click favorite on details screen!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(bookmarks) { bookmark ->
                        val uMedia = UnifiedMedia(
                            id = bookmark.id,
                            title = bookmark.title,
                            type = MediaType.valueOf(bookmark.type),
                            posterUrl = bookmark.poster,
                            bannerUrl = bookmark.banner,
                            year = bookmark.year,
                            rating = bookmark.rating,
                            description = bookmark.description
                        )
                        MediaPosterCard(media = uMedia, onClick = { onMediaSelected(uMedia) })
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No playback history records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(history) { item ->
                        HistoryRow(item = item, onClick = {
                            val uMedia = UnifiedMedia(
                                id = item.mediaId,
                                title = item.title,
                                type = MediaType.valueOf(item.type),
                                posterUrl = item.poster
                            )
                            onMediaSelected(uMedia)
                        }, onDelete = {
                            viewModel.deleteHistory(item.mediaId)
                        })
                    }
                }
            }
        }
    }
}

// Icon helper removed

@Composable
fun HistoryRow(
    item: WatchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val desc = when (MediaType.valueOf(item.type)) {
                    MediaType.MOVIE -> "Watched progress"
                    MediaType.SERIES, MediaType.ANIME -> "Last: Season ${item.lastWatchedSeason} Ep ${item.lastWatchedEpisode}"
                    MediaType.MANGA -> "Last: Chapter ${item.lastReadChapter}"
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.lastWatchedTotal > 0 && MediaType.valueOf(item.type) != MediaType.MANGA) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.lastWatchedProgress.toFloat() / item.lastWatchedTotal.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_hist_${item.mediaId}")) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 5. MediaDetailsScreen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    media: UnifiedMedia,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onPlayStream: (String) -> Unit,
    onReadChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val details by viewModel.selectedMediaDetails.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isDetailsLoading by viewModel.isDetailsLoading.collectAsState()
    val isBookmarked by viewModel.isBookmarkedFlow(media.id).collectAsState(initial = false)

    var selectedEpisodeForStream by remember { mutableStateOf<UnifiedEpisode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = details?.title ?: media.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("details_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark(media, isBookmarked) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = "Favorite",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Dynamic Header Banner with blur overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = details?.bannerUrl?.ifEmpty { details?.posterUrl ?: media.posterUrl },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                startY = 100f
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Info Section Title
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = details?.posterUrl ?: media.posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(100.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = details?.title ?: media.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = media.type.name,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = details?.year ?: media.year,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (details?.rating?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = details!!.rating, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details?.description?.ifEmpty { "Synchronizing synopsis info from extension database nodes..." }
                        ?: media.description.ifEmpty { "Synchronizing synopsis info from extension database nodes..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isDetailsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Display Chapters or Episodes based on MediaType
                    when (media.type) {
                        MediaType.MOVIE -> {
                            Text(
                                text = "Available Community Stream Servers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (streams.isEmpty()) {
                                Text("Scanning and crawling Torrentio/Debrid gateways for video streams...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    streams.forEach { stream ->
                                        StreamServerListItem(stream = stream, onClick = { onPlayStream(stream.url) })
                                    }
                                }
                            }
                        }
                        MediaType.SERIES, MediaType.ANIME -> {
                            Text(
                                text = "Episodes Collection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (episodes.isEmpty()) {
                                Text("No episodes parsed for this feed catalog.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(episodes) { ep ->
                                        FilterChip(
                                            selected = selectedEpisodeForStream == ep,
                                            onClick = {
                                                selectedEpisodeForStream = ep
                                                viewModel.fetchEpisodeStreams(ep)
                                            },
                                            label = { Text("Episode ${ep.episodeNumber}") },
                                            modifier = Modifier.testTag("ep_chip_${ep.episodeNumber}")
                                        )
                                    }
                                }

                                if (selectedEpisodeForStream != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Stream Servers for Episode ${selectedEpisodeForStream!!.episodeNumber}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (streams.isEmpty()) {
                                        Text("Extracting media links from server nodes...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            streams.forEach { stream ->
                                                StreamServerListItem(stream = stream, onClick = {
                                                    // Track history first
                                                    viewModel.trackVideoWatchProgress(
                                                        mediaId = media.id,
                                                        title = media.title,
                                                        type = media.type,
                                                        poster = media.posterUrl,
                                                        episodeNum = selectedEpisodeForStream!!.episodeNumber,
                                                        seasonNum = selectedEpisodeForStream!!.seasonNumber,
                                                        progressMs = 0L,
                                                        totalMs = 0L
                                                    )
                                                    onPlayStream(stream.url)
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        MediaType.MANGA -> {
                            Text(
                                text = "Chapters Directory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (chapters.isEmpty()) {
                                Text("No chapters parsed from MangaDex node indices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chapters.take(40).forEach { chap ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.trackMangaReadProgress(
                                                        mediaId = media.id,
                                                        title = media.title,
                                                        poster = media.posterUrl,
                                                        chapterNum = chap.chapterNumber.toInt(),
                                                        pageIndex = 0
                                                    )
                                                    viewModel.loadMangaPages(chap.id)
                                                    onReadChapter(chap.id)
                                                }
                                                .testTag("manga_chap_${chap.chapterNumber.toInt()}"),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(text = "Chapter ${chap.chapterNumber} : ${chap.title}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamServerListItem(
    stream: UnifiedStream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stream_link_${stream.title.hashCode()}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stream.serverName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = stream.title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = stream.quality, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// 6. Fullscreen Video Player Screen (Jetpack Media3 ExoPlayer Wrapper)
@Composable
fun VideoPlayerScreen(
    streamUrl: String,
    media: UnifiedMedia,
    viewModel: MediaViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val testUrl = if (streamUrl.isBlank() || streamUrl.contains("stream/")) {
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    } else {
        streamUrl
    }

    // Instantiates Media3 ExoPlayer inside Composable Lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(testUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Track playback state for saving progress periodically
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            val progress = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (duration > 0) {
                viewModel.trackVideoWatchProgress(
                    mediaId = media.id,
                    title = media.title,
                    type = media.type,
                    poster = media.posterUrl,
                    episodeNum = 1,
                    seasonNum = 1,
                    progressMs = progress,
                    totalMs = duration
                )
            }
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_video_player")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Superimposes overlay close buttons
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .size(48.dp)
                .testTag("player_close_btn_overlay")
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close Player", tint = Color.White)
        }
    }
}


// 7. Fullscreen MangaReaderScreen Composable (MangaDex Images)
@Composable
fun MangaReaderScreen(
    chapterId: String,
    manga: UnifiedMedia,
    viewModel: MediaViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages by viewModel.mangaPages.collectAsState()
    val isReaderLoading by viewModel.isReaderLoading.collectAsState()
    var currentPageIndex by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.trackMangaReadProgress(
                mediaId = manga.id,
                title = manga.title,
                poster = manga.posterUrl,
                chapterNum = 1,
                pageIndex = currentPageIndex
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_manga_reader")
    ) {
        if (isReaderLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to retrieve chap sheets.", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onClose) {
                        Text("Return")
                    }
                }
            }
        } else {
            // Horizontal full width manga pager
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Touch next page logic
                            if (currentPageIndex < pages.size - 1) {
                                currentPageIndex++
                            } else {
                                onClose()
                            }
                        }
                ) {
                    AsyncImage(
                        model = pages[currentPageIndex],
                        contentDescription = "Chapter sheet page ${currentPageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Controls and Page number overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Previous page", tint = if (currentPageIndex > 0) Color.White else Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Sheet ${currentPageIndex + 1} / ${pages.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ },
                        enabled = currentPageIndex < pages.size - 1
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next page", tint = if (currentPageIndex < pages.size - 1) Color.White else Color.Gray)
                    }
                }
            }
        }

        // Overlay close button top right
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .size(48.dp)
                .testTag("reader_close_btn_overlay")
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close reader", tint = Color.White)
        }
    }
}
