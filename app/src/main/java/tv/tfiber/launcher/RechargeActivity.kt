package tv.tfiber.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RechargeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recharge)

        val recyclerView = findViewById<RecyclerView>(R.id.iconRecyclerView)

        // Define icon list with increased size
        val leftIcons = listOf(
            IconItem(R.drawable.ulkarecharge, "Ulka Recharge", url="https://ulka.tv/customers/login"),
            IconItem(R.drawable.tgspdcl, "TGSPDCL", url="https://tgsouthernpower.org/paybillonline")
        )

        // Display exactly 5 icons per row & increase icon size
        recyclerView.layoutManager = GridLayoutManager(this, 8)
        recyclerView.adapter = LauncherAdapter(leftIcons) { iconItem ->
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
                iconItem.packageName == "com.tsat.tv" || iconItem.packageName == "com.tvapp.tsat" -> {
                    launchApp(iconItem.packageName)
                }
                !iconItem.url.isNullOrEmpty() -> {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(iconItem.url))
                    startActivity(browserIntent)
                }
                !iconItem.packageName.isNullOrEmpty() -> {
                    launchApp(iconItem.packageName)
                }
                else -> {
                    Log.e("WebViewActivity", "No valid action for this icon")
                }
            }
        }

        // Set default focus on first icon
        recyclerView.post {
            val firstItemView = recyclerView.getChildAt(0)
            firstItemView?.requestFocus()
        }

        // Focus change listener for hover effect
        recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        Log.d("WebViewActivity", "Icon focused!")
                        view.scaleX = 1.2f // Increased size when focused
                        view.scaleY = 1.2f
                    } else {
                        view.scaleX = 1.0f // Reset size
                        view.scaleY = 1.0f
                    }
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                // No action needed
            }
        })
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Log.e("WebViewActivity", "App not found: $packageName")
            }
        } catch (e: Exception) {
            Log.e("WebViewActivity", "Error launching app: $packageName", e)
        }
    }
}
