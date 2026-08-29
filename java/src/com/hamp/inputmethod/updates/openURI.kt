package com.hamp.inputmethod.updates

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hamp.inputmethod.latin.BuildConfig

fun Context.openURI(uri: String, newTask: Boolean = false) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    } catch(e: ActivityNotFoundException) {
        Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
    }
}

fun Context.openManualUpdateCheck() {
    openURI("https://keyboard.futo.tech/manual_update?version=${BuildConfig.VERSION_CODE}&build=${BuildConfig.FLAVOR}".let {
        if(BuildConfig.BRANCH != "master") {
            "${it}&branch=${BuildConfig.BRANCH}"
        } else {
            it
        }
    })
}