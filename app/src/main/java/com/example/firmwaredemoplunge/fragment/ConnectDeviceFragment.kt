package com.example.firmwaredemoplunge.fragment


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firmwaredemoplunge.R
import com.example.firmwaredemoplunge.data.adapter.WifiListAdapter
import com.example.firmwaredemoplunge.data.api.RetrofitHelper
import com.example.firmwaredemoplunge.data.api.RouterApi
import com.example.firmwaredemoplunge.data.model.WfiNameList
import com.example.firmwaredemoplunge.data.util.DialogUtil
import com.example.firmwaredemoplunge.data.util.PrefManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ConnectDeviceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */


class ConnectDeviceFragment : Fragment() {

    private var dialogWifiList: Dialog? = null
    private var dialogConnectToInternet: Dialog? = null
    private var dialogConnectToPlunge: Dialog? = null
    private var prefManager: PrefManager? = null
    private val mProgressDialog: Dialog by lazy { DialogUtil.progressBarLoader(requireContext()) }

    private var isPlungeAlreadyClicked = false
    private var isConnectingWithPlungeAgain = false

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var deviceName = ""
    var isWifiEnabled: Boolean? = false
    private val mqttAddress = "a1k3wmadt0ja18-ats.iot.us-east-1.amazonaws.com"
    val routerApi = RetrofitHelper.getInstance("http://192.168.1.1").create(RouterApi::class.java)
    private val createThingApi =
        RetrofitHelper.getInstance("https://zvy5ofzzch.execute-api.us-east-1.amazonaws.com/default/")
            .create(RouterApi::class.java)

    val intentFilter = IntentFilter()

