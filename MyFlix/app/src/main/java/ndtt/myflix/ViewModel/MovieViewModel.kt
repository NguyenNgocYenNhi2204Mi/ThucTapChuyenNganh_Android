package ndtt.myflix.ViewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import androidx.room.Room
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.codelab.translate.util.Language
import com.google.mlkit.codelab.translate.util.ResultOrError
import com.google.mlkit.codelab.translate.util.SmoothedMutableLiveData
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
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
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
        .setConfidenceThreshold(0.5f)
        .build()
    )
    val targetLang = MutableLiveData<Language>()
    var sourceText : String = ""

    val translatedText = MediatorLiveData<ResultOrError>()
    private val translating = MutableLiveData<Boolean>()
    val modelDownloading = SmoothedMutableLiveData<Boolean>(SMOOTHING_DURATION)

    private var modelDownloadTask: Task<Void> = Tasks.forCanceled()

    private val translators =
        object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?
            ) {
                oldValue.close()
            }
        }

    override fun onCleared() {
        languageIdentifier.close()
        translators.evictAll()
    }

    // Gets a list of all available translation languages.
    val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages()
        .map { Language(it) }

    private fun translate(): Task<String> {
        val text = sourceText
        val target = targetLang.value
        if (modelDownloading.value != false || translating.value != false) {
            return Tasks.forCanceled()
        }
        if (target == null || text == null || text.isEmpty()) {
            return Tasks.forResult("")
        }

        val targetLangCode = TranslateLanguage.fromLanguageTag(target.code) ?: return Tasks.forCanceled()

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLangCode)
            .build()
        val translator = translators[options]
        modelDownloading.setValue(true)

        // Register watchdog to unblock long running downloads
        Handler(Looper.getMainLooper()).postDelayed({ modelDownloading.setValue(false) }, 15000)
        modelDownloadTask = translator.downloadModelIfNeeded().addOnCompleteListener {
            modelDownloading.setValue(false)
        }
        translating.value = true
        return modelDownloadTask.onSuccessTask {
            translator.translate(text)
        }.addOnCompleteListener {
            translating.value = false
        }
    }

    init {
        modelDownloading.setValue(false)
        translating.value = false
        // Create a translation result or error object.
        val processTranslation =
            OnCompleteListener<String> { task ->
                if (task.isSuccessful) {
                    translatedText.value = ResultOrError(task.result, null)
                } else {
                    if (task.isCanceled) {
                        // Tasks are cancelled for reasons such as gating; ignore.
                        return@OnCompleteListener
                    }
                    translatedText.value = ResultOrError(null, task.exception)
                }
            }
        // Start translation if any of the following change: detected text, source lang, target lang.
        translatedText.addSource(targetLang) { translate().addOnCompleteListener(processTranslation) }
    }

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

    companion object {
        // Amount of time (in milliseconds) to wait for detected text to settle
        private const val SMOOTHING_DURATION = 50L

        private const val NUM_TRANSLATORS = 1
    }
}