package com.vudev.pet

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.vudev.pet.core.Personality
import com.vudev.pet.data.PetRepository
import com.vudev.pet.data.PetSettings
import com.vudev.pet.render.PetLooks
import com.vudev.pet.service.PetOverlayService

/** Cài đặt: tên, hình dáng, tính cách, kích thước, âm thanh, giờ yên tĩnh, đặt lại. */
class SettingsActivity : Activity() {

    private lateinit var settings: PetSettings
    private lateinit var repo: PetRepository

    private lateinit var nameEdit: EditText
    private lateinit var personalityGroup: RadioGroup
    private lateinit var lookGroup: RadioGroup
    private lateinit var sizeSeek: SeekBar
    private lateinit var sizeLabel: TextView
    private lateinit var opacitySeek: SeekBar
    private lateinit var opacityLabel: TextView
    private lateinit var soundCheck: CheckBox
    private lateinit var bubbleCheck: CheckBox
    private lateinit var quietCheck: CheckBox
    private lateinit var quietStart: Spinner
    private lateinit var quietEnd: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = PetSettings(this)
        repo = PetRepository(this)
        setContentView(buildUi())
        loadValues()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = getString(R.string.settings_title)
            textSize = 24f
            setTextColor(Color.parseColor("#16233F"))
            setPadding(0, 0, 0, dp(14))
        })

        root.addView(label(getString(R.string.name_label)))
        nameEdit = EditText(this).apply {
            setSingleLine(true)
            hint = getString(R.string.name_label)
        }
        root.addView(nameEdit)

        root.addView(label(getString(R.string.personality_label)))
        personalityGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val personalities = listOf(
            Personality.PLAYFUL to R.string.personality_playful,
            Personality.SHY to R.string.personality_shy,
            Personality.LAZY to R.string.personality_lazy,
            Personality.FRIENDLY to R.string.personality_friendly,
            Personality.CURIOUS to R.string.personality_curious,
        )
        personalities.forEach { (personality, labelRes) ->
            personalityGroup.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = getString(labelRes)
                tag = personality
            })
        }
        root.addView(personalityGroup)

        root.addView(label(getString(R.string.look_label)))
        lookGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        PetLooks.ALL.forEach { look ->
            lookGroup.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = look.label
                tag = look.id
            })
        }
        root.addView(lookGroup)

        root.addView(label(getString(R.string.size_label)))
        sizeLabel = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#5B6B8C")) }
        root.addView(sizeLabel)
        sizeSeek = SeekBar(this).apply {
            max = 170
            setOnSeekBarChangeListener(simpleListener { sizeLabel.text = (90 + progress).toString() + " dp" })
        }
        root.addView(sizeSeek)

        root.addView(label(getString(R.string.opacity_label)))
        opacityLabel = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#5B6B8C")) }
        root.addView(opacityLabel)
        opacitySeek = SeekBar(this).apply {
            max = 65
            setOnSeekBarChangeListener(simpleListener { opacityLabel.text = (35 + progress).toString() + "%" })
        }
        root.addView(opacitySeek)

        soundCheck = CheckBox(this).apply { text = getString(R.string.sound_label) }
        root.addView(soundCheck)
        bubbleCheck = CheckBox(this).apply { text = getString(R.string.bubble_label) }
        root.addView(bubbleCheck)
        quietCheck = CheckBox(this).apply { text = getString(R.string.quiet_label) }
        root.addView(quietCheck)

        val quietRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        quietRow.addView(TextView(this).apply {
            text = getString(R.string.quiet_start_label)
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            width = dp(96)
        })
        quietStart = hourSpinner()
        quietRow.addView(quietStart, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        quietRow.addView(TextView(this).apply {
            text = getString(R.string.quiet_end_label)
            textSize = 13f
            setTextColor(Color.parseColor("#5B6B8C"))
            setPadding(dp(12), 0, dp(8), 0)
        })
        quietEnd = hourSpinner()
        quietRow.addView(quietEnd, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(quietRow)

        val save = Button(this).apply {
            text = getString(R.string.action_save)
            setOnClickListener { save() }
        }
        root.addView(save, topMargin(dp(10)))

        val reset = Button(this).apply {
            text = getString(R.string.reset_pet)
            setOnClickListener { confirmReset() }
        }
        root.addView(reset, topMargin(dp(8)))

        root.addView(TextView(this).apply {
            text = getString(R.string.about_body)
            textSize = 12f
            setTextColor(Color.parseColor("#8A96B2"))
            setPadding(0, dp(18), 0, 0)
        })
        return scroll
    }

    private fun hourSpinner(): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@SettingsActivity,
            android.R.layout.simple_spinner_item,
            (0..23).map { it.toString().padStart(2, '0') + ":00" },
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#16233F"))
        setPadding(0, dp(14), 0, dp(4))
    }

    private fun simpleListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange()
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun loadValues() {
        nameEdit.setText(settings.petName)
        for (i in 0 until personalityGroup.childCount) {
            val button = personalityGroup.getChildAt(i) as RadioButton
            if (button.tag == settings.personality) button.isChecked = true
        }
        for (i in 0 until lookGroup.childCount) {
            val button = lookGroup.getChildAt(i) as RadioButton
            if (button.tag == settings.lookId) button.isChecked = true
        }
        sizeSeek.progress = settings.sizeDp - 90
        sizeLabel.text = settings.sizeDp.toString() + " dp"
        opacitySeek.progress = ((settings.opacity * 100).toInt() - 35).coerceIn(0, 65)
        opacityLabel.text = (settings.opacity * 100).toInt().toString() + "%"
        soundCheck.isChecked = settings.soundOn
        bubbleCheck.isChecked = settings.bubbleOn
        quietCheck.isChecked = settings.quietEnabled
        quietStart.setSelection(settings.quietStartHour)
        quietEnd.setSelection(settings.quietEndHour)
    }

    private fun save() {
        settings.petName = nameEdit.text.toString()
        settings.personality = selectedPersonality()
        settings.lookId = selectedLook()
        settings.sizeDp = 90 + sizeSeek.progress
        settings.opacity = (35 + opacitySeek.progress) / 100f
        settings.soundOn = soundCheck.isChecked
        settings.bubbleOn = bubbleCheck.isChecked
        settings.quietEnabled = quietCheck.isChecked
        settings.quietStartHour = quietStart.selectedItemPosition
        settings.quietEndHour = quietEnd.selectedItemPosition
        if (PetOverlayService.isRunning) {
            startService(
                Intent(this, PetOverlayService::class.java).setAction(PetOverlayService.ACTION_CONFIG_CHANGED),
            )
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun selectedPersonality(): Personality {
        val id = personalityGroup.checkedRadioButtonId
        if (id == -1) return settings.personality
        val button = findViewById<RadioButton>(id)
        return (button.tag as? Personality) ?: settings.personality
    }

    private fun selectedLook(): String {
        val id = lookGroup.checkedRadioButtonId
        if (id == -1) return settings.lookId
        val button = findViewById<RadioButton>(id)
        return (button.tag as? String) ?: settings.lookId
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_body)
            .setPositiveButton(R.string.reset_yes) { _, _ ->
                repo.clear()
                settings.petName = "Bông"
                settings.personality = Personality.FRIENDLY
                settings.lookId = PetLooks.BONG.id
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun topMargin(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = value
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
