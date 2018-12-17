package com.noblemajesty.newsapplication.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.util.Log
import com.noblemajesty.newsapplication.R.id.news
import com.noblemajesty.newsapplication.database.News
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_ABSTRACT
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_BYLINE
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_IMAGE
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_NEWS_TYPE
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_PUBLISHED_DATE
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_TITLE
import com.noblemajesty.newsapplication.database.NewsApplicationDataBase.Companion.COLUMN_ID
import com.noblemajesty.newsapplication.models.NYTimesResponse
import com.noblemajesty.newsapplication.models.Result
import com.noblemajesty.newsapplication.network.NYTimesRetrofitBuilder
import com.noblemajesty.newsapplication.network.NYTimesService
import com.noblemajesty.newsapplication.providers.NewsApplicationProvider.Companion.CONTENT_URI
import com.noblemajesty.newsapplication.utils.Constants.FOOD
import com.noblemajesty.newsapplication.utils.Constants.HOME_NEWS
import com.noblemajesty.newsapplication.utils.Constants.SPORTS
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception

class NewsActivityViewModel(application: Application): AndroidViewModel(application) {
    private var disposable: Disposable? = null
    private val sqliteDataBase = NewsApplicationDataBase(application.applicationContext)
    private var retrofitInstance = NYTimesRetrofitBuilder.getInstance()
            .createService(NYTimesService::class.java)
    val newsArray = MutableLiveData<ArrayList<News>>()
    var contentResolver: ContentResolver? = null

    fun getNews(newsType: String): Observable<NYTimesResponse>? {
        var observable: Observable<NYTimesResponse>? = null
        when (newsType) {
            HOME_NEWS -> { observable = retrofitInstance.getNews() }
            FOOD -> { observable = retrofitInstance.getFood() }
            SPORTS -> { observable = retrofitInstance.getSports() }
        }
        return observable
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
//                ?.subscribe(
//                        { apiResponse ->
//                            apiResponse.results.map { news ->
//                                val multimedia = news.multimedia
//                                val image = if (multimedia.isNotEmpty()) { multimedia[3].url } else { null }
//                                val contentValues = ContentValues().apply {
//                                    put(COLUMN_TITLE, news.title)
//                                    put(COLUMN_ABSTRACT, news.abstract)
//                                    put(COLUMN_BYLINE, news.byline)
//                                    put(COLUMN_PUBLISHED_DATE, news.published_date)
//                                    put(COLUMN_IMAGE, image)
//                                    put(COLUMN_NEWS_TYPE, newsType)
//                                }
//                                Log.e("NewsTYPE IS", newsType)
//                                saveNewsToDB(contentValues)
//                            }
//                            Log.e("Hereeeeeeee", "after map")
//                            result.addAll(fetchNewsFromDataBase(newsType))
//                            Log.e("Hereeeeeeee", "${result.size}")
//                         },
//                        {
//                            Log.e("Error from inside API", "$it")
//                            it.printStackTrace()
//                        }
//                )
    }

    private fun saveNewsToDB(response: List<Result>, newsType: String) {
        sqliteDataBase.saveNewsToDB(response, newsType)
//        response.map { news ->
//            val multimedia = news.multimedia
//            val image = if (multimedia.isNotEmpty()) { multimedia[3].url } else { null }
//            val contentValues = ContentValues().apply {
//                put(COLUMN_TITLE, news.title)
//                put(COLUMN_ABSTRACT, news.abstract)
//                put(COLUMN_BYLINE, news.byline)
//                put(COLUMN_PUBLISHED_DATE, news.published_date)
//                put(COLUMN_IMAGE, image)
//                put(COLUMN_NEWS_TYPE, newsType)
//            }
//            Log.e("NewsTYPE IS", newsType)
//            sqliteDataBase.writableDatabase.
//                    insert(NewsApplicationDataBase.TABLE_NAME, null, contentValues)
//        }
    }

