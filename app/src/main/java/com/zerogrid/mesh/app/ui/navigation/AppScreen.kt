package com.zerogrid.mesh.app.ui.navigation

/**
 * App navigation states.
 * Login/Register handle auth; ProfileCompletion is shown after first registration;
 * UserDashboard enters the mesh app; AdminPanel is the admin-only area.
 */
sealed class AppScreen {
    object Login             : AppScreen()
    object Register          : AppScreen()
    object ProfileCompletion : AppScreen()
    object UserDashboard     : AppScreen()
    object AdminPanel        : AppScreen()
}
