package com.supernova.launcher.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.supernova.launcher.R

import android.content.DialogInterface
import android.content.res.Configuration
import android.widget.ListView
import android.widget.TextView
import com.supernova.launcher.SupernovaApp
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.ui.theme.OneUiTheme
import com.supernova.launcher.ui.theme.SurfaceShapes
import com.supernova.launcher.ui.theme.ThemeEngine
import com.supernova.launcher.ui.theme.ThemeStyleVariant

// The app's bottom sheets. The plate, corners and navigation-bar color come
// from the theme's bottomSheetDialogTheme; this only fixes how a sheet opens.
object Sheets {

    // A sheet that opens fully. Material's default parks a tall sheet at a
    // "peek" height - about the top three quarters of the screen - and
    // leaves the rest, which is where the Save button lives, hanging below
    // the fold until the user thinks to drag. An editor should open open.
    // A drag down from the top still dismisses it.
    fun open(context: Context): BottomSheetDialog =
        BottomSheetDialog(context).apply {
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            val currentPrefs = SupernovaApp.currentPrefs
            if (currentPrefs != null) {
                val variant = OneUiTheme.activeVariant(context, currentPrefs)
                val bg = when (variant) {
                    ThemeStyleVariant.DARK_AMOLED -> OneUiTheme.AMOLED_CARD
                    ThemeStyleVariant.DARK_DEFAULT -> 0xFF1C1C1E.toInt()
                    ThemeStyleVariant.LIGHT_FROSTED -> 0xFFF0F2F6.toInt()
                    ThemeStyleVariant.LIGHT_DEFAULT -> 0xFFFFFFFF.toInt()
                }
                setOnShowListener {
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheetView ->
                        sheetView.backgroundTintList = ColorStateList.valueOf(bg)
                    }
                }
            }
            OpenDialogs.track(this)
        }
}

// The dialogs and sheets that may be standing over an activity. The launcher
// takes its own down on a Home press, as it does its menus and the drawer: an
// Edit sheet left up had the press run the home button's action behind it,
// and the drawer opened under the sheet.
//
// Held weakly and only asked while showing, so nothing has to be taken out.
// Main thread only.
object OpenDialogs {

    private val tracked = java.util.Collections.newSetFromMap(java.util.WeakHashMap<Dialog, Boolean>())

    fun track(dialog: Dialog) {
        tracked += dialog
    }

    // Dismisses every one opened from this activity; whether any was up.
    fun dismissAll(owner: Activity): Boolean {
        val open = tracked.filter { it.isShowing && it.context.owningActivity() === owner }
        open.forEach { it.dismiss() }
        return open.isNotEmpty()
    }

    private fun Context.owningActivity(): Activity? {
        var context: Context? = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}

// show(), for a dialog the launcher may open: a Home press closes it.
// Applies active OneUI variant styling (AMOLED, Frosted, Dark, Light) and OneUI corner radius.
fun MaterialAlertDialogBuilder.showTracked(prefs: LauncherPrefs? = null): AlertDialog {
    val dialog = this.create()
    dialog.applyOneUiTheme(context, prefs)
    dialog.show()
    OpenDialogs.track(dialog)
    return dialog
}

private const val DIALOG_ROUNDED_CORNER_DP = 24

fun AlertDialog.applyOneUiTheme(context: Context, prefs: LauncherPrefs? = null): AlertDialog {
    val currentPrefs = prefs ?: SupernovaApp.currentPrefs
    val variant = if (currentPrefs != null) {
        OneUiTheme.activeVariant(context, currentPrefs)
    } else {
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMask == Configuration.UI_MODE_NIGHT_YES) {
            ThemeStyleVariant.DARK_DEFAULT
        } else {
            ThemeStyleVariant.LIGHT_DEFAULT
        }
    }

    val (bgColor, textColor, secondaryColor) = when (variant) {
        ThemeStyleVariant.DARK_AMOLED -> Triple(OneUiTheme.AMOLED_CARD, 0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt())
        ThemeStyleVariant.DARK_DEFAULT -> Triple(0xFF1C1C1E.toInt(), 0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt())
        ThemeStyleVariant.LIGHT_FROSTED -> Triple(0xFFF0F2F6.toInt(), 0xFF111111.toInt(), 0xFF666666.toInt())
        ThemeStyleVariant.LIGHT_DEFAULT -> Triple(0xFFFFFFFF.toInt(), 0xFF111111.toInt(), 0xFF666666.toInt())
    }

    window?.setBackgroundDrawable(
        SurfaceShapes.background(bgColor, null, context.dp(DIALOG_ROUNDED_CORNER_DP).toFloat())
    )

    setOnShowListener {
        findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(textColor)
        findViewById<TextView>(android.R.id.message)?.setTextColor(secondaryColor)
        val accent = currentPrefs?.accentColor?.let { ThemeEngine.accentColor(context, it) } ?: textColor
        getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(accent)
        getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(secondaryColor)
        getButton(DialogInterface.BUTTON_NEUTRAL)?.setTextColor(secondaryColor)

        val listView = findViewById<ListView>(androidx.appcompat.R.id.select_dialog_listview)
        listView?.let { list ->
            for (i in 0 until list.childCount) {
                (list.getChildAt(i) as? TextView)?.setTextColor(textColor)
            }
            list.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    (child as? TextView)?.setTextColor(textColor)
                }
                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            })
        }
    }
    return this
}

// A text field for a dialog. A bare EditText handed to setView lands flush
// against the dialog's edges with an underline and nothing else; this wraps
// it in the theme's outlined field (the same one the sheets use) and gives it
// the dialog's own 24dp gutter, so a rename dialog reads like the rest of the
// app rather than like a form from 2014.
object DialogField {

    fun wrap(input: EditText): View {
        val context = input.context
        val density = context.resources.displayMetrics.density
        // The field's own hint becomes the floating label; without this the
        // hint would draw twice, once in the box and once on its border.
        val hint = input.hint
        input.hint = null
        // An EditText that arrives with the platform underline keeps it:
        // TextInputLayout only draws its box over a field with no background
        // of its own.
        input.background = null
        // Names of apps, folders, groups and cards: nothing an autofill
        // service has an answer for.
        input.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        // Configured here rather than left to the theme's textInputStyle: an
        // EditText built in code arrives wrapped in Material's own edit-text
        // overlay, and a TextInputLayout created from that context does not
        // pick the app's outlined style up.
        val field = TextInputLayout(context).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            val radius = context.dp(14).toFloat()
            setBoxCornerRadii(radius, radius, radius, radius)
            setBoxStrokeColorStateList(
                AppCompatResources.getColorStateList(context, R.color.text_input_stroke)
            )
            val accent = context.themeColor(android.R.attr.colorAccent)
            hintTextColor = ColorStateList.valueOf(accent)
            defaultHintTextColor = ColorStateList.valueOf(context.getColor(R.color.textSecondary))
            // TextInputLayout is a LinearLayout and casts the child's params
            // to its own; generic ViewGroup params make addView throw.
            addView(
                input,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        return FrameLayout(context).apply {
            setPadding(context.dp(24), context.dp(8), context.dp(24), 0)
            addView(
                field,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}

private fun Context.themeColor(attr: Int): Int {
    val typed = obtainStyledAttributes(intArrayOf(attr))
    val color = typed.getColor(0, 0)
    typed.recycle()
    return color
}
