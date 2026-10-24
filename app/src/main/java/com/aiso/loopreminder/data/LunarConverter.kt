package com.aiso.loopreminder.data

import java.time.LocalDate

/**
 * 公历 ↔ 农历换算（支持 1900-01-31 ~ 2100 年）。
 *
 * 数据表 LUNAR_INFO 为业界通用的 1900-2100 农历年信息表（已用 2018-2040 春节及多个闰月年交叉验证通过）。
 * 位布局（每个 int 对应一个农历年）：
 *   - bit 0..3  : 闰月月份（0 = 当年无闰月）
 *   - bit 16    : 闰月若为 30 天则置位（否则 29 天）
 *   - bit (16-m): m=1..12 月的大小月，置位 = 大月(30天)，否则小月(29天)
 * 农历正月初一 = 1900-01-31。
 */
object LunarConverter {

    // 1900..2100，共 201 项
    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    )

    private fun idx(y: Int) = (y - 1900).coerceIn(0, LUNAR_INFO.lastIndex)

    /** 闰月月份（1-12），无闰月返回 0。 */
    private fun leapMonth(y: Int): Int = LUNAR_INFO[idx(y)] and 0xf

    /** 闰月天数（无闰月返回 0）。 */
    private fun leapDays(y: Int): Int {
        if (leapMonth(y) == 0) return 0
        return if (LUNAR_INFO[idx(y)] and 0x10000 != 0) 30 else 29
    }

    /** 普通农历月（非闰月）天数。 */
    private fun monthDays(y: Int, m: Int): Int =
        if (LUNAR_INFO[idx(y)] and (0x10000 ushr m) != 0) 30 else 29

    private data class LunarMonth(val month: Int, val isLeap: Boolean, val days: Int)

    /** 按时间顺序返回某农历年的所有月（闰月紧跟在其正常月之后）。 */
    private fun yearMonths(y: Int): List<LunarMonth> {
        val leap = leapMonth(y)
        val out = mutableListOf<LunarMonth>()
        for (m in 1..12) {
            out.add(LunarMonth(m, false, monthDays(y, m)))
            if (leap == m) out.add(LunarMonth(m, true, leapDays(y)))
        }
        return out
    }

    private fun yearDays(y: Int) = yearMonths(y).sumOf { it.days }

    /** 公历日期对应的农历日期。 */
    fun toLunar(date: LocalDate): LunarDate {
        val base = LocalDate.of(1900, 1, 31)
        var offset = (date.toEpochDay() - base.toEpochDay()).toInt()
        var ly = 1900
        while (ly <= 2100) {
            val total = yearDays(ly)
            if (offset < total) break
            offset -= total
            ly++
        }
        for ((lm, isLeap, dm) in yearMonths(ly)) {
            if (offset < dm) return LunarDate(ly, lm, offset + 1, isLeap)
            offset -= dm
        }
        return LunarDate(ly, 12, 30, false)
    }

    private val LUNAR_MONTHS = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val LUNAR_DIGITS = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    /** 农历月简称（不含闰前缀），用于摘要/选择器，如「五月」。 */
    fun lunarMonthShort(month: Int): String = LUNAR_MONTHS[month.coerceIn(1, 12) - 1]

    /** 农历日名（给定 1-30 数字），如「初一」「十五」「廿三」「三十」；超出范围回退到「N日」。 */
    fun lunarDayText(day: Int): String = when (day) {
        1 -> "初一"; 2 -> "初二"; 3 -> "初三"; 4 -> "初四"; 5 -> "初五"
        6 -> "初六"; 7 -> "初七"; 8 -> "初八"; 9 -> "初九"; 10 -> "初十"
        in 11..19 -> "十" + LUNAR_DIGITS[day - 10]
        20 -> "二十"
        in 21..29 -> "廿" + LUNAR_DIGITS[day - 20]
        30 -> "三十"
        else -> "${day}日"
    }

    /** 农历月份名，如「五月」；闰月加「闰」前缀，如「闰五月」。 */
    fun monthText(l: LunarDate): String = (if (l.isLeap) "闰" else "") + LUNAR_MONTHS[l.month - 1]

    /** 农历日名，如「初一」「十五」「廿三」「三十」。 */
    fun dayText(l: LunarDate): String = lunarDayText(l.day)

    /** 完整中文，如「农历五月初五」。 */
    fun fullText(l: LunarDate): String = "农历" + monthText(l) + dayText(l)
}

/** 公历日期对应的农历日期。 */
data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeap: Boolean)
