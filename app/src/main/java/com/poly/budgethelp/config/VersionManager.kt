package com.poly.budgethelp.config

import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.poly.budgethelp.MainActivity

class VersionManager {
    data class Version (
        val major: Int,
        val minor: Int,
        val subMinor: Int
    ) {
        override fun toString(): String {
            return String.format("%d.%d.%d", major, minor, subMinor)
        }
    }
    companion object {
        private const val VERSION_URL: String = "https://drive.google.com/uc?export=download&id=1DmYsg4Gg9dYsCpBpEHakehx-t8dJKrX1"
        const val RELEASE_URL: String = "GITHUB_LINK_HERE"

        var currentVersion: Version = Version(1, 0, 0)

        fun fetchLatestVersion(context: MainActivity) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = StringRequest(Request.Method.GET, VERSION_URL,
                {response ->
                    val versionData: Version = stringToVersionOrNull(response) ?: return@StringRequest
                    context.onVersionRetrieved(currentVersion, versionData)
                },
                {error ->
                    context.onVersionRetrieved(currentVersion, currentVersion)
                })
            queue.add(stringRequest)
        }

        fun stringToVersionOrNull(versionString: String): Version? {
            val contents = versionString.split(".")
            if (contents.size != 3)
                return null

            val major: Int? = contents[0].toIntOrNull()
            val minor: Int? = contents[1].toIntOrNull()
            val subMinor: Int? = contents[2].toIntOrNull()
            if (major == null || minor == null || subMinor == null)
                return null

            return Version(major, minor, subMinor)
        }
    }
}