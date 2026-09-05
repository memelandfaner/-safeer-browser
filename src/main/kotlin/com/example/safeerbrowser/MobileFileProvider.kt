package com.example.safeerbrowser

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class MobileFileProvider : ContentProvider() {

    private lateinit var authority: String

    override fun onCreate(): Boolean {
        return true
    }

    override fun attachInfo(context: Context, info: ProviderInfo) {
        super.attachInfo(context, info)
        authority = info.authority
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val file = getFileForUri(uri)
        val proj = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(proj, 1)
        val row = cursor.newRow()
        for (col in proj) {
            when (col) {
                OpenableColumns.DISPLAY_NAME -> row.add(file.name)
                OpenableColumns.SIZE -> row.add(file.length())
                else -> row.add(null)
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val file = getFileForUri(uri)
        val ext = file.extension
        return if (ext.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "application/octet-stream"
        } else {
            "application/octet-stream"
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = getFileForUri(uri)
        val fileMode = when (mode) {
            "r" -> ParcelFileDescriptor.MODE_READ_ONLY
            "w", "wa" -> ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE
            "rw" -> ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
            else -> ParcelFileDescriptor.MODE_READ_ONLY
        }
        return ParcelFileDescriptor.open(file, fileMode)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun safeChild(parent: File?, child: String): File {
        if (parent == null) throw FileNotFoundException("Korenska mapa shrambe ni na voljo")
        val cleanChild = child.removePrefix("/").replace('\\', '/')
        val base = parent.canonicalFile
        val target = File(base, cleanChild).canonicalFile
        val basePath = base.path
        val targetPath = target.path
        if (!targetPath.startsWith(basePath + File.separator) && targetPath != basePath) {
            throw FileNotFoundException("Zavrnjen neveljaven poskus dostopa ali pot: $child")
        }
        return target
    }

    private fun getFileForUri(uri: Uri): File {
        val path = uri.path ?: throw FileNotFoundException("Prazen URI")
        val ctx = context ?: throw FileNotFoundException("Context ni na voljo")
        
        val file = if (path.startsWith("/external_files/")) {
            val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
            safeChild(base, path.removePrefix("/external_files/"))
        } else if (path.startsWith("/cache_files/")) {
            safeChild(ctx.cacheDir, path.removePrefix("/cache_files/"))
        } else {
            safeChild(ctx.filesDir, path)
        }
        
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        return file
    }
}
