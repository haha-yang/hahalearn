package com.haha.binding

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.kmp.SharedStudy

class BindingAdapterViewModel(application: Application) : BaseViewModel(application) {

    val greeting = ObservableField<Pair<String, String>>()
    val detailVisible = ObservableField(false)
    val fibonacciHint = ObservableField<Pair<String, String>>()

    private val _tags = MutableLiveData<List<TagItem>>(emptyList())
    val tags: LiveData<List<TagItem>> = _tags

    fun loadDemoData() {
        greeting.set("平台" to SharedStudy.greeting().substringAfter("当前平台 "))
        fibonacciHint.set("斐波那契(10)" to SharedStudy.fibonacci(10).toString())
        _tags.value = listOf(
            TagItem("ViewBinding", R.color.teal_200),
            TagItem("BindingAdapter", R.color.purple_500),
            TagItem("DataBinding", R.color.orange),
        )
    }

    fun toggleDetailVisible() {
        detailVisible.set(detailVisible.get() != true)
    }
}

data class TagItem(
    val name: String,
    val colorRes: Int,
)
