package com.apkextractor.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 설치된 앱을 파일로 만든다.
 * - 단일 APK 앱: base APK 를 그대로 복사 (.apk)
 * - 분할 앱: base + split APK 들과 manifest.json, icon.png 를 ZIP 으로 묶음 (.xapk)
 *   XAPK 는 APKPure 등이 쓰는 형식으로, 분할 APK 설치 도구에서 그대로 설치할 수 있다.
 */
object Extractor {

    const val FOLDER = "ApkExtractor"
    private const val MIME_APK = "application/vnd.android.package-archive"
    private const val MIME_XAPK = "application/octet-stream"

    fun fileName(app: AppEntry): String {
        val safeLabel = app.label.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1f]"), "_").trim().take(60).ifBlank { app.packageName }
        val version = app.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(30)
        val ext = if (app.isSplit) "xapk" else "apk"
        return if (version.isNotEmpty()) "${safeLabel}_$version.$ext" else "$safeLabel.$ext"
    }

    private fun mime(app: AppEntry) = if (app.isSplit) MIME_XAPK else MIME_APK

    /** 앱 내용을 out 에 씀. out 은 닫지 않음 */
    fun writeTo(context: Context, app: AppEntry, out: OutputStream) {
        if (!app.isSplit) {
            FileInputStream(app.baseApk).use { it.copyTo(out, 64 * 1024) }
            out.flush()
            return
        }
        val zip = ZipOutputStream(BufferedOutputStream(out, 64 * 1024))
        // APK 는 이미 압축돼 있으므로 가장 빠른 압축만 사용
        zip.setLevel(Deflater.BEST_SPEED)
        val splitFiles = app.splitApks.map { path -> splitEntryName(path) to path }
        zip.putNextEntry(ZipEntry("${app.packageName}.apk"))
        FileInputStream(app.baseApk).use { it.copyTo(zip, 64 * 1024) }
        zip.closeEntry()
        for ((name, path) in splitFiles) {
            zip.putNextEntry(ZipEntry(name))
            FileInputStream(path).use { it.copyTo(zip, 64 * 1024) }
            zip.closeEntry()
        }
        iconPng(context, app)?.let { png ->
            zip.putNextEntry(ZipEntry("icon.png"))
            zip.write(png)
            zip.closeEntry()
        }
        zip.putNextEntry(ZipEntry("manifest.json"))
        zip.write(manifestJson(app, splitFiles.map { it.first }).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.finish()
        zip.flush()
    }

    /** "split_config.arm64_v8a.apk" → "config.arm64_v8a.apk" */
    private fun splitEntryName(path: String): String {
        val name = File(path).name
        return if (name.startsWith("split_")) name.removePrefix("split_") else name
    }

    private fun manifestJson(app: AppEntry, splitNames: List<String>): String {
        val splits = JSONArray()
        splits.put(JSONObject().put("file", "${app.packageName}.apk").put("id", "base"))
        val configIds = JSONArray()
        for (name in splitNames) {
            val id = name.removeSuffix(".apk")
            splits.put(JSONObject().put("file", name).put("id", id))
            configIds.put(id)
        }
        return JSONObject()
            .put("xapk_version", 2)
            .put("package_name", app.packageName)
            .put("name", app.label)
            .put("version_code", app.versionCode.toString())
            .put("version_name", app.versionName)
            .put("min_sdk_version", app.minSdk.toString())
            .put("target_sdk_version", app.targetSdk.toString())
            .put("split_configs", configIds)
            .put("split_apks", splits)
            .put("total_size", app.totalSize)
            .put("icon", "icon.png")
            .toString(2)
    }

    private fun iconPng(context: Context, app: AppEntry): ByteArray? = try {
        val drawable = context.packageManager.getApplicationIcon(app.packageName)
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val bytes = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        bitmap.recycle()
        bytes.toByteArray()
    } catch (e: Exception) {
        null
    }

    /** 다운로드/ApkExtractor 에 저장하고, 사용자에게 보여줄 경로를 돌려줌. 백그라운드 스레드에서 호출 */
    fun saveToDownloads(context: Context, app: AppEntry): String {
        val name = fileName(app)
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime(app))
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Cannot create file in Downloads")
            try {
                resolver.openOutputStream(uri)?.use { writeTo(context, app, it) }
                    ?: throw IllegalStateException("Cannot open output stream")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            // 같은 이름이 있으면 MediaStore 가 이름을 바꾸므로 실제 이름을 다시 읽음
            val finalName = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: name
            return "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/$finalName"
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        if (!dir.isDirectory && !dir.mkdirs()) throw IllegalStateException("Cannot create ${dir.path}")
        var file = File(dir, name)
        var n = 1
        while (file.exists()) {
            file = File(dir, name.replaceAfterLast('.', "").removeSuffix(".") + " ($n)." + name.substringAfterLast('.'))
            n++
        }
        try {
            FileOutputStream(file).use { writeTo(context, app, it) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/${file.name}"
    }

    /**
     * 공유용 파일을 앱 캐시에 만들고 공유 Intent 를 돌려줌. 백그라운드 스레드에서 호출.
     * 여러 앱이면 ACTION_SEND_MULTIPLE 로 한 번에 보냄. onProgress(완료 개수) 로 진행 상황을 알림
     */
    fun prepareShare(context: Context, apps: List<AppEntry>, onProgress: (Int) -> Unit = {}): Intent {
        require(apps.isNotEmpty())
        val dir = File(context.cacheDir, "share")
        dir.deleteRecursively()
        if (!dir.mkdirs()) throw IllegalStateException("Cannot create share cache")
        val used = HashSet<String>()
        val uris = ArrayList<Uri>()
        apps.forEachIndexed { i, app ->
            val file = File(dir, uniqueName(fileName(app), used))
            FileOutputStream(file).use { writeTo(context, app, it) }
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            onProgress(i + 1)
        }
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime(apps[0])
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (apps.all { !it.isSplit }) MIME_APK else "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val title = if (apps.size == 1) {
            context.getString(R.string.share_title, apps[0].label)
        } else {
            context.getString(R.string.share_title_multi, apps.size)
        }
        return Intent.createChooser(send, title)
    }

    /** 같은 이름이 이미 있으면 "이름 (2).apk" 처럼 바꿈 */
    private fun uniqueName(name: String, used: MutableSet<String>): String {
        var candidate = name
        var n = 2
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        while (!used.add(candidate.lowercase())) {
            candidate = if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext"
            n++
        }
        return candidate
    }
}
