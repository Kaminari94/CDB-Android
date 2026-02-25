package com.example.centraledellebolle.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothPrinterService(private val context: Context) {

    // Standard UUID for SPP (Serial Port Profile)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Suppress("MissingPermission")
    suspend fun printTest(macAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            return@withContext Result.failure(Exception("Bluetooth not supported on this device."))
        }

        if (!bluetoothAdapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth is not enabled."))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure(Exception("Bluetooth connect permission not granted."))
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(macAddress)
        } catch (e: IllegalArgumentException) {
            return@withContext Result.failure(Exception("Invalid MAC address."))
        }

        try {
            val socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            val outputStream = socket.outputStream
            outputStream.write("TEST OK\n".toByteArray())
            outputStream.flush()
            socket.close()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}