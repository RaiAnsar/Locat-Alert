package com.woxich.locatalert.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woxich.locatalert.NavDest
import com.woxich.locatalert.R

/**
 * These are the possible states of the home screen sub navigation.
 */
sealed class HomeNavigationItem(

    /**
     * Sub navigation route (in home screen).
     */
    var route: String,

    /**
     * Image icon of this menu item.
     */
    @DrawableRes
    var icon: Int,

    /**
     * Title of this menu item.
     */
    @StringRes
    var title: Int

) {

    object Chats : HomeNavigationItem(
        route = NavDest.HOME_CHATS,
        icon = R.drawable.icon_chat,
        title = R.string.home_chats
    )

    object Friends: HomeNavigationItem(
        route = NavDest.HOME_FRIENDS,
        icon = R.drawable.icon_friends,
        title = R.string.home_contacts
    )

    object Blocked: HomeNavigationItem(
        route = NavDest.HOME_BLOCKED,
        icon = R.drawable.icon_block,
        title = R.string.home_blocked
    )

    object Groups: HomeNavigationItem(
        route = NavDest.HOME_GROUPS,
        icon = R.drawable.icon_group,
        title = R.string.home_groups
    )

}