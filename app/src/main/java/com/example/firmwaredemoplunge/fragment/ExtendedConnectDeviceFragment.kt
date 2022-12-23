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
import android.os.*
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
import com.example.firmwaredemoplunge.data.model.CommonResponse
import com.example.firmwaredemoplunge.data.model.ConnectDeviceWithWifiReq
import com.example.firmwaredemoplunge.data.model.WfiNameList
import com.example.firmwaredemoplunge.data.util.DialogUtil
import com.example.firmwaredemoplunge.data.util.LocationService
import com.example.firmwaredemoplunge.data.util.MobileDataService
import com.example.firmwaredemoplunge.data.util.PrefManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ExtendedConnectDeviceFragment : Fragment() {

    private var isDeviceRegistered: Boolean?=null
    private var dialogConnectToInternet: Dialog? = null
    private var mobileData: Boolean? = null
    private var wifiManager: WifiManager? = null
    private var locationGps: Boolean? = null
    private val mqttAddress = "a1k3wmadt0ja18-ats.iot.us-east-1.amazonaws.com"


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

    private var staticIp = false

    private val connectApiRouter =
        RetrofitHelper.getInstance("http://10.11.4.64").create(RouterApi::class.java)


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


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
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
//                    prefManager.saveAddedDevice(deviceName)

                    val bundle = Bundle()
                    bundle.putString("SSID", deviceName)

                    if (dialogWifiList != null && dialogWifiList!!.isShowing) {
                        dialogWifiList?.dismiss()
                    }

                    switchNetwork()

                } else {
                    Toast.makeText(requireContext(), result.message(), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun switchNetwork() {
        if (isNetworkAvailable()) {
            if (dialogConnectToInternet != null && dialogConnectToInternet!!.isShowing) {
                dialogConnectToInternet?.dismiss()
            }
            createThingApiResponse(deviceName)
        } else {
            connectToInternet()
        }

    }

    private fun connectToInternet() {
        if (dialogConnectToInternet != null && dialogConnectToInternet!!.isShowing) {
            dialogConnectToInternet?.dismiss()
        }
        dialogConnectToInternet = Dialog(requireContext())
        dialogConnectToInternet?.setCancelable(false)
        dialogConnectToInternet?.setContentView(R.layout.connect_to_internet_dialog)
        dialogConnectToInternet?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            if (!isNetworkAvailable()) {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } else {
                switchNetwork()
            }
        }
        dialogConnectToInternet?.show()
        val window: Window? = dialogConnectToInternet?.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    private fun createThingApiResponse(device: String) {
        lifecycleScope.launch {
            mProgressDialog.show()
            try {
                val result = createThingApi.createThing(device)
                if (result.isSuccessful) {
                    mProgressDialog.dismiss()

                    if (result.code() == 200) {

                        try {
                            val response = result.body()

                            val certificatePem = response?.certificatePem
                            val certificateKey = response?.privateKey

                            val directory: String =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path +
                                        File.separator.toString() + "Plunge File"

                            var folder = File(directory)
                            var success = true
                            if (!folder.exists()) {
                                success = folder.mkdir()
                            }

                            var certFilePath: String

                            if (success) {
                                val certFile =
                                    "2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate.crt"
                                certFilePath = directory + File.separator.toString() + certFile
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to create  directory",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            val clientCert = File(certFilePath)
                            clientCert.writeText(certificatePem!!)

                            var privateFilePath: String

                            if (success) {
                                val certFile = "private.key"
                                privateFilePath = directory + File.separator.toString() + certFile
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to create  directory",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            val clientKey = File(privateFilePath)
                            clientKey.writeText(certificateKey!!)


                            Log.e("TAG", "createThingApiResponse: 1")

                            if (!isNetworkAvailable() && deviceName.contains("Cold_Plunge_") && wifiManager!!.isWifiEnabled) {
                                staticConnectApi()
                            } else {
                                isDeviceRegistered=true
                                isAgainPlunge(isDeviceRegistered!!)
                               /* connectToPlungeDialog(true,
                                    locationGps!!,
                                    dataState!!.isDataConnected)*/
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(requireContext(),
                                getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT).show()

                        }

                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(),
                                getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    mProgressDialog.dismiss()
                    val message = result.errorBody()
                    Log.e("error message", Gson().toJson(message))
                    Log.e("error message", Gson().toJson(result.message()))
                    Toast.makeText(requireContext(),
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mProgressDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(requireContext(),
                    getString(R.string.something_went_wrong),
                    Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun isAgainPlunge(isAgain:Boolean){
        if(isAgain){
            connectToPlungeDialog(true,
                locationGps!!,
                dataState!!.isDataConnected)
        }else{

        }
    }


    private fun readDownloadStream(fileName: String): InputStream? {
        var inputStream: InputStream? = null

        val directory: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path +
                    File.separator.toString() + "Plunge File"

        var folder = File(directory)
        var success = true
        if (!folder.exists()) {
            success = folder.mkdir()
        }
        if (success) {
            try {
                inputStream = File(directory + File.separator.toString() + fileName).inputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return inputStream
    }

    private fun readAssetFile(fileName: String): InputStream? {
        val assetManager = context?.assets
        var inputStream: InputStream? = null
        try {
            inputStream = assetManager?.open(fileName)
            return inputStream
        } catch (e: IOException) {
            Log.e("message: ", e.message!!)
            return inputStream
        }
    }


    @Throws(IOException::class)
    fun writeBytesToFile(`is`: InputStream, file: File?): MultipartBody.Part? {

        var fos: FileOutputStream? = null
        try {
            val data = ByteArray(2048)
            var nbread = 0
            fos = FileOutputStream(file)
            while (`is`.read(data).also { nbread = it } > -1) {
                fos.write(data, 0, nbread)
            }

            return MultipartBody.Part.createFormData("file",
                file?.name,
                file!!.asRequestBody("image/*".toMediaTypeOrNull()))

        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        } finally {
            if (fos != null) {
                fos.close()
            }
        }
    }


    private fun staticConnectApi() {
        try {
            lifecycleScope.launch {
                mProgressDialog.show()
                val result = connectApiRouter.getConnectedToIP()
                if (result.isSuccessful) {
                    mProgressDialog.show()
                    staticIp = true

                    registerDevice(deviceName, mqttAddress)
                    Log.e("Success", "jhjdshj")
                } else {
                    mProgressDialog.show()
                    staticIp = false
                    Log.e("Failure", "jhjdshj")

                    Handler(Looper.getMainLooper()).postDelayed({
                        staticConnectApi()
                    }, 50000)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to Connect", Toast.LENGTH_SHORT).show()
        }
    }


    private fun registerDevice(/*ssid:String,pass:String,*/device: String, mqtt: String) {
        val fileContent1 = readAssetFile("raw/AmazonRootCA1.pem")
        val fileContent2 =
            readDownloadStream("/2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate.crt")
        val fileContent3 = readDownloadStream("/private.key")

        val outputDir1 = context?.cacheDir // context being the Activity pointer
        val outputFile1 = File.createTempFile("AmazonRootCA1", ".pem", outputDir1)

        val outputDir2 = context?.cacheDir // context being the Activity pointer
        val outputFile2 =
            File.createTempFile("2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate",
                ".crt",
                outputDir2)

        val outputDir3 = context?.cacheDir // context being the Activity pointer
        val outputFile3 = File.createTempFile("private", ".key", outputDir3)

        val part1 = writeBytesToFile(fileContent1!!, outputFile1)
        val part2 = writeBytesToFile(fileContent2!!, outputFile2)
        val part3 = writeBytesToFile(fileContent3!!, outputFile3)

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            /*.addFormDataPart("wifi_ssid", ssid)
            .addFormDataPart("wifi_pass", pass)*/
            .addFormDataPart("device_id", device)
            .addFormDataPart("mqtt_addr", mqtt)
            .addFormDataPart("root_cert", outputFile1.name.toString(),
                MultipartBody.Builder().addPart(part1!!).build())
            .addFormDataPart("client_cert", outputFile2.name.toString(),
                MultipartBody.Builder().addPart(part2!!).build())
            .addFormDataPart("cert_key", outputFile3.name.toString(),
                MultipartBody.Builder().addPart(part3!!).build())
            .build()

        Log.d("requestBody", Gson().toJson(requestBody))
        mProgressDialog.show()
        lifecycleScope.launch {
            val result = connectApiRouter.getConnectedToIPStaticRes(requestBody)
            val data = result.body() as CommonResponse
            if (result.isSuccessful) {
                mProgressDialog.dismiss()
                var bundle = Bundle()
                bundle.putString("SSID", deviceName)
                navigate(DeviceDetailFragment.newInstance(bundle))

            } else {
                mProgressDialog.dismiss()
                Toast.makeText(requireContext(), "${data.msg}", Toast.LENGTH_SHORT).show()
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
