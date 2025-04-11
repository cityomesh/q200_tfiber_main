

package tv.tfiber.launcher

import android.content.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EntertainmentActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private var clickSoundId: Int = 0
    private lateinit var appGamesAdapter: LauncherAdapter
    private lateinit var videoAdapter: LauncherAdapter // New adapter for videos
    private val appGamesIcons = mutableListOf<IconItem>()
    private val videoIcons = mutableListOf<IconItem>() // New list for video icons
    private lateinit var iconRecyclerView: RecyclerView
    private lateinit var videoRecyclerView: RecyclerView

    private val allIcons = mutableListOf<IconItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entertainment) // Assuming you want to reuse the layout for now. If you need to change layout, update here.

        // Load sound
        initSoundPool()

        iconRecyclerView = findViewById(R.id.iconRecyclerView)
        videoRecyclerView = findViewById(R.id.videoRecyclerView) // Initialize video RecyclerView

        // Define your Entertainment content here (Apps and Videos)
        val appGamesAdditionalIcons = listOf(
            IconItem(R.drawable.zee5, "", "com.graymatrix.did"),
            IconItem(R.drawable.ottplay, "", "com.htlabs.ottplay"),
            IconItem(R.drawable.jiostar, "", "in.startv.hotstar"),
            IconItem(R.drawable.netflix, "", "com.netflix.mediaclient")
            
        )
        appGamesIcons.addAll(appGamesAdditionalIcons)

        appGamesAdapter = LauncherAdapter(appGamesIcons) { iconItem ->
            handleIconClick(iconItem) // Call the click handler
        }
        iconRecyclerView.layoutManager = GridLayoutManager(this, 7)
        iconRecyclerView.adapter = appGamesAdapter

        // --- Video Icons ---
        val videoAdditionalIcons = listOf(
            IconItem(R.drawable.img_1, "", "com.google.android.youtube", thumbnailUri = Uri.parse("android.resource://" + packageName + "/" + R.drawable.img_1))
        )
        videoIcons.addAll(videoAdditionalIcons)

        videoAdapter = LauncherAdapter(videoIcons) { iconItem ->
            handleIconClick(iconItem) // Call the click handler
        }
        videoRecyclerView.layoutManager = GridLayoutManager(this, 7)
        videoRecyclerView.adapter = videoAdapter


        // --- Initial Focus ---
        iconRecyclerView.post {
            if (appGamesIcons.isNotEmpty()) {
                iconRecyclerView.getChildAt(0)?.requestFocus()
            }
        }

        // --- Shared Focus Change Listener ---
        val onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.scaleX = 1.2f
                view.scaleY = 1.2f
                playSound()
            } else {
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }

        // Apply focus listener to both RecyclerViews
        iconRecyclerView.addOnChildAttachStateChangeListener(object :
            RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.onFocusChangeListener = onFocusChangeListener
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })
        videoRecyclerView.addOnChildAttachStateChangeListener(object :
            RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.onFocusChangeListener = onFocusChangeListener
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })

    }


    private fun handleIconClick(iconItem: IconItem) {
        when {
            iconItem.packageName == "com.google.android.youtube" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                    intent.setPackage("com.google.android.youtube")
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                }
            }
            !iconItem.url.isNullOrEmpty() -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(iconItem.url))
                startActivity(browserIntent)
            }
            !iconItem.packageName.isNullOrEmpty() -> {
                launchApp(iconItem.packageName)
            }
            else -> {
                Log.e("EntertainmentActivity", "No valid action for ${iconItem.label}")
                // You might want to add a default action here, like displaying a Toast message.
                //Toast.makeText(this, "No action available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Log.e("VodActivity", "App not found: $packageName")
            }
        } catch (e: Exception) {
            Log.e("VodActivity", "Error launching app: $packageName", e)
        }
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        clickSoundId = soundPool.load(this, R.raw.click_sound, 1)
    }

    private fun playSound() {
        if (clickSoundId != 0) {
            soundPool.play(clickSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}