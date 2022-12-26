package ndtt.myflix.DAO

import androidx.lifecycle.MutableLiveData
import androidx.room.*
import ndtt.myflix.Models.Results

@Dao
interface DetailDao {
    @Query("SELECT * FROM movie")
    fun getAll(): List<Results>

    @Query("SELECT * FROM movie WHERE id=:id ")
    fun getDetail(id: Int): List<Results>

//    @Query("SELECT * FROM movies WHERE id=:id ")
//    fun checkDetail(id: Int): List<Results>

    @Update
    fun updateDetail(movieDetail: ArrayList<Results>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDetail(movieDetail: ArrayList<Results>)
}