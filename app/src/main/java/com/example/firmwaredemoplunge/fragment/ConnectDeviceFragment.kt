package com.example.firmwaredemoplunge.fragment


import android.app.Dialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    val routerApi = RetrofitHelper.getInstance("http://192.168.1.1").create(RouterApi::class.java)
    val createThingApi =
        RetrofitHelper.getInstance("https://zvy5ofzzch.execute-api.us-east-1.amazonaws.com/default/")
            .create(RouterApi::class.java)

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
        return inflater.inflate(R.layout.fragment_connect_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createThingApiResponse()

        view.findViewById<ImageView>(R.id.ivAddDevice).setOnClickListener {
            getWifiListResponse()
        }

    }

    private fun createThingApiResponse() {
        lifecycleScope.launch {
            val result = createThingApi.createThing("TA_001_1234591")


            val response = result.body()
            val certificatePem = response?.certificatePem
            val certificateKey = response?.privateKey

            val downloadFolder =
                requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)


            val clientCert =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate.crt")

            clientCert.writeText(certificatePem!!)


            val clientKey =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/private.key")
            clientKey.writeText(certificateKey!!)

        }
    }


    private fun getWifiListResponse() {
        lifecycleScope.launch {
            val result = routerApi.getWifiList()

            if (result != null) {
                val list = result.body() as ArrayList<WfiNameList.WfiNameListItem>
                wifiListDialog(list)
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
            val result = request(dialog.findViewById<EditText>(R.id.etSsid).text.toString(),
                dialog.findViewById<EditText>(R.id.etPass).text.toString(),
                "plunge_demo",
                "a1k3wmadt0ja18-ats.iot.us-east-1.amazonaws.com")
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
            routerApi.getRouterResponse(
                requestBody
            )


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
                RequestBody.create("image/*".toMediaTypeOrNull(), file!!))

        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        } finally {
            if (fos != null) {
                fos.close()
            }
        }
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