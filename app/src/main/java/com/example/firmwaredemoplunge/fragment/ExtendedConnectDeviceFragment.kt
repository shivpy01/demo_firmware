package com.example.firmwaredemoplunge.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firmwaredemoplunge.R
import com.example.firmwaredemoplunge.data.adapter.WifiListAdapter
import com.example.firmwaredemoplunge.data.api.RetrofitHelper
import com.example.firmwaredemoplunge.data.api.RouterApi
import com.example.firmwaredemoplunge.data.model.ConnectDeviceWithWifiReq
import com.example.firmwaredemoplunge.data.model.WfiNameList
import com.example.firmwaredemoplunge.data.util.DialogUtil
import com.example.firmwaredemoplunge.data.util.LocationService
import com.example.firmwaredemoplunge.data.util.MobileDataService
import com.example.firmwaredemoplunge.data.util.PrefManager
import kotlinx.coroutines.launch

class ExtendedConnectDeviceFragment : Fragment() {

    private var mobileData: Boolean? = null
    private var wifiManager: WifiManager? = null
    private var locationGps: Boolean? = null


    var readPhoneState: Boolean? = null
    private var dialogConnectToPlunge: Dialog? = null
    private var dialogTurnOffData: Dialog? = null
    private var dialogWifiList: Dialog? = null
    private lateinit var wifiAdapter: WifiListAdapter

    private var dataState: MobileDataService? = null
    private var location: LocationService? = null

    private var prefManager: PrefManager? = null
    private val mProgressDialog: Dialog by lazy { DialogUtil.progressBarLoader(requireContext()) }


    private var deviceName = ""

    private val routerApi =
        RetrofitHelper.getInstance("http://192.168.1.1").create(RouterApi::class.java)

    private val createThingApi =
        RetrofitHelper.getInstance("https://zvy5ofzzch.execute-api.us-east-1.amazonaws.com/default/")
            .create(RouterApi::class.java)

    private val deleteThingApi =
        RetrofitHelper.getInstance("https://vu0p9laxa5.execute-api.us-east-1.amazonaws.com/default/")
            .create(RouterApi::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* arguments?.let {
             param1 = it.getString("")
             param2 = it.getString("")
         }*/
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        return inflater.inflate(R.layout.fragment_connect_device, container, false)
    }


    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PrefManager(requireContext())
        val addedDevice = prefManager?.getAddedDevice()

