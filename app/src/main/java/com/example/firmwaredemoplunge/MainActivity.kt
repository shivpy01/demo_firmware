package com.example.firmwaredemoplunge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.firmwaredemoplunge.fragment.ConnectDeviceFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigate(ConnectDeviceFragment())


        //  val routerApi = RetrofitHelper.getInstance().create(RouterApi::class.java)

//        GlobalScope.launch {
//            val result = routerApi.getRouterResponse()
//            if (result != null)
//            // Checking the results
//                Log.e("shivam: ", result.body().toString())
////            navigate(FormRouterFragment())
//
//
//        }

    }

    private fun navigate(frm: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fl_container, frm).commit()
    }
}