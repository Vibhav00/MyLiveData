package com.vk.mylivedata

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vk.mylivedata.databinding.ActivitySecondBinding

class SecondActivity : AppCompatActivity() {
    private lateinit var activitySecondBinding: ActivitySecondBinding
    private lateinit var appViewModel:AppViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activitySecondBinding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(activitySecondBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        appViewModel = AppViewModelStore.getAppScopedViewModel()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appViewModel.testLiveData.observe(this){

            activitySecondBinding.mainTv.text = it.toString()
        }
        appViewModel.ld.observe(this){
            activitySecondBinding.secondTv.text = it.toString()
        }

        activitySecondBinding.dismissDialog.setOnClickListener {
          finish()
        }
    }
}