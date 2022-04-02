package vic.sample.countdownservice.service

sealed class TimerEvent{
    object START : TimerEvent()
    object END : TimerEvent()
    object FINISH : TimerEvent()
}