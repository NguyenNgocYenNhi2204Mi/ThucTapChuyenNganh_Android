package ndtt.myflix.Service

import ndtt.myflix.Models.Movies
import ndtt.myflix.Models.Results
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface MovieApi {
    @GET("/3/movie/top_rated?api_key=005034fdbc49e9c5e99a9ee875047614")
    fun getPopularMovies() : Call<Movies>

    @GET("/3/movie/{id}?api_key=005034fdbc49e9c5e99a9ee875047614")
    fun getDetailMovie(@Path("id") id: Int) : Call<Results>
}