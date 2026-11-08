package com.apkextractor.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apkextractor.app.databinding.ActivityMainBinding
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
    private var sort = Sort.NAME
    private var busy = false
    private var pendingSave: AppEntry? = null

    // 안드로이드 9 이하에서만 쓰는 저장소 권한 요청
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val app = pendingSave
        pendingSave = null
        if (granted && app != null) save(app) else if (app != null) snack(getString(R.string.storage_permission_needed))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = AppAdapter(packageManager, lifecycleScope) { app -> showActions(app) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val search = menu.findItem(R.id.action_search).actionView as SearchView
        search.queryHint = getString(R.string.search_hint)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean = true
            override fun onQueryTextChange(text: String?): Boolean {
                query = text.orEmpty()
                applyFilter()
                return true
            }
        })
        menu.findItem(R.id.action_show_system).isChecked = showSystem
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    private fun showActions(app: AppEntry) {
        if (busy) return
        val items = arrayOf(getString(R.string.action_save), getString(R.string.action_share), getString(R.string.action_app_info))
        MaterialAlertDialogBuilder(this)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> requestSave(app)
                    1 -> share(app)
                    2 -> openAppInfo(app)
                }
            }
            .show()
    }

    private fun requestSave(app: AppEntry) {
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSave = app
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        save(app)
    }

    private fun save(app: AppEntry) = runBusy(app) {
        val path = withContext(Dispatchers.IO) { Extractor.saveToDownloads(applicationContext, app) }
        snack(getString(R.string.saved_to, path))
    }

    private fun share(app: AppEntry) = runBusy(app) {
        val intent = try {
            withContext(Dispatchers.IO) { Extractor.prepareShare(applicationContext, app) }
        } catch (e: Exception) {
            snack(getString(R.string.share_failed, e.message ?: e.javaClass.simpleName))
            return@runBusy
        }
        startActivity(intent)
    }

    private fun openAppInfo(app: AppEntry) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName)))
    }

    /** 한 번에 하나의 추출만 실행하고, 진행 표시와 오류 안내를 처리 */
    private fun runBusy(app: AppEntry, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        binding.progress.visibility = View.VISIBLE
        snack(getString(R.string.extracting, app.label))
        lifecycleScope.launch {
            try {
                block()
            } catch (e: Exception) {
                snack(getString(R.string.extract_failed, e.message ?: e.javaClass.simpleName))
            } finally {
                busy = false
                binding.progress.visibility = View.GONE
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
