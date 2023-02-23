package com.example.clock.models

import kotlinx.serialization.Serializable

@Serializable
data class Alarm(var id: Int, var hour: Int, var minute: Int, var state: Int)