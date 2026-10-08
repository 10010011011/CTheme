package com.reid.mist

object Reg {
    val m: Map<String, Def> = mapOf(
        "fr_clock" to Def(360, 110, ::frClock, ticks = true),
        "fr_cal" to Def(170, 190, ::frCal, tap = "cal"),
        "fr_photo_a" to Def(170, 190, ::frPhotoA),
        "fr_photo_b" to Def(170, 190, ::frPhotoB),
        "fr_quote" to Def(360, 120, ::frQuote),
        "fr_weather" to Def(360, 120, ::frWeather, ticks = true, tap = "wx"),
        "fr_tasks" to Def(170, 190, ::frTasks),
        "fr_music" to Def(360, 150, ::frMusic, tap = "mus"),
        "hm_orbit" to Def(360, 500, ::hmOrbit, ticks = true),
        "hm_link" to Def(360, 64, ::hmLink),
        "hm_cal" to Def(170, 140, ::hmCal, tap = "cal"),
        "hm_status" to Def(170, 140, ::hmStatus, ticks = true),
        "hm_weather" to Def(360, 300, ::hmWeather, ticks = true, tap = "wx"),
        "hm_tasks" to Def(360, 190, ::hmTasks),
        "hm_music" to Def(360, 100, ::hmMusic, tap = "mus"),
        "hm_earth" to Def(360, 170, ::hmEarth),
        "tv_header" to Def(360, 64, ::tvHeader),
        "tv_clock" to Def(170, 130, ::tvClock, ticks = true),
        "tv_weather" to Def(170, 130, ::tvWeather, ticks = true, tap = "wx"),
        "tv_timeline" to Def(360, 170, ::tvTimeline),
        "tv_log" to Def(230, 190, ::tvLog),
        "tv_miss" to Def(130, 170, ::tvMiss, tap = "ast"),
        "tv_music" to Def(360, 100, ::tvMusic, tap = "mus"),
        "tv_quote" to Def(360, 48, ::tvQuote),
        "vc_clock" to Def(360, 170, ::vcClock, ticks = true),
        "vc_music" to Def(360, 150, ::vcMusic, tap = "mus"),
        "vc_tasks" to Def(200, 170, ::vcTasks),
        "vc_ring" to Def(130, 150, ::vcRing),
        "vc_banner" to Def(360, 110, ::vcBanner)
    )
}
