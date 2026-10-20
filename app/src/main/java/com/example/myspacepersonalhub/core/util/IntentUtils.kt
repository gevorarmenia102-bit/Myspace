package com.example.myspacepersonalhub.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

object IntentUtils {

    fun openDialer(context: Context, phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            SnackbarController.showMessage("Cannot open dialer: ${e.message}")
            false
        }
    }

    fun sendEmail(context: Context, email: String, subject: String = "", body: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${email.trim()}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            SnackbarController.showMessage("Cannot open email client: ${e.message}")
            false
        }
    }

    fun openMapAddress(context: Context, address: String): Boolean {
        val clean = address
            .replace("\r\n", ", ")
            .replace("\r", ", ")
            .replace("\n", ", ")
            .replace(Regex("\\s*,\\s*"), ", ")
            .replace(Regex(",+"), ",")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',')

        if (clean.isBlank()) return false

        val encodedQuery = Uri.encode(clean)
        return try {
            // geo:?q= without 0,0 coordinate bias so map apps (Yandex, Google Maps) use user's region/GPS
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:?q=$encodedQuery"))
            val chooser = Intent.createChooser(geoIntent, "Open location with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            try {
                // Secondary attempt with geo:0,0?q= for older navigation clients
                val fallbackGeo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
                val chooser = Intent.createChooser(fallbackGeo, "Open location with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
                true
            } catch (e2: Exception) {
                try {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                    )
                    val chooser = Intent.createChooser(webIntent, "Open location with").apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(chooser)
                    true
                } catch (ex: Exception) {
                    SnackbarController.showMessage("Cannot open maps: ${ex.message}")
                    false
                }
            }
        }
    }

    fun openWebUrl(context: Context, rawUrl: String): Boolean {
        return try {
            val trimmed = rawUrl.trim()
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                SnackbarController.showMessage("Blocked untrusted URL scheme: $scheme")
                return false
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            SnackbarController.showMessage("Cannot open browser: ${e.message}")
            false
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            SnackbarController.showMessage("Cannot share: ${e.message}")
            false
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        SnackbarController.showMessage("📋 $label copied to clipboard")
    }
}
