package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.InstalledExtension
import com.example.data.database.MediaBookmark
import com.example.data.database.WatchHistory
import com.example.data.model.*
import com.example.data.repository.StreamingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaViewModel(
    application: Application,
    private val repository: StreamingRepository
) : AndroidViewModel(application) {

    // Dynamic Lists from Database
    val extensions: StateFlow<List<InstalledExtension>> = repository.allExtensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<MediaBookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistory>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State Holders
    private val _homeMovies = MutableStateFlow<List<UnifiedMedia>>(emptyList())
    val homeMovies: StateFlow<List<UnifiedMedia>> = _homeMovies.asStateFlow()

    private val _homeAnime = MutableStateFlow<List<UnifiedMedia>>(emptyList())
    val homeAnime: StateFlow<List<UnifiedMedia>> = _homeAnime.asStateFlow()

    private val _homeManga = MutableStateFlow<List<UnifiedMedia>>(emptyList())
    val homeManga: StateFlow<List<UnifiedMedia>> = _homeManga.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    // Search state
    private val _searchResults = MutableStateFlow<List<UnifiedMedia>>(emptyList())
    val searchResults: StateFlow<List<UnifiedMedia>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Details state
    private val _selectedMediaDetails = MutableStateFlow<UnifiedMedia?>(null)
    val selectedMediaDetails: StateFlow<UnifiedMedia?> = _selectedMediaDetails.asStateFlow()

    private val _episodes = MutableStateFlow<List<UnifiedEpisode>>(emptyList())
    val episodes: StateFlow<List<UnifiedEpisode>> = _episodes.asStateFlow()

    private val _chapters = MutableStateFlow<List<UnifiedChapter>>(emptyList())
    val chapters: StateFlow<List<UnifiedChapter>> = _chapters.asStateFlow()

    private val _streams = MutableStateFlow<List<UnifiedStream>>(emptyList())
    val streams: StateFlow<List<UnifiedStream>> = _streams.asStateFlow()

    private val _isDetailsLoading = MutableStateFlow(false)
    val isDetailsLoading: StateFlow<Boolean> = _isDetailsLoading.asStateFlow()

    // Reader state
    private val _mangaPages = MutableStateFlow<List<String>>(emptyList())
    val mangaPages: StateFlow<List<String>> = _mangaPages.asStateFlow()

    private val _isReaderLoading = MutableStateFlow(false)
    val isReaderLoading: StateFlow<Boolean> = _isReaderLoading.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                // 1. Fetch Movies from Stremio Cinemeta popular catalogue
                val stremioMovies = repository.fetchStremioCatalog(
                    baseUrl = "https://v3-cinemeta.strem.io",
                    type = "movie",
                    catalogId = "top"
                )
                _homeMovies.value = stremioMovies.take(15)

                // 2. Fetch popular Anime from Pahe/Gogo index
                val animeList = repository.fetchAnimePahePopular()
                _homeAnime.value = animeList.take(15)

                // 3. Fetch popular Manga from MangaDex index
                val mangaList = repository.browseMangaDexPopular()
                _homeManga.value = mangaList.take(15)

            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed to load home content", e)
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun searchAll(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Perform concurrent searches
                val stremioResults = repository.searchStremio("https://v3-cinemeta.strem.io", query, "movie")
                val animeResults = repository.searchAnimePahe(query)
                val mangaResults = repository.searchMangaDex(query)

                // Merge and update results
                val merged = (stremioResults + animeResults + mangaResults).sortedByDescending { it.rating }
                _searchResults.value = merged
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Search failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectMedia(media: UnifiedMedia) {
        viewModelScope.launch {
            _selectedMediaDetails.value = media
            _episodes.value = emptyList()
            _chapters.value = emptyList()
            _streams.value = emptyList()
            _isDetailsLoading.value = true

            try {
                when (media.type) {
                    MediaType.MOVIE, MediaType.SERIES -> {
                        // Query Cinemeta meta detail
                        val stremioMetaId = media.id.substringAfterLast(":")
                        val detail = repository.fetchStremioDetails(
                            baseUrl = "https://v3-cinemeta.strem.io",
                            type = if (media.type == MediaType.MOVIE) "movie" else "series",
                            id = stremioMetaId
                        )
                        if (detail != null) {
                            _selectedMediaDetails.value = media.copy(
                                description = detail.description,
                                bannerUrl = detail.background.ifEmpty { media.bannerUrl }
                            )
                            if (media.type == MediaType.SERIES) {
                                _episodes.value = detail.videos.map {
                                    UnifiedEpisode(
                                        id = it.id,
                                        title = "${it.title} (S${it.season}E${it.episode})",
                                        episodeNumber = it.episode,
                                        seasonNumber = it.season,
                                        released = it.released,
                                        overview = it.overview
                                    )
                                }.sortedWith(compareBy<UnifiedEpisode> { it.seasonNumber }.thenBy { it.episodeNumber })
                            } else {
                                // For single movies, fetch streams immediately
                                fetchMovieStreams(stremioMetaId, media.type)
                            }
                        }
                    }
                    MediaType.ANIME -> {
                        val animeId = media.id.substringAfter("animepahe:")
                        val episodesList = repository.fetchAnimePaheEpisodes(animeId)
                        _episodes.value = episodesList
                    }
                    MediaType.MANGA -> {
                        val mangaId = media.id.substringAfter("mangadex:")
                        val chaptersList = repository.fetchMangaDexChapters(mangaId)
                        _chapters.value = chaptersList
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed to load details", e)
            } finally {
                _isDetailsLoading.value = false
            }
        }
    }

    fun fetchEpisodeStreams(episode: UnifiedEpisode) {
        val media = _selectedMediaDetails.value ?: return
        viewModelScope.launch {
            _streams.value = emptyList()
            _isDetailsLoading.value = true
            try {
                when (media.type) {
                    MediaType.SERIES -> {
                        // Series episode query format: "imdb_id:season:episode" or similar
                        val stremioMetaId = media.id.substringAfterLast(":")
                        val encodedId = java.net.URLEncoder.encode("${stremioMetaId}:${episode.seasonNumber}:${episode.episodeNumber}", "UTF-8")
                        val results = repository.fetchStremioStreams("https://torrentio.strem.fun", "series", encodedId)
                        // Fallback to community if Torrentio empty
                        if (results.isEmpty()) {
                            val communityResult = repository.fetchStremioStreams("https://v3-cinemeta.strem.io", "series", encodedId)
                            _streams.value = communityResult
                        } else {
                            _streams.value = results
                        }
                    }
                    MediaType.ANIME -> {
                        val results = repository.fetchAnimePaheStreams(episode.id)
                        _streams.value = results
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed fetching streams", e)
            } finally {
                _isDetailsLoading.value = false
            }
        }
    }

    private suspend fun fetchMovieStreams(metaId: String, type: MediaType) {
        val results = repository.fetchStremioStreams("https://torrentio.strem.fun", "movie", metaId)
        if (results.isEmpty()) {
            val communityStream = repository.fetchStremioStreams("https://v3-cinemeta.strem.io", "movie", metaId)
            _streams.value = communityStream
        } else {
            _streams.value = results
        }
    }

    fun loadMangaPages(chapterId: String) {
        viewModelScope.launch {
            _mangaPages.value = emptyList()
            _isReaderLoading.value = true
            try {
                val pages = repository.fetchMangaDexPages(chapterId)
                _mangaPages.value = pages
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed to load manga pages", e)
            } finally {
                _isReaderLoading.value = false
            }
        }
    }

    // Bookmark actions
    fun toggleBookmark(media: UnifiedMedia, isBookmarked: Boolean) {
        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeBookmark(media.id)
            } else {
                repository.addBookmark(media)
            }
        }
    }

    fun isBookmarkedFlow(id: String): Flow<Boolean> = repository.isBookmarked(id)

    // History tracking actions
    fun trackVideoWatchProgress(
        mediaId: String,
        title: String,
        type: MediaType,
        poster: String,
        episodeNum: Int,
        seasonNum: Int,
        progressMs: Long,
        totalMs: Long
    ) {
        viewModelScope.launch {
            repository.saveWatchProgress(
                mediaId = mediaId,
                title = title,
                type = type,
                poster = poster,
                episode = episodeNum,
                season = seasonNum,
                progress = progressMs,
                total = totalMs
            )
        }
    }

    fun trackMangaReadProgress(
        mediaId: String,
        title: String,
        poster: String,
        chapterNum: Int,
        pageIndex: Int
    ) {
        viewModelScope.launch {
            repository.saveWatchProgress(
                mediaId = mediaId,
                title = title,
                type = MediaType.MANGA,
                poster = poster,
                readChapter = chapterNum,
                readPage = pageIndex
            )
        }
    }

    fun deleteHistory(mediaId: String) {
        viewModelScope.launch {
            repository.clearHistoryItem(mediaId)
        }
    }

    // Add extensions
    fun addNewExtension(name: String, type: String, url: String, desc: String) {
        viewModelScope.launch {
            repository.installExtension(name, type, url, desc)
        }
    }

    fun removeExtension(ext: InstalledExtension) {
        viewModelScope.launch {
            repository.removeExtension(ext)
        }
    }
}

class MediaViewModelFactory(
    private val application: Application,
    private val repository: StreamingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
