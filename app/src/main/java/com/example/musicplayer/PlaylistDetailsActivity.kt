package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.Adapter.MusicAdapter
import com.example.musicplayer.databinding.ActivityPlaylistDetailsBinding

class PlaylistDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlaylistDetailsBinding
    lateinit var adapter: MusicAdapter

    companion object{
        var currentPlaylistPos: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.coolPink)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtnPD.setOnClickListener { finish() }



        currentPlaylistPos = intent.extras?.get("index") as Int

        binding.playlistDetailsRV.setItemViewCacheSize(18)
        binding.playlistDetailsRV.setHasFixedSize(true)
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this)

        PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.addAll(MainActivity.MusicListMA)
        PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.shuffle()

        adapter = MusicAdapter(this, PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist, playlistDetails = true)
        binding.playlistDetailsRV.adapter = adapter

        binding.suffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index",0)
            intent.putExtra("class","PlaylistDetailsShuffle")
            startActivity(intent)
        }

        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        binding.playlistNamePD.text = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].name
        binding.moreInfoPD.text = "Total ${adapter.itemCount} Songs. \n\n"+
                "Created On:\n${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdOn}\n\n" +
                "-- ${PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].createdBy}"

        if (adapter.itemCount > 0){
            Glide.with(this)
                .load(PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon).centerCrop())
                .into(binding.playlistImgPD)
            binding.suffleBtnPD.visibility = View.VISIBLE
        }

    }
}