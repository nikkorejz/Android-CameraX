package ru.nikkorejz.camerax.Utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    val isPermissionGranted = MutableLiveData(false)

}