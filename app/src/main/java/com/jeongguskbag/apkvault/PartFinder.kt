package com.jeongguskbag.apkvault

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * 사용자가 고른 폴더에서 합칠 수 있는 조각 묶음(.001 이 있는 것)을 찾는다.
 * 안드로이드 파일 선택 화면은 이름으로 거를 수 없어서(모든 파일이 다 보임)
 * 폴더만 고르게 하고, 그 폴더의 .001 목록은 앱이 직접 보여 준다.
 * 백그라운드 스레드에서 호출
 */
object PartFinder {

    /** 처음 폴더를 고를 때 열어 줄 곳: ApkVault 가 저장하는 Download/ApkExtractor */
    val DEFAULT_FOLDER: Uri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents", "primary:Download/${Extractor.FOLDER}",
    )

    /** 폴더(트리 주소) 바로 아래 조각 묶음들. 첫 조각(.001)이 있는 것만, 이름순 */
    fun scan(context: Context, tree: Uri): List<PartFiles.Group<Uri>> = PartFiles.groups(listFolder(context, tree))

    /** 폴더 선택 화면을 지난번 고른 폴더에서 열기 위한 주소 */
    fun startOf(tree: Uri): Uri? = runCatching {
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    }.getOrNull()

    /** "primary:Download/KakaoTalk" → "Download/KakaoTalk" */
    fun folderLabel(tree: Uri): String {
        val id = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return tree.toString()
        return id.substringAfter(':').ifEmpty { id }
    }

    /** 폴더 바로 아래 조각 이름 파일들 (주소, 이름) */
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
}
