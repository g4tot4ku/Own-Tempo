package pt.ubi.clp.owntempo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import pt.ubi.clp.owntempo.data.AppSettings

class OwnTempoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the saved day/night choice before any screen is created
        AppCompatDelegate.setDefaultNightMode(AppSettings(this).themeMode.nightMode)
    }
}