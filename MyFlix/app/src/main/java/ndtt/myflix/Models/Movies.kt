package ndtt.myflix.Models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson

@Entity(tableName = "MovieList")
data class Movies(
    @PrimaryKey val id: Int,
    val page: Int,
    val results: List<Results>,
    val total_pages: Int,
    val total_results: Int
)

class Converters {

    @TypeConverter
    fun listToJson(value: ArrayList<Results>?) = Gson().toJson(value)

    @TypeConverter
    fun jsonToList(value: String) = Gson().fromJson(value, Array<Results>::class.java).toList()
}