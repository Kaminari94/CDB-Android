package com.example.centraledellebolle.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothPrinterService(private val context: Context) {

    private val printMutex = Mutex()

    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val sppUuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Suppress("MissingPermission")
    suspend fun printText(macAddress: String, text: String): Result<Unit> =
        printMutex.withLock {
            withContext(Dispatchers.IO) {
                val adapter = bluetoothAdapter

                if (adapter == null) {
                    return@withContext Result.failure(Exception("Bluetooth non supportato su questo dispositivo."))
                }

                if (!adapter.isEnabled) {
                    return@withContext Result.failure(Exception("Bluetooth non abilitato."))
                }

                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext Result.failure(Exception("Permessi per la connessione Bluetooth mancanti."))
                }

                val device = try {
                    adapter.getRemoteDevice(macAddress)
                } catch (e: IllegalArgumentException) {
                    return@withContext Result.failure(Exception("MAC Address sbagliato."))
                }

                var socket: BluetoothSocket? = null

                try {
                    socket = device.createRfcommSocketToServiceRecord(sppUuid)

                    // adapter.cancelDiscovery()

                    socket.connect()

                    val outputStream = socket.outputStream

                    val commandList = mutableListOf<Byte>()

                    // Initialize printer
                    commandList.addAll(byteArrayOf(0x1B, 0x40).toList())

                    // Piccola pausa iniziale dopo reset stampante
                    delay(300)

                    val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
                    val boldOff = byteArrayOf(0x1B, 0x45, 0x00)

                    var lastIndex = 0
                    val regex = Regex("(?i)\\[/?b\\]")

                    regex.findAll(text).forEach { match ->
                        val plainText = text.substring(lastIndex, match.range.first)
                        commandList.addAll(plainText.toByteArray(Charsets.UTF_8).toList())

                        if (match.value.equals("[b]", ignoreCase = true)) {
                            commandList.addAll(boldOn.toList())
                        } else {
                            commandList.addAll(boldOff.toList())
                        }

                        lastIndex = match.range.last + 1
                    }

                    if (lastIndex < text.length) {
                        val remainingText = text.substring(lastIndex)
                        commandList.addAll(remainingText.toByteArray(Charsets.UTF_8).toList())
                    }

                    commandList.addAll("\n".toByteArray(Charsets.UTF_8).toList())
                    commandList.addAll(byteArrayOf(0x1B, 0x64, 3).toList())

                    val commandBytes = commandList.toByteArray()

                    val chunkSize = 128
                    var offset = 0

                    while (offset < commandBytes.size) {
                        val size = minOf(chunkSize, commandBytes.size - offset)

                        outputStream.write(commandBytes, offset, size)
                        outputStream.flush()

                        delay(80)

                        offset += size
                    }

                    delay(4000)

                    Result.success(Unit)

                } catch (e: IOException) {
                    Result.failure(e)
                } finally {
                    try {
                        socket?.close()
                    } catch (e: IOException) {
                        // Ignora errore in chiusura socket
                    }
                }
            }
        }
}