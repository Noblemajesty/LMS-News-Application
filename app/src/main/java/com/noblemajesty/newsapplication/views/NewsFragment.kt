package com.noblemajesty.newsapplication.views


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.noblemajesty.newsapplication.R
import com.noblemajesty.newsapplication.adapters.NewsAdapter
import com.noblemajesty.newsapplication.databinding.FragmentNewsBinding
import com.noblemajesty.newsapplication.utils.Constants.HOME_NEWS
import com.noblemajesty.newsapplication.utils.NetworkConnectivity
import com.noblemajesty.newsapplication.viewmodels.NewsActivityViewModel
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_news.*


/**
 * A simple [Fragment] subclass.
 *
 */
class NewsFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: FragmentNewsBinding
    private lateinit var viewModel: NewsActivityViewModel
    private val newsAdapter by lazy { NewsAdapter() }
    private var disposable: Disposable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(NewsActivityViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_news, container, false)
        initializeRecyclerView()
        getData()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newsSwipeRefresh.setOnRefreshListener(this)
        binding.display = true
    }

    override fun onRefresh() {
        onSwipeRefresh(binding.newsSwipeRefresh) { getData() }
    }

    private fun initializeRecyclerView() {
        binding.newsRecyclerView.apply {
            this.adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun getData() {
        // Show progressbar
        // Fetch data from db, if not empty hide progressbar
        // On background, fetch data from server and save response to db
        // on complete saving to db, update screen with the item (switch thread)
        if (!NetworkConnectivity(activity!!).isConnected()) {
            displaySnackbar(activity!!.newsActivity, "check you internet", ::getData)
        }
        viewModel.fetchNewsFromDataBase(HOME_NEWS)
        viewModel.newsArray.observe(this, Observer {
            it?.let { newsList ->
                if (!newsList.isEmpty()) {
                    newsAdapter.updateList(it)
                    binding.display = false
                }
                Log.e("News Size", "${newsList.size}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        viewModel.clearDisposable()
    }
}
