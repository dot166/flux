package io.github.dot166.flux

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.navigation.NavigationView
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.app.jConfigActivity.jLIBSettingsFragment
import io.github.dot166.jlib.rss.RSSAlarmScheduler
import io.github.dot166.jlib.rss.RSSFragment
import io.github.dot166.jlib.time.ReminderItem
import io.github.dot166.jlib.utils.ErrorUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.net.URI
import java.util.Calendar

class MainActivity : jActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rssUrls: Array<String?> =
            PreferenceManager.getDefaultSharedPreferences(this).getString("rssUrls", "")!!
                .split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        setContentView(R.layout.activity_main)
        val topAppBar = findViewById<Toolbar>(R.id.actionbar)
        setSupportActionBar(topAppBar)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        rebuildMenu(navigationView, rssUrls, 0)
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, RSSFragment(0))
            .commit()
        topAppBar.setNavigationOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                drawerLayout.open()
            }
        })

        navigationView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
                // Handle menu item selected
                for (i in 0..<navigationView.getMenu().size()) {
                    navigationView.getMenu().getItem(i).setChecked(false)
                }
                menuItem.setChecked(true)
                if (menuItem.getItemId() != Int.Companion.MAX_VALUE) {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, RSSFragment(menuItem.getItemId())).commit()
                } else {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, PreferenceFragment() as jLIBSettingsFragment)
                        .commit()
                }
                drawerLayout.close()
                return true
            }
        })
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) + 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val reminderItem = ReminderItem(cal.getTimeInMillis(), 1)
        RSSAlarmScheduler(this).schedule(reminderItem)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun rebuildMenu(navigationView: NavigationView, rssUrls: Array<String?>, requiredId: Int) {
        GlobalScope.future {
            navigationView.getMenu().clear()
            navigationView.setItemIconTintList(null)
            navigationView.getMenu().add(0, 0, 0, "All Feeds")
            navigationView.getMenu().findItem(0).setIcon(R.drawable.ic_launcher_foreground)
            for (i in rssUrls.indices) {
                val parser = RssParserBuilder().build()
                val channel: RssChannel
                try {
                    channel = parser.getRssChannel(rssUrls[i]!!)
                } catch (e: Exception) {
                    ErrorUtils.handle(e, this@MainActivity)
                    return@future
                }
                navigationView.getMenu().add(0, i + 1, 0, channel.title)
                val uri = URI.create(rssUrls[i])

                var uriNoPath = uri.resolve("/")
                if (uriNoPath.toString().contains("feeds.bbci.co.uk")) {
                    uriNoPath = URI.create("bbc.co.uk")
                }
                val finalI = i
                if (channel.image != null) {
                    Glide.with(this@MainActivity)
                        .load(channel.image!!.url)
                        .into(object : CustomTarget<Drawable?>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable?>?
                            ) {
                                val ld = LayerDrawable(
                                    arrayOf<Drawable>(
                                        ColorDrawable(
                                            obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurface)).getColor(
                                                0,
                                                0
                                            )
                                        ), resource
                                    )
                                )
                                navigationView.getMenu().findItem(finalI + 1).setIcon(ld)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        })
                } else {
                    Glide.with(this@MainActivity)
                        .load("https://www.google.com/s2/favicons?domain=" + uriNoPath.toString())
                        .into(object : CustomTarget<Drawable?>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable?>?
                            ) {
                                val ld = LayerDrawable(
                                    arrayOf<Drawable>(
                                        ColorDrawable(
                                            obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurface)).getColor(
                                                0,
                                                0
                                            )
                                        ), resource
                                    )
                                )
                                navigationView.getMenu().findItem(finalI + 1).setIcon(ld)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                        })
                }
                if (navigationView.getMenu().findItem(i + 1).getIcon() == null) {
                    navigationView.getMenu().findItem(i + 1)
                        .setIcon(R.drawable.ic_launcher_foreground)
                }
            }
            navigationView.getMenu().add(
                0,
                Int.Companion.MAX_VALUE,
                0,
                getString(io.github.dot166.jlib.R.string.settings_name)
            )
            navigationView.getMenu().findItem(Int.Companion.MAX_VALUE)
                .setIcon(io.github.dot166.jlib.R.mipmap.settings)
            navigationView.getMenu().findItem(requiredId).setChecked(true)
        }
    }
}
