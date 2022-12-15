package com.example.firmwaredemoplunge.fragment


import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
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


    }

    override fun onResume() {
        super.onResume()
        wifiAction()



        getView()?.findViewById<ImageView>(R.id.ivAddDevice)?.setOnClickListener {
            /*createThingApiResponse(deviceName)*/
            connectToPlungeDialog(false)

        }

    }

    override fun onPause() {
        super.onPause()
//        requireContext().unregisterReceiver(wificonn)

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
                return
            }
            if (ssid.contains("Cold_Plunge_")) {
                deviceName = ssid
                Log.e("deviceWifi", deviceName.trim())

                Log.e("TAG", "wifiAction: $isConnectingWithPlungeAgain")
                if (isConnectingWithPlungeAgain == true) {
                    getWifiListResponse()
                } else {
                    connectToInternet()
                }

            } else {
                Log.e("deviceWifixyz", deviceName)
                createThingApiResponse(deviceName.trim())
            }

        } else {
//            connectToPlungeDialog(true)
        }

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun connectToInternet() {
        val dialog = Dialog(requireContext())
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.connect_to_internet_dialog)
        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            /*val edit = dialog.findViewById(R.id.etDeviceName) as EditText
            val text = edit.text.trim().toString()

            dialog.dismiss()
            val name = "Cold_Plunge_$text"*/
//            deviceName = name
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            dialog.dismiss()
        }
        dialog.show()
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    private fun createThingApiResponse(device: String) {
        lifecycleScope.launch {
            val result = createThingApi.createThing(device)
            if (result.isSuccessful) {
                val response = result.body()

                val certificatePem = response?.certificatePem
                val certificateKey = response?.privateKey

                val clientCert =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "/2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate.crt")
                clientCert.writeText(certificatePem!!)

                val clientKey =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "/private.key")
                clientKey.writeText(certificateKey!!)

                connectToPlungeDialog(true)
            } else {
                val message = result.errorBody()
                Log.e("error message", Gson().toJson(message))
                Log.e("error message", Gson().toJson(result.message()))

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
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_connect_plunge)
        dialog.setCancelable(false)

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            dialog.dismiss()
            if (isShowingDevice == true) {
                isConnectingWithPlungeAgain = true
            }
        }

        if (!isShowingDevice) {
            dialog.findViewById<Button>(R.id.btnShowDevices).visibility = View.GONE
            dialog.findViewById<TextView>(R.id.tvHeadingNo).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvHeading).visibility = View.GONE
        }

        dialog.findViewById<Button>(R.id.btnShowDevices).setOnClickListener {
            getWifiListResponse()
            dialog.dismiss()
        }

        dialog.show()
        val window: Window? = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun getWifiListResponse() {
        lifecycleScope.launch {
            val result = routerApi.getWifiList()

            if (result.isSuccessful) {
                val list = result.body() as ArrayList<WfiNameList.WfiNameListItem>
                wifiListDialog(list)
            } else {
                Toast.makeText(requireContext(), result.message(), Toast.LENGTH_LONG).show()
            }
        }

    }


    private fun wifiListDialog(list: ArrayList<WfiNameList.WfiNameListItem>) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_wifi_list)
        val rvList = dialog.findViewById<RecyclerView>(R.id.rvWifiList)
        wifiAdapter = WifiListAdapter(list, object : WifiListAdapter.IwifiConnect {
            override fun wifiItemClick(name: String) {
                connectDeviceDialog(name)
                dialog.dismiss()
            }
        })
        rvList.layoutManager = LinearLayoutManager(requireContext())
        rvList.adapter = wifiAdapter
        dialog.show()
        val window: Window? = dialog.window
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
                    val bundle = Bundle()
                    bundle.putString("SSID", deviceName)
                    /*val fragment = DeviceDetailFragment()
                fragment.arguments = bundle*/
                    navigate(DeviceDetailFragment.newInstance(bundle))
                } else {
                    Toast.makeText(requireContext(), result.message(), Toast.LENGTH_LONG).show()
                }
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }

    private fun readDownloadStream(fileName: String): InputStream? {
        var inputStream: InputStream? = null

        try {
            inputStream =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/$fileName").inputStream()
        } catch (e: IOException) {
            e.printStackTrace()
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
        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.fl_container, frm)
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