        dataState = MobileDataService()
        activity?.registerReceiver(dataState, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))


        location = LocationService()
        activity?.registerReceiver(location, IntentFilter(LocationManager.GPS_PROVIDER))

        readPhoneState = (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this.requireContext(), Manifest.permission.READ_PHONE_STATE
        ))

        mobileData = getMobileDataAndLocationStatus(readPhoneState!!)


        locationGps = isLocationEnabled(requireContext())

        if (!isLocationEnabled(requireContext())) {
            dialogTurnOffMobileData()
        }



        if (!addedDevice.isNullOrEmpty()) {

            view.findViewById<TextView>(R.id.tv_ConnectedDeviceName).text = addedDevice

            view.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                .setOnClickListener {
                    val bundle = Bundle()
                    bundle.putString("SSID", addedDevice)
                    navigate(DeviceDetailFragment.newInstance(bundle))
                }

            view.findViewById<Button>(R.id.bt_RemoveConnectedDevice)
                .setOnClickListener {

                    deviceName = ""

                    prefManager?.clearAllData()

                    view.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                        .visibility = View.GONE
                    it.visibility = View.GONE
                }

            view.findViewById<TextView>(R.id.tvNoDeviceConnected).visibility = View.VISIBLE

        } else {

            view.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                .visibility = View.GONE

            view.findViewById<Button>(R.id.bt_RemoveConnectedDevice).visibility =
                View.GONE
            view.findViewById<TextView>(R.id.tvNoDeviceConnected).visibility = View.GONE

            getView()?.findViewById<ImageView>(R.id.ivAddDevice)?.setOnClickListener {
                //isPlusButtonAlreadyClicked = true
                wifiAction()
                Log.e("TAG", "createThingApiResponse: 5")
                //connectToPlungeDialog(false)
            }

        }
    }


    private fun dialogTurnOffMobileData() {
        dialogTurnOffData = Dialog(requireContext())
        dialogTurnOffData?.setContentView(R.layout.dialog_turn_off_data)
        dialogTurnOffData?.setCancelable(false)
        dialogTurnOffData?.findViewById<TextView>(R.id.btnOk)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        dialogTurnOffData?.show()
        val window: Window? = dialogTurnOffData?.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    fun isLocationEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lm: LocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return lm.isLocationEnabled
        } else {
            // This was deprecated in API 28
            val mode =
                Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF)
            return (mode != Settings.Secure.LOCATION_MODE_OFF)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getMobileDataAndLocationStatus(phoneStatus: Boolean): Boolean {
        val tm = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var isenabled = false
        if (phoneStatus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isenabled = tm.isDataEnabled
            } else {
                isenabled =
                    (tm.simState == TelephonyManager.SIM_STATE_READY && tm.dataState != TelephonyManager.DATA_DISCONNECTED)
            }
            Toast.makeText(requireContext(),
                "Mobile Data $isenabled",
                Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(),
                "Please give Phone Status Permission",
                Toast.LENGTH_LONG).show()
        }
        return isenabled
    }

    private fun wifiAction() {

        wifiManager =
            requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager?.isWifiEnabled!!) {
            val wifiInfo = wifiManager?.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "")
            Log.e("deviceSSID", ssid!!)

            if (ssid == "<unknown ssid>") {
                Log.e("in else condition", "djajshdfjsdhfcjk")

                connectToPlungeDialog(true, locationGps!!, dataState!!.isDataConnected)
                return
            }

            if (ssid.contains("Cold_Plunge_")) {
                Log.e("deviceWifi", deviceName.trim())
                deviceName = ssid

            } else {
                Log.e("deviceWifixyz", deviceName)
                connectToPlungeDialog(true, locationGps!!, dataState!!.isDataConnected)
            }
        } else {
            connectToPlungeDialog(true, locationGps!!, dataState!!.isDataConnected)
        }

    }


    private fun connectToPlungeDialog(
        isShowingDevice: Boolean,
        location: Boolean,
        mobileData: Boolean,
    ) {

        if (dialogConnectToPlunge != null && dialogConnectToPlunge!!.isShowing) {
            dialogConnectToPlunge?.dismiss()
        }

        dialogConnectToPlunge = Dialog(requireContext())
        dialogConnectToPlunge?.setContentView(R.layout.dialog_connect_plunge)
        dialogConnectToPlunge?.setCancelable(false)


        if (location == true) {
            dialogConnectToPlunge?.findViewById<CheckBox>(R.id.cbCheckLocation)?.isChecked = true
        } else {
            dialogConnectToPlunge?.findViewById<CheckBox>(R.id.cbCheckLocation)?.isChecked = false
        }


        if (mobileData == false) {
            dialogConnectToPlunge?.findViewById<CheckBox>(R.id.cbCheckMobile)?.isChecked = true
        } else {
            dialogConnectToPlunge?.findViewById<CheckBox>(R.id.cbCheckMobile)?.isChecked = false
        }

        if (location == true && mobileData == false) {
            dialogConnectToPlunge?.findViewById<Button>(R.id.btnOk)?.isEnabled = true
        } else {
            dialogConnectToPlunge?.findViewById<Button>(R.id.btnOk)?.isEnabled = false
        }

        dialogConnectToPlunge?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {

            if (wifiManager?.isWifiEnabled!!) {
                dialogConnectToPlunge?.dismiss()
                getWifiListResponse()
            } else {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            /*if (isShowingDevice == true) {
                isConnectingWithPlungeAgain = true
            }*/
        }

        if (!isShowingDevice) {
            dialogConnectToPlunge?.findViewById<Button>(R.id.btnShowDevices)?.visibility = View.GONE
            dialogConnectToPlunge?.findViewById<TextView>(R.id.tvHeadingNo)?.visibility =
                View.VISIBLE
            dialogConnectToPlunge?.findViewById<TextView>(R.id.tvHeading)?.visibility = View.GONE
        }



        dialogConnectToPlunge?.findViewById<Button>(R.id.btnShowDevices)?.setOnClickListener {
//            getWifiListResponse()
            dialogConnectToPlunge?.dismiss()
        }

        dialogConnectToPlunge?.show()
        val window: Window? = dialogConnectToPlunge?.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    private fun connectDeviceDialog(ssid: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_connect_device)
        dialog.findViewById<EditText>(R.id.etSsid).setText(ssid)

        dialog.findViewById<Button>(R.id.submit).setOnClickListener {
            request(
                dialog.findViewById<EditText>(R.id.etSsid).text.toString(),
                dialog.findViewById<EditText>(R.id.etPass).text.toString(),
            )
            dialog.dismiss()
        }

        dialog.show()
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    private fun getWifiListResponse() {

        lifecycleScope.launch {
            mProgressDialog.show()

            try {

                val result = routerApi.getWifiList()

                if (result.isSuccessful) {
                    mProgressDialog.dismiss()
                    val list = result.body() as ArrayList<WfiNameList.WfiNameListItem>
                    wifiListDialog(list)

                } else {
                    mProgressDialog.dismiss()
                    Toast.makeText(requireContext(),
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {

                mProgressDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(requireContext(),
                    getString(R.string.something_went_wrong_while_getting_wifi_list),
                    Toast.LENGTH_SHORT).show()

            }
        }

    }


    private fun wifiListDialog(list: ArrayList<WfiNameList.WfiNameListItem>) {
        if (dialogWifiList != null && dialogWifiList!!.isShowing) {
            dialogWifiList?.dismiss()
        }
        dialogWifiList = Dialog(requireContext())
        dialogWifiList?.setContentView(R.layout.dialog_wifi_list)
        val rvList = dialogWifiList?.findViewById<RecyclerView>(R.id.rvWifiList)
        wifiAdapter = WifiListAdapter(list, object : WifiListAdapter.IwifiConnect {
            override fun wifiItemClick(name: String) {
                connectDeviceDialog(name)
                dialogWifiList?.dismiss()
            }
        })
        rvList?.layoutManager = LinearLayoutManager(requireContext())
        rvList?.adapter = wifiAdapter
        dialogWifiList?.show()
        val window: Window? = dialogWifiList?.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    override fun onResume() {
        super.onResume()


    }


    private fun request(ssid: String, password: String) {

        val connectWithWifiReq = ConnectDeviceWithWifiReq().apply {
            this.wifi_ssid = ssid
            this.wifi_pass = password
        }

        lifecycleScope.launch {

            try {
                val result = routerApi.connectDeviceWithWifi(connectWithWifiReq)
                if (result.isSuccessful) {

                    val prefManager = PrefManager(requireContext())
                    prefManager.saveAddedDevice(deviceName)

                    val bundle = Bundle()
                    bundle.putString("SSID", deviceName)
                    /*val fragment = DeviceDetailFragment()
                      fragment.arguments = bundle*/

                    if (dialogWifiList != null && dialogWifiList!!.isShowing) {
                        dialogWifiList?.dismiss()
                    }


                    navigate(DeviceDetailFragment.newInstance(bundle))


                } else {
                    Toast.makeText(requireContext(), result.message(), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun navigate(frm: Fragment) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.replace(R.id.fl_container, frm)
            ?.addToBackStack("DeviceDetailFragment")
            ?.commit()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ExtendedConnectDeviceFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExtendedConnectDeviceFragment().apply {
                arguments = Bundle().apply {
                    putString("", param1)
                    putString("", param2)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(dataState)
        activity?.unregisterReceiver(location)
    }

}
