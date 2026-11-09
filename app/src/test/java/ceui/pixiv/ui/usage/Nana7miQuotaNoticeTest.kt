package ceui.pixiv.ui.usage

import android.app.Application
import android.os.Looper
import androidx.lifecycle.Observer
import ceui.pixiv.shaftapi.Nana7miResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class Nana7miQuotaNoticeTest {
    @Test
    fun quotaNoticeAppearsOncePerResetAndIgnoresRateLimits() {
        val uid = 100L
        val quota = Nana7miResult.RateLimited(null, "uid_5h", resetsAt = 1000L)
        Nana7miQuotaNotice.consume()
        var notices = 0
        val observer = Observer<Boolean> { if (it == true) notices++ }
        Nana7miQuotaNotice.pending.observeForever(observer)
        try {
            Nana7miQuotaNotice.report(uid, quota.copy(scope = "ip"))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0, notices)

            Nana7miQuotaNotice.report(uid, quota)
            shadowOf(Looper.getMainLooper()).idle()
            Nana7miQuotaNotice.consume()
            Nana7miQuotaNotice.report(uid, quota)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, notices)

            Nana7miQuotaNotice.report(uid, quota.copy(resetsAt = 2000L))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(2, notices)
        } finally {
            Nana7miQuotaNotice.pending.removeObserver(observer)
            Nana7miQuotaNotice.consume()
        }
    }
}
