package com.example.data.model

import com.squareup.moshi.JsonClass

// Unified types
enum class MediaType {
    MOVIE, SERIES, ANIME, MANGA
}

data class UnifiedMedia(
    val id: String,
    val title: String,
    val type: MediaType,
    val posterUrl: String,
    val bannerUrl: String = "",
    val year: String = "",
    val rating: String = "",
    val description: String = "",
    val sourceId: String = "" // installed extension identifier
)

data class UnifiedEpisode(
    val id: String,
    val title: String,
    val episodeNumber: Int,
    val seasonNumber: Int = 1,
    val released: String = "",
    val overview: String = ""
)

data class UnifiedChapter(
    val id: String,
    val title: String,
    val chapterNumber: Double,
    val datePosted: String = ""
)

data class UnifiedStream(
    val title: String,
    val url: String = "",
    val infoHash: String = "", // for torrent streams
    val season: Int = 1,
    val episode: Int = 1,
    val quality: String = "1080p",
    val serverName: String = "Direct",
    val description: String = ""
)

// Stremio Addon Protocol Specific Models
@JsonClass(generateAdapter = true)
data class StremioManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val types: List<String> = emptyList(),
    val resources: List<Any> = emptyList(), // Can be strings or resource objects
    val catalogs: List<StremioCatalog> = emptyList(),
    val background: String = "",
    val logo: String = ""
)

@JsonClass(generateAdapter = true)
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<StremioExtra>? = null
)

@JsonClass(generateAdapter = true)
data class StremioExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioCatalogResult(
    val metas: List<StremioMetaSummary> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioMetaSummary(
    val id: String,
    val type: String,
    val name: String,
    val poster: String = "",
    val posterShape: String = "poster",
    val banner: String = "",
    val background: String = "",
    val logo: String = "",
    val year: String = "",
    val imdbRating: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class StremioMetaResult(
    val meta: StremioMetaDetail
)

@JsonClass(generateAdapter = true)
data class StremioMetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String = "",
    val background: String = "",
    val logo: String = "",
    val year: String = "",
    val imdbRating: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val videos: List<StremioVideo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioVideo(
    val id: String,
    val title: String,
    val released: String = "",
    val episode: Int = 0,
    val season: Int = 0,
    val overview: String = "",
    val thumbnail: String = ""
)

@JsonClass(generateAdapter = true)
data class StremioStreamResult(
    val streams: List<StremioStream> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StremioStream(
    val title: String = "",
    val name: String = "",
    val url: String = "",
    val infoHash: String = "",
    val fileIdx: Int = 0,
    val behaviorHints: StremioBehaviorHints? = null
)

@JsonClass(generateAdapter = true)
data class StremioBehaviorHints(
    val notSearchable: Boolean = false,
    val headers: Map<String, String> = emptyMap()
)
