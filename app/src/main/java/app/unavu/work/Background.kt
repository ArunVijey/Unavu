package app.unavu.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.unavu.MainActivity
import app.unavu.R
import app.unavu.data.Backup
import app.unavu.data.Meal
import app.unavu.data.Reminder
import app.unavu.data.Store
import app.unavu.data.entriesOn
import app.unavu.data.liquidsMl
import app.unavu.data.today
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// ---------------- daily backup ----------------

class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val d = Store.get(applicationContext).data.value
        if (d.backup.folderUri == null) return Result.success()
        val r = Backup.run(applicationContext)
        if (r.isSuccess) return Result.success()
        // IO errors (storage busy, card not ready) are worth retrying a few times; a lost folder permission is not
        val io = r.exceptionOrNull()?.cause is java.io.IOException
        return if (io && runAttemptCount < 3) Result.retry() else Result.success()
    }
}

object Scheduler {
    private const val BACKUP_WORK = "daily-backup"

    fun scheduleBackup(context: Context) {
        val d = Store.get(context).data.value
        val wm = WorkManager.getInstance(context)
        if (!d.backup.daily || d.backup.folderUri == null) {
            wm.cancelUniqueWork(BACKUP_WORK); return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(d.backup.hour, d.backup.minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = java.time.Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(BACKUP_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    // ---------------- reminders (inexact alarms, rescheduled after each one fires) ----------------

    const val CHANNEL = "reminders"
    private const val EXTRA_ID = "reminder_id"

    fun scheduleReminders(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        Store.get(context).data.value.reminders.forEach { r ->
            val pi = pending(context, r.id)
            am.cancel(pi)
            if (r.on) try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(r), pi) } catch (_: SecurityException) { }
        }
    }

    private fun pending(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(
        context, id.hashCode(), Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Water repeats every 2 h from its start time until 9 PM; others once a day. */
    fun nextTrigger(r: Reminder, now: LocalDateTime = LocalDateTime.now()): Long {
        var t: LocalDateTime
        if (r.id == "water") {
            val start = now.toLocalDate().atTime(r.hour, r.minute)
            t = start
            while (!t.isAfter(now) && t.toLocalTime() <= LocalTime.of(21, 0)) t = t.plusHours(2)
            if (!t.isAfter(now) || t.toLocalTime() > LocalTime.of(21, 0)) t = start.plusDays(1)
        } else {
            t = now.toLocalDate().atTime(r.hour, r.minute)
            if (!t.isAfter(now)) t = t.plusDays(1)
        }
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.a_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.a_channel_desc)
            },
        )
    }

    fun handle(context: Context, id: String) {
        val store = Store.get(context)
        val d = store.data.value
        val r = d.reminders.firstOrNull { it.id == id } ?: return
        if (r.on) {
            val day = today()
            val meal = Meal.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
            val skip = when {
                meal != null -> d.entriesOn(day).any { it.meal == meal }     // only if not logged
                id == "water" -> d.liquidsMl(day).first >= d.settings.waterGoalMl
                id == "weigh" -> d.weights.any { it.date == day }
                else -> false
            }
            if (!skip) notify(context, id, title(context, id), body(context, id, meal))
        }
        scheduleReminders(context)
    }

    private fun title(c: Context, id: String) = when (id) {
        "water" -> c.getString(R.string.a_notif_water_title)
        "weigh" -> c.getString(R.string.a_notif_weigh_title)
        else -> Meal.entries.first { it.name.equals(id, true) }.let { c.getString(R.string.a_notif_meal_title, it.emoji, it.label) }
    }

    private fun body(c: Context, id: String, meal: Meal?) = when {
        id == "water" -> c.getString(R.string.a_notif_water_body)
        id == "weigh" -> c.getString(R.string.a_notif_weigh_body)
        else -> c.getString(R.string.a_notif_meal_body, meal!!.label.lowercase())
    }

    private fun notify(context: Context, id: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, id.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTION, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_unavu)
            .setContentTitle(title).setContentText(body)
            .setContentIntent(open).setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(id.hashCode(), n) } catch (_: SecurityException) { }
    }

    fun extraId() = EXTRA_ID
}

/** Runs [block] on a background thread while keeping the receiver alive (Store loads off the main thread). */
private fun BroadcastReceiver.async(block: () -> Unit) {
    val pending = goAsync()
    Thread {
        try { block() } catch (t: Throwable) { android.util.Log.e("UnavuWork", "receiver failed", t) } finally { pending.finish() }
    }.start()
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Scheduler.extraId()) ?: return
        val app = context.applicationContext
        async { Scheduler.handle(app, id) }
    }
}

/** Boot, app update, clock change and time zone change: alarms are lost or wrong, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        async {
            Scheduler.scheduleReminders(app)
            Scheduler.scheduleBackup(app)
        }
    }
}
