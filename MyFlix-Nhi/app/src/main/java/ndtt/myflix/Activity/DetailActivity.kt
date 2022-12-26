package ndtt.myflix.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.mlkit.codelab.translate.util.Language
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
            viewModel.sourceText = movieList.overview
            Glide
                .with(this@DetailActivity)
                .load("https://image.tmdb.org/t/p/w500" + movieList.poster_path)
                .centerCrop()
                .into(imgView)
        })

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )

        targetLangSelector.adapter = adapter
        targetLangSelector.setSelection(adapter.getPosition(Language("en")))
        targetLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.targetLang.value = adapter.getItem(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        viewModel.translatedText.observe(this, Observer { resultOrError ->
            resultOrError?.let {
                if (it.error != null) {
                    tvOverview.error = resultOrError.error?.localizedMessage
                } else {
                    tvOverview.text = resultOrError.result
                }
            }
        })
        viewModel.modelDownloading.observe(this, Observer { isDownloading ->
            progressBar.visibility = if (isDownloading) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
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