package com.example.authIgel

import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.authIgel.databinding.ActivityMainBinding
import com.example.authIgel.domain.otp.OtpGenerator
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val secretKey = "12345678901234567890".toByteArray(Charsets.US_ASCII)

        binding.appBarMain.fab.setOnClickListener { view ->
            val code = OtpGenerator.generateTOTP(secretKey);

            val parentLayout = binding.appBarMain.contentMain.contentMainLayout;

            val card = MaterialCardView(this).apply {
                id = View.generateViewId()
                radius = 16f
                cardElevation = 12f
                setContentPadding(48, 48, 48, 48)
                setCardBackgroundColor(getColor(android.R.color.white))
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(32, 64, 32, 0)
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            // Add text inside the card
            val text = TextView(this).apply {
                text = context.getString(R.string.otp, code)
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val deleteCardButton = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = null
                contentDescription = "Close"
                setOnClickListener {
                    (card.parent as? ConstraintLayout)?.removeView(card)
                    Snackbar.make(binding.root, "Card removed", Snackbar.LENGTH_SHORT).show()
                }
            }

            val refreshButton = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_popup_sync)
                background = null
                contentDescription = "Refresh"
                setOnClickListener {
                    text.text = context.getString(R.string.otp, OtpGenerator.generateTOTP(secretKey))
                    Snackbar.make(binding.root, "Card refreshed", Snackbar.LENGTH_SHORT).show()
                }
            }

            row.addView(text)
            row.addView(refreshButton)
            row.addView(deleteCardButton)
            card.addView(row)
            parentLayout.addView(card)

            Snackbar.make(view, "Card added!", Snackbar.LENGTH_SHORT).show()

        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}