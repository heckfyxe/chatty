package com.heckfyxe.chatty.ui.message

import com.heckfyxe.chatty.model.Message
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*

class MessageDateHelperTest {

    private val calendar = Calendar.getInstance()
    private val expected: Long

    init {
        calendar.set(2020, 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        expected = calendar.timeInMillis
    }

    @Test
    fun millisToDay_givenOnlyDay() {
        calendar.set(2020, 1, 1, 0, 0, 0)
        val time = calendar.timeInMillis

        val actual = millisToDay(time)

        assertThat(actual, `is`(expected))
    }

    @Test
    fun millisToDate_givenHourMinuteSecond() {
        calendar.set(2020, 1, 1, 12, 12, 12)
        val time = calendar.timeInMillis

        val actual = millisToDay(time)

        assertThat(actual, `is`(expected))
    }

    @Test
    fun millisToDate_givenHourMinuteSecondMillisecond() {
        calendar.set(2020, 1, 1, 12, 1, 12)
        calendar.timeInMillis += 12
        val time = calendar.timeInMillis

        val actual = millisToDay(time)

        assertThat(actual, `is`(expected))
    }

    private fun getDayInMillis(date: Int, month: Int = 1, year: Int = 2020): Long {
        calendar.set(year, month, date, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    @Test
    fun getMessageTimeHeadersTest() {
        val daysMillis = listOf(1, 1, 2, 3, 4, 2).map {
            getDayInMillis(it)
        }
        val expected = daysMillis.toSet()
        val messages = daysMillis.map {
            val message = mockk<Message>()
            every { message.time } returns it
            message
        }

        val headers = getMessageTimeHeaders(messages).map { it.time }.toSet()

        assertThat(headers, `is`(expected))
    }
}