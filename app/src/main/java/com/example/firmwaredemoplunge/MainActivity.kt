package com.example.firmwaredemoplunge

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.firmwaredemoplunge.fragment.ConnectDeviceFragment
import java.security.Permission

class MainActivity : AppCompatActivity() {

    val listPermission = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main)



    }

    private fun navigate(frm: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_container, frm)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {

        val fragmentCount = supportFragmentManager.backStackEntryCount
        if (fragmentCount == 1) {
            askToExit()
        } else {
            onBackPressedDispatcher.onBackPressed()

        }
    }

    private fun askToExit() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.are_you_sure_you_want_to_exit))
            .setPositiveButton(getString(R.string.yes), object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                    finishAffinity()
                }

            })
            .setNegativeButton(getString(R.string.no), object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                }

            })
            .show()
    }


    override fun onResume() {
        super.onResume()
        checkForRequiredPermission()

    }

    private fun checkForRequiredPermission(){


        val networkState = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_NETWORK_STATE
                ))

        val wifiState = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_WIFI_STATE
                ))

        val changeWifiState =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.CHANGE_WIFI_STATE
            ))

        val readIntenalStorage =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ))

        val writeExternalStorage =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))

        val manageExternalStorage =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ))

        val accessCoarseLocation =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ))

        val accessFineLocation =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ))

        if(networkState && wifiState && changeWifiState
            && readIntenalStorage && writeExternalStorage
            && manageExternalStorage && accessCoarseLocation
            && accessFineLocation){

            navigate(ConnectDeviceFragment())

            return
        }

        if(!networkState){
            listPermission.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }
        if(!wifiState){
            listPermission.add(Manifest.permission.ACCESS_WIFI_STATE)
        }
        if(!changeWifiState){
            listPermission.add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        if(!readIntenalStorage){
            listPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(!writeExternalStorage){
            listPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!manageExternalStorage){
            listPermission.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }
        if(!accessCoarseLocation){
            listPermission.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if(!accessFineLocation){
            listPermission.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(listPermission.toTypedArray() ,101)
        }else{
            ActivityCompat.requestPermissions(
                this@MainActivity,
                listPermission.toTypedArray(),101)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        grantResults.forEachIndexed { index, s ->
            if(grantResults[index] != PackageManager.PERMISSION_GRANTED){
                getString(R.string.all_permission_is_required)
                return
            }
        }

        navigate(ConnectDeviceFragment())
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }
}