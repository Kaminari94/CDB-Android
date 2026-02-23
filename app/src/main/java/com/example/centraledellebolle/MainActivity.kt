package com.example.centraledellebolle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.centraledellebolle.network.RetrofitInstance
import com.example.centraledellebolle.data.HealthRepository
import com.example.centraledellebolle.ui.health.HealthScreen
import com.example.centraledellebolle.ui.health.HealthViewModel
import com.example.centraledellebolle.ui.health.HealthViewModelFactory
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val repo = HealthRepository(RetrofitInstance.api)
            val factory = HealthViewModelFactory(repo)
            val vm: HealthViewModel = viewModel(factory = factory)

            HealthScreen(vm = vm)
        }
    }
}