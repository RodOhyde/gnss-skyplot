package com.example.gnssapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.*
import android.location.GnssMeasurementsEvent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gnssapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager
    private val gnssCallback = createGnssCallback()

    // NMEA
    private var nmeaListener: OnNmeaMessageListener? = null
    private var nmeaWriter: FileWriter? = null
    private val nmeaLogging = AtomicBoolean(false)

    // Raw measurements
    private var measurementCallback: GnssMeasurementsEvent.Callback? = null
    private var measurementWriter: FileWriter? = null
    private val measurementLogging = AtomicBoolean(false)

    // Background handler for callbacks that produce a lot of data
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        handlerThread = HandlerThread("gnss-thread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        requestPermissions()

        setupUi()
    }

    private fun setupUi() {
        val btnNmea = findViewById<Button>(R.id.btn_nmea_toggle)
        val btnRaw = findViewById<Button>(R.id.btn_raw_toggle)

        btnNmea.setOnClickListener {
            if (nmeaLogging.get()) stopNmeaLogging() else startNmeaLogging()
            btnNmea.text = if (nmeaLogging.get()) "Stop NMEA" else "Start NMEA"
        }

        btnRaw.setOnClickListener {
            if (measurementLogging.get()) stopMeasurementLogging() else startMeasurementLogging()
            btnRaw.text = if (measurementLogging.get()) "Stop Raw" else "Start Raw"
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startGnss()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGnss()
        }
    }

    private fun startGnss() {
        try {
            // Use a handler to avoid slamming the main thread with GNSS callbacks
            locationManager.registerGnssStatusCallback(gnssCallback, handler)

            // Prepare NMEA listener (but register only when logging starts)
            nmeaListener = OnNmeaMessageListener { message, timestamp ->
                if (nmeaLogging.get()) {
                    try {
                        nmeaWriter?.apply {
                            write("${getTimeIso()} | $timestamp | $message\n")
                            flush()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            // Prepare measurement callback
            measurementCallback = object : GnssMeasurementsEvent.Callback() {
                override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent) {
                    if (!measurementLogging.get()) return
                    // Log a compact representation: timeNs, full bias, per-satellite raw measurements
                    try {
                        measurementWriter?.apply {
                            val header = "MEASUREMENT ${getTimeIso()} ${eventArgs.clock.timeNanos}\n"
                            write(header)
                            for (m in eventArgs.measurements) {
                                val line = StringBuilder()
                                with(m) {
                                    line.append("svid=$svid constellation=${constellationType} ")
                                    line.append("cno=${cn0DbHz} doppler=${pseudorangeRateMetersPerSecond} ")
                                    line.append("acc=${accumulatedDeltaRangeState}\n")
                                }
                                write(line.toString())
                            }
                            write("END\n")
                            flush()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                override fun onStatusChanged(status: Int) {
                    // optional
                }
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun startNmeaLogging() {
        try {
            val logsDir = File(getExternalFilesDir("logs"), "nmea")
            logsDir.mkdirs()
            val file = File(logsDir, "nmea_${timeStampFile()}.log")
            nmeaWriter = FileWriter(file, true)
            locationManager.addNmeaListener(nmeaListener!!, handler)
            nmeaLogging.set(true)
            Log.i("GNSS", "Started NMEA logging to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopNmeaLogging() {
        try {
            locationManager.removeNmeaListener(nmeaListener!!)
        } catch (e: Exception) {
            // ignore
        }
        try {
            nmeaWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        nmeaWriter = null
        nmeaLogging.set(false)
        Log.i("GNSS", "Stopped NMEA logging")
    }

    private fun startMeasurementLogging() {
        try {
            val logsDir = File(getExternalFilesDir("logs"), "raw")
            logsDir.mkdirs()
            val file = File(logsDir, "raw_${timeStampFile()}.log")
            measurementWriter = FileWriter(file, true)

            // Register callback
            locationManager.registerGnssMeasurementsCallback(measurementCallback!!, handler)
            measurementLogging.set(true)
            Log.i("GNSS", "Started raw measurement logging to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMeasurementLogging() {
        try {
            locationManager.unregisterGnssMeasurementsCallback(measurementCallback!!)
        } catch (e: Exception) {
            // ignore
        }
        try {
            measurementWriter?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        measurementWriter = null
        measurementLogging.set(false)
        Log.i("GNSS", "Stopped raw measurement logging")
    }

    override fun onDestroy() {
        super.onDestroy()
        try { stopNmeaLogging() } catch (e: Exception) {}
        try { stopMeasurementLogging() } catch (e: Exception) {}
        try { locationManager.unregisterGnssStatusCallback(gnssCallback) } catch (e: Exception) {}
        handlerThread.quitSafely()
    }

    private fun createGnssCallback(): GnssStatus.Callback {
        return object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val satellites = mutableListOf<SatelliteData>()

                for (i in 0 until status.satelliteCount) {
                    satellites.add(
                        SatelliteData(
                            az = status.getAzimuthDegrees(i),
                            el = status.getElevationDegrees(i),
                            snr = status.getCn0DbHz(i),
                            used = status.usedInFix(i),
                            constellation = status.getConstellationType(i),
                            svid = status.getSvid(i)
                        )
                    )
                }

                runOnUiThread {
                    binding.skyplotView.updateSatellites(satellites)
                }
            }
        }
    }

    // Utilities
    private fun timeStampFile(): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return fmt.format(Date())
    }

    private fun getTimeIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
