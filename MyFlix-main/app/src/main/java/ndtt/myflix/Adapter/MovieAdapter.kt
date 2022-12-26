package ndtt.myflix.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ndtt.myflix.Models.Results
import ndtt.myflix.databinding.MovieLayoutBinding
import kotlin.math.roundToInt

class MovieAdapter : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {
    private var movieList = ArrayList<Results>()
    private var onClickListener: OnClickListener? = null

    fun setMovieList(movieList: List<Results>) {
        this.movieList = movieList as ArrayList<Results>
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: MovieLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MovieLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = movieList[position]
        Glide.with(holder.itemView)
            .load("https://image.tmdb.org/t/p/w500" + movieList[position].poster_path)
            .into(holder.binding.ivItemMovie)

        holder.binding.tvTitle.text = movieList[position].title
        holder.binding.tvRD.text = setReleaseDate(movieList[position].release_date)
        holder.binding.ratingBar.rating = (movieList[position].vote_average/2).toFloat()
        holder.binding.tvRating.text = ((movieList[position].vote_average * 10.0).roundToInt() / 10.0).toString()

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, model)
            }
        }
    }

    private fun setReleaseDate(releaseDate: String): String{
        val date = releaseDate.substring(8,10)
        val month = releaseDate.substring(5,7)
        val year = releaseDate.substring(0,4)
        val newRD = date + "-" + month + "-" + year
        return newRD
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int,model: Results)
    }

}