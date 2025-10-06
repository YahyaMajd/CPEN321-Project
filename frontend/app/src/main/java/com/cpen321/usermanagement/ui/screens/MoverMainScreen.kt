package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.AvailableJobCard
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.MainUiState
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.JobViewModel
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.data.local.models.JobStatus

enum class MoverScreen {
    CURRENT_JOBS,
    AVAILABLE_JOBS,
    SET_AVAILABILITY
}

@Composable
fun MoverMainScreen(
    mainViewModel: MainViewModel,
    onProfileClick: () -> Unit,
    jobViewModel: JobViewModel = hiltViewModel()
) {
    val mainUiState by mainViewModel.uiState.collectAsState()
    val jobUiState by jobViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    // Load jobs when screen first composes
    LaunchedEffect(Unit) {
        jobViewModel.loadMoverJobs()
        jobViewModel.loadAvailableJobs()
    }

    // Filter current jobs (assigned or in progress)
    val currentJobs = remember(jobUiState.moverJobs) {
        jobUiState.moverJobs.filter { 
            it.status == JobStatus.ASSIGNED || it.status == JobStatus.IN_PROGRESS 
        }
    }

    MainContent(
        mainUiState = mainUiState,
        currentJobs = currentJobs,
        isLoading = jobUiState.isLoading,
        error = jobUiState.error,
        snackBarHostState = snackBarHostState,
        onProfileClick = onProfileClick,
        onSuccessMessageShown = mainViewModel::clearSuccessMessage,
        onJobDetails = { /* TODO: Navigate to job details if needed */ },
        onRefresh = {
            jobViewModel.loadMoverJobs()
            jobViewModel.loadAvailableJobs()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    mainUiState: MainUiState,
    currentJobs: List<com.cpen321.usermanagement.data.local.models.Job>,
    isLoading: Boolean,
    error: String?,
    snackBarHostState: SnackbarHostState,
    onProfileClick: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    onJobDetails: (com.cpen321.usermanagement.data.local.models.Job) -> Unit,
    onRefresh: () -> Unit,
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
                    icon = { Icon(Icons.Default.Work, contentDescription = "Current Jobs") },
                    label = { Text("Current Jobs") }
                )
                NavigationBarItem(
                    selected = currentScreen == MoverScreen.AVAILABLE_JOBS,
                    onClick = { currentScreen = MoverScreen.AVAILABLE_JOBS },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Find Jobs") },
                    label = { Text("Find Jobs") }
                )
                NavigationBarItem(
                    selected = currentScreen == MoverScreen.SET_AVAILABILITY,
                    onClick = { currentScreen = MoverScreen.SET_AVAILABILITY },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Set Availability") },
                    label = { Text("Availability") }
                )
            }
        },
        snackbarHost = {
            MainSnackbarHost(
                hostState = snackBarHostState,
                successMessage = mainUiState.successMessage,
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
                    isLoading = isLoading,
                    error = error,
                    onJobDetails = onJobDetails,
                    onRefresh = onRefresh
                )
                MoverScreen.AVAILABLE_JOBS -> AvailableJobsScreen(
                    onJobDetails = onJobDetails,
                    modifier = Modifier.fillMaxSize()
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
    Row {
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
        painter = painterResource(id = R.drawable.ic_account_circle),
        contentDescription = "Profile",
        tint = MaterialTheme.colorScheme.onSurface
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
private fun CurrentJobsScreen(
    jobs: List<com.cpen321.usermanagement.data.local.models.Job>,
    isLoading: Boolean,
    error: String?,
    onJobDetails: (com.cpen321.usermanagement.data.local.models.Job) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        Text(
            text = "Current Jobs",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                ErrorCard(
                    message = "Failed to load current jobs",
                    onRetry = onRefresh
                )
            }
            jobs.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Current Jobs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're all caught up! Check back later for new assignments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(jobs) { job ->
                        JobCard(
                            job = job,
                            onDetailsClick = { onJobDetails(job) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: com.cpen321.usermanagement.data.local.models.Job,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = job.jobType.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Status: ${job.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onDetailsClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("View Details")
            }
        }
    }
}

@Composable
private fun AvailableJobsScreen(
    onJobDetails: (com.cpen321.usermanagement.data.local.models.Job) -> Unit,
    modifier: Modifier = Modifier
) {
    val jobViewModel: JobViewModel = hiltViewModel()
    val jobUiState by jobViewModel.uiState.collectAsState()

    val jobs = jobUiState.availableJobs
    val isLoading = jobUiState.isLoading
    val error = jobUiState.error

    val totalAvailable = jobs.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Available Jobs",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            error != null -> {
                ErrorCard(
                    message = "Failed to load available jobs",
                    onRetry = { jobViewModel.loadAvailableJobs() }
                )
            }
            jobs.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Jobs Available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check back later for new opportunities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(jobs) { job ->
                        AvailableJobCard(
                            job = job,
                            onAcceptClick = { /* TODO: Handle job acceptance */ },
                            onDetailsClick = { onJobDetails(job) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetAvailabilityScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Set Availability Screen",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}
