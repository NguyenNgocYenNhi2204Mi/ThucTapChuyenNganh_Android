package ndtt.myflix.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_detail.*
import ndtt.myflix.R
import ndtt.myflix.ViewModel.MovieViewModel
import ndtt.myflix.databinding.ActivityDetailBinding
import kotlin.properties.Delegates

class DetailActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDetailBinding
    private lateinit var viewModel: MovieViewModel
    private var movieId by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        movieId = intent.getIntExtra("id", 0)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MovieViewModel::class.java]
        viewModel.getDetailMovie(movieId)

        viewModel.observeMovieDetailData().observe(this, Observer { movieList ->
            tvTitle.text = movieList.title
            tvOverview.text = movieList.overview
            tvRD.text = "Date released: " + setReleaseDate(movieList.release_date)
            Glide
                .with(this@DetailActivity)
                .load("https://image.tmdb.org/t/p/w500" + movieList.poster_path)
                .centerCrop()
                .into(imgView)
        })
    }

    private fun setReleaseDate(releaseDate: String): String{
        val date = releaseDate.substring(8,10)
        val month = releaseDate.substring(5,7)
        val year = releaseDate.substring(0,4)
        val newRD = date + "-" + month + "-" + year
        return newRD
    }
}