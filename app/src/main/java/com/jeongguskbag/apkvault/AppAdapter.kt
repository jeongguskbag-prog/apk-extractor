package com.jeongguskbag.apkvault

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.format.Formatter
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jeongguskbag.apkvault.databinding.ItemAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppAdapter(
    private val pm: PackageManager,
    private val scope: CoroutineScope,
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit,
) : ListAdapter<AppEntry, AppAdapter.Holder>(Diff) {

    private val icons = LruCache<String, Drawable>(200)

    /** 선택된 앱 (패키지 이름). 비어 있지 않으면 선택 모드 */
    var selected: Set<String> = emptySet()
        set(value) {
            val wasSelecting = field.isNotEmpty()
            val old = field
            field = value
            if (wasSelecting != value.isNotEmpty()) {
                notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION) // 체크박스 표시·숨김
            } else {
                currentList.forEachIndexed { i, app ->
                    if ((app.packageName in old) != (app.packageName in value)) notifyItemChanged(i, PAYLOAD_SELECTION)
                }
            }
        }

    class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SELECTION }) {
            bindSelection(holder, getItem(position))
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private fun bindSelection(holder: Holder, app: AppEntry) {
        val b = holder.binding
        b.check.visibility = if (selected.isNotEmpty()) View.VISIBLE else View.GONE
        b.check.isChecked = app.packageName in selected
        b.root.isActivated = app.packageName in selected
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        b.label.text = app.label
        b.packageName.text = app.packageName
        val size = Formatter.formatShortFileSize(ctx, app.totalSize)
        val version = app.versionName.ifEmpty { app.versionCode.toString() }
        b.meta.text = if (app.isSplit) {
            ctx.getString(R.string.item_split, version, size, app.splitApks.size)
        } else {
            ctx.getString(R.string.item_meta, version, size)
        }
        b.root.setOnClickListener { onClick(app) }
        b.root.setOnLongClickListener { onLongClick(app); true }
        bindSelection(holder, app)

        // 아이콘은 느릴 수 있어 백그라운드에서 불러오고 캐시함 (스크롤 중 다른 앱 아이콘이 잘못 들어가지 않게 tag 로 확인)
        b.icon.tag = app.packageName
        val cached = icons.get(app.packageName)
        if (cached != null) {
            b.icon.setImageDrawable(cached)
            return
        }
        b.icon.setImageDrawable(null)
        scope.launch {
            val icon = withContext(Dispatchers.IO) {
                try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { pm.defaultActivityIcon }
            }
            icons.put(app.packageName, icon)
            if (b.icon.tag == app.packageName) b.icon.setImageDrawable(icon)
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }

    private object Diff : DiffUtil.ItemCallback<AppEntry>() {
        override fun areItemsTheSame(a: AppEntry, b: AppEntry) = a.packageName == b.packageName
        override fun areContentsTheSame(a: AppEntry, b: AppEntry) = a == b
    }
}
