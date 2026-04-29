package io.github.dot166.flux

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference

class RSSUrlsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {
    override fun onClick() {
        context.startActivity(Intent(context, RSSUrlsPreferenceActivity::class.java))
    }
}
