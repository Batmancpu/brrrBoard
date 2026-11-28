// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import helium314.keyboard.settings.screens.TypingSettingsHub
import helium314.keyboard.settings.screens.KeyboardSettingsHub
import helium314.keyboard.settings.screens.InteractionsSettingsHub
import helium314.keyboard.settings.screens.ToolsSettingsHub
import helium314.keyboard.settings.screens.TextToolsScreen
import helium314.keyboard.settings.screens.ResetOnlyScreen
import helium314.keyboard.settings.screens.AdvancedSettingsHub
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.VaultGestureStore
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.settings.screens.PrivateSettingsHub
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import helium314.keyboard.settings.screens.AboutScreen
import helium314.keyboard.settings.screens.VersionHistoryScreen
import helium314.keyboard.settings.screens.AdvancedSettingsScreen
import helium314.keyboard.settings.screens.AppearanceScreen
import helium314.keyboard.settings.screens.ColorsScreen
import helium314.keyboard.settings.screens.DebugScreen
import helium314.keyboard.settings.screens.DictionaryScreen
import helium314.keyboard.settings.screens.GestureTypingScreen
import helium314.keyboard.settings.screens.BrainDashboardScreen
import helium314.keyboard.settings.screens.LanguageScreen
import helium314.keyboard.settings.screens.MainSettingsScreen
import helium314.keyboard.settings.screens.PersonalDictionariesScreen
import helium314.keyboard.settings.screens.PersonalDictionaryScreen
import helium314.keyboard.settings.screens.PreferencesScreen
import helium314.keyboard.settings.screens.SecondaryLayoutScreen
import helium314.keyboard.settings.screens.SubtypeScreen
import helium314.keyboard.settings.screens.TextCorrectionScreen
import helium314.keyboard.settings.screens.ToolbarScreen
import helium314.keyboard.settings.screens.KeysLayoutScreen
import helium314.keyboard.settings.screens.SoundFeedbackScreen
import helium314.keyboard.settings.screens.ClipboardSettingsScreen
import helium314.keyboard.settings.screens.SmartSnippetSettingsScreen
import helium314.keyboard.settings.screens.PrivacySettingsScreen
import helium314.keyboard.settings.screens.BackupSettingsScreen
import helium314.keyboard.settings.screens.VoiceSettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun SettingsNavHost(
    showWelcomeWizard: Boolean,
    onShowWelcomeWizardChanged: (Boolean) -> Unit,
    onClickBack: () -> Unit,
    startDestination: String? = null,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var privateDeadline by remember { mutableStateOf(0L) }
    val unlocked = PrivateSettingsSession.active(privateDeadline, SystemClock.elapsedRealtime()) &&
        VaultGestureStore.isDeviceUnlocked(context)
    // The NavHost remembers its graph. Destination closures must read live state,
    // not the Boolean captured when the graph was originally constructed.
    val currentAuthorization by rememberUpdatedState(unlocked)
    // Not saveable: recreation or process death must never restore authorization.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) privateDeadline = 0L
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); privateDeadline = 0L }
    }
    LaunchedEffect(privateDeadline) {
        if (privateDeadline != 0L) {
            delay((privateDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            privateDeadline = 0L
        }
    }
    DisposableEffect(unlocked) {
        val window = if (unlocked) context.getActivity()?.window else null
        val alreadySecure = window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val target = SettingsDestination.navTarget.collectAsState()

    // duration does not change when system setting changes, but that's rare enough to not care
    val duration = (220 * getTransitionAnimationScale(LocalContext.current)).toInt().coerceAtLeast(0)
    val animation = tween<IntOffset>(durationMillis = duration, easing = FastOutSlowInEasing)

    fun goBack() {
        if (!navController.popBackStack()) onClickBack()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination ?: if (showWelcomeWizard) SettingsDestination.WelcomeWizard else SettingsDestination.Settings,
        enterTransition = { slideInHorizontally(initialOffsetX = { +it * dir }, animationSpec = animation) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it * dir / 4 }, animationSpec = animation) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it * dir / 4 }, animationSpec = animation) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { +it * dir }, animationSpec = animation) }
    ) {
        composable(SettingsDestination.Settings) {
            MainSettingsScreen(
                onClickAbout = { navController.navigate(SettingsDestination.About) },
                onClickPreferences = { navController.navigate(SettingsDestination.Preferences) },
                onClickToolbar = { navController.navigate(SettingsDestination.Toolbar) },
                onClickGestureTyping = { navController.navigate(SettingsDestination.GestureTyping) },
                onClickBrainDashboard = { navController.navigate(SettingsDestination.BrainDashboard) },
                onClickTextCorrection = { navController.navigate(SettingsDestination.TextCorrection) },
                onClickAdvanced = { navController.navigate(SettingsDestination.Advanced) },
                onClickAppearance = { navController.navigate(SettingsDestination.Appearance) },
                onClickLanguage = { navController.navigate(SettingsDestination.Languages) },
                onClickLayouts = { navController.navigate(SettingsDestination.Layouts) },
                onClickDictionaries = { navController.navigate(SettingsDestination.Dictionaries) },
                onClickWelcomeWizard = { navController.navigate(SettingsDestination.WelcomeWizard) },
                onClickKeys = { navController.navigate(SettingsDestination.Keys) },
                onClickSound = { navController.navigate(SettingsDestination.Sound) },
                onClickClipboard = { navController.navigate(SettingsDestination.Clipboard) },
                onClickPrivacy = { navController.navigate(SettingsDestination.Privacy) },
                // Called only after the masked header search verifies the enrolled code.
                onClickVault = {
                    if (VaultGestureStore.isDeviceUnlocked(context)) {
                        privateDeadline = PrivateSettingsSession.deadline(SystemClock.elapsedRealtime())
                        navController.navigate(SettingsDestination.ProtectedPrivacy)
                    }
                },
                onRecoverVault = { navController.navigate("private_vault_setup") },
                onClickBackup = { navController.navigate(SettingsDestination.Backup) },
                onClickVoice = { navController.navigate(SettingsDestination.Voice) },
                onClickBack = ::goBack,
                onClickTypingHub = { navController.navigate(SettingsDestination.TypingHub) },
                onClickAdvancedHub = { navController.navigate(SettingsDestination.AdvancedHub) },
                onClickKeyboardHub = { navController.navigate(SettingsDestination.KeyboardHub) },
                onClickInteractionsHub = { navController.navigate(SettingsDestination.InteractionsHub) },
                onClickToolsHub = { navController.navigate(SettingsDestination.ToolsHub) },
                onClickCustom = { navController.navigate(SettingsDestination.Custom) },
            )
        }
        composable(SettingsDestination.KeyboardHub) { KeyboardSettingsHub(onClickBack = ::goBack, navigate = { navController.navigate(it) }) }
        composable(SettingsDestination.InteractionsHub) { InteractionsSettingsHub(onClickBack = ::goBack, navigate = { navController.navigate(it) }) }
        composable(SettingsDestination.Custom) { helium314.keyboard.settings.screens.CustomAdditionsScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.ToolsHub) { ToolsSettingsHub(onClickBack = ::goBack, navigate = { navController.navigate(it) }) }
        composable(SettingsDestination.GlassEditor) {
            if (helium314.keyboard.latin.FrostedGlassHelper.isFrostedTheme(LocalContext.current)) {
                helium314.keyboard.settings.dialogs.FrostedGlassAdjustDialog(
                    hazeState = remember { dev.chrisbanes.haze.HazeState() }, onDismissRequest = ::goBack)
            } else AppearanceScreen(onClickBack = ::goBack, section = "glass")
        }
        composable(SettingsDestination.Glass) { AppearanceScreen(onClickBack = ::goBack, section = "glass") }
        composable(SettingsDestination.Size) { AppearanceScreen(onClickBack = ::goBack, section = "size") }
        composable(SettingsDestination.Lettering) { AppearanceScreen(onClickBack = ::goBack, section = "lettering") }
        composable(SettingsDestination.Emoji) { AppearanceScreen(onClickBack = ::goBack, section = "emoji") }
        composable(SettingsDestination.Motion) { AppearanceScreen(onClickBack = ::goBack, section = "motion") }
        composable(SettingsDestination.KeySymbols) { KeysLayoutScreen(onClickBack = ::goBack, section = "keys") }
        composable(SettingsDestination.GestureAssignments) { KeysLayoutScreen(onClickBack = ::goBack, section = "gestures") }
        composable(SettingsDestination.Correction) { TextCorrectionScreen(onClickBack = ::goBack, section = "correction") }
        composable(SettingsDestination.Suggestions) { TextCorrectionScreen(onClickBack = ::goBack, section = "suggestions") }
        composable(SettingsDestination.Writing) { TextCorrectionScreen(onClickBack = ::goBack, section = "writing") }
        composable(SettingsDestination.TextTools) { TextToolsScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Reset) { ResetOnlyScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Compatibility) { AdvancedSettingsScreen(onClickBack = ::goBack, section = "compatibility") }
        composable(SettingsDestination.Diagnostics) { AboutScreen(onClickBack = ::goBack, section = "diagnostics") }
        composable(SettingsDestination.TypingHub) {
            TypingSettingsHub(onClickBack = ::goBack, navigate = { navController.navigate(it) })
        }
        composable(SettingsDestination.AdvancedHub) {
            AdvancedSettingsHub(onClickBack = ::goBack, navigate = { navController.navigate(it) })
        }
        composable(SettingsDestination.About) {
            AboutScreen(onClickBack = ::goBack, section = "about")
        }
        composable(SettingsDestination.VersionHistory) {
            VersionHistoryScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.TextCorrection) {
            TextCorrectionScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Preferences) {
            PreferencesScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Keys) { KeysLayoutScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Sound) { SoundFeedbackScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Clipboard) { ClipboardSettingsScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.SmartSnippets) { SmartSnippetSettingsScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Privacy) { PrivacySettingsScreen(onClickBack = ::goBack) }
        composable("private_vault_setup") {
            helium314.keyboard.settings.screens.VaultSetupScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.ProtectedPrivacy) {
            if (currentAuthorization) PrivateSettingsHub(
                onClickBack = ::goBack,
                onOpenBrainDetails = { navController.navigate(SettingsDestination.ProtectedBrain) },
                onManageCode = { navController.navigate("private_vault_setup") },
                onLock = { privateDeadline = 0L; goBack() },
            ) else LaunchedEffect(Unit) {
                if (!navController.popBackStack(SettingsDestination.Settings, false))
                    navController.navigate(SettingsDestination.Settings) { launchSingleTop = true }
            }
        }
        composable(SettingsDestination.Backup) { BackupSettingsScreen(onClickBack = ::goBack) }
        composable(SettingsDestination.Voice) {
            VoiceSettingsScreen(
                onClickBack = ::goBack,
            )
        }
        composable(SettingsDestination.Toolbar) {
            ToolbarScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.GestureTyping) {
            GestureTypingScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.BrainDashboard) {
            BrainDashboardScreen(
                onClickBack = ::goBack,
                onOpenCorrection = { navController.navigate(SettingsDestination.Correction) },
                onOpenDetails = if (currentAuthorization) {
                    { navController.navigate(SettingsDestination.ProtectedBrain) }
                } else null,
            )
        }
        composable(SettingsDestination.ProtectedBrain) {
            if (currentAuthorization) BrainDashboardScreen(
                onClickBack = ::goBack,
                showDetailedStats = true,
                onOpenCorrection = { navController.navigate(SettingsDestination.Correction) },
            ) else LaunchedEffect(Unit) {
                if (!navController.popBackStack(SettingsDestination.Settings, false))
                    navController.navigate(SettingsDestination.Settings) { launchSingleTop = true }
            }
        }
/*      will be added as part of passive data gathering
        composable(SettingsDestination.DataReview) {
            ReviewScreen(onClickBack = ::goBack)
        }*/
        composable(SettingsDestination.Advanced) {
            AdvancedSettingsScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Debug) {
            DebugScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Appearance) {
            AppearanceScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.PersonalDictionary + "{locale}") {
            val locale = it.arguments?.getString("locale")?.takeIf { loc -> loc.isNotBlank() }?.constructLocale()
            PersonalDictionaryScreen(
                onClickBack = ::goBack,
                locale = locale
            )
        }
        composable(SettingsDestination.PersonalDictionaries) {
            PersonalDictionariesScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Languages) {
            LanguageScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Dictionaries) {
            DictionaryScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Layouts) {
            SecondaryLayoutScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Colors + "{theme}") {
            ColorsScreen(isNight = false, theme = it.arguments?.getString("theme"), onClickBack = ::goBack)
        }
        composable(SettingsDestination.ColorsNight + "{theme}") {
            ColorsScreen(isNight = true, theme = it.arguments?.getString("theme"), onClickBack = ::goBack)
        }
        composable(SettingsDestination.Subtype + "{subtype}") {
            SubtypeScreen(initialSubtype = it.arguments?.getString("subtype")!!.toSettingsSubtype(), onClickBack = ::goBack)
        }
        composable(SettingsDestination.WelcomeWizard) {
            WelcomeWizard(
                close = {
                    onShowWelcomeWizardChanged(false)
                    navController.navigate(SettingsDestination.Settings) {
                        popUpTo(SettingsDestination.WelcomeWizard) { inclusive = true }
                    }
                },
                finish = onClickBack
            )
        }
    }
    LaunchedEffect(target.value) {
        if (target.value != SettingsDestination.Settings
            && target.value != navController.currentBackStackEntry?.destination?.route)
            navController.navigate(route = target.value)
    }
}

object SettingsDestination {
    const val Settings = "settings"
    const val KeyboardHub = "keyboard_hub"
    const val InteractionsHub = "interactions_hub"
    const val ToolsHub = "tools_hub"
    const val Custom = "custom_additions"
    const val Glass = "glass_studio"
    const val GlassEditor = "glass_editor"
    const val Size = "keyboard_dimensions"
    const val Lettering = "lettering_fonts"
    const val Emoji = "emoji_settings"
    const val KeySymbols = "keys_symbols"
    const val GestureAssignments = "gesture_assignments"
    const val Motion = "motion_settings"
    const val Correction = "correction"
    const val Suggestions = "suggestions"
    const val Writing = "writing_conventions"
    const val TextTools = "text_tools"
    const val Reset = "reset_settings"
    const val Compatibility = "compatibility"
    const val Diagnostics = "diagnostics"
    const val TypingHub = "typing_hub"
    const val AdvancedHub = "advanced_hub"
    const val WelcomeWizard = "welcome_wizard"
    const val About = "about"
    const val VersionHistory = "version_history"
    const val TextCorrection = "text_correction"
    const val Preferences = "preferences"
    const val Keys = "keys_layout"
    const val Sound = "sound_feedback"
    const val Clipboard = "clipboard"
    const val SmartSnippets = "smart_snippets"
    const val Privacy = "privacy"
    const val ProtectedPrivacy = "protected_privacy"
    const val ProtectedBrain = "protected_brain"
    const val Backup = "backup"
    const val Voice = "voice"
    const val Toolbar = "toolbar"
    const val GestureTyping = "gesture_typing"
    const val BrainDashboard = "brain_dashboard"
    const val DataGathering = "data_gathering" // remove when data gathering phase is done (end of 2026 latest)
    const val DataReview = "data_review" // remove when data gathering phase is done (end of 2026 latest)
    const val Advanced = "advanced"
    const val Debug = "debug"
    const val Appearance = "appearance"
    const val Colors = "colors/"
    const val ColorsNight = "colors_night/"
    const val PersonalDictionaries = "personal_dictionaries"
    const val PersonalDictionary = "personal_dictionary/"
    const val Languages = "languages"
    const val Subtype = "subtype/"
    const val Layouts = "layouts"
    const val Dictionaries = "dictionaries"
    val navTarget = MutableStateFlow(Settings)

    private val navScope = CoroutineScope(Dispatchers.Default)
    fun navigateTo(target: String) {
        if (navTarget.value == target) {
            // triggers recompose twice, but that's ok as it's a rare event
            navTarget.value = Settings
            navScope.launch { delay(10); navTarget.value = target }
        } else
            navTarget.value = target
        navScope.launch { delay(50); navTarget.value = Settings }
    }
}
