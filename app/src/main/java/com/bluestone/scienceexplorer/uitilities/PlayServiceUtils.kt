package com.bluestone.scienceexplorer.uitilities

import android.app.Activity
import android.app.Dialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

fun checkGooglePlayServices(activity: Activity): Boolean {
    val googlePlayServicesCheck: Int =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
    return when (googlePlayServicesCheck) {
        ConnectionResult.SUCCESS ->  true
        else -> {
            val dialog: Dialog? =
                GoogleApiAvailability.getInstance().getErrorDialog(activity, googlePlayServicesCheck, 0)
            dialog?.show()
            false
        }
    }
}
