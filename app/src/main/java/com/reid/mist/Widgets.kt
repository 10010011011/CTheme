package com.reid.mist

class FrClock : BaseW("fr_clock")
class FrCal : BaseW("fr_cal")
class FrPhotoA : BaseW("fr_photo_a")
class FrPhotoB : BaseW("fr_photo_b")
class FrQuote : BaseW("fr_quote")
class FrWeather : BaseW("fr_weather")
class FrTasks : BaseW("fr_tasks")
class FrMusic : BaseW("fr_music")
class HmOrbit : BaseW("hm_orbit")
class HmLink : BaseW("hm_link")
class HmCal : BaseW("hm_cal")
class HmStatus : BaseW("hm_status")
class HmWeather : BaseW("hm_weather")
class HmTasks : BaseW("hm_tasks")
class HmMusic : BaseW("hm_music")
class HmEarth : BaseW("hm_earth")
class TvHeader : BaseW("tv_header")
class TvClock : BaseW("tv_clock")
class TvWeather : BaseW("tv_weather")
class TvTimeline : BaseW("tv_timeline")
class TvLog : BaseW("tv_log")
class TvMiss : BaseW("tv_miss")
class TvMusic : BaseW("tv_music")
class TvQuote : BaseW("tv_quote")
class VcClock : BaseW("vc_clock")
class VcMusic : BaseW("vc_music")
class VcTasks : BaseW("vc_tasks")
class VcRing : BaseW("vc_ring")
class VcBanner : BaseW("vc_banner")

val CLS: Map<String, Class<*>> = mapOf(
    "fr_clock" to FrClock::class.java,
    "fr_cal" to FrCal::class.java,
    "fr_photo_a" to FrPhotoA::class.java,
    "fr_photo_b" to FrPhotoB::class.java,
    "fr_quote" to FrQuote::class.java,
    "fr_weather" to FrWeather::class.java,
    "fr_tasks" to FrTasks::class.java,
    "fr_music" to FrMusic::class.java,
    "hm_orbit" to HmOrbit::class.java,
    "hm_link" to HmLink::class.java,
    "hm_cal" to HmCal::class.java,
    "hm_status" to HmStatus::class.java,
    "hm_weather" to HmWeather::class.java,
    "hm_tasks" to HmTasks::class.java,
    "hm_music" to HmMusic::class.java,
    "hm_earth" to HmEarth::class.java,
    "tv_header" to TvHeader::class.java,
    "tv_clock" to TvClock::class.java,
    "tv_weather" to TvWeather::class.java,
    "tv_timeline" to TvTimeline::class.java,
    "tv_log" to TvLog::class.java,
    "tv_miss" to TvMiss::class.java,
    "tv_music" to TvMusic::class.java,
    "tv_quote" to TvQuote::class.java,
    "vc_clock" to VcClock::class.java,
    "vc_music" to VcMusic::class.java,
    "vc_tasks" to VcTasks::class.java,
    "vc_ring" to VcRing::class.java,
    "vc_banner" to VcBanner::class.java
)

val LABELS: Map<String, String> = mapOf(
    "fr_clock" to "Frieren - Clock",
    "fr_cal" to "Frieren - Calendar",
    "fr_photo_a" to "Frieren - Image A",
    "fr_photo_b" to "Frieren - Image B",
    "fr_quote" to "Frieren - Quote",
    "fr_weather" to "Frieren - Weather",
    "fr_tasks" to "Frieren - Tasks",
    "fr_music" to "Frieren - Music",
    "hm_orbit" to "Hail Mary - Orbit and clock",
    "hm_link" to "Hail Mary - Link bar",
    "hm_cal" to "Hail Mary - Calendar",
    "hm_status" to "Hail Mary - Status",
    "hm_weather" to "Hail Mary - Weather",
    "hm_tasks" to "Hail Mary - Tasks",
    "hm_music" to "Hail Mary - Music",
    "hm_earth" to "Hail Mary - Earth art",
    "tv_header" to "TVA - Terminal",
    "tv_clock" to "TVA - Clock",
    "tv_weather" to "TVA - Weather",
    "tv_timeline" to "TVA - Timeline",
    "tv_log" to "TVA - Variance log",
    "tv_miss" to "TVA - Miss Minutes",
    "tv_music" to "TVA - Music",
    "tv_quote" to "TVA - Quote",
    "vc_clock" to "Vocaloid - Vinyl clock",
    "vc_music" to "Vocaloid - Music",
    "vc_tasks" to "Vocaloid - Tasks",
    "vc_ring" to "Vocaloid - Progress ring",
    "vc_banner" to "Vocaloid - Quote banner"
)
