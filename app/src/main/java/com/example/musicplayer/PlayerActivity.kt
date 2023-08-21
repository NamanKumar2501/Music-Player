package com.example.musicplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.Models.Music
import com.example.musicplayer.Models.exitApplication
import com.example.musicplayer.Models.favouriteChecker
import com.example.musicplayer.Models.formatDuration
import com.example.musicplayer.databinding.ActivityPlayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicListPA: ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying: Boolean = false
        var musicService: MusicService? = null
        lateinit var binding: ActivityPlayerBinding
        var repeat: Boolean = false
        var min15: Boolean = false
        var min60: Boolean = false
        var min30: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setTheme(R.style.Theme_MusicPlayer)
        setTheme(R.style.coolPink)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeLayout()

        //Back Button
        binding.backBtnPA.setOnClickListener {
            finish()
        }

        // Play Pause
        binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }
        binding.priviousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
        binding.nextBtnPA.setOnClickListener {
            prevNextSong(increment = true)
        }

        // SeekBar
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        // Repeat Song
        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_700))
            } else {
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }

        // Equalizer
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION,
                    musicService!!.mediaPlayer!!.audioSessionId
                )
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult(eqIntent, 13)
            } catch (e: Exception) {
                Toast.makeText(this, "Equalizer Feature not Support!!", Toast.LENGTH_SHORT).show()
            }
        }

        // Timer
        binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) {
                showBottomSheetDialog()
            } else {
                // Show Alert Dialog
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to Stop Timer ? ")
                    .setPositiveButton("Yes") { _, _ ->
                        min15 = false
                        min30 = false
                        min60 = false
                        binding.timerBtnPA.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.cool_pink
                            )
                        )
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
            }
        }

        // Share
        binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
        }

        // Favourite Button
        binding.favouriteBtnPA.setOnClickListener {
            if (isFavourite) {
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favorite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else {
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favorite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
        }


        // Functions Start ----
    }

    // Important Function
    private fun initializeLayout() {
        songPosition = intent.getIntExtra("index", 0)
        when (intent.getStringExtra("class")) {

            "FavouriteAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)

                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                setLayout()
            }

            "NowPlaying" -> {
                setLayout()
                binding.tvSeekBarStart.text =
                    formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text =
                    formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if (isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }


            "MusicAdapterSearch" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.musicListSearch)
                setLayout()
            }

            "MusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                setLayout()
            }

            "MainActivity" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)

                musicListPA = ArrayList()
                musicListPA.addAll(MainActivity.MusicListMA)
                musicListPA.shuffle()
                setLayout()
            }

            "FavouriteShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)

                musicListPA = ArrayList()
                musicListPA.addAll(FavouriteActivity.favouriteSongs)
                musicListPA.shuffle()
                setLayout()
            }


            "PlaylistDetailsAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(PlaylistActivity.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist)
                setLayout()
            }

            "PlaylistDetailsShuffle" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                musicListPA = ArrayList()
                musicListPA.addAll(PlaylistActivity.musicPlaylist.ref[PlaylistDetailsActivity.currentPlaylistPos].playlist)
                musicListPA.shuffle()
                setLayout()
            }

        }
    }

    // Set Layout
    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].title
        if (repeat) binding.repeatBtnPA.setColorFilter(
            ContextCompat.getColor(
                applicationContext,
                R.color.purple_700
            )
        )
        if (min15 || min30 || min60) binding.timerBtnPA.setColorFilter(
            ContextCompat.getColor(
                this,
                R.color.purple_700
            )
        )
        if (isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favorite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favorite_empty_icon)
    }

    // Create Media Player
    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) {
                musicService!!.mediaPlayer = MediaPlayer()

                musicService!!.mediaPlayer!!.reset()
                musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
                musicService!!.mediaPlayer!!.prepare()
                musicService!!.mediaPlayer!!.start()
                isPlaying = true

                binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                musicService!!.showNotification(R.drawable.pause_icon)

                binding.tvSeekBarStart.text =
                    formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text =
                    formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = 0
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration

                musicService!!.mediaPlayer!!.setOnCompletionListener(this)

                nowPlayingId = musicListPA[songPosition].id

            } else {
                musicService!!.mediaPlayer!!.reset()
                musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
                musicService!!.mediaPlayer!!.prepare()
                musicService!!.mediaPlayer!!.start()
                isPlaying = true

                binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                musicService!!.showNotification(R.drawable.pause_icon)

                binding.tvSeekBarStart.text =
                    formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text =
                    formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = 0
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration

                musicService!!.mediaPlayer!!.setOnCompletionListener(this)

                nowPlayingId = musicListPA[songPosition].id

            }

        } catch (e: Exception) {
            return
        }

    }


    private fun playMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
    }

    private fun pauseMusic() {
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
    }

    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(false)
            setLayout()
            createMediaPlayer()
        }
    }

    private fun setSongPosition(increment: Boolean) {
        if (increment) {
            if (musicListPA.size - 1 == songPosition) {
                songPosition = 0
            } else {
                ++songPosition
            }
        } else {
            if (0 == songPosition) {
                songPosition = musicListPA.size - 1
            } else {
                --songPosition
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        musicService = binder.currentService()
        createMediaPlayer()
        musicService!!.seekbarSetup()

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        try {
            setLayout()
        } catch (e: Exception) {
            return
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || requestCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialop)
        dialog.show()

        dialog.findViewById<LinearLayoutCompat>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 15 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_700))
            min15 = true
            Thread {
                //    Thread.sleep(5000) // 5 sec timer for testing == 1 sec 1 thousand milliSecond
                Thread.sleep((15 * 60000).toLong()) // Music stop in 15 min
                if (min15) exitApplication()
            }.start()
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayoutCompat>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_700))
            min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication()
            }.start()
            dialog.dismiss()
        }

        dialog.findViewById<LinearLayoutCompat>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 60 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.purple_700))
            min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (min60) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }

}

