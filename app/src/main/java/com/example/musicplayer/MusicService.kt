package com.example.musicplayer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.example.musicplayer.Models.formatDuration
import com.example.musicplayer.Models.getImageArt

class MusicService: Service(){
    private var myBinder = MyBinder()
    var mediaPlayer:MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext,"My Music")
        return myBinder
    }

    inner class MyBinder:Binder(){
        fun currentService(): MusicService{
            return this@MusicService
        }
    }

    fun showNotification(playPauseBtn: Int){

        val prevIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext,0,prevIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext,0,playIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val exitIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(baseContext,0,exitIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val imgArt = getImageArt(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null){
            BitmapFactory.decodeByteArray(imgArt,0,imgArt.size)
        }else{
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon)
        }

        val notification = NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
            .setContentTitle(PlayerActivity.musicListPA[PlayerActivity.songPosition].title)
            .setContentText(PlayerActivity.musicListPA[PlayerActivity.songPosition].artist)
            .setSmallIcon(R.drawable.music_icon)
            .setLargeIcon(image)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.previous_icon, "Previous",prevPendingIntent)
            .addAction(playPauseBtn, "Play",playPendingIntent)
            .addAction(R.drawable.next_icon, "Next",nextPendingIntent)
            .addAction(R.drawable.exit_icon, "Exit",exitPendingIntent)
            .build()

        startForeground(13,notification)
    }

    fun createMediaPlayer(){
        try {
            if (PlayerActivity.musicService!!.mediaPlayer == null){
                PlayerActivity.musicService!!.mediaPlayer = MediaPlayer()

                PlayerActivity.musicService!!.mediaPlayer!!.reset()
                PlayerActivity.musicService!!.mediaPlayer!!.setDataSource(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
                PlayerActivity.musicService!!.mediaPlayer!!.prepare()
                PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
                PlayerActivity.nowPlayingId = PlayerActivity.musicListPA[PlayerActivity.songPosition].id
            }else{
                PlayerActivity.musicService!!.mediaPlayer!!.reset()
                PlayerActivity.musicService!!.mediaPlayer!!.setDataSource(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
                PlayerActivity.musicService!!.mediaPlayer!!.prepare()
                PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)

                PlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
                PlayerActivity.binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
                PlayerActivity.binding.seekBarPA.progress = 0
                PlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration
                PlayerActivity.nowPlayingId = PlayerActivity.musicListPA[PlayerActivity.songPosition].id
            }
        }catch (e: Exception){ return }

    }

    fun seekbarSetup(){
        runnable = Runnable {
            PlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable,200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }
}