    val wificonn = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {

                } else {

                }
            }
        }
    }
    private lateinit var wifiAdapter: WifiListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        requireContext().registerReceiver(wificonn, intentFilter)
        return inflater.inflate(R.layout.fragment_connect_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PrefManager(requireContext())
        val addedDevice = prefManager?.getAddedDevice()

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

                    isPlungeAlreadyClicked = false
                    isConnectingWithPlungeAgain = false
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

        }

        getView()?.findViewById<ImageView>(R.id.ivAddDevice)?.setOnClickListener {
            //isPlusButtonAlreadyClicked = true
            wifiAction()
            Log.e("TAG", "createThingApiResponse: 5")
            //connectToPlungeDialog(false)
        }

    }


    override fun onResume() {
        super.onResume()

        if(isLocationEnabled(requireContext()) == false){

            startActivity( Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        val addedDevice = prefManager?.getAddedDevice()
        if (!addedDevice.isNullOrEmpty()) {

            view?.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                ?.text = addedDevice

            view?.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                ?.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putString("SSID", addedDevice)
                    navigate(DeviceDetailFragment.newInstance(bundle))
                }

            view?.findViewById<Button>(R.id.bt_RemoveConnectedDevice)
                ?.setOnClickListener {

                    isPlungeAlreadyClicked = false
                    isConnectingWithPlungeAgain = false
                    deviceName = ""

                    prefManager?.clearAllData()

                    view?.findViewById<TextView>(R.id.tv_ConnectedDeviceName)
                        ?.visibility = View.GONE
                    it.visibility = View.GONE
                }

            view?.findViewById<TextView>(R.id.tvNoDeviceConnected)
                ?.visibility = View.GONE

        } else {
            view?.findViewById<TextView>(R.id.tv_ConnectedDeviceName)?.visibility = View.GONE

            view?.findViewById<Button>(R.id.bt_RemoveConnectedDevice)?.visibility = View.GONE

            view?.findViewById<TextView>(R.id.tvNoDeviceConnected)?.visibility = View.VISIBLE

            Log.e("isPlungeAlreadyClicked $isPlungeAlreadyClicked", "")

            wifiAction()

        }




        /*if (isPlusButtonAlreadyClicked == true) {
            wifiAction()
        }*/


    }

    fun isLocationEnabled(context : Context) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            val lm : LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return lm.isLocationEnabled
        } else {
            // This was deprecated in API 28
            val mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF)
            return (mode != Settings.Secure.LOCATION_MODE_OFF)
        }
    }


    private fun wifiAction() {
        val wifiManager =
            requireContext().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid.replace("\"", "")
            Log.e("deviceSSID", ssid)
            if (ssid == "<unknown ssid>") {
                Log.e("in else condition", "djajshdfjsdhfcjk")
                Log.e("TAG", "createThingApiResponse: 4")
                connectToPlungeDialog(false)
                return
            }


            if (ssid.contains("Cold_Plunge_")) {
                Log.e("deviceWifi", deviceName.trim())

                deviceName = ssid
                isPlungeAlreadyClicked = true

                Log.e("TAG", "wifiAction: $isConnectingWithPlungeAgain")
                if (isConnectingWithPlungeAgain) {
                    getWifiListResponse()
                } else {
                    connectToInternet()
                }

            } else {
                Log.e("deviceWifixyz", deviceName)
                Log.e("isPlungeAlreadyClicked " + isPlungeAlreadyClicked, "")

                if (isPlungeAlreadyClicked == true) {
                    if (!deviceName.isNullOrEmpty() && isNetworkAvailable()) {
                        createThingApiResponse(deviceName.trim())
                    } else {
                        Toast.makeText(requireContext(),
                            getString(R.string.no_internet),
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("TAG", "createThingApiResponse: 3")
                    connectToPlungeDialog(false)
                }

            }

        } else {
            Log.e("TAG", "createThingApiResponse: 2")

            connectToPlungeDialog(false)
        }

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun connectToInternet() {
        if (dialogConnectToInternet != null && dialogConnectToInternet!!.isShowing) {
            dialogConnectToInternet?.dismiss()
        }
        dialogConnectToInternet = Dialog(requireContext())
        dialogConnectToInternet?.setCancelable(false)
        dialogConnectToInternet?.setContentView(R.layout.connect_to_internet_dialog)
        dialogConnectToInternet?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            dialogConnectToInternet?.dismiss()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
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
                            connectToPlungeDialog(true)

                        } catch (e: Exception) {
                            isPlungeAlreadyClicked = false
                            e.printStackTrace()
                            Toast.makeText(requireContext(),
                                getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT).show()

                        }

                    } else {
                        isPlungeAlreadyClicked = false

                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(),
                                getString(R.string.something_went_wrong),
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    isPlungeAlreadyClicked = false
                    mProgressDialog.dismiss()
                    val message = result.errorBody()
                    Log.e("error message", Gson().toJson(message))
                    Log.e("error message", Gson().toJson(result.message()))
                    Toast.makeText(requireContext(),
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {

                isPlungeAlreadyClicked = false

                mProgressDialog.dismiss()
                e.printStackTrace()
                Toast.makeText(requireContext(),
                    getString(R.string.something_went_wrong),
                    Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun addDeviceDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_device)
        dialog.findViewById<Button>(R.id.submit).setOnClickListener {
            val edit = dialog.findViewById(R.id.etDeviceName) as EditText
            val text = edit.text.trim().toString()

            dialog.dismiss()
            val name = "Cold_Plunge_$text"
//            deviceName = name
            createThingApiResponse(deviceName)
        }
        dialog.show()
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun connectToPlungeDialog(isShowingDevice: Boolean) {

        if (dialogConnectToPlunge != null && dialogConnectToPlunge!!.isShowing) {
            dialogConnectToPlunge?.dismiss()
        }

        dialogConnectToPlunge = Dialog(requireContext())
        dialogConnectToPlunge?.setContentView(R.layout.dialog_connect_plunge)
        dialogConnectToPlunge?.setCancelable(false)

        dialogConnectToPlunge?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            dialogConnectToPlunge?.dismiss()
            if (isShowingDevice == true) {
                isConnectingWithPlungeAgain = true
            }
        }

        if (!isShowingDevice) {
            dialogConnectToPlunge?.findViewById<Button>(R.id.btnShowDevices)?.visibility = View.GONE
            dialogConnectToPlunge?.findViewById<TextView>(R.id.tvHeadingNo)?.visibility =
                View.VISIBLE
            dialogConnectToPlunge?.findViewById<TextView>(R.id.tvHeading)?.visibility = View.GONE
        }

        dialogConnectToPlunge?.findViewById<Button>(R.id.btnShowDevices)?.setOnClickListener {
            getWifiListResponse()
            dialogConnectToPlunge?.dismiss()
        }

        dialogConnectToPlunge?.show()
        val window: Window? = dialogConnectToPlunge?.window
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


    private fun connectDeviceDialog(ssid: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_connect_device)
        dialog.findViewById<EditText>(R.id.etSsid).setText(ssid)

        dialog.findViewById<Button>(R.id.submit).setOnClickListener {
            request(dialog.findViewById<EditText>(R.id.etSsid).text.toString(),
                dialog.findViewById<EditText>(R.id.etPass).text.toString(),
                deviceName,
                mqttAddress)
            dialog.dismiss()
        }

        dialog.show()
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun request(ssid: String, pass: String, device: String, mqtt: String) {

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
            .addFormDataPart("wifi_ssid", ssid)
            .addFormDataPart("wifi_pass", pass)
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

        lifecycleScope.launch {

            try {
                val result = routerApi.getRouterResponse(
                    requestBody
                )
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

                    if (dialogConnectToInternet != null && dialogConnectToInternet!!.isShowing) {
                        dialogConnectToInternet?.dismiss()
                    }

                    if (dialogConnectToPlunge != null && dialogConnectToPlunge!!.isShowing) {
                        dialogConnectToPlunge?.dismiss()
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
         * @return A new instance of fragment ConnectDeviceFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConnectDeviceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}