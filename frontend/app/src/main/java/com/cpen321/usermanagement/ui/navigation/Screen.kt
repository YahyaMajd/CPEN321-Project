package com.cpen321.usermanagement.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object SelectRole : Screen("select_role")
    object MoverMain : Screen("mover_main")
    object AvailableJobs : Screen("available_jobs")
    object JobDetails : Screen("job_details")
}