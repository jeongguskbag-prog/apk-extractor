package com.jeongguskbag.apkvault

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/** 설치된 앱 하나의 정보. 추출에 필요한 파일 경로(sourceDir, splitSourceDirs)를 함께 가짐 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val isSystem: Boolean,
    val baseApk: String,
    val splitApks: List<String>,
    val totalSize: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val lastUpdate: Long,
) {
    val isSplit: Boolean get() = splitApks.isNotEmpty()
}

object AppRepository {

    /** 설치된 앱 목록. 느리므로 백그라운드 스레드에서 호출할 것 */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        return packages.mapNotNull { info -> toEntry(pm, info) }
    }

    private fun toEntry(pm: PackageManager, info: PackageInfo): AppEntry? {
        val app = info.applicationInfo ?: return null
        val base = app.sourceDir ?: return null
        if (!File(base).canRead()) return null
        val splits = app.splitSourceDirs?.filter { File(it).canRead() } ?: emptyList()
        val size = (listOf(base) + splits).sumOf { File(it).length() }
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        val isSystem = (app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        return AppEntry(
            packageName = info.packageName,
            label = app.loadLabel(pm).toString().ifBlank { info.packageName },
            versionName = info.versionName ?: "",
            versionCode = versionCode,
            isSystem = isSystem,
            baseApk = base,
            splitApks = splits,
            totalSize = size,
            minSdk = app.minSdkVersion,
            targetSdk = app.targetSdkVersion,
            lastUpdate = info.lastUpdateTime,
        )
    }
}
