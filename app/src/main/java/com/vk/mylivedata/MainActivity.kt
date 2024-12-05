package com.vk.mylivedata

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vk.mylivedata.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var appViewModel:AppViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityMainBinding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        appViewModel = AppViewModelStore.getAppScopedViewModel()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appViewModel.testLiveData.observe(this){
            activityMainBinding.mainTv.text = it.toString()
        }
        appViewModel.ld.observe(this){
            activityMainBinding.secondTv.text = it.toString()
        }

        activityMainBinding.createDialog.setOnClickListener {
             startActivity(Intent(this,SecondActivity::class.java))
        }
    }
}