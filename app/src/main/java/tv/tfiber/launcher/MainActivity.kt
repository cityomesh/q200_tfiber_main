package tv.tfiber.launcher

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tv.tfiber.launcher.updates.UpdateChecker
import tv.tfiber.launcher.updates.UpdateInfo
import java.io.File
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.content.*
import kotlinx.coroutines.delay


class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var soundPool: SoundPool
    private var clickSoundId: Int = 0
    private var audioFocusRequestGranted = false
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private lateinit var updateChecker: UpdateChecker
    private lateinit var updateDialog: AlertDialog
    private lateinit var progressDialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var viewFlipper: ViewFlipper
    private var uninstallCallback: (() -> Unit)? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234 && resultCode == RESULT_OK) {
            uninstallCallback?.invoke()
            uninstallCallback = null
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("MainActivity", "Audio focus gained")
                audioFocusRequestGranted = true
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("MainActivity", "Audio focus lost")
                audioFocusRequestGranted = false
                mediaPlayer?.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d("MainActivity", "Audio focus lost transient")
                mediaPlayer?.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("MainActivity", "Audio focus lost transient can duck")
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val liveTvLogo = findViewById<ImageView>(R.id.livetv_logo)

        liveTvLogo.setOnClickListener {
            val packageName = "tv.ulka.ulkalite"
            val apkUrl = "https://github.com/cityomesh/Tfiber_Project-Main/releases/download/apkproject/UlkaLite.apk"
            val sharedPref = getSharedPreferences("ulka_prefs", Context.MODE_PRIVATE)

            // If already installed, just open
            if (isAppInstalled(packageName)) {
                Toast.makeText(this, "UlkaLite App Installed", Toast.LENGTH_SHORT).show()
                launchApp(packageName)
            } else {
                // Not installed: show popup and install
                Toast.makeText(this, "Installing UlkaLite...", Toast.LENGTH_SHORT).show()

                downloadAndInstallApk(apkUrl) {
                    sharedPref.edit().putBoolean("ulka_installed", true).apply()
                    launchApp(packageName)
                }
            }
        }


        // Initialize SoundPool
        createSoundPool()
        // Load the click sound
        clickSoundId = soundPool.load(this, R.raw.click_sound, 1)
        if (clickSoundId == 0) {
            Log.e("MainActivity", "Failed to load click sound!")
        } else {
            Log.d("MainActivity", "Click sound loaded with ID: $clickSoundId")
        }
        textureView = findViewById(R.id.bannerTextureView)
        val settingsIcon = findViewById<ImageView>(R.id.settingsIcon)
        val updateIcon: ImageView = findViewById(R.id.updateIcon)
        viewFlipper = findViewById(R.id.imageFlipper)

        updateChecker = UpdateChecker(this)

        settingsIcon.setOnClickListener {
            openSettings()
        }

        updateIcon.setOnClickListener {
            openAppDetails()
        }

        settingsIcon.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                playSound()
                highlightIcon(view)
            } else {
                removeHighlight(view)
            }
        }

        updateIcon.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                playSound()
            }
        }

        updateIcon.setOnHoverListener { v, event ->
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                val width = v.width
                val height = v.height
                Toast.makeText(this, "Width: ${width}px, Height: ${height}px", Toast.LENGTH_SHORT).show()
            }
            false
        }

        val imageUrls = listOf(
            "https://raw.githubusercontent.com/cityomesh/Tfiber_Project-Main/refs/heads/main/tfiber.png",
            "https://raw.githubusercontent.com/cityomesh/Tfiber_Project-Main/refs/heads/main/tfibergoverenment.png",
            "https://raw.githubusercontent.com/cityomesh/Tfiber_Project-Main/refs/heads/main/tfibergovernmentone.png",
            "https://raw.githubusercontent.com/cityomesh/Tfiber_Project-Main/refs/heads/main/tfibergovernmenttwo.png",
        )

        for (url in imageUrls) {
            val imageView = ImageView(this)
            imageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            imageView.scaleType = ImageView.ScaleType.FIT_XY

            val updatedUrl = "$url?timestamp=${System.currentTimeMillis()}"

            Glide.with(this)
                .load(updatedUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imageView)

            viewFlipper.addView(imageView)
        }

        viewFlipper.flipInterval = 30000 // 1 minute
        viewFlipper.startFlipping()

        setupRecyclerView()
        setupTextureView()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioFocusRequest = createAudioFocusRequest()
        requestAudioFocus()
        checkForUpdates()
    }


    private fun createAudioFocusRequest(): AudioFocusRequest {
        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }

    private fun checkForUpdates() {
        Log.d("MainActivity", "checkForUpdates() called")
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = updateChecker.checkForUpdates()
            Log.d("MainActivity", "updateInfo: $updateInfo")
            if (updateInfo != null) {
                Log.d("MainActivity", "UpdateInfo is not null")
                showUpdateDialog(updateInfo)
            } else {
                Log.d("MainActivity", "UpdateInfo is null")
            }
        }
    }

    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        Log.d("MainActivity", "showUpdateDialog() called")
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Update Available")
        builder.setMessage("Version ${updateInfo.versionName} is available. Release Notes: ${updateInfo.releaseNotes}")
        builder.setPositiveButton("Install") { dialog, _ ->
            Log.d("MainActivity", "Install button clicked")
            dialog.dismiss()
            downloadAndInstallApk(updateInfo.apkUrl)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        updateDialog = builder.create()
        Log.d("MainActivity", "updateDialog.show() about to be called")
        updateDialog.show()
    }

    private fun showProgressDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Downloading Update")
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.isIndeterminate = true
        builder.setView(progressBar)
        builder.setCancelable(false)
        progressDialog = builder.create()
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Download Failed")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun handleUpdate(updateInfo: UpdateInfo) {
        // Display a dialog or notification to the user
        // ... (e.g., show a dialog with updateInfo.releaseNotes)

        // For now, let's just log the update info and start the download/install
        Log.d("MainActivity", "Update available: ${updateInfo.versionName}")
        Log.d("MainActivity", "Release Notes: ${updateInfo.releaseNotes}")

        // Start the download and installation
        downloadAndInstallApk(updateInfo.apkUrl)
    }

    private fun downloadAndInstallApk(apkUrl: String) {
        Log.d("MainActivity", "downloadAndInstallApk() called with URL: $apkUrl")
        showProgressDialog() // Show the dialog
        CoroutineScope(Dispatchers.Main).launch { // Use Dispatchers.Main for UI updates
            val apkFile = updateChecker.downloadApk(apkUrl) { progress ->
                // Update the progress bar on the UI thread
                updateProgress(progress)
            }
            if (apkFile != null) {
                Log.d("MainActivity", "APK file path before installApk(): ${apkFile.absolutePath}")
                Log.d("MainActivity", "APK file size: ${apkFile.length()}")
                installApk(apkFile)
            } else {
                // Handle download failure
                Log.e("MainActivity", "Failed to download APK")
                hideProgressDialog() // Hide the dialog
                showErrorDialog("Failed to download update") // Show error message
            }
        }
    }





    private fun downloadAndInstallApk(apkUrl: String, onInstalled: () -> Unit) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("UlkaLite Update")
            .setDescription("Downloading...")
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "UlkaLite.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val uri = dm.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(installIntent)

                    // Delay to allow install to complete, then call onInstalled
                    Handler(Looper.getMainLooper()).postDelayed({
                        onInstalled()
                    }, 1000)
                }
                unregisterReceiver(this)
            }
        }

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }


    private fun updateProgress(progress: Int) {
        progressBar.isIndeterminate = false
        progressBar.progress = progress
    }

    private fun installApk(apkFile: File) {
        Log.d("MainActivity", "installApk() called with file: $apkFile")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                Log.d("MainActivity", "Requesting permission to install packages")
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:$packageName")))
                return
            }
        }

        Log.d("MainActivity", "Permission to install packages granted")

        val apkUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            apkFile // ✅ Use the function argument, not destinationFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Log.d("MainActivity", "Starting installation intent")
        startActivity(intent)
    }

    private fun requestAudioFocus() {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioManager.requestAudioFocus(audioFocusRequest)
        /* } else {
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }     */
    }

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private fun createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            soundPool = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        }
    }

    private fun playSound() {
        Log.d("MainActivity", "playSound() called. audioFocusRequestGranted: $audioFocusRequestGranted")
        if (audioFocusRequestGranted) {
            soundPool.play(clickSoundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }

    private fun openAppDetails() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun highlightIcon(view: View) {
        val color = (view.background as? ColorDrawable)?.color
        if (color != Color.YELLOW) {
            view.setBackgroundColor(Color.YELLOW)
        }
    }

    private fun removeHighlight(view: View) {
        view.setBackgroundColor(Color.TRANSPARENT)
    }


    private fun setupRecyclerView() {
        val leftIcons = listOf(
            IconItem(R.drawable.livetv_logo, "", "tv.ulka.ulkalite"),
            IconItem(R.drawable.youtube, "", "com.google.android.youtube.tv"),
            IconItem(R.drawable.entertainment, "", "com.example.entertainment"),
            IconItem(R.drawable.recharge, "", "com.example.recharge"),
            IconItem(R.drawable.apps, "", "in.webgrid.ulkatv"),
            IconItem(R.drawable.troubleshoot, "", "com.example.vodapp")

        )

        val rightIcons = listOf(
            IconItem(R.drawable.virtual_pc, "", "com.tfiber_vdi"),
            IconItem(R.drawable.e_health, "", url = "https://health.telangana.gov.in"),
            IconItem(R.drawable.e_education, "", "com.example.education"),
            IconItem(R.drawable.community, "", "com.example.community"),
            IconItem(R.drawable.about, "", "com.android.settings"),
            IconItem(R.drawable.digital_nidhi, "", url = "https://usof.gov.in/en/home")
        )

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)

        val recyclerViewLeft = findViewById<RecyclerView>(R.id.recyclerViewLeft)
        val recyclerViewRight = findViewById<RecyclerView>(R.id.recyclerViewRight)

        recyclerViewLeft.addItemDecoration(SpacingItemDecoration(spacingInPixels))
        recyclerViewRight.addItemDecoration(SpacingItemDecoration(spacingInPixels))

        recyclerViewLeft.layoutManager = GridLayoutManager(this, 2)
        recyclerViewRight.layoutManager = GridLayoutManager(this, 2)


        recyclerViewLeft.adapter = LauncherAdapter(leftIcons) { iconItem ->
            when (iconItem.packageName) {

                "tv.ulka.ulkalite" -> {
                    val apkUrl = "https://github.com/cityomesh/Tfiber_Project-Main/releases/download/apkproject/UlkaLite.apk"
                    val packageName = "tv.ulka.ulkalite"
                    val latestVersion = "1.38" // ✅ Update this if new version comes

                    val installedVersion = getAppVersion(packageName)

                    if (installedVersion == latestVersion) {
                        Toast.makeText(this, "UlkaLite App Installed", Toast.LENGTH_SHORT).show()
                        launchApp(packageName)
                    } else {
                        Toast.makeText(this, "Installing UlkaLite...", Toast.LENGTH_SHORT).show()

                        // ✅ Optional: Uninstall old version first
                        uninstallAppIfExists(packageName) {
                            downloadAndInstallApk(apkUrl) {
                                launchApp(packageName)
                            }
                        }
                    }
                }


                "com.example.vodapp" -> {
                    val intent = Intent(this, VodActivity::class.java)
                    startActivity(intent)
                }

                "in.webgrid.ulkatv" -> {
                    Log.d("MainActivity", "Opening AppsActivity")
                    val intent = Intent(this, AppsActivity::class.java)
                    startActivity(intent)
                }
                "com.example.entertainment" -> {
                    Log.d("MainActivity", "Opening EntertainmentActivity")
                    val intent = Intent(this, EntertainmentActivity::class.java)
                    startActivity(intent)
                }
                "com.example.recharge" -> {
                    Log.d("MainActivity", "Opening RechargeActivity")
                    val intent = Intent(this, RechargeActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                    if (iconItem.url != null) {
                        openWebPage(iconItem.url)
                    } else if (iconItem.packageName != null) {
                        launchApp(iconItem.packageName)
                    }
                }
            }
        }

        recyclerViewRight.adapter = LauncherAdapter(rightIcons) { iconItem ->
            when {
                iconItem.packageName == "com.example.community" -> {
                    val intent = Intent(this, CommunityActivity::class.java)
                    startActivity(intent)
                }
                iconItem.url == "https://health.telangana.gov.in" -> {
                    val intent = Intent(this, EHealthActivity::class.java)
                    startActivity(intent)
                }
                iconItem.packageName == "com.example.education" -> {
                    val intent = Intent(this, EEducationActivity::class.java)
                    startActivity(intent)
                }
                !iconItem.url.isNullOrEmpty() -> {
                    openWebPage(iconItem.url)
                }
                !iconItem.packageName.isNullOrEmpty() -> {
                    launchApp(iconItem.packageName)
                }
                else -> {
                    Log.e("MainActivity", "No valid action found for icon")
                }
            }
        }

        // Set default focus to the first item in the left RecyclerView
        recyclerViewLeft.post {
            val firstItemView = recyclerViewLeft.getChildAt(0)
            firstItemView?.requestFocus()
        }


        // Add focus change listener to each item in the RecyclerView
        recyclerViewLeft.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    Log.d("MainActivity", "RecyclerView item focus changed. hasFocus: $hasFocus")
                    if (hasFocus) {
                        playSound()
                    }
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                // No action needed when detached
            }
        })

        recyclerViewRight.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        playSound()
                    }
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                // No action needed when detached
            }
        })
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }


    private fun installApk(file: File, onInstalled: () -> Unit) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            FileProvider.getUriForFile(this, "${packageName}.provider", file),
            "application/vnd.android.package-archive"
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)

        // Wait and try to open after install
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Wait for install
            onInstalled()
        }
    }



    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    private fun getAppVersion(packageName: String): String? {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }


    private fun openApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallIfExists(packageName: String, onUninstalled: () -> Unit) {
        val uri = Uri.parse("package:$packageName")
        val intent = Intent(Intent.ACTION_DELETE, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)

        // Wait for a few seconds before calling the callback
        Handler(Looper.getMainLooper()).postDelayed({
            onUninstalled()
        }, 1000) // Adjust delay as needed
    }



    fun uninstallApp(packageName: String, onUninstalled: () -> Unit) {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)

        // You can wait for some delay or listen to uninstall event
        Handler(Looper.getMainLooper()).postDelayed({
            onUninstalled()
        }, 4000) // 4 seconds delay before trying install
    }


    private fun uninstallAppIfExists(packageName: String, onUninstalled: () -> Unit) {
        if (!isAppInstalled(packageName)) {
            onUninstalled()
            return
        }

        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        // Small delay to wait for uninstall to complete (you can improve using Broadcast)
        Handler(Looper.getMainLooper()).postDelayed({
            onUninstalled()
        }, 2500)
    }


    fun getInstalledVersionName(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun setupTextureView() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                initializeMediaPlayer(surface)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                releaseMediaPlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun initializeMediaPlayer(surfaceTexture: SurfaceTexture) {
        try {
            val videoUri = Uri.parse("android.resource://$packageName/${R.raw.tfiber_intro}")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, videoUri)
                setSurface(Surface(surfaceTexture))
                isLooping = true
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing MediaPlayer", e)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        abandonAudioFocus()
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer == null && textureView.isAvailable) {
            initializeMediaPlayer(textureView.surfaceTexture!!) // Safe unwrapping
        } else {
            mediaPlayer?.start()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        soundPool.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("MainActivity", "onKeyDown: keyCode = $keyCode")
        return when (keyCode) {
            KeyEvent.KEYCODE_SETTINGS -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                true
            }
            KeyEvent.KEYCODE_POWER -> {
                Toast.makeText(this, "Power button pressed", Toast.LENGTH_SHORT).show()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                Toast.makeText(this, "Back button pressed", Toast.LENGTH_SHORT).show()
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun launchApp(packageName: String?) {
        Log.d("MainActivity", "Attempting to launch: $packageName")

        if (packageName.isNullOrEmpty()) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
            return
        }

        // Special case: open Android Settings
        if (packageName == "com.android.settings") {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
            return
        }

        // Try Leanback launcher
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            setPackage(packageName)
        }

        val resolveInfo = packageManager.queryIntentActivities(intent, 0).firstOrNull()
        if (resolveInfo != null) {
            val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                component = ComponentName(packageName, resolveInfo.activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(leanbackIntent)
            return
        }

        // 👉 Special case for UlkaLite app — no Google Play redirect
        if (packageName == "tv.ulka.ulkalite") {
            Toast.makeText(this, "Installing UlkaLite App...", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔁 For all other apps: redirect to Play Store
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
