package com.cpen321.usermanagement.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.screens.AuthScreen
import com.cpen321.usermanagement.ui.screens.AvailableJobsScreen
import com.cpen321.usermanagement.ui.screens.JobDetailsScreen
import com.cpen321.usermanagement.ui.screens.LoadingScreen
import com.cpen321.usermanagement.ui.screens.MainScreen
import com.cpen321.usermanagement.ui.screens.ManageOrdersScreen
import com.cpen321.usermanagement.ui.screens.MoverJobHistoryScreen
import com.cpen321.usermanagement.ui.screens.ManageProfileScreen
import com.cpen321.usermanagement.ui.screens.MoverMainScreen
import com.cpen321.usermanagement.ui.screens.ProfileScreenActions
import com.cpen321.usermanagement.ui.screens.ProfileCompletionScreen
import com.cpen321.usermanagement.ui.screens.ProfileScreen
import com.cpen321.usermanagement.ui.screens.RoleSelectionScreen
import com.cpen321.usermanagement.ui.viewmodels.JobViewModel
import com.cpen321.usermanagement.ui.screens.StudentMainScreen
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModel
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.NavigationViewModel
import com.cpen321.usermanagement.ui.viewmodels.OrderViewModel
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel

object NavRoutes {
    const val LOADING = "loading"
    const val AUTH = "auth"
    const val MAIN = "main"

    const val STUDENT = "student"

    const val MOVER = "mover"
    const val PROFILE = "profile"
    const val ROLE_SELECTION = "role_selection"
    const val MANAGE_PROFILE = "manage_profile"

    const val MANAGE_ORDERS = "manage_orders"
    const val JOB_HISTORY = "job_history"
    const val PROFILE_COMPLETION = "profile_completion"
    const val AVAILABLE_JOBS = "mover/available_jobs"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val navigationStateManager = navigationViewModel.navigationStateManager
    val navigationEvent by navigationStateManager.navigationEvent.collectAsState()

    // Initialize view models required for navigation-level scope
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val jobViewModel: JobViewModel = hiltViewModel()
    // Handle navigation events from NavigationStateManager
    LaunchedEffect(navigationEvent) {
        handleNavigationEvent(
            navigationEvent,
            navController,
            navigationStateManager,
            authViewModel,
            orderViewModel,
            mainViewModel
        )
    }

    AppNavHost(
        navController = navController,
        authViewModel = authViewModel,
        profileViewModel = profileViewModel,
        mainViewModel = mainViewModel,
        orderViewModel = orderViewModel,
        jobViewModel = jobViewModel,
        navigationStateManager = navigationStateManager
    )
}

private fun handleNavigationEvent(
    navigationEvent: NavigationEvent,
    navController: NavHostController,
    navigationStateManager: NavigationStateManager,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    mainViewModel: MainViewModel
) {
    when (navigationEvent) {
        is NavigationEvent.NavigateToAuth -> {
            navController.navigate(NavRoutes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToAuthWithMessage -> {
            authViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMain -> {
            navController.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMainWithMessage -> {
            mainViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToProfileCompletion -> {
            navController.navigate(NavRoutes.PROFILE_COMPLETION) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToProfile -> {
            navController.navigate(NavRoutes.PROFILE)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToManageProfile -> {
            navController.navigate(NavRoutes.MANAGE_PROFILE)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToManageOrders -> {
            val route = when (navigationStateManager.getCurrentUserRole()?.uppercase()) {
                "MOVER" -> NavRoutes.JOB_HISTORY
                else -> NavRoutes.MANAGE_ORDERS
            }
            navController.navigate(route)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToRoleSelection -> {
            navController.navigate(NavRoutes.ROLE_SELECTION) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToStudentMain -> {
            navController.navigate(NavRoutes.STUDENT) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToStudentMainWithMessage -> {
            mainViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.STUDENT) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMoverMain -> {
            navController.navigate(NavRoutes.MOVER) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMoverMainWithMessage -> {
            mainViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.MOVER) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateBack -> {
            navController.popBackStack()
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.ClearBackStack -> {
            navController.popBackStack(navController.graph.startDestinationId, false)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NoNavigation -> {
            // Do nothing
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    jobViewModel: JobViewModel,
    navigationStateManager: NavigationStateManager
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOADING
    ) {
        composable(NavRoutes.LOADING) {
            LoadingScreen(message = stringResource(R.string.checking_authentication))
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(authViewModel = authViewModel, profileViewModel = profileViewModel)
        }

        composable(NavRoutes.PROFILE_COMPLETION) {
            ProfileCompletionScreen(
                profileViewModel = profileViewModel,
                onProfileCompleted = { navigationStateManager.handleProfileCompletion() },
                onProfileCompletedWithMessage = { message ->
                    Log.d("AppNavigation", "Profile completed with message: $message")
                    navigationStateManager.handleProfileCompletionWithMessage(message)
                }
            )
        }

        composable(NavRoutes.MAIN) {
            MainScreen(
                mainViewModel = mainViewModel,
                onProfileClick = { navigationStateManager.navigateToProfile() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                userRole = navigationStateManager.getCurrentUserRole(),
                actions = ProfileScreenActions(
                    onBackClick = { navigationStateManager.navigateBack() },
                    onManageProfileClick = { navigationStateManager.navigateToManageProfile() },
                    onManageOrdersClick = {navigationStateManager.navigateToManageOrders()},
                    onAccountDeleted = { navigationStateManager.handleAccountDeletion() },
                    onSignOut = {navigationStateManager.handleSignOut()}
                )
            )
        }

        composable(NavRoutes.MANAGE_PROFILE) {
            ManageProfileScreen(
                profileViewModel = profileViewModel,
                onBackClick = { navigationStateManager.navigateBack() }
            )
        }

        composable (NavRoutes.MANAGE_ORDERS){
            ManageOrdersScreen(
                profileViewModel = profileViewModel,
                orderViewModel = orderViewModel,
                onBackClick = { navigationStateManager.navigateBack()}
            )
        }

        composable (NavRoutes.JOB_HISTORY){
            MoverJobHistoryScreen(
                jobViewModel = jobViewModel,
                profileViewModel = profileViewModel,
                onBackClick = { navigationStateManager.navigateBack()}
            )
        }
        composable(NavRoutes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onRoleSelected = { role: String ->
                    authViewModel.selectUserRole(role)
                }
            )
        }

        composable(NavRoutes.STUDENT) {
            StudentMainScreen(
                mainViewModel = mainViewModel,
                orderViewModel = orderViewModel,
                onProfileClick = { navigationStateManager.navigateToProfile() }
                )
        }

        composable(NavRoutes.MOVER) {
            MoverMainScreen(
                mainViewModel = mainViewModel,
                onProfileClick = { navigationStateManager.navigateToProfile() },
                onJobDetails = { jobId ->
                    navController.navigate("${Screen.JobDetails.route}/${jobId}")
                }
            )
        }

        composable(NavRoutes.AVAILABLE_JOBS) {
            AvailableJobsScreen(
                onJobDetails = { job ->
                    navController.navigate("${Screen.JobDetails.route}/${job.id}")
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = "${Screen.JobDetails.route}/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            JobDetailsScreen(
                jobId = jobId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