    fun fetchNewsFromDataBase(newsType: String) {
        Log.e("called", "calledddddddddddd")
        disposable = Flowable.fromCallable<ArrayList<News>> {
            val allNews = ArrayList<News>()
//            val cursor = sqliteDataBase
//                    .readableDatabase.query(NewsApplicationDataBase.TABLE_NAME,
//                    NewsApplicationDataBase.TABLE_ROWS, "$COLUMN_NEWS_TYPE = ?",
//                    arrayOf(newsType), null, null, "$COLUMN_ID ASC")
            val cursor = contentResolver?.query(Uri.parse("$CONTENT_URI/$newsType"),
                    NewsApplicationDataBase.TABLE_ROWS, null, null, null
                    )

            cursor?.let {
                while (it.moveToNext()) {
                    val news = News().apply {
                        id = it.getInt(it.getColumnIndex(COLUMN_ID))
                        title = it.getString(it.getColumnIndex(COLUMN_TITLE))
                        abstract = it.getString(it.getColumnIndex(COLUMN_ABSTRACT))
                        byline = it.getString(it.getColumnIndex(COLUMN_BYLINE))
                        publishedDate = it.getString(it.getColumnIndex(COLUMN_PUBLISHED_DATE))
                        image = it.getString(it.getColumnIndex(COLUMN_IMAGE))
                    }
                    allNews.add(news)
                }
                it.close()
            }
            allNews
//        }.subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnNext { newsArray.value = it }
//                .flatMap { getNews(newsType)?.toFlowable(BackpressureStrategy.BUFFER) }
//                .doOnNext { saveNewsToDB(it.results, newsType) }
//                .map { saveNewsToDB(it.results, newsType) }
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe({
//
//                }, {})
//
//    }

        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    newsArray.value = it }
                .flatMap {
                    getNews(newsType)?.toFlowable(BackpressureStrategy.BUFFER)
                }
                .doOnNext {
//                    saveNewsToDB(it.results, newsType)
                    saveNewsToDBWithContentProvider(it.results, newsType)
                }
                .map {
                    it.results.map { it.toNews() }
//                    it.results.map { it.toContentValue() }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    newsArray.value = ArrayList(it)
                }, {})
    }

    private fun Result.toNews(): News {
        val stuff = this
        return News().apply {
            title = stuff.title
            byline = stuff.byline
            abstract = stuff.abstract
            publishedDate = stuff.published_date
            image = if (stuff.multimedia.isNotEmpty()) stuff.multimedia[3].url else null
        }
    }

    fun clearDisposable() = disposable?.dispose()

    private fun saveWithContentProvider(uri: Uri, contentValues: ContentValues) {
        Log.e("content resolver", "$contentResolver")
        try {
            val uri = contentResolver?.insert(uri, contentValues)
            Log.e("URI In ViewModel", "$uri")
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

//    private fun Result.toContentValue(newsType: String): ContentValues {
//        val item = this
//        return ContentValues().apply {
//            put(COLUMN_TITLE, title)
//            put(COLUMN_ABSTRACT, abstract)
//            put(COLUMN_BYLINE, byline)
//            put(COLUMN_PUBLISHED_DATE, published_date)
//            put(COLUMN_IMAGE, if (item.multimedia.isNotEmpty()) item.multimedia[3].url else null)
//            put(COLUMN_NEWS_TYPE, newsType)
//        }
//    }

    private fun saveNewsToDBWithContentProvider(data: List<Result>, newsType: String) {
        data.map {
            val contentValues = ContentValues().apply {
                put(COLUMN_TITLE, it.title)
                put(COLUMN_ABSTRACT, it.abstract)
                put(COLUMN_BYLINE, it.byline)
                put(COLUMN_PUBLISHED_DATE, it.published_date)
                put(COLUMN_IMAGE, if (it.multimedia.isNotEmpty()) { it.multimedia[3].url } else { null })
                put(COLUMN_NEWS_TYPE, newsType)
            }
            Log.e("saveWithContentProvider", "Hereeeeeeeeeeeeee")
            saveWithContentProvider( Uri.parse("$CONTENT_URI/$newsType"), contentValues)
        }
    }
}

