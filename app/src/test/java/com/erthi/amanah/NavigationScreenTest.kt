package com.erthi.amanah

import com.erthi.amanah.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NavigationScreenTest {

    @Test
    fun allScreenRoutes_areUnique() {
        val screens = listOf(
            Screen.Splash, Screen.Onboarding, Screen.SetPin, Screen.Auth,
            Screen.Home, Screen.Will, Screen.WillEditor, Screen.Finance,
            Screen.Debts, Screen.Assets, Screen.Entrusted, Screen.DigitalAccounts,
            Screen.Documents, Screen.Beneficiaries, Screen.Emergency,
            Screen.Backup, Screen.Settings, Screen.AboutApp, Screen.AboutErthi,
        )
        val routes = screens.map { it.route }
        assertEquals("All routes must be unique", routes.size, routes.distinct().size)
    }

    @Test
    fun homeScreen_route_isHome() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun emergencyScreen_route_isEmergency() {
        assertEquals("emergency", Screen.Emergency.route)
    }
}
