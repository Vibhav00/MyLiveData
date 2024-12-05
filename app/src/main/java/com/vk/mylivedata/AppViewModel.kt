package com.vk.mylivedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vk.mylivedata.ld.MyCustomLd
import com.vk.mylivedata.ld.MyCustomMLD
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AppViewModel : ViewModel() {

    private var _testLiveData:MyCustomMLD<Int> = MyCustomMLD(34)
    var  testLiveData: MyCustomLd<Int> = _testLiveData


    private var _ld:MutableLiveData<Int> = MutableLiveData(34)
    var  ld: LiveData<Int> = _ld
    init {
        setLiveData()
    }

    private fun setLiveData(){
        viewModelScope.launch {
            while (true){
                delay(200)
                _testLiveData.postValue(generateRandomNumber())
                _ld.postValue(generateRandomNumber())
            }
        }

    }

    private fun generateRandomNumber(): Int {
        return (1..100).random()
    }

}

