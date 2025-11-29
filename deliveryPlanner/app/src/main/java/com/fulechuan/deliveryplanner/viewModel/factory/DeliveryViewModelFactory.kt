package com.fulechuan.deliveryplanner.viewModel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fulechuan.deliveryplanner.viewModel.DeliveryViewModel

class DeliveryViewModelFactory(private val application: Application): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeliveryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
