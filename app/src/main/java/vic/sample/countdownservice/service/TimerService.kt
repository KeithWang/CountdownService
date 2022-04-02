package vic.sample.countdownservice.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vic.sample.countdownservice.MainActivity
import vic.sample.countdownservice.R
import vic.sample.countdownservice.service.*

class TimerService : LifecycleService() {
    companion object {
        val timerEvent = MutableLiveData<TimerEvent>()
        val timerInMillis = MutableLiveData<Long>()
    }

    private var isServiceStopped = false

    lateinit var notificationBuilder: NotificationCompat.Builder

    lateinit var notificationManager: NotificationManager

    //Timer properties
    private var lapTime = 0L
    private var timeStarted = 0L

    override fun onCreate() {
        super.onCreate()
        initValues()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    startForegroundService()
                }
                ACTION_STOP_SERVICE -> {
                    stopService(false)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initValues() {
        timerEvent.postValue(TimerEvent.END)
        timerInMillis.postValue(0L)

        val targetTimeStr = TimerUtil.getFormattedTime((TARGET_COUNTDOWN_TIME * 1000L), false)

        notificationBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle("Countdown Service")
                .setContentText(targetTimeStr)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        420,
                        Intent(applicationContext, MainActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )

        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    private fun startForegroundService() {
        timerEvent.postValue(TimerEvent.START)
        startTimer()

        if (SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        timerInMillis.observe(this, Observer {
            if (!isServiceStopped) {
                notificationBuilder.setContentText(
                    TimerUtil.getFormattedTime((TARGET_COUNTDOWN_TIME * 1000L - it), false)
                )
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
            }
        })
    }

    private fun stopService(countdownFinish: Boolean) {
        isServiceStopped = true
        if (countdownFinish)
            timerEvent.postValue(TimerEvent.FINISH)
        else
            initValues()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, IMPORTANCE_LOW
            )
        notificationManager.createNotificationChannel(channel)
    }

    private fun startTimer() {
        timeStarted = System.currentTimeMillis()
        lifecycleScope.launch(Main) {
            while (timerEvent.value!! == TimerEvent.START && !isServiceStopped) {
                lapTime = System.currentTimeMillis() - timeStarted
                timerInMillis.postValue(lapTime)
                delay(50L)
                if (lapTime / 1000L >= TARGET_COUNTDOWN_TIME) {
                    stopService(true)
                }
            }
        }
    }
}