package com.example.firmwaredemoplunge.fragment

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.firmwaredemoplunge.R
import com.example.firmwaredemoplunge.data.api.RetrofitHelper
import com.example.firmwaredemoplunge.data.api.RouterApi
import com.example.firmwaredemoplunge.data.model.ReqRouter
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class FormRouterFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    val routerApi = RetrofitHelper.getInstance("http://192.168.1.1").create(RouterApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("")
            param2 = it.getString("")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_form_router, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<Button>(R.id.submit).setOnClickListener {
            try {

                request("TA_001_F87EF8",
                    "PaMDk55y",
                    "plunge_demo",
                    "a1k3wmadt0ja18-ats.iot.us-east-1.amazonaws.com")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun request(ssid: String, pass: String, device: String, mqtt: String) {

        val fileContent1 = readAssetFile("raw/AmazonRootCA1.pem")
        val fileContent2 =
            readAssetFile("raw/2fbee0846ae15022e0b5d25be29f9563de1b1ac8ca1c7eb0e7aa8ce97c8e25be-certificate.crt")
        val fileContent3 = readAssetFile("raw/private.key")

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
            .addFormDataPart("root_cert",outputFile1.name.toString(),MultipartBody.Builder().addPart(part1!!).build())
            .addFormDataPart("client_cert",outputFile2.name.toString(),MultipartBody.Builder().addPart(part2!!).build())
            .addFormDataPart("cert_key",outputFile3.name.toString(),MultipartBody.Builder().addPart(part3!!).build())
            .build()

        Log.d("requestBody", Gson().toJson(requestBody))

        lifecycleScope.launch {
            val result = routerApi.getRouterResponse(
                requestBody
            )
            if (result != null)
                Log.e("shivam: ", result.body().toString())
        }

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

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FormRouterFragment().apply {
                arguments = Bundle().apply {
                    putString("", param1)
                    putString("", param2)
                }
            }
    }
}