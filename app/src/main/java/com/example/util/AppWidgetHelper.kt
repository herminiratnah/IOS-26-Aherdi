package com.example.util

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log

object AppWidgetHelper {
    private const val TAG = "AppWidgetHelper"
    private const val APPWIDGET_HOST_ID = 4026
    private const val PREFS_NAME = "ios_native_widgets"
    private const val KEY_WHATSAPP_WIDGET_ID = "whatsapp_widget_id"

    private var appWidgetHost: AppWidgetHost? = null
    private var isListening = false

    fun getHost(context: Context): AppWidgetHost {
        if (appWidgetHost == null) {
            appWidgetHost = AppWidgetHost(context.applicationContext, APPWIDGET_HOST_ID)
        }
        return appWidgetHost!!
    }

    fun startListening(context: Context) {
        if (!isListening) {
            try {
                getHost(context).startListening()
                isListening = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting AppWidgetHost", e)
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                appWidgetHost?.stopListening()
                isListening = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AppWidgetHost", e)
            }
        }
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun getWhatsAppPackageName(context: Context): String? {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", 0)
            "com.whatsapp"
        } catch (_: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", 0)
                "com.whatsapp.w4b"
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    fun getWhatsAppAppIcon(context: Context): Drawable? {
        val pkg = getWhatsAppPackageName(context) ?: return null
        return try {
            context.packageManager.getApplicationIcon(pkg)
        } catch (_: Exception) {
            null
        }
    }

    fun findWhatsAppProvider(context: Context): AppWidgetProviderInfo? {
        val manager = AppWidgetManager.getInstance(context)
        val providers = try {
            manager.installedProviders ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return providers.firstOrNull {
            val pkg = it.provider.packageName.lowercase()
            pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b"
        }
    }

    fun getOrCreateWhatsAppWidgetView(context: Context): Pair<Int, AppWidgetHostView?> {
        val provider = findWhatsAppProvider(context) ?: return Pair(-1, null)
        val manager = AppWidgetManager.getInstance(context)
        val host = getHost(context)
        startListening(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var widgetId = prefs.getInt(KEY_WHATSAPP_WIDGET_ID, -1)

        if (widgetId != -1) {
            val info = manager.getAppWidgetInfo(widgetId)
            if (info != null && info.provider == provider.provider) {
                try {
                    val hostView = host.createView(context, widgetId, info)
                    return Pair(widgetId, hostView)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reuse widgetId $widgetId, will reallocate", e)
                }
            }
        }

        // Allocate new widget ID
        try {
            widgetId = host.allocateAppWidgetId()
            var bound = manager.bindAppWidgetIdIfAllowed(widgetId, provider.provider)

            if (!bound) {
                // Try grantbind via root
                try {
                    RootExecutor.execute("appwidget grantbind --package ${context.packageName} --user 0")
                    bound = manager.bindAppWidgetIdIfAllowed(widgetId, provider.provider)
                } catch (_: Exception) {}
            }

            if (bound) {
                prefs.edit().putInt(KEY_WHATSAPP_WIDGET_ID, widgetId).apply()
                val info = manager.getAppWidgetInfo(widgetId) ?: provider
                val hostView = host.createView(context, widgetId, info)
                return Pair(widgetId, hostView)
            } else {
                return Pair(widgetId, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to allocate or bind WhatsApp widget", e)
            return Pair(-1, null)
        }
    }

    fun requestBindWidgetPermission(activity: Activity, widgetId: Int, provider: AppWidgetProviderInfo, requestCode: Int = 4027) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun saveBoundWidgetId(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_WHATSAPP_WIDGET_ID, widgetId)
            .apply()
    }
}
