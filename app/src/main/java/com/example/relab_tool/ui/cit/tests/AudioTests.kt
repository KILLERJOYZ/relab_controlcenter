package com.example.relab_tool.ui.cit.tests

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

fun playSineWave(streamType: Int): AudioTrack {
    val sampleRate = 44100
    val freqOfTone = 440.0 // Hz
    val numSamples = sampleRate * 3 // 3 seconds
    val generatedSnd = ByteArray(2 * numSamples)

    for (i in 0 until numSamples) {
        val sample = sin(2 * Math.PI * i / (sampleRate / freqOfTone))
        val valShort = (sample * 32767).toInt().toShort()
        generatedSnd[i * 2] = (valShort.toInt() and 0x00ff).toByte()
        generatedSnd[i * 2 + 1] = (valShort.toInt() and 0xff00).ushr(8).toByte()
    }

    val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(if (streamType == AudioManager.STREAM_VOICE_CALL) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(generatedSnd.size)
        .build()

    audioTrack.write(generatedSnd, 0, generatedSnd.size)
    audioTrack.setLoopPoints(0, generatedSnd.size / 2, -1)
    audioTrack.play()
    
    return audioTrack
}

@Composable
fun EarpieceTest(onResult: (CITTestResult) -> Unit) {
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    
    DisposableEffect(Unit) {
        audioTrack = playSineWave(AudioManager.STREAM_VOICE_CALL)
        onDispose {
            audioTrack?.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(id = R.string.earpiece_test_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(id = R.string.earpiece_test_instruction))
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun SpeakerTest(onResult: (CITTestResult) -> Unit) {
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    
    DisposableEffect(Unit) {
        audioTrack = playSineWave(AudioManager.STREAM_MUSIC)
        onDispose {
            audioTrack?.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(id = R.string.loudspeaker_test_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(id = R.string.loudspeaker_test_instruction))
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun MicrophoneTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) 
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    val startToRecordStr = stringResource(id = R.string.mic_press_to_record)
    var statusText by remember { mutableStateOf(startToRecordStr) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val audioFile = remember { File(context.cacheDir, "cit_mic_test.3gp") }
    
    val recordingStr = stringResource(id = R.string.mic_recording)
    val playingStr = stringResource(id = R.string.mic_playing)

    DisposableEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.RECORD_AUDIO)
        onDispose {
            if (audioFile.exists()) audioFile.delete()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(id = R.string.mic_test_title), style = MaterialTheme.typography.headlineMedium)
            Text(statusText, fontWeight = FontWeight.Bold, color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(16.dp))

            if (!showResults) {
                Button(
                    enabled = hasPermission && !isRecording && !isPlaying,
                    onClick = {
                        scope.launch {
                            var recorder: MediaRecorder? = null
                            try {
                                isRecording = true
                                statusText = recordingStr
                                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                                recorder.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                    setOutputFile(audioFile.absolutePath)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                    prepare()
                                    start()
                                }
                                delay(3000)
                                recorder.stop()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                recorder?.release()
                                isRecording = false
                            }
                            
                            if (audioFile.exists() && audioFile.length() > 0) {
                                statusText = playingStr
                                isPlaying = true
                                var player: MediaPlayer? = null
                                try {
                                    player = MediaPlayer()
                                    player.setDataSource(audioFile.absolutePath)
                                    player.prepare()
                                    player.start()
                                    delay(3000)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    player?.release()
                                    isPlaying = false
                                }
                            }
                            showResults = true
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.mic_start_test))
                }
            } else {
                Text(stringResource(id = R.string.mic_hear_yourself))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                    Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
                }
            }
        }
    }
}

@Composable
fun HeadphoneTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    var isPlugged by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    isPlugged = state == 1
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        
        // Initial check
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        isPlugged = devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(id = R.string.headphone_jack_test_title), style = MaterialTheme.typography.headlineMedium)
            
            Text(stringResource(id = R.string.status) + ": ${if (isPlugged) stringResource(id = R.string.headphone_plugged) else stringResource(id = R.string.headphone_not_detected)}", 
                fontWeight = FontWeight.Black, 
                color = if (isPlugged) Color(0xFF4CAF50) else Color.Red)
            
            Text(stringResource(id = R.string.headphone_instruction))
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
            }
        }
    }
}
