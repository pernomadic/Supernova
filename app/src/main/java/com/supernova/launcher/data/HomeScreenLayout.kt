package com.supernova.launcher.data

// Home screen layout mode: HOME_AND_APPS keeps the standard two-tier launcher
// (home screen + app drawer), while HOME_ONLY eliminates the drawer and places
// all installed apps directly onto home screen pages in drawer sort order,
// matching the OneUI launcher behavior.
@kotlinx.serialization.Serializable(with = HomeScreenLayout.Names::class)
enum class HomeScreenLayout {
    HOME_AND_APPS,
    HOME_ONLY;

    object Names : SettingEnumSerializer<HomeScreenLayout>("HomeScreenLayout", entries, HOME_AND_APPS)

    companion object {
        fun fromName(name: String): HomeScreenLayout = Names.parse(name)
    }
}
