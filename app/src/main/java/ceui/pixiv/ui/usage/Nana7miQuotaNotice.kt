package ceui.pixiv.ui.usage

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ceui.lisa.R
import ceui.pixiv.shaftapi.Nana7miResult
import androidx.appcompat.app.AlertDialog

internal object Nana7miQuotaNotice {
    private val pendingNotice = MutableLiveData<Boolean>()
    val pending: LiveData<Boolean> = pendingNotice
    private val announcedBuckets = mutableMapOf<Pair<Long, String>, Long?>()

    @Synchronized
    fun report(uid: Long, result: Nana7miResult.RateLimited) {
        val scope = result.scope ?: return
        if (!result.isQuota) return
        val bucket = uid to scope
        if (bucket in announcedBuckets && announcedBuckets[bucket] == result.resetsAt) return
        announcedBuckets[bucket] = result.resetsAt
        pendingNotice.postValue(true)
    }

    fun consume() {
        pendingNotice.value = false
    }
}

/**
 * 「还有多久」的人话，和用量页共用同一批字符串，免得两处对同一段时间说法不一样。
 */
object Nana7miQuotaFormat {

    fun duration(context: Context, ms: Long): String {
        val totalMinutes = (ms + 59_999L) / 60_000L // 向上取整：别显示「0 分后恢复」
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0L -> context.getString(R.string.nana7mi_usage_dur_dh, days.toInt(), hours.toInt())
            hours > 0L -> context.getString(R.string.nana7mi_usage_dur_hm, hours.toInt(), minutes.toInt())
            else -> context.getString(R.string.nana7mi_usage_dur_m, minutes.toInt())
        }
    }
}

fun Fragment.observeNana7miQuotaNotice() {
    Nana7miQuotaNotice.pending.observe(viewLifecycleOwner) { pending ->
        if (pending != true) return@observe
        Nana7miQuotaNotice.consume()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.nana7mi_quota_dialog_title)
            .setMessage(R.string.nana7mi_quota_dialog_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
