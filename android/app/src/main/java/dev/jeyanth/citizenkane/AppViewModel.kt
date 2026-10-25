package dev.jeyanth.citizenkane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppState(
    val preferences: Preferences = Preferences(), val articles: List<Article> = emptyList(),
    val newsLoading: Boolean = false, val newsError: String? = null,
    val prediction: Prediction? = null, val rashiLoading: Boolean = false, val rashiError: String? = null,
)

class AppViewModel(
    private val settings: SettingsStore, private val cache: ArticleCache, private val repository: NewsRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(AppState(preferences = settings.load(), articles = cache.load()))
    val state = mutable.asStateFlow()
    private var newsJob: Job? = null
    private var rashiJob: Job? = null
    init { refreshNews(); refreshRashi() }

    fun updatePreferences(value: Preferences) {
        val old = mutable.value.preferences
        settings.save(value)
        mutable.update { it.copy(preferences = value) }
        if (old.enabledProviders != value.enabledProviders) refreshNews()
        if (old.sign != value.sign) {
            mutable.update { it.copy(prediction = null) }
            refreshRashi()
        }
    }

    fun refreshNews() {
        newsJob?.cancel()
        val enabled = mutable.value.preferences.enabledProviders
        if (enabled.isEmpty()) {
            mutable.update { it.copy(articles = emptyList(), newsLoading = false, newsError = null) }
            return
        }
        newsJob = viewModelScope.launch {
            mutable.update { it.copy(newsLoading = true, newsError = null) }
            try {
                val result = repository.news(enabled)
                if (result.articles.isNotEmpty()) {
                    withContext(Dispatchers.IO) { runCatching { cache.save(result.articles) } }
                    mutable.update { it.copy(articles = result.articles, newsLoading = false, newsError = null) }
                } else {
                    mutable.update { it.copy(newsLoading = false, newsError = "The providers could not return news. Check your connection and try again.") }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutable.update { it.copy(newsLoading = false, newsError = "Couldn't connect. Pull to refresh and try again.") }
            }
        }
    }

    fun refreshRashi() {
        rashiJob?.cancel()
        val sign = mutable.value.preferences.sign
        rashiJob = viewModelScope.launch {
            mutable.update { it.copy(rashiLoading = true, rashiError = null) }
            try {
                val prediction = repository.rashi(sign)
                mutable.update { it.copy(prediction = prediction, rashiLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutable.update { it.copy(rashiLoading = false, rashiError = "Check your connection and pull to refresh.") }
            }
        }
    }
}
