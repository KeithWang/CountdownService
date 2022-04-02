package vic.sample.countdownservice

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import vic.sample.countdownservice.databinding.ActivityMainBinding
import vic.sample.countdownservice.service.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewInit()
        setViewListener()
    }

    private fun viewInit() {
        binding.countdownProgress.max = TARGET_COUNTDOWN_TIME.toInt()

        countdownTheTime(TimerEvent.END)

        TimerService.timerEvent.observe(this, {
            countdownTheTime(it)
        })

        TimerService.timerInMillis.observe(this, {
            binding.countdownProgress.progress = (it / 1000).toInt()
            binding.countdownTxtCount.text =
                ((TARGET_COUNTDOWN_TIME / 60) - ((it / 1000) / 60)).toString()
        })
    }

    private fun setViewListener() {
        binding.countdownBtnStart.setOnClickListener {
            startTimeService(ACTION_START_SERVICE)
        }

        binding.countdownBtnCancel.setOnClickListener {
            startTimeService(ACTION_STOP_SERVICE)
        }
    }

    /*
    * About Time Service
    * */
    private fun startTimeService(action: String) {
        startService(Intent(this, TimerService::class.java).apply {
            this.action = action
        })
    }

    /*
    * About The Countdown UI
    * */
    private fun countdownTheTime(event: TimerEvent) {
        if (event is TimerEvent.START) {
            binding.countdownBtnStart.visibility = View.INVISIBLE
            binding.countdownViewCountMask.visibility = View.INVISIBLE
            binding.countdownTxtCount.setTextColor(
                ContextCompat.getColor(baseContext, R.color.teal_200)
            )
            binding.countdownTxtCountMin.setTextColor(
                ContextCompat.getColor(baseContext, R.color.teal_200)
            )

        } else {
            binding.countdownBtnStart.visibility = View.VISIBLE
            binding.countdownViewCountMask.visibility = View.VISIBLE
            binding.countdownTxtCount.setTextColor(
                ContextCompat.getColor(baseContext, R.color.white)
            )
            binding.countdownTxtCountMin.setTextColor(
                ContextCompat.getColor(baseContext, R.color.white)
            )

            binding.countdownTxtCount.text = (TARGET_COUNTDOWN_TIME / 60).toString()

            binding.countdownProgress.progress = 0

            if (event is TimerEvent.FINISH) {
                TimerService.timerEvent.postValue(TimerEvent.END)
            }
        }
    }
}