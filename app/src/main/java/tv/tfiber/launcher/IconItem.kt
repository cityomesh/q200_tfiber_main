package tv.tfiber.launcher

import android.net.Uri

data class IconItem(
    val iconResId: Int,
    val label: String = "",
    val packageName: String? = null,
    val url: String? = null,
    val bottomImageResId: Int? = null,
    val thumbnailUri: Uri? = null
)
