package ndtt.myflix.ViewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import ndtt.myflix.DAO.DetailDao
import ndtt.myflix.DAO.DetailDb
import ndtt.myflix.Models.Movies
import ndtt.myflix.Models.Results
import ndtt.myflix.Service.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieViewModel (application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    private var movieLiveData = MutableLiveData<List<Results>>()
    private var movideDetailData = MutableLiveData<Results>()
    private lateinit var detailDao: DetailDao

    //------room------
    val db = Room.databaseBuilder(
        context,
        DetailDb::class.java, "MovieList"
    ).allowMainThreadQueries().build()

    @RequiresApi(Build.VERSION_CODES.M)
    fun getPopularMovies() {
        detailDao = db.detailDao()
        val list: ArrayList<Results> = detailDao.getAll() as ArrayList<Results>

        if (isOnline(context)) {
            RetrofitInstance.api.getPopularMovies().enqueue(object  :
                Callback<Movies> {
                override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                    if (response.body()!=null){
                        movieLiveData.value = response.body()!!.results
                    }
                    else{
                        return
                    }
                }
                override fun onFailure(call: Call<Movies>, t: Throwable) {
                    Log.d("TAG",t.message.toString())
                }
            })
        } else {
            movieLiveData.value = list
        }
    }

    fun getDetailMovie(id: Int) {
        var detailMovie: ArrayList<Results> = ArrayList()

        detailDao = db.detailDao()

        val list: ArrayList<Results> = detailDao.getDetail(id) as ArrayList<Results>

        if (list.size == 0) {
            RetrofitInstance.api.getDetailMovie(id).enqueue(object  :
                Callback<Results> {
                override fun onResponse(call: Call<Results>, response: Response<Results>) {
                    if (response.body()!=null){
                        movideDetailData.value = response.body()!!
                        detailMovie.addAll(listOf(response.body()!!))
                        detailDao.insertDetail(detailMovie)
                    }
                    else{
                        return
                    }
                }

                override fun onFailure(call: Call<Results>, t: Throwable) {
                    Log.d("TAG",t.message.toString())
                }
            })
        } else if (list[0].id == id) {
            movideDetailData.value = list[0]
        }
    }

    fun observeMovieLiveData() : LiveData<List<Results>> {
        return movieLiveData
    }
    fun observeMovieDetailData() : MutableLiveData<Results> {
        return movideDetailData
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }
}