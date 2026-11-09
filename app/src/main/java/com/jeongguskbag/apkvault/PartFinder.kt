package com.jeongguskbag.apkvault

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * 합칠 수 있는 조각 파일(.001)을 찾는다. 안드로이드 파일 선택 화면은 이름으로 거를 수 없어서
 * (모든 APK 가 다 보임) 앱이 직접 찾아 .001 만 목록으로 보여 준다.
 * - ApkVault 가 다운로드 폴더에 저장한 조각: 권한 없이 보임 (MediaStore, 안드로이드 10 이상)
 * - 다른 앱으로 받은 조각: 사용자가 "폴더 추가" 로 한 번 허용한 폴더 (허용은 앱이 기억)
 * 백그라운드 스레드에서 호출
 */
object PartFinder {

    /** 찾은 묶음과 위치(폴더 경로, ApkVault 저장 조각이면 null) */
    class Found(val group: PartFiles.Group<Uri>, val folder: String?)

    fun scan(context: Context): List<Found> {
        val out = ArrayList<Found>()
        ownDownloads(context).let { items -> PartFiles.groups(items).forEach { out.add(Found(it, null)) } }
        for (tree in allowedFolders(context)) {
            val items = listFolder(context, tree)
            PartFiles.groups(items).forEach { out.add(Found(it, folderLabel(tree))) }
        }
        // 같은 조각이 ApkVault 저장 목록과 허용 폴더 양쪽에 보이면 한 번만 (허용 폴더 쪽을 남김)
        val inFolders = out.filter { it.folder != null }.map { it.group.firstName }.toSet()
        return out.filter { it.folder != null || it.group.firstName !in inFolders }
    }

    /** 사용자가 허용한 폴더들 */
    fun allowedFolders(context: Context): List<Uri> =
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
            .map { it.uri }

    /** "primary:Download/KakaoTalk" → "Download/KakaoTalk" */
    private fun folderLabel(tree: Uri): String {
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return tree.toString()
        return id.substringAfter(':').ifEmpty { id }
    }

    /** 허용받은 폴더 바로 아래 파일들 (주소, 이름) */
    private fun listFolder(context: Context, tree: Uri): List<Pair<Uri, String>> = try {
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
        val items = ArrayList<Pair<Uri, String>>()
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                if (PartFiles.parse(name) != null) items.add(DocumentsContract.buildDocumentUriUsingTree(tree, id) to name)
            }
        }
        items
    } catch (e: Exception) {
        emptyList() // 폴더가 지워졌거나 허용이 풀림
    }

    /** ApkVault 가 다운로드 폴더에 저장한 조각 파일들 (안드로이드 10 이상, 권한 없이 자기 파일만 보임) */
    private fun ownDownloads(context: Context): List<Pair<Uri, String>> {
        if (Build.VERSION.SDK_INT < 29) return emptyList()
        return try {
            val items = ArrayList<Pair<Uri, String>>()
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
                arrayOf("%.___"), // 끝이 .세 글자 → 아래에서 숫자 세 자리인지 다시 확인
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    if (PartFiles.parse(name) == null) continue
                    items.add(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0)) to name)
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }
}
