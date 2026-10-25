package dev.jeyanth.citizenkane

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val ntr = FontFamily(Font(R.font.ntr_regular))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fixture = RuntimeMode.fixture(intent)
        val settings = SettingsStore(this, if (fixture == null) "settings" else "fixture-settings")
        if (fixture == null) MorningReminder.schedule(this, settings.load())
        val cache = ArticleCache(this, if (fixture == null) "cached-articles.json" else "fixture-articles.json")
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(settings, cache, NewsRepository(RuntimeMode.transport(fixture))) as T
        })[AppViewModel::class.java]
        setContent {
            val state by model.state.collectAsState()
            val dark = when (state.preferences.appearance) {
                Appearance.System -> isSystemInDarkTheme(); Appearance.Light -> false; Appearance.Dark -> true
            }
            val ink = if (dark) Color.White else Color.Black
            val paper = if (dark) Color.Black else Color.White
            val container = if (dark) Color(0xff242424) else Color(0xffeeeeee)
            val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
                primary = ink, onPrimary = paper, primaryContainer = container, onPrimaryContainer = ink,
                secondary = ink, onSecondary = paper, secondaryContainer = container, onSecondaryContainer = ink,
                tertiary = ink, onTertiary = paper, tertiaryContainer = container, onTertiaryContainer = ink,
                background = paper, onBackground = ink, surface = paper, onSurface = ink,
                surfaceVariant = container, onSurfaceVariant = ink.copy(alpha = .55f),
                outline = ink.copy(alpha = .4f), outlineVariant = ink.copy(alpha = .15f),
                inverseSurface = ink, inverseOnSurface = paper, inversePrimary = paper, surfaceTint = ink,
                error = ink, onError = paper, errorContainer = container, onErrorContainer = ink,
                surfaceDim = container, surfaceBright = paper, surfaceContainer = container,
                surfaceContainerLowest = paper, surfaceContainerLow = container,
                surfaceContainerHigh = container, surfaceContainerHighest = container,
            )
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
                }
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = paper, contentColor = ink) {
                    CitizenKane(state, model, fixture != null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitizenKane(state: AppState, model: AppViewModel, fixture: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var articleId by rememberSaveable { mutableStateOf<String?>(null) }
    val article = state.articles.firstOrNull { it.id == articleId }
    val tabState = rememberSaveableStateHolder()
    val newsBarState = rememberTopAppBarState()
    val rashiBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(if (tab == 0) newsBarState else rashiBarState)
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.background
    val end = if (paper == Color.Black) Color(0xff1a1a1a) else Color(0xffe8e8e8)
    val backdrop = Modifier.background(Brush.verticalGradient(listOf(paper, end)))
    BackHandler(tab != 0 && !settings && articleId == null) { tab = 0 }
    Scaffold(containerColor = Color.Transparent, modifier = Modifier.fillMaxSize().then(backdrop).nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Let Material's large/inline title slots crossfade at their proper sizes.
            MaterialTheme(typography = MaterialTheme.typography.copy(
                headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = ntr, fontSize = 34.sp, lineHeight = 44.sp),
                titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = ntr, fontSize = 17.sp, lineHeight = 22.sp),
            )) {
            LargeTopAppBar(title = {
                Text(if (tab == 0) "సిటిజన్ కేన్" else "Rashi Phalalu", fontFamily = ntr,
                    color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }, actions = {
                if (tab == 0) Surface(shape = RoundedCornerShape(28.dp), color = paper.copy(alpha = .8f), shadowElevation = 4.dp,
                    modifier = Modifier.padding(end = 12.dp)) {
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            }, colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent,
                scrolledContainerColor = paper.copy(alpha = .9f)), scrollBehavior = scrollBehavior)
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(40.dp), color = paper.copy(alpha = .86f), shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ink.copy(alpha = .06f))) {
                    Row(Modifier.width(196.dp).padding(4.dp).selectableGroup()) {
                        listOf("News" to Icons.Outlined.Newspaper, "Rashi" to Icons.Outlined.AutoAwesome).forEachIndexed { index, (label, icon) ->
                            Column(Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(32.dp))
                                .background(if (tab == index) ink.copy(alpha = .08f) else Color.Transparent)
                                .selectable(selected = tab == index, role = Role.Tab, onClick = { tab = index })
                                .padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, Modifier.size(26.dp)); Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = tab, animationSpec = tween(180), label = "Tab transition") { activeTab ->
                tabState.SaveableStateProvider(activeTab) {
                    if (activeTab == 0) NewsScreen(state, model::refreshNews) { articleId = it.id }
                    else RashiScreen(state, model::refreshRashi)
                }
            }
        }
    }
    if (settings) ModalBottomSheet(onDismissRequest = { settings = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = paper) {
        SettingsScreen(state.preferences, model::updatePreferences, { settings = false }, fixture)
    }
    if (article != null) ModalBottomSheet(onDismissRequest = { articleId = null },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = paper) {
        ArticleDetail(article, fixture, Modifier.then(backdrop))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsScreen(state: AppState, refresh: () -> Unit, open: (Article) -> Unit) {
    PullToRefreshBox(isRefreshing = state.newsLoading, onRefresh = refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().testTag("news-list"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.articles, key = { "${it.source}:${it.id}" }) { ArticleCard(it) { open(it) } }
            if (state.articles.isEmpty()) item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        state.newsLoading -> CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading news" })
                        state.newsError != null -> EmptyState("Couldn't load news", state.newsError, refresh)
                        else -> EmptyState("No sources enabled", "Enable a provider in Settings.", null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, open: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(onClick = open, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, ink.copy(alpha = .07f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(article.category.uppercase(), fontFamily = ntr, fontSize = 12.sp, color = ink)
                Text(article.title, fontFamily = ntr, fontSize = 18.sp, lineHeight = 24.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, color = ink)
                Text(article.source, fontSize = 12.sp, color = ink.copy(alpha = .55f))
            }
            ArticleImage(article.imageUrl, Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
        }
    }
}

@Composable
private fun ArticleDetail(article: Article, fixture: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    var browserError by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth().fillMaxHeight(.94f).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .7f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ArticleImage(article.imageUrl, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)))
                Text(article.category.uppercase(), fontFamily = ntr, fontSize = 12.sp)
                Text(article.title, fontFamily = ntr, fontSize = 24.sp, lineHeight = 32.sp)
                Text(article.source, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                if (!article.snippet.isNullOrEmpty()) {
                    HorizontalDivider()
                    Text(article.snippet, fontFamily = ntr, fontSize = 17.sp, lineHeight = 25.sp)
                }
                Button(onClick = {
                    if (fixture) browserError = "Synthetic article preview. External navigation is disabled for fixture tests."
                    else try { CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(article.url)) }
                    catch (_: ActivityNotFoundException) { browserError = "No browser is available to read this article." }
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Language, null); Spacer(Modifier.width(8.dp)); Text("Read Full Article") }
            }
        }
    }
    browserError?.let { message -> AlertDialog(onDismissRequest = { browserError = null },
        title = { Text("Read Full Article") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { browserError = null }) { Text("OK") } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RashiScreen(state: AppState, refresh: () -> Unit) {
    PullToRefreshBox(isRefreshing = state.rashiLoading, onRefresh = refresh, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            val prediction = state.prediction
            when {
                prediction != null -> {
                    Text(prediction.sign.telugu, fontFamily = ntr, fontSize = 24.sp)
                    Text(prediction.sign.english, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                    if (prediction.date.isNotEmpty()) Text(prediction.date, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    PredictionSection("Today", prediction.today)
                    if (prediction.weekly.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 20.dp))
                        PredictionSection("This Week", prediction.weekly)
                    }
                }
                state.rashiLoading -> Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading rashi phalalu" })
                }
                state.rashiError != null -> EmptyState("Couldn't load rashi phalalu", state.rashiError, refresh)
            }
        }
    }
}

@Composable private fun PredictionSection(title: String, body: String) {
    Text(title.uppercase(), fontFamily = ntr, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    Text(body, fontFamily = ntr, fontSize = 17.sp, lineHeight = 25.sp)
}

@Composable private fun EmptyState(title: String, message: String, retry: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Newspaper, null, Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
        if (retry != null) TextButton(onClick = retry) { Text("Try Again") }
    }
}

@Composable
private fun SettingsScreen(preferences: Preferences, update: (Preferences) -> Unit, done: () -> Unit, fixture: Boolean) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var licenses by rememberSaveable { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val latest by rememberUpdatedState(preferences)
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val value = latest.copy(reminder = granted && MorningReminder.permitted(context))
        update(value); MorningReminder.schedule(context, value)
        if (!value.reminder) denied = true
    }
    BackHandler(picker || licenses) { picker = false; licenses = false }
    Column(Modifier.fillMaxWidth().fillMaxHeight(.95f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (picker || licenses) IconButton(onClick = { picker = false; licenses = false }) { Icon(Icons.Outlined.ArrowBack, "Back to Settings") }
            Text(if (licenses) "Licenses" else if (picker) "Your Sign" else "Settings", fontFamily = ntr, fontSize = 28.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = done) { Text("Done") }
        }
        if (licenses) LicenseContent() else if (picker) LazyColumn {
            items(RashiSign.entries) { sign ->
                Row(Modifier.fillMaxWidth().clickable { update(preferences.copy(sign = sign)); picker = false }
                    .padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(sign.telugu, fontSize = 18.sp); Text(sign.english, fontSize = 12.sp) }
                    if (sign == preferences.sign) Icon(Icons.Outlined.Check, "Selected")
                }
            }
        } else Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SettingsHeading("Providers")
            providers.forEach { (id, name) ->
                SettingsToggle(name, id in preferences.enabledProviders) { checked ->
                    update(preferences.copy(enabledProviders = if (checked) preferences.enabledProviders + id else preferences.enabledProviders - id))
                }
            }
            SettingsHeading("Appearance")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Appearance.entries.forEachIndexed { index, mode ->
                    SegmentedButton(selected = mode == preferences.appearance, onClick = { update(preferences.copy(appearance = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, Appearance.entries.size)) { Text(mode.name) }
                }
            }
            SettingsHeading("Rashi Phalalu")
            Row(Modifier.fillMaxWidth().clickable { picker = true }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Your Sign", modifier = Modifier.weight(1f)); Text(preferences.sign.telugu); Icon(Icons.Outlined.ChevronRight, null)
            }
            SettingsHeading("Morning Notification")
            SettingsToggle("Remind me every morning", preferences.reminder) { enabled ->
                if (fixture && enabled) denied = true
                else if (!enabled) { val value = preferences.copy(reminder = false); update(value); MorningReminder.schedule(context, value) }
                else if (MorningReminder.permitted(context)) {
                    val value = preferences.copy(reminder = true); update(value); MorningReminder.schedule(context, value)
                } else if (Build.VERSION.SDK_INT >= 33) request.launch(Manifest.permission.POST_NOTIFICATIONS)
                else denied = true
            }
            if (preferences.reminder) TextButton(onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    val value = latest.copy(hour = hour, minute = minute); update(value); MorningReminder.schedule(context, value)
                }, preferences.hour, preferences.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
            }) { Text("Time: %02d:%02d".format(preferences.hour, preferences.minute)) }
            SettingsHeading("About")
            TextButton(onClick = { licenses = true }, modifier = Modifier.fillMaxWidth()) { Text("Open-source licenses") }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (denied) AlertDialog(onDismissRequest = { denied = false }, title = { Text("Notifications Disabled") },
        text = { Text(if (fixture) "Fixture tests do not request notification permissions or schedule reminders."
            else "Enable notifications for Citizen Kane in Android Settings to get a morning reminder.") },
        confirmButton = { TextButton(onClick = { denied = false }) { Text("OK") } })
}

@Composable private fun LicenseContent() {
    val context = LocalContext.current
    val notices by produceState<String?>(null) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("THIRD_PARTY_NOTICES.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
    if (notices == null) CircularProgressIndicator(Modifier.padding(24.dp).semantics { contentDescription = "Loading licenses" })
    else SelectionContainer {
        Text(notices!!, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun SettingsHeading(title: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 12.dp))
    Text(title.uppercase(), fontFamily = ntr, fontSize = 14.sp)
}
@Composable private fun SettingsToggle(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = changed, modifier = Modifier.semantics { contentDescription = label })
    }
}
