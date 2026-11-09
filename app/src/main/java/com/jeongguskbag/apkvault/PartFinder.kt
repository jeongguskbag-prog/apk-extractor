package com.jeongguskbag.apkvault

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns

/**
 * 조각 하나만 골랐을 때 같은 파일의 나머지 조각(.001, .002 …)을 찾는다.
 * 안드로이드는 파일 하나를 고르면 그 파일만 읽게 해 주므로, 다음 순서로 찾는다:
 *  1. 이미 접근을 허용받은 폴더 중 고른 파일이 들어 있는 폴더
 *  2. ApkVault 가 직접 다운로드 폴더에 저장한 조각 (MediaStore, 안드로이드 10 이상)
 *  3. 이미 허용받은 다른 폴더 (이름이 같은 조각)
 *  4. 없으면 그 폴더 접근을 한 번 허용해 달라고 함 (NeedFolder)
 * 백그라운드 스레드에서 호출
 */
object PartFinder {

    sealed class Result {
        class Found(val uris: List<Uri>) : Result()
        class NeedFolder(val base: String, val initialFolder: Uri?) : Result()
    }

    private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    fun find(context: Context, picked: Uri): Result {
        val name = displayName(context, picked) ?: picked.lastPathSegment.orEmpty()
        val base = PartFiles.parse(name)?.first ?: throw PartFiles.Problem(PartFiles.Code.NOT_PART, name)
        val pickedDocId = runCatching { DocumentsContract.getDocumentId(picked) }.getOrNull()
        val trees = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }
            .map { it.uri }

        // 1. 고른 파일이 들어 있는 허용 폴더
        if (pickedDocId != null && picked.authority == EXTERNAL_STORAGE) {
            for (tree in trees.filter { it.authority == EXTERNAL_STORAGE }) {
                val treeId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
                if (pickedDocId.substringBeforeLast('/') == treeId) {
                    findInTree(context, tree, base)?.let { return Result.Found(it) }
                }
            }
        }
        // 2. ApkVault 가 저장한 조각
        findInOwnDownloads(context, base)?.let { return Result.Found(it) }
        // 3. 다른 허용 폴더
        for (tree in trees) findInTree(context, tree, base)?.let { return Result.Found(it) }
        // 4. 폴더 허용 요청 — 가능하면 고른 파일이 있는 폴더를 처음에 열어 줌
        return Result.NeedFolder(base, parentFolder(picked, pickedDocId))
    }

    /** 허용받은 폴더(tree) 바로 아래에서 base 의 조각을 찾음. 001 부터 빠짐없이 있어야 함 */
    fun findInTree(context: Context, tree: Uri, base: String): List<Uri>? = try {
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
                items.add(DocumentsContract.buildDocumentUriUsingTree(tree, id) to name)
            }
        }
        PartFiles.siblings(base, items)?.map { it.first }
    } catch (e: Exception) {
        null // 폴더가 지워졌거나 허용이 풀림
    }

    /** ApkVault 가 다운로드 폴더에 저장한 파일 중 base 의 조각 (안드로이드 10 이상, 권한 없이 자기 파일만 보임) */
    private fun findInOwnDownloads(context: Context, base: String): List<Uri>? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            val items = ArrayList<Pair<Uri, String>>()
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
                arrayOf("$base.%"), // 밑줄·% 가 섞여도 아래 siblings() 가 이름을 정확히 다시 비교함
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
                    items.add(uri to (c.getString(1) ?: continue))
                }
            }
            PartFiles.siblings(base, items)?.map { it.first }
        } catch (e: Exception) {
            null
        }
    }

    /** 내장 저장소 파일이면 그 파일이 든 폴더의 문서 주소 (폴더 고르기 화면을 거기서 시작) */
    private fun parentFolder(picked: Uri, docId: String?): Uri? {
        if (picked.authority != EXTERNAL_STORAGE || docId == null || '/' !in docId) return null
        return runCatching { DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, docId.substringBeforeLast('/')) }.getOrNull()
    }
}
