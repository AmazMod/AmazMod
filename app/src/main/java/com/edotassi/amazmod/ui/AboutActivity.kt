package com.edotassi.amazmod.ui

import amazmod.com.models.Reply
import amazmod.com.transport.Constants
import amazmod.com.transport.Transport
import amazmod.com.transport.data.NotificationData
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.view.LayoutInflaterCompat
import butterknife.OnLongClick
import com.edotassi.amazmod.BuildConfig
import com.edotassi.amazmod.R
import com.edotassi.amazmod.notification.NotificationService
import com.edotassi.amazmod.transport.TransportService
import com.edotassi.amazmod.transport.TransportService.DataTransportResultCallback
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huami.watch.notification.data.StatusBarNotificationData
import com.huami.watch.transport.DataBundle
import com.huami.watch.transport.DataTransportResult
import com.mikepenz.iconics.context.IconicsLayoutInflater2
import com.pixplicity.easyprefs.library.Prefs
import de.mateware.snacky.Snacky
import kotlinx.android.synthetic.main.activity_about.*
import org.tinylog.kotlin.Logger
import java.util.*

class AboutActivity : BaseAppCompatActivity(), DataTransportResultCallback {

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LayoutInflaterCompat.setFactory2(layoutInflater, IconicsLayoutInflater2(delegate))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle(R.string.about)
        } catch (exception: NullPointerException) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.message)
        }
        activity_about_version.text = BuildConfig.VERSION_NAME
        activity_about_version.append(" (Build ${BuildConfig.VERSION_CODE} )")
        if (Prefs.getBoolean(Constants.PREF_ENABLE_DEVELOPER_MODE, false)) {
            activity_about_version.append(" - " + BuildConfig.VERSION_CODE + ":dev")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_about, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.action_activity_about_custom_ui_test -> sendTestMessage('C')
            R.id.action_activity_about_standard_test -> sendTestMessage('S')
            R.id.action_activity_about_notification_test -> sendTestMessage('N')
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            println("D/AmazMod AboutActivity ORIENTATION PORTRAIT")
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            println("D/AmazMod AboutActivity ORIENTATION LANDSCAPE")
        }
    }

    private fun sendTestMessage(type: Char) {
        val notificationData = NotificationData()
        notificationData.text = "Test Notification"
        Snacky.builder()
                .setActivity(this@AboutActivity)
                .setText(R.string.sending)
                .setDuration(Snacky.LENGTH_INDEFINITE)
                .build().show()
        when (type) {
            'C' -> notificationData.forceCustom = true
            'S' -> notificationData.forceCustom = false
            'N' ->
            {
                notificationData.forceCustom = false
                sendNotificationWithStandardUI(notificationData)
                return
            }
            else -> Logger.debug("AboutActivity sendTestMessage: something went wrong...")
        }
        notificationData.id = 999
        notificationData.key = "amazmod|test|999"
        notificationData.title = "AmazMod"
        notificationData.time = "00:00"
        notificationData.vibration = Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION))
        notificationData.hideReplies = true
        notificationData.hideButtons = false
        try {
            val drawable = resources.getDrawable(R.drawable.ic_launcher_foreground, theme)
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val width = bitmap.width
            val height = bitmap.height
            val intArray = IntArray(width * height)
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height)
            notificationData.icon = intArray
            notificationData.iconWidth = width
            notificationData.iconHeight = height
        } catch (e: Exception) {
            notificationData.icon = intArrayOf()
            Logger.error("AboutActivity notificationData Failed to get bitmap $e")
        }
        TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, null, notificationData.toDataBundle(), this)
    }

    private fun sendNotificationWithStandardUI(nd: NotificationData) {
        val dataBundle = DataBundle()
        val intent = Intent()
        val nextId = (System.currentTimeMillis() % 10000L).toInt()
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val pic = BitmapFactory.decodeResource(resources, R.drawable.art_canteen_intro1)
        val buttonIntent = Intent(baseContext, AboutActivity::class.java)
        buttonIntent.putExtra("notificationId", nextId)
        val dismissIntent = PendingIntent.getBroadcast(baseContext, 0, buttonIntent, 0)
        val action2 = NotificationCompat.Action.Builder(android.R.drawable.ic_delete, "Dismiss", dismissIntent)
                .build()
        val bundle = Bundle()
        bundle.putParcelable(NotificationCompat.EXTRA_LARGE_ICON_BIG, pic)
        bundle.putParcelable(NotificationCompat.EXTRA_LARGE_ICON, pic)
        val builder = NotificationCompat.Builder(this, "")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addExtras(bundle)
                .addAction(android.R.drawable.ic_delete, "DISMISS", dismissIntent)
                .setContentIntent(pendingIntent)
                .setContentText(nd.text)
                .setContentTitle("Test")
        val INTENT_ACTION_REPLY = "com.amazmod.action.reply"
        val EXTRA_REPLY = "extra.reply"
        val EXTRA_NOTIFICATION_KEY = "extra.notification.key"
        val EXTRA_NOTIFICATION_ID = "extra.notification.id"
        val repliesList = loadReplies()
        val wearableExtender = NotificationCompat.WearableExtender()
                .setContentIcon(R.drawable.ic_launcher_foreground)
                .setContentIntentAvailableOffline(true)
                .addAction(action2)
                .setBackground(pic)
        for (reply in repliesList) {
            intent.setPackage("com.amazmod.service")
            intent.action = INTENT_ACTION_REPLY
            intent.putExtra(EXTRA_REPLY, reply.value)
            intent.putExtra(EXTRA_NOTIFICATION_KEY, "0|com.edotassi.amazmod|" + (nextId + 1).toString() + "|tag|0")
            intent.putExtra(EXTRA_NOTIFICATION_ID, nextId + 1)
            val replyIntent = PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_ONE_SHOT)
            val replyAction = NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, reply.value, replyIntent).build()
            wearableExtender.addAction(replyAction)
        }
        builder.extend(wearableExtender)
        val notification = builder.build()
        val sbn = StatusBarNotification("com.edotassi.amazmod", "",
                nextId + 1, "tag", 0, 0, 0,
                notification, Process.myUserHandle(),
                System.currentTimeMillis())
        val sbnd = StatusBarNotificationData.from(this, sbn, false)
        dataBundle.putParcelable("data", sbnd)
        TransportService.sendWithTransporterHuami("add", null, dataBundle, this)
    }

    private fun loadReplies(): List<Reply> {
        val replies = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES, "[]")
        return try {
            val listType = object : TypeToken<List<Reply?>?>() {}.type
            Gson().fromJson(replies, listType)
        } catch (ex: Exception) {
            ArrayList()
        }
    }

    @OnLongClick(R.id.amazmod_logo)
    fun onAmazmodLogoLongClick(): Boolean {
        NotificationService.cancelPendingJobs()
        Toast.makeText(this, "All pending jobs cancelled!", Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onSuccess(dataTransportResult: DataTransportResult, key: String) {
        when (dataTransportResult.resultCode) {
            DataTransportResult.RESULT_FAILED_TRANSPORT_SERVICE_UNCONNECTED, DataTransportResult.RESULT_FAILED_CHANNEL_UNAVAILABLE, DataTransportResult.RESULT_FAILED_IWDS_CRASH, DataTransportResult.RESULT_FAILED_LINK_DISCONNECTED -> {
                Snacky.builder()
                        .setActivity(this@AboutActivity)
                        .setText(R.string.failed_to_send_test_notification)
                        .setDuration(Snacky.LENGTH_SHORT)
                        .build().show()
            }
            DataTransportResult.RESULT_OK -> {
                Snacky.builder()
                        .setActivity(this@AboutActivity)
                        .setText(R.string.test_notification_sent)
                        .setDuration(Snacky.LENGTH_SHORT)
                        .build().show()
            }
        }
    }

    override fun onFailure(error: String, key: String) {
        Snacky.builder()
                .setActivity(this@AboutActivity)
                .setText(error.toUpperCase(Locale.getDefault()))
                .setDuration(Snacky.LENGTH_SHORT)
                .build().show()
        Logger.error(error)
    }
}