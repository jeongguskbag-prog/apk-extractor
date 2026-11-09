package com.jeongguskbag.apkvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeongguskbag.apkvault.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private enum class Sort { NAME, UPDATED, SIZE }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppAdapter

    private var allApps: List<AppEntry> = emptyList()
    private var query = ""
    private var showSystem = false

    // "큰 파일 30MB씩 나눠 저장" 옵션 (앱을 다시 켜도 유지)
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var splitLarge: Boolean
        get() = prefs.getBoolean("split_large", false)
        set(value) { prefs.edit().putBoolean("split_large", value).apply() }
    private var sort = Sort.NAME
    private var busy = false
    private var pendingSave: List<AppEntry> = emptyList()

    /** 여러 앱 선택 모드에서 고른 앱 (패키지 이름, 고른 순서 유지) */
    private val selected = LinkedHashSet<String>()
    private val selecting get() = selected.isNotEmpty()

    // 선택 모드에서 뒤로가기 → 선택 해제
    private val backToList = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = clearSelection()
    }

    // 안드로이드 9 이하에서만 쓰는 저장소 권한 요청
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val apps = pendingSave
        val parts = pendingJoin
        pendingSave = emptyList()
        pendingJoin = emptyList()
        if (apps.isEmpty() && parts.isEmpty()) return@registerForActivityResult
        if (!granted) { snack(getString(R.string.storage_permission_needed)); return@registerForActivityResult }
        if (apps.isNotEmpty()) save(apps)
        if (parts.isNotEmpty()) join(parts)
    }

    // 조각 합치기: 합칠 조각(.001) 목록은 앱이 직접 찾아 보여 줌
    private var pendingJoin: List<Uri> = emptyList()

    // 조각이 있는 폴더를 고름 → 그 폴더의 .001 만 목록으로
    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree == null) return@registerForActivityResult
        rememberFolder(tree)
        showParts(tree)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { clearSelection() }
        onBackPressedDispatcher.addCallback(this, backToList)

        adapter = AppAdapter(
            packageManager,
            lifecycleScope,
            onClick = { app -> if (selecting) toggle(app) else showActions(app) },
            onLongClick = { app -> toggle(app) },
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        loadApps()
    }

    override fun onRestart() {
        super.onRestart()
        loadApps() // 앱을 설치·삭제하고 돌아왔을 수 있음
    }

    private fun loadApps() {
        if (allApps.isEmpty()) {
            binding.progress.visibility = View.VISIBLE
            binding.emptyText.text = getString(R.string.loading)
            binding.emptyText.visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { AppRepository.load(applicationContext) }
            allApps = apps
            if (!busy) binding.progress.visibility = View.GONE
            // 지워진 앱은 선택에서도 뺌
            val installed = apps.mapTo(HashSet()) { it.packageName }
            if (selected.retainAll(installed)) onSelectionChanged()
            applyFilter()
        }
    }

    private fun applyFilter() {
        val q = query.trim().lowercase()
        var list = allApps.filter { showSystem || !it.isSystem }
        if (q.isNotEmpty()) {
            list = list.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
        list = when (sort) {
            Sort.NAME -> list.sortedBy { it.label.lowercase() }
            Sort.UPDATED -> list.sortedByDescending { it.lastUpdate }
            Sort.SIZE -> list.sortedByDescending { it.totalSize }
        }
        adapter.submitList(list)
        binding.emptyText.text = getString(R.string.empty)
        binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- 여러 앱 선택 ----------

    private fun toggle(app: AppEntry) {
        if (busy) return
        if (!selected.remove(app.packageName)) selected.add(app.packageName)
        onSelectionChanged()
    }

    private fun selectAllVisible() {
        adapter.currentList.forEach { selected.add(it.packageName) }
        onSelectionChanged()
    }

    private fun clearSelection() {
        if (!selecting) return
        selected.clear()
        onSelectionChanged()
    }

    private fun onSelectionChanged() {
        adapter.selected = selected.toSet()
        backToList.isEnabled = selecting
        if (selecting) {
            binding.toolbar.title = getString(R.string.selection_count, selected.size)
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
            binding.toolbar.navigationContentDescription = getString(R.string.action_clear_selection)
        } else {
            binding.toolbar.title = getString(R.string.app_name)
            binding.toolbar.navigationIcon = null
        }
        invalidateOptionsMenu()
    }

    /** 선택한 앱들 (목록에 보이는 순서, 검색으로 가려진 선택 앱은 뒤에) */
    private fun selectedApps(): List<AppEntry> {
        val visible = adapter.currentList.filter { it.packageName in selected }
        val visibleNames = visible.mapTo(HashSet()) { it.packageName }
        return visible + allApps.filter { it.packageName in selected && it.packageName !in visibleNames }
    }

    // ---------- 메뉴 ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (selecting) {
            menuInflater.inflate(R.menu.selection, menu)
            return true
        }
        menuInflater.inflate(R.menu.main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val search = searchItem.actionView as SearchView
        search.queryHint = getString(R.string.search_hint)
        if (query.isNotEmpty()) {
            // 선택 모드에서 돌아와 메뉴를 다시 만들 때 검색어 유지
            searchItem.expandActionView()
            search.setQuery(query, false)
        }
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean = true
            override fun onQueryTextChange(text: String?): Boolean {
                query = text.orEmpty()
                applyFilter()
                return true
            }
        })
        menu.findItem(R.id.action_show_system).isChecked = showSystem
        menu.findItem(R.id.action_split_large).isChecked = splitLarge
        menu.findItem(
            when (sort) {
                Sort.NAME -> R.id.sort_name
                Sort.UPDATED -> R.id.sort_updated
                Sort.SIZE -> R.id.sort_size
            }
        ).isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sel_save -> requestSave(selectedApps())
            R.id.sel_share -> share(selectedApps())
            R.id.sel_all -> selectAllVisible()
            R.id.sel_clear -> clearSelection()
            R.id.action_join -> {
                if (!busy) {
                    val last = prefs.getString("join_folder", null)?.let(Uri::parse)
                    snack(getString(R.string.join_pick_folder))
                    pickFolder.launch(last?.let(PartFinder::startOf) ?: PartFinder.DEFAULT_FOLDER)
                }
            }
            R.id.action_split_large -> {
                splitLarge = !item.isChecked
                item.isChecked = splitLarge
                snack(getString(if (splitLarge) R.string.split_on else R.string.split_off))
            }
            R.id.action_show_system -> {
                showSystem = !item.isChecked
                item.isChecked = showSystem
                applyFilter()
            }
            R.id.sort_name -> { sort = Sort.NAME; item.isChecked = true; applyFilter() }
            R.id.sort_updated -> { sort = Sort.UPDATED; item.isChecked = true; applyFilter() }
            R.id.sort_size -> { sort = Sort.SIZE; item.isChecked = true; applyFilter() }
            R.id.action_about -> MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.about_text) + "\n\nv" + BuildConfigInfo.versionName(this))
                .setPositiveButton(R.string.ok, null)
                .show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ---------- 한 앱 동작 ----------

    private fun showActions(app: AppEntry) {
        if (busy) return
        val items = arrayOf(getString(R.string.action_save), getString(R.string.action_share), getString(R.string.action_app_info))
        MaterialAlertDialogBuilder(this)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> requestSave(listOf(app))
                    1 -> share(listOf(app))
                    2 -> openAppInfo(app)
                }
            }
            .show()
    }

    private fun openAppInfo(app: AppEntry) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName)))
    }

    // ---------- 저장 · 공유 (한 개든 여러 개든 같은 경로) ----------

    private fun requestSave(apps: List<AppEntry>) {
        if (apps.isEmpty()) return
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSave = apps
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        save(apps)
    }

    /** 하나씩 저장. 하나가 실패해도 나머지는 계속하고 마지막에 결과를 알림 */
    private fun save(apps: List<AppEntry>) = runBusy(apps) {
        var saved = 0
        var last: Extractor.Saved? = null
        var splitCount = 0
        var lastError = ""
        val failed = ArrayList<String>()
        val split = splitLarge
        apps.forEachIndexed { i, app ->
            showProgress(i, apps.size)
            try {
                val result = withContext(Dispatchers.IO) { Extractor.saveToDownloads(applicationContext, app, split) }
                last = result
                if (result.parts > 1) splitCount++
                saved++
            } catch (e: Exception) {
                failed.add(app.label)
                lastError = e.message ?: e.javaClass.simpleName
            }
        }
        val folder = "${Environment.DIRECTORY_DOWNLOADS}/${Extractor.FOLDER}"
        snack(
            when {
                apps.size == 1 && saved == 1 && last!!.parts > 1 ->
                    getString(R.string.saved_split, last!!.names.first(), last!!.parts, folder)
                apps.size == 1 && saved == 1 -> getString(R.string.saved_to, last!!.firstPath)
                failed.isEmpty() && splitCount > 0 ->
                    getString(R.string.batch_saved, saved, folder) + " · " + getString(R.string.batch_split_note, splitCount)
                failed.isEmpty() -> getString(R.string.batch_saved, saved, folder)
                apps.size == 1 -> getString(R.string.extract_failed, lastError)
                else -> getString(R.string.batch_partial, saved, failed.size, failed.joinToString(", "))
            }
        )
        if (failed.isEmpty()) clearSelection()
    }

    private fun share(apps: List<AppEntry>) = runBusy(apps) {
        if (apps.isEmpty()) return@runBusy
        val intent = try {
            withContext(Dispatchers.IO) {
                Extractor.prepareShare(applicationContext, apps, splitLarge) { done -> runOnUiThread { showProgress(done, apps.size) } }
            }
        } catch (e: Exception) {
            snack(getString(R.string.share_failed, e.message ?: e.javaClass.simpleName))
            return@runBusy
        }
        startActivity(intent)
        clearSelection()
    }

    // ---------- 조각 합치기 ----------

    private fun needsStoragePermission() = Build.VERSION.SDK_INT < 29 &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    private fun requestJoin(uris: List<Uri>) {
        if (needsStoragePermission()) {
            pendingJoin = uris
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        join(uris)
    }

    /** 고른 폴더를 다음에 다시 열 수 있게 기억 (예전에 허용받은 다른 폴더는 놓아 줌) */
    private fun rememberFolder(tree: Uri) {
        runCatching { contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        for (old in contentResolver.persistedUriPermissions) {
            if (old.uri != tree) runCatching { contentResolver.releasePersistableUriPermission(old.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        prefs.edit().putString("join_folder", tree.toString()).apply()
    }

    /** 고른 폴더의 조각 묶음 중 첫 조각(.001)만 목록으로 보여 줌. 고르면 나머지까지 합침 */
    private fun showParts(tree: Uri) {
        if (busy) return
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val groups = try {
                withContext(Dispatchers.IO) { PartFinder.scan(applicationContext, tree) }
            } finally {
                if (!busy) binding.progress.visibility = View.GONE
            }
            if (isFinishing || isDestroyed) return@launch
            if (groups.isEmpty()) {
                snack(getString(R.string.join_none_in_folder, PartFinder.folderLabel(tree)))
                return@launch
            }
            val labels = groups.map { g ->
                if (g.complete) getString(R.string.join_item, g.firstName, g.parts.size)
                else getString(R.string.join_item_incomplete, g.firstName)
            }.toTypedArray()
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.join_title, PartFinder.folderLabel(tree)))
                .setItems(labels) { _, i ->
                    val g = groups[i]
                    if (g.complete) requestJoin(g.parts)
                    else snack(getString(R.string.join_err_missing, g.missing.joinToString(", ") { String.format(java.util.Locale.ROOT, "%03d", it) }))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun join(uris: List<Uri>) = runTask(getString(R.string.joining)) {
        val result = try {
            withContext(Dispatchers.IO) { Extractor.join(applicationContext, uris) }
        } catch (p: PartFiles.Problem) {
            snack(problemText(p))
            return@runTask
        }
        val folder = "${Environment.DIRECTORY_DOWNLOADS}/${Extractor.FOLDER}"
        snack(
            when (result.check) {
                PartFiles.ZipCheck.CORRUPT -> getString(R.string.join_done_corrupt, result.name)
                PartFiles.ZipCheck.UNKNOWN -> getString(R.string.join_done_unverified, result.name, result.parts, folder)
                else -> getString(R.string.join_done, result.name, result.parts, folder)
            }
        )
    }

    private fun problemText(p: PartFiles.Problem): String = when (p.code) {
        PartFiles.Code.NOT_PART -> getString(R.string.join_err_not_part, p.detail)
        PartFiles.Code.MIXED -> getString(R.string.join_err_mixed, p.detail)
        PartFiles.Code.DUPLICATE -> getString(R.string.join_err_duplicate, p.detail)
        PartFiles.Code.MISSING -> getString(R.string.join_err_missing, p.detail)
        PartFiles.Code.INCOMPLETE -> getString(R.string.join_err_incomplete)
    }

    private fun showProgress(done: Int, total: Int) {
        binding.toolbar.subtitle = if (total > 1) getString(R.string.batch_progress, minOf(done + 1, total), total) else null
    }

    /** 한 번에 하나의 작업만 실행하고, 진행 표시와 예상치 못한 오류 안내를 처리 */
    private fun runBusy(apps: List<AppEntry>, block: suspend () -> Unit) {
        if (apps.isEmpty()) return
        runTask(if (apps.size == 1) getString(R.string.extracting, apps[0].label) else null, block)
    }

    /** 한 번에 하나의 작업만 실행하고, 진행 표시와 예상치 못한 오류 안내를 처리 */
    private fun runTask(startMessage: String?, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        binding.progress.visibility = View.VISIBLE
        if (startMessage != null) snack(startMessage)
        lifecycleScope.launch {
            try {
                block()
            } catch (e: Exception) {
                snack(getString(R.string.extract_failed, e.message ?: e.javaClass.simpleName))
            } finally {
                busy = false
                binding.progress.visibility = View.GONE
                binding.toolbar.subtitle = null
            }
        }
    }

    private fun snack(text: String) {
        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
    }
}

/** BuildConfig 생성을 켜지 않고 버전 이름을 읽음 */
object BuildConfigInfo {
    fun versionName(activity: AppCompatActivity): String = try {
        val pm = activity.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(activity.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(activity.packageName, 0)
        }
        info.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}
