package com.example.carelink.util

import android.content.Context
import java.io.File

fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.packageName).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}


