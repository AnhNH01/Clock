package com.example.clock.data

import com.example.clock.models.Alarm


class MockAlarmData {
    fun loadData(): MutableList<Alarm> {
        return mutableListOf(
            Alarm(1,12, 12, 1),
            Alarm(2, 12, 0, 0),
            Alarm(3,13, 20, 0),
            Alarm(4,14, 30, 1),

        )
    }
}