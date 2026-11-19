package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.i18n.AppLanguage
import com.example.i18n.BengaliStrings
import com.example.i18n.EnglishStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.QrLensTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object ScannerRoute

@Serializable
object HistoryRoute

@Serializable
object GeneratorRoute

private data class NavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun QrLensApp(viewModel: MainViewModel = viewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val strings = if (prefs.language == AppLanguage.BN) BengaliStrings else EnglishStrings

    val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResult.collectAsStateWithLifecycle()
    val formatFilter by viewModel.formatFilter.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()
    val useFrontCamera by viewModel.useFrontCamera.collectAsStateWithLifecycle()
    val generatorState by viewModel.generatorState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is UiEvent.ShowMessage -> event.message
                UiEvent.NoCodeInGalleryImage -> strings.noCodeFoundInImage
                UiEvent.SavedToVault -> strings.genSavedToVaultToast
                UiEvent.ExportSuccess -> strings.exportSuccessToast
                UiEvent.ImportSuccess -> strings.importSuccessToast
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        QrLensTheme(themeMode = prefs.themeMode) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val navItems = listOf(
                NavItem(
                    route = ScannerRoute,
                    label = strings.navScan,
                    selectedIcon = Icons.Filled.QrCodeScanner,
                    unselectedIcon = Icons.Outlined.QrCodeScanner,
                    testTag = "nav_scan"
                ),
                NavItem(
                    route = HistoryRoute,
                    label = strings.navHistory,
                    selectedIcon = Icons.Filled.History,
                    unselectedIcon = Icons.Outlined.History,
                    testTag = "nav_history"
                ),
                NavItem(
                    route = GeneratorRoute,
                    label = strings.navGenerator,
                    selectedIcon = Icons.Filled.QrCode2,
                    unselectedIcon = Icons.Outlined.QrCode2,
                    testTag = "nav_generator"
                )
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp
                val isExpandedScreen = maxWidth >= 840.dp

                val showBottomSheet = activeResult != null &&
                    !(isExpandedScreen && currentDestination?.hasRoute<HistoryRoute>() == true)

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                navItems.forEach { item ->
                                    val selected = when (item.route) {
                                        is ScannerRoute -> currentDestination?.hasRoute<ScannerRoute>() == true
                                        is HistoryRoute -> currentDestination?.hasRoute<HistoryRoute>() == true
                                        is GeneratorRoute -> currentDestination?.hasRoute<GeneratorRoute>() == true
                                        else -> false
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                navItems.forEach { item ->
                                    val selected = when (item.route) {
                                        is ScannerRoute -> currentDestination?.hasRoute<ScannerRoute>() == true
                                        is HistoryRoute -> currentDestination?.hasRoute<HistoryRoute>() == true
                                        is GeneratorRoute -> currentDestination?.hasRoute<GeneratorRoute>() == true
                                        else -> false
                                    }
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ScannerRoute,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable<ScannerRoute> {
                                ScannerScreen(
                                    formatFilter = formatFilter,
                                    torchEnabled = torchEnabled,
                                    zoomLevel = zoomLevel,
                                    useFrontCamera = useFrontCamera,
                                    themeMode = prefs.themeMode,
                                    isResultSheetOpen = activeResult != null,
                                    onFormatFilterChange = viewModel::setFormatFilter,
                                    onToggleTorch = viewModel::toggleTorch,
                                    onSelectZoom = viewModel::setZoomLevel,
                                    onToggleCameraLens = viewModel::toggleCameraLens,
                                    onToggleTheme = viewModel::toggleThemeMode,
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onLiveCodeDetected = viewModel::onLiveCodeDetected,
                                    onGalleryImagePicked = viewModel::scanFromGalleryUri,
                                    onManualCodeSubmitted = viewModel::onManualCodeEntered
                                )
                            }

                            composable<HistoryRoute> {
                                HistoryScreen(
                                    records = filteredHistory,
                                    searchQuery = searchQuery,
                                    selectedFilter = historyFilter,
                                    selectedDetailRecord = activeResult,
                                    isExpandedScreen = isExpandedScreen,
                                    themeMode = prefs.themeMode,
                                    onSearchQueryChange = viewModel::setHistorySearchQuery,
                                    onFilterChange = viewModel::setHistoryFilter,
                                    onSelectRecord = viewModel::openRecordDetail,
                                    onToggleBookmark = viewModel::toggleBookmark,
                                    onSaveNote = viewModel::saveRecordNote,
                                    onDeleteRecord = viewModel::deleteRecord,
                                    onClearAll = viewModel::clearAllHistory,
                                    onExportJsonUri = viewModel::exportHistoryJsonToUri,
                                    onExportCsvUri = viewModel::exportHistoryCsvToUri,
                                    onImportJsonUri = viewModel::importHistoryJsonFromUri,
                                    onToggleTheme = viewModel::toggleThemeMode,
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }

                            composable<GeneratorRoute> {
                                GeneratorScreen(
                                    state = generatorState,
                                    themeMode = prefs.themeMode,
                                    onTypeChange = viewModel::updateGeneratorType,
                                    onUrlChange = viewModel::updateGeneratorUrl,
                                    onTextChange = viewModel::updateGeneratorText,
                                    onWifiChange = viewModel::updateGeneratorWifi,
                                    onVcardChange = viewModel::updateGeneratorVcard,
                                    onBarcode128Change = viewModel::updateGeneratorBarcode128,
                                    onPaletteChange = viewModel::updateGeneratorPalette,
                                    onSizeChange = viewModel::updateGeneratorSize,
                                    onEccChange = viewModel::updateGeneratorEcc,
                                    onBuildExportBitmap = viewModel::buildExportBitmapForGenerator,
                                    onExportSvgUri = viewModel::exportGeneratedSvgToUri,
                                    onSaveToVault = viewModel::saveGeneratedToVault,
                                    onToggleTheme = viewModel::toggleThemeMode,
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showBottomSheet && activeResult != null) {
                    ScanResultBottomSheet(
                        record = activeResult!!,
                        onDismiss = viewModel::dismissActiveResult,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onSaveNote = viewModel::saveRecordNote,
                        onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }
        }
    }
}
