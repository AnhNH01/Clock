package com.example.clock.time

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clock.*
import com.example.clock.databinding.FragmentTimeBinding
import com.example.clock.models.Date
import com.example.clock.models.Time
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat


class TimeFragment : Fragment() {

    private var _binding: FragmentTimeBinding? = null
    private val binding get() = _binding!!
    private lateinit var textViewTime: MaterialTextView
    private lateinit var textViewDate: MaterialTextView
    private lateinit var timePicker: MaterialTimePicker
    private val datePicker = MaterialDatePicker.Builder.datePicker().build()
    private lateinit var mqttClient: MqttAndroidClient

    companion object {
        const val TAG = "TimeFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textViewTime = binding.textViewTime
        textViewDate = binding.textViewDate
        setUpTimePicker()
        setUpDatePicker()

        textViewTime.setOnClickListener {
            timePicker.show(parentFragmentManager, javaClass.simpleName)
        }

        textViewDate.setOnClickListener {
            datePicker.show(parentFragmentManager, javaClass.simpleName)
        }

        connectToBroker()
    }

    private fun setUpTimePicker() {
        timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Set Time")
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val hour: String =
                if (timePicker.hour < 10) "0${timePicker.hour}" else "${timePicker.hour}"
            val minute: String =
                if (timePicker.minute < 10) "0${timePicker.minute}" else "${timePicker.minute}"
            val timeString = "$hour:$minute"
            textViewTime.text = timeString

            val messageSetTime = Json.encodeToString(Time(hour.toInt(), minute.toInt()))

            publish(MQTT_TOPIC_SET_TIME, messageSetTime, 0, false);
        }

    }

    private fun setUpDatePicker() {
        datePicker.addOnPositiveButtonClickListener {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy")
            val date = dateFormatter.format(it)
            val dateSplit = date.split("/")

            textViewDate.text = date
            val messageSetDate = Json.encodeToString(Date(dateSplit[0].toInt(), dateSplit[1].toInt(), dateSplit[2].toInt()))
            publish(MQTT_TOPIC_SET_DATE, messageSetDate, 1, false)
        }
    }

    private fun connectToBroker() {
        mqttClient = MqttAndroidClient(requireContext(), MQTT_SERVER_URI, "timefragmentClient")
        mqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "connectionLost: ")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if(!this@TimeFragment.isVisible) {
                    Log.d(TAG, "messageArrived: ${message.toString()}, topic: $topic")

                } else  {
                    if(topic.equals(MQTT_TOPIC_TIME)) {
                        binding.textViewTime.text = message.toString()
                        Log.d(TAG, "Updated text in  the textview Time")
                    } else if (topic.equals(MQTT_TOPIC_DATE)) {
                        binding.textViewDate.text = message.toString()
                        Log.d(TAG, "Updated text in  the textview Date")
                    }
                }

            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "deliveryComplete: ")
            }

        })

        val options = MqttConnectOptions()
        mqttClient.connect(options, null, object: IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                subscribe(MQTT_TOPIC_TIME, 0)
                subscribe(MQTT_TOPIC_DATE, 0)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Toast.makeText(requireContext(), "Can't connect to broker", Toast.LENGTH_SHORT).show()
            }
        })


    }

    fun subscribe(topic: String, qos: Int) {
        try {
            mqttClient.subscribe(topic, qos, null, object: IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "onSuccess: Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "onFailure: Can't subscribe to $topic")
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
}


