// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package helium314.keyboard.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.key
import helium314.keyboard.settings.preferences.LocalGroupedSettingsRows
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.SystemClock
import android.view.WindowManager
import helium314.keyboard.latin.VaultGestureStore
import helium314.keyboard.latin.utils.getActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.BackButton
import helium314.keyboard.latin.utils.CloseIcon
import helium314.keyboard.latin.utils.SearchIcon
import helium314.keyboard.settings.preferences.PreferenceCategory
import helium314.keyboard.settings.preferences.Preference

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }
val LocalSearchInnerPadding = staticCompositionLocalOf<PaddingValues> { PaddingValues(0.dp) }

data class SearchState(
    val showSearch: Boolean,
    val searchText: TextFieldValue,
    val onSearchChange: (TextFieldValue) -> Unit,
    val setShowSearch: (Boolean) -> Unit,
    val searchField: @Composable () -> Unit
)
val LocalSearchState = staticCompositionLocalOf<SearchState?> { null }

// Same exact style as the former variable font; the installed icon font is static.
val materialSymbols = FontFamily(Font(R.font.material_symbols_rounded))

val googleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_sans_flex, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_sans_flex, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_sans_flex, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.Setting("ROND", 100f)))
)

@Composable
fun SearchSettingsScreen(
    onClickBack: () -> Unit,
    title: String,
    settings: List<Any?>,
    hideTopSearchBar: Boolean = false,
    showBackButton: Boolean = true,
    filteredItems: ((String) -> List<Setting>)? = null,
    onTitleHold: (() -> Unit)? = null,
    onTitleRecovery: (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null, // overrides settings if not null
    header: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titleView = LocalView.current
    val vaultDeadline = remember { mutableStateOf(0L) }
    val holdModifier = if (onTitleHold == null) Modifier else Modifier.pointerInput(Unit) {
        while (true) {
            awaitPointerEventScope { awaitFirstDown(requireUnconsumed = true) }
            val released = withTimeoutOrNull(3_000L) {
                awaitPointerEventScope { waitForUpOrCancellation() != null }
            }
            if (released == null) {
                titleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                // Arm at the vibration, not on release. Never navigate from a hold.
                vaultDeadline.value = VaultSearchWindow.deadline(SystemClock.elapsedRealtime())
                awaitPointerEventScope { waitForUpOrCancellation() }
            }
        }
    }
    SearchScreen(
        onClickBack = onClickBack,
        hideTopSearchBar = hideTopSearchBar,
        showBackButton = showBackButton,
        vaultDeadline = if (onTitleHold != null) vaultDeadline else null,
        onVaultUnlock = onTitleHold,
        onVaultRecovery = onTitleRecovery,
        title = { Text(title, modifier = holdModifier, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        content = {
            header()
            if (content == null) {
                GroupedSettingsRows(settings)
            } else content.invoke(this)
            footer()
        },
        filteredItems = filteredItems ?: { SettingsActivity.settingsContainer.filter(it) },
        itemContent = { setting ->
            val destination = SettingsActivity.settingsContainer.routeFor(setting.key)
            val context = SettingsActivity.settingsContainer.breadcrumbFor(setting.key)
            Preference(
                name = setting.title,
                description = if (setting.description.isNullOrBlank()) context else "$context · ${setting.description}",
                onClick = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    SettingsDestination.navigateTo(destination)
                },
            ) {
                Text("chevron_right", fontFamily = materialSymbols, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T: Any?> SearchScreen(
    onClickBack: () -> Unit,
    title: @Composable () -> Unit,
    filteredItems: (String) -> List<T>,
    itemContent: @Composable (T) -> Unit,
    itemKey: ((T) -> Any)? = null,
    icon: @Composable (() -> Unit)? = null,
    menu: List<Pair<String, () -> Unit>>? = null,
    hideTopSearchBar: Boolean = false,
    showBackButton: Boolean = true,
    vaultDeadline: MutableState<Long>? = null,
    onVaultUnlock: (() -> Unit)? = null,
    onVaultRecovery: (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    var searchText by remember { mutableStateOf(TextFieldValue()) }
    var showSearch by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val isBlurSupported = remember { android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S }
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val unlockAction by rememberUpdatedState(onVaultUnlock)
    var secretInput by remember { mutableStateOf(TextFieldValue()) }

    fun disarm() {
        vaultDeadline?.value = 0L
        secretInput = TextFieldValue()
    }
    fun routeSearch(value: TextFieldValue) {
        val deadline = vaultDeadline?.value ?: 0L
        if (deadline != 0L) {
            // Never route a secret or an expired private edit to the normal filter.
            if (VaultSearchWindow.active(deadline, SystemClock.elapsedRealtime())) {
                if (value.text.length <= 512) secretInput = value
            } else disarm()
        } else searchText = value
    }
    fun setShowSearch(value: Boolean) {
        showSearch = value
        if (!value) { searchText = TextFieldValue(); disarm() }
    }
    val deadline = vaultDeadline?.value ?: 0L
    LaunchedEffect(deadline) {
        secretInput = TextFieldValue()
        if (deadline != 0L) {
            searchText = TextFieldValue()
            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            if (vaultDeadline?.value == deadline) disarm()
        }
    }
    // Private input never participates in settings search, saved state or history.
    // Exact comparison runs off the UI thread; stale results cannot open a route.
    LaunchedEffect(deadline, secretInput.text) {
        val candidate = secretInput.text
        if (!VaultSearchWindow.active(deadline, SystemClock.elapsedRealtime()) || candidate.length < 4)
            return@LaunchedEffect
        delay(120L)
        if (!VaultSearchWindow.active(deadline, SystemClock.elapsedRealtime())) return@LaunchedEffect
        val matched = withContext(Dispatchers.IO) {
            VaultGestureStore.verifySettingsCode(ctx.applicationContext, candidate)
        }
        if (matched && vaultDeadline?.value == deadline && secretInput.text == candidate &&
            VaultSearchWindow.active(deadline, SystemClock.elapsedRealtime())) {
            disarm()
            searchText = TextFieldValue()
            focusManager.clearFocus()
            keyboard?.hide()
            unlockAction?.invoke()
        }
    }
    DisposableEffect(owner, vaultDeadline) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) disarm()
        }
        if (vaultDeadline != null) owner.lifecycle.addObserver(observer)
        onDispose {
            if (vaultDeadline != null) owner.lifecycle.removeObserver(observer)
            disarm()
        }
    }
    DisposableEffect(deadline != 0L) {
        val window = if (deadline != 0L) ctx.getActivity()?.window else null
        val alreadySecure = window != null &&
            window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    BackHandler {
        if (deadline != 0L || showSearch || searchText.text.isNotEmpty()) setShowSearch(false)
        else onClickBack()
    }
    
    val isDark = isSystemInDarkTheme()
    val scaffoldBg = MaterialTheme.colorScheme.background
    val topBarBg = MaterialTheme.colorScheme.background
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val searchFieldContent = remember {
        movableContentOf {
            StaticSearchField(
                search = if ((vaultDeadline?.value ?: 0L) != 0L) secretInput else searchText,
                privateMode = (vaultDeadline?.value ?: 0L) != 0L,
                onSearchChange = ::routeSearch,
                onClearClick = { setShowSearch(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .heightIn(min = 50.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }

    val searchState = remember(showSearch, searchText, searchFieldContent) {
        SearchState(
            showSearch = showSearch,
            searchText = searchText,
            onSearchChange = ::routeSearch,
            setShowSearch = ::setShowSearch,
            searchField = searchFieldContent
        )
    }
    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalSearchState provides searchState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = scaffoldBg,
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    val topBarHazeStyle = remember(topBarBg) {
                        HazeStyle(blurRadius = 22.dp, tint = topBarBg.copy(alpha = 0.86f))
                    }
                    AnimatedVisibility(
                        visible = !SettingsActivity.isTopBarHidden && SettingsActivity.activeOverlay == null,
                        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(initialOffsetY = { -it }, animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(180))
                    ) {
                    Box {
                        if (isBlurSupported) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = scrollBehavior.state.collapsedFraction }
                                    .hazeChild(state = hazeState, style = topBarHazeStyle)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(topBarBg)
                            )
                        }
                        Column(
                            modifier = Modifier.layout { measurable, constraints ->
                                val fraction = scrollBehavior.state.collapsedFraction
                                val bottomPadding = (18.dp.toPx() * (1f - fraction)).roundToInt()
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height + bottomPadding) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                        ) {
                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme,
                                shapes = MaterialTheme.shapes,
                                typography = MaterialTheme.typography.copy(
                                    headlineMedium = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = googleSansFlex,
                                        fontSize = 34.sp,
                                        lineHeight = 42.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    titleLarge = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = googleSansFlex,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            ) {
                                LargeTopAppBar(
                                    title = { 
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .layout { measurable, constraints ->
                                                    val fraction = scrollBehavior.state.collapsedFraction
                                                    val topPadding = (20.dp.toPx() * (1f - fraction)).roundToInt()
                                                    val placeable = measurable.measure(constraints)
                                                    layout(placeable.width, placeable.height + topPadding) {
                                                        placeable.placeRelative(0, topPadding)
                                                    }
                                                }
                                        ) { title() } 
                                    },
                                scrollBehavior = scrollBehavior,
                                navigationIcon = {
                                    if (showBackButton) {
                                        IconButton(
                                            onClick = {
                                                if (showSearch) setShowSearch(false)
                                                else onClickBack()
                                            },
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            ) {
                                                Text("chevron_left", fontFamily = materialSymbols, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    if (icon != null)
                                        icon()
                                    if (menu != null)
                                        Box {
                                            var showMenu by remember { mutableStateOf(false) }
                                            IconButton(
                                                onClick = { showMenu = true }
                                            ) { Text("more_vert", fontFamily = materialSymbols, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface) }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                menu.forEach {
                                                    DropdownMenuItem(
                                                        text = { Text(it.first) },
                                                        onClick = { showMenu = false; it.second() }
                                                    )
                                                }
                                            }
                                        }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent
                                )
                            )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val bottomPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 16.dp

            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyLarge,
                LocalSearchInnerPadding provides innerPadding
            ) {
                val isSearching = searchText.text.isNotBlank()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isBlurSupported) {
                                Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .haze(state = hazeState)
                            } else Modifier
                        )
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = bottomPadding
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!hideTopSearchBar) {
                            item(key = "search_field_item") {
                                searchFieldContent()
                            }
                        }

                        if (deadline != 0L && onVaultRecovery != null) {
                            item(key = "private_recovery") {
                                androidx.compose.material3.TextButton(onClick = {
                                    disarm()
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    onVaultRecovery()
                                }) { Text("Recover or change secret code") }
                            }
                        }

                        if (isSearching || content == null) {
                            val items = filteredItems(searchText.text)
                            if (isSearching && items.isEmpty()) {
                                item(key = "empty_search") {
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp)) {
                                        Text("No matching settings", style = MaterialTheme.typography.titleMedium)
                                        Text("Try a feature or related word, such as glass, swipe, privacy or clipboard.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                            if (itemKey == null) {
                                items(items) { itemContent(it) }
                            } else {
                                items(items, key = itemKey) { itemContent(it) }
                            }
                        } else {
                            item(key = "screen_content_body") {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
        SettingsActivity.activeOverlay?.invoke(hazeState)
    }
}
}

// from StreetComplete
/** Static text field for searching */
@Composable
fun StaticSearchField(
    search: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onClearClick: (() -> Unit)? = null,
    privateMode: Boolean = false,
) {
    TextField(
        value = search,
        onValueChange = onSearchChange,
        shape = RoundedCornerShape(17.dp),
        modifier = modifier,
        leadingIcon = {
            Text(
                "search",
                fontFamily = materialSymbols,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        trailingIcon = {
            if (search.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchChange(TextFieldValue(""))
                        onClearClick?.invoke()
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Text(
                            "close",
                            fontFamily = materialSymbols,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        placeholder = { Text(if (privateMode) "Enter code" else "Search settings", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        colors = colors,
        textStyle = contentTextDirectionStyle,
        visualTransformation = if (privateMode) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (privateMode) KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Search)
            else KeyboardOptions(imeAction = ImeAction.Search)
    )
}

/** Preserve order and stable preference identity while omitting unavailable rows. */
internal data class SettingsRowGroup(val title: Int?, val keys: List<Any>)
internal fun groupSettingsRows(settings: List<Any?>): List<SettingsRowGroup> {
    val groups = mutableListOf<SettingsRowGroup>()
    var title: Int? = null
    val keys = mutableListOf<Any>()
    fun flush() { if (keys.isNotEmpty()) { groups += SettingsRowGroup(title, keys.toList()); keys.clear() } }
    for (item in settings) when (item) {
        null -> Unit
        is Int -> { flush(); title = item }
        else -> keys.add(item)
    }
    flush()
    return groups
}

@Composable
private fun GroupedSettingsRows(settings: List<Any?>) {
    for (group in groupSettingsRows(settings)) {
        val resolved = group.keys.mapNotNull { id -> SettingsActivity.settingsContainer[id]?.let { id to it } }
        if (resolved.isEmpty()) continue
        key(group.title ?: "untitled") {
            group.title?.let { PreferenceCategory(stringResource(it)) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)) {
                CompositionLocalProvider(LocalGroupedSettingsRows provides true) {
                    resolved.forEachIndexed { index, (id, setting) ->
                        key(id) {
                            if (index > 0) HorizontalDivider(Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                            setting.Preference()
                        }
                    }
                }
            }
        }
    }
}
