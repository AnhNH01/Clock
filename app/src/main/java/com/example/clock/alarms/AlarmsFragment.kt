package com.example.clock.alarms

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.clock.MQTT_SERVER_URI
import com.example.clock.MQTT_TOPIC_ALARM
import com.example.clock.MQTT_TOPIC_SET_ALARM
import com.example.clock.adapter.AlarmAdapter
import com.example.clock.databinding.FragmentAlarmsBinding
import com.example.clock.models.Alarm
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class AlarmsFragment : Fragment() {

    private var _binding: FragmentAlarmsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataSet: MutableList<Alarm>
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlarmAdapter
    private lateinit var floatingButton: FloatingActionButton


    private var timePicker: MaterialTimePicker =
        MaterialTimePicker.Builder()
            .setTimeFormat(CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Set Alarm")
            .setInputMode(INPUT_MODE_CLOCK)
            .build()

    private lateinit var mqttClient: MqttAndroidClient

    companion object {
        const val TAG = "AlarmFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAlarmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        floatingButton = binding.floatingBtnAddAlarm
        floatingButton.setOnClickListener {
            timePicker.show(parentFragmentManager, "TAG")
            timePicker.addOnPositiveButtonClickListener {
                val id = dataSet[dataSet.size - 1].id + 1
                Log.d(TAG, "Add alarm: ${id} ")
                val jsonMsg = Json.encodeToString(
                    Alarm(
                        id,
                        timePicker.hour,
                        timePicker.minute,
                        state = 1
                    )
                )
                publish(MQTT_TOPIC_SET_ALARM, jsonMsg)
                Toast.makeText(requireContext(), "Alarm Added", Toast.LENGTH_SHORT)
                    .show()
                timePicker.clearOnPositiveButtonClickListeners()
            }
        }

        // Get reference to Recycler View
        recyclerView = binding.alarmRecyclerView
        recyclerView.setHasFixedSize(true)

        // Init dataSet
        dataSet = mutableListOf()

        // Set Adapter
        adapter = AlarmAdapter(dataSet)
        recyclerView.adapter = adapter
        adapter.setOnRecyclerViewItemClickedListener(object :
            AlarmAdapter.OnRecyclerViewItemClickedListener {
            override fun onAlarmClicked(position: Int) {
                timePicker.show(parentFragmentManager, "UPDATE_ALARM")

                timePicker.addOnPositiveButtonClickListener {
                    Log.d(TAG, "Update Alarm: id = ${dataSet[position].id}")
                    val id = dataSet[position].id
                    val state = dataSet[position].state
                    val jsonMsg =
                        Json.encodeToString(Alarm(id, timePicker.hour, timePicker.minute, state))
                    publish(MQTT_TOPIC_SET_ALARM, jsonMsg)
                    Log.d(TAG, "Update Alarm jsonmsg:${jsonMsg} ")
                    Toast.makeText(requireContext(), "Changed Alarm Time", Toast.LENGTH_SHORT)
                        .show()
                    timePicker.clearOnPositiveButtonClickListeners()
                }
            }

            override fun onSwitchClicked(position: Int) {
                if (dataSet[position].state == 1) {
                    dataSet[position].state = 0
                } else {
                    dataSet[position].state = 1
                }
                val jsonMsg = Json.encodeToString(dataSet[position])
                publish(MQTT_TOPIC_SET_ALARM, jsonMsg)

            }

        })


        Log.d("TAG", "onViewCreated: dataset$dataSet")

        connectToBroker()

    }

    private fun connectToBroker() {

        mqttClient = MqttAndroidClient(requireContext(), MQTT_SERVER_URI, "alarmfragmentClient")

        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "connectionLost:")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic.equals(MQTT_TOPIC_ALARM)) {
                    Log.d(TAG, "messageArrived: ${message.toString()}")
                    extractListAlarm(message.toString())
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })

        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "onSuccess: Connected to broker")
                    subscribe(MQTT_TOPIC_ALARM)
                    publish(MQTT_TOPIC_SET_ALARM, "GET")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "onFailure: Can't connect to broker")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    private fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "onSuccess: Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "onFailure: Unable to subscribe")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mqttClient.disconnect()
    }

    private fun extractListAlarm(mqttMsg: String) {
        val listAlarm = ArrayList<Alarm>();
        if (mqttMsg.isNotEmpty()) {
            val splits = mqttMsg.split(';');
            if (splits.isNotEmpty()) {
                for (s in splits) {
                    val alarmData = s.split('-')
                    listAlarm.add(
                        Alarm(
                            alarmData[0].toInt(),
                            alarmData[1].toInt(),
                            alarmData[2].toInt(),
                            alarmData[3].toInt()
                        )
                    )
                }
                dataSet.clear()
                dataSet.addAll(listAlarm)
                Log.d(TAG, "extractListAlarm: ")
                adapter.notifyDataSetChanged()
            }
        }
    }

}