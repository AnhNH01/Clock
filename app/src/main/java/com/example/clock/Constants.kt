package com.example.clock

//const val MQTT_SERVER_URI = "tcp://172.16.0.165:1883"
const val MQTT_SERVER_URI= "tcp://192.168.1.15:1883"
//const val MQTT_SERVER_URI= "tcp://172.31.99.43:1883"
//const val MQTT_SERVER_URI= "tcp://172.20.10.3:1883"
const val MQTT_TOPIC_TIME = "clock/time"
const val MQTT_TOPIC_SET_TIME = "clock/time/set"
const val MQTT_TOPIC_DATE = "clock/date"
const val MQTT_TOPIC_SET_DATE = "clock/date/set"
const val MQTT_TOPIC_ALARM = "clock/alarm"
const val MQTT_TOPIC_SET_ALARM = "clock/alarm/set"