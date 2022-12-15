package com.example.firmwaredemoplunge.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.firmwaredemoplunge.R
import com.example.firmwaredemoplunge.data.api.RetrofitHelper
import com.example.firmwaredemoplunge.data.api.RouterApi
import com.example.firmwaredemoplunge.data.model.PlungeLightModel
import com.example.firmwaredemoplunge.data.model.PumpDetailModal
import com.example.firmwaredemoplunge.data.model.TemperartureDetailModel
import com.example.firmwaredemoplunge.data.util.CommonUtil
import com.example.firmwaredemoplunge.data.util.DialogUtil
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class DeviceDetailFragment : Fragment() {
    private var device: String? = null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val mProgressDialog: Dialog by lazy { DialogUtil.progressBarLoader(requireContext()) }

    val detailApi =
        RetrofitHelper
            .getInstance("https://uhem5myyla.execute-api.us-east-1.amazonaws.com/default/")
            .create(RouterApi::class.java)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_device_detail, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            device = arguments?.getString("SSID")
            Log.e("deviceNameDeiceFragment", "onCreate: $device")
        }

        view.findViewById<TextView>(R.id.tvDeviceName)?.text = device

        view.findViewById<SwitchCompat>(R.id.sbLight)
            .setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {

                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getLightResponse(plungeLight(1))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
                    }

                } else {

                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getLightResponse(plungeLight(0))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()

                    }
                }
            }

        view.findViewById<SwitchCompat>(R.id.sbPump)
            .setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getResponse(plungePump(device!!, 1, 50))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getResponse(plungePump(device!!, 0, 50))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        view.findViewById<SeekBar>(R.id.seekPump)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    //
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    var value = 0
                    if (seekBar?.progress == null) {
                        value = 0
                    } else {
                        value = seekBar.progress
                    }



                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getResponse(plungePump(device!!, 1, value))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
                    }
                }

            })


        view.findViewById<SeekBar>(R.id.seekTemp)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    //
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    var value = 0
                    if (seekBar?.progress == null) {
                        value = 0
                    } else {
                        value = seekBar.progress
                    }
                    if (CommonUtil.isNetworkAvailable(requireContext())) {
                        getTempResponse(plungeTemp(value))
                    } else {
                        Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show()
                    }

                }

            })


    }

    private fun getResponse(model: Any) {
        lifecycleScope.launch {
            mProgressDialog.show()

            try {
                val getRespo =
                    device?.let {
                        detailApi.getPlungeDetailResponse(model as PumpDetailModal)
                    }
                Toast.makeText(context, "name: " + device, Toast.LENGTH_LONG).show()


                if (getRespo?.isSuccessful == true) {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "if", Toast.LENGTH_LONG).show()


                } else {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "else", Toast.LENGTH_LONG).show()

                }
            } catch (e: Exception) {

                mProgressDialog.hide()

                e.printStackTrace()
                Toast.makeText(context, getString(R.string.something_went_wrong), Toast.LENGTH_LONG)
                    .show()

            }
        }
    }


    private fun getLightResponse(model: Any) {
        lifecycleScope.launch {
            mProgressDialog.show()

            try {
                val getRespo =
                    device?.let {
                        detailApi.getPlungeLightDetailResponse(model as PlungeLightModel)
                    }
                Toast.makeText(context, "name: " + device, Toast.LENGTH_LONG).show()


                if (getRespo?.isSuccessful == true) {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "if", Toast.LENGTH_LONG).show()


                } else {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "else", Toast.LENGTH_LONG).show()

                }
            } catch (e: Exception) {

                mProgressDialog.hide()

                e.printStackTrace()
                Toast.makeText(context, getString(R.string.something_went_wrong), Toast.LENGTH_LONG)
                    .show()

            }
        }
    }

    private fun getTempResponse(model: Any) {
        lifecycleScope.launch {
            mProgressDialog.show()

            try {
                val getRespo =
                    device?.let {
                        detailApi.getPlungeTempDetailResponse(model as TemperartureDetailModel)
                    }
                Toast.makeText(context, "name: " + device, Toast.LENGTH_LONG).show()


                if (getRespo?.isSuccessful == true) {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "if", Toast.LENGTH_LONG).show()


                } else {
                    mProgressDialog.dismiss()
                    Toast.makeText(context, "else", Toast.LENGTH_LONG).show()

                }
            } catch (e: Exception) {

                mProgressDialog.hide()

                e.printStackTrace()
                Toast.makeText(context, getString(R.string.something_went_wrong), Toast.LENGTH_LONG)
                    .show()

            }
        }
    }

    private fun plungeLight(state: Int): PlungeLightModel {

        return PlungeLightModel(2, "ABCDEF", PlungeLightModel.Peripherals("light", state), 100)

    }

    private fun plungePump(deviceName: String, state: Int, value: Int): PumpDetailModal {
        return PumpDetailModal(deviceName,
            2,
            "ABCDEF",
            PumpDetailModal.Peripherals("pump", state, value),
            100)
    }

    private fun plungeTemp(value: Int): TemperartureDetailModel {

        return TemperartureDetailModel(2,
            "ABCDEF",
            TemperartureDetailModel.Pehripherals("pump", value),
            100)

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DeviceDetailFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(arg: Bundle? = null) =
            DeviceDetailFragment().apply {
                arguments = arg
            }
    }
}