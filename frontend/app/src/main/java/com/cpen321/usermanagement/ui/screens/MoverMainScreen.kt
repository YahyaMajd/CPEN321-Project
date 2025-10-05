package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.MainUiState
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.theme.LocalFontSizes
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.data.local.models.Job
import java.time.LocalDate
import java.time.LocalTime

enum class MoverScreen {
    CURRENT_JOBS,
    AVAILABLE_JOBS,
    SET_AVAILABILITY
}

@Composable
fun MoverMainScreen(
    mainViewModel: MainViewModel,
    onProfileClick: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    // Sample data - Replace with actual data from viewModel
    val currentJobs = remember {
        listOf(
            Job(
                id = "J-102",
                price = 85.00,
                date = LocalDate.now(),
                startTime = LocalTime.of(9, 0),
                pickupAddress = "123 Main St",
                dropoffAddress = "456 Oak Ave",
                status = com.cpen321.usermanagement.data.local.models.JobStatus.ACCEPTED
            )
        )
    }

    val availableJobs = remember {
        listOf(
            Job(
                id = "J-103",
                price = 75.00,
                date = LocalDate.now(),
                startTime = LocalTime.of(14, 0),
                pickupAddress = "789 Pine St",
                dropoffAddress = "321 Elm St",
                status = com.cpen321.usermanagement.data.local.models.JobStatus.AVAILABLE
            )
        )
    }

    MainContent(
        uiState = uiState,
        currentJobs = currentJobs,
        availableJobs = availableJobs,
        snackBarHostState = snackBarHostState,
        onProfileClick = onProfileClick,
        onSuccessMessageShown = mainViewModel::clearSuccessMessage,
        onJobAccept = { /* TODO: Implement job acceptance */ },
        onJobDetails = { /* TODO: Implement job details view */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    uiState: MainUiState,
    currentJobs: List<Job>,
    availableJobs: List<Job>,
    snackBarHostState: SnackbarHostState,
    onProfileClick: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onJobAccept: (Job) -> Unit,
    onJobDetails: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(MoverScreen.CURRENT_JOBS) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(onProfileClick = onProfileClick)
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == MoverScreen.CURRENT_JOBS,
                    onClick = { currentScreen = MoverScreen.CURRENT_JOBS },
                    icon = { Icon(Icons.Default.Work, "Current Jobs") },
                    label = { Text("Current Jobs") }
                )
                NavigationBarItem(
                    selected = currentScreen == MoverScreen.AVAILABLE_JOBS,
                    onClick = { currentScreen = MoverScreen.AVAILABLE_JOBS },
                    icon = { Icon(Icons.Default.Search, "Find Jobs") },
                    label = { Text("Find Jobs") }
                )
                NavigationBarItem(
                    selected = currentScreen == MoverScreen.SET_AVAILABILITY,
                    onClick = { currentScreen = MoverScreen.SET_AVAILABILITY },
                    icon = { Icon(Icons.Default.Schedule, "Set Availability") },
                    label = { Text("Availability") }
                )
            }
        },
        snackbarHost = {
            MainSnackbarHost(
                hostState = snackBarHostState,
                successMessage = uiState.successMessage,
                onSuccessMessageShown = onSuccessMessageShown
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                MoverScreen.CURRENT_JOBS -> CurrentJobsScreen(
                    jobs = currentJobs,
                    onJobDetails = onJobDetails
                )
                MoverScreen.AVAILABLE_JOBS -> AvailableJobsScreen(
                    jobs = availableJobs,
                    onJobDetails = onJobDetails,
                    onJobAccept = onJobAccept
                )
                MoverScreen.SET_AVAILABILITY -> SetAvailabilityScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            AppTitle()
        },
        actions = {
            ProfileActionButton(onClick = onProfileClick)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier
) {
    Row{
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
        Text(
            text = " (Mover)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    IconButton(
        onClick = onClick,
        modifier = modifier.size(spacing.extraLarge2)
    ) {
        ProfileIcon()
    }
}

@Composable
private fun ProfileIcon() {
    Icon(
        name = R.drawable.ic_account_circle,
    )
}

@Composable
private fun MainSnackbarHost(
    hostState: SnackbarHostState,
    successMessage: String?,
    onSuccessMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageSnackbar(
        hostState = hostState,
        messageState = MessageSnackbarState(
            successMessage = successMessage,
            errorMessage = null,
            onSuccessMessageShown = onSuccessMessageShown,
            onErrorMessageShown = { }
        ),
        modifier = modifier
    )
}

@Composable
private fun MainBody(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
    }
}
