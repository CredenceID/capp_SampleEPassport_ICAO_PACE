package com.credenceid.sample.epassport

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.constants.ServiceConstants
import com.credenceid.icao.ICAODocumentData
import com.credenceid.icao.ICAOReadIntermediateCode
import com.credenceid.sample.epassport.databinding.MrzActivityBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


/**
 * Used for Android Logcat.
 */
private val TAG = "CID-EPass"
/**
 * Keeps track of card reader sensor state.
 */
private var isCardReaderOpen = false
private var isPassportReaderOpen = false
private var isDocumentPresent = false

class MRZActivity : AppCompatActivity() {

    private lateinit var binding: MrzActivityBinding

    //Listener used to receive the Card status information from card reader
    private var onCardStatusListener = OnCardStatusListener { _, _, currState ->
        Log.d(TAG, "Card reader status : " + currState)
        if (currState in 2..6) {
            binding.paceWithCanBtn.isEnabled = true
            binding.bacWithMrzBtn.isEnabled = true
            isDocumentPresent = true
        } else {
            setReadButtons (false)
            isDocumentPresent = false
        }
    }

    //Listener used to receive the document status information from e-passport reader
    private var onEPassportStatusListener = OnEPassportStatusListener { _, currState ->
        Log.d(TAG, "Epassport status : " + currState)
        if (currState in 2..6) {
            binding.bacWithMrzBtn.isEnabled = true
            isDocumentPresent = true
        } else {
            setReadButtons (false)
            isDocumentPresent = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MrzActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.docCanEditText.setText("965008")

        this.configureLayoutComponents()
    }

    override fun onDestroy() {

        super.onDestroy()

        /* Make sure to close all peripherals on application exit. */
        App.BioManager!!.ePassportCloseCommand()
        App.BioManager!!.closeMRZ()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        binding.openCardBtn.setOnClickListener {
            if (!isCardReaderOpen)
                openCardReader()
            else App.BioManager!!.cardCloseCommand()
        }

        binding.openEpassBtn.setOnClickListener {
            if (!isPassportReaderOpen)
                openMrzReader()
            else App.BioManager!!.ePassportCloseCommand()
        }

        setReadButtons (false)
        binding.paceWithCanBtn.setOnClickListener {
            binding.icaoDG2ImageView.setImageBitmap(null)
            binding.icaoTextView1.setText("")
            binding.icaoTextView2.setText("")
            this.readGenericIcaoIdDocument(binding.docCanEditText.text.toString())
        }

        binding.bacWithMrzBtn.setOnClickListener {
            binding.icaoDG2ImageView.setImageBitmap(null)
            binding.icaoTextView1.setText("")
            binding.icaoTextView2.setText("")
            val mrzString = "I<GHAL898902C<3<<<<<<<<<<<<<<<" +
                    "8006226M2001012GHA<<<<<<<<<<<8" +
                    "HEMINGWAY<<ERNEST<<<<<<<<<<<<<"
            this.readICAODocument(mrzString)
        }


        binding.generateCertificateRequestBtn.setOnClickListener {
            val certificateName = "MyCertificate"
            App.BioManager!!.generateTerminalIsCertificate()
            { resultCode, data, s ->
                when (resultCode) {
                    OK -> {
                        binding.statusTextView.text = "Certificate generation OK"
                        Log.i(TAG, "Generation Data = " + data.decodeToString());
                        Log.i(TAG, "Generation hint = " + s)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        binding.statusTextView.text = "Certificate generation failed"
                    }

                    else -> {}
                }
            }
        }

        binding.registerCertificateBtn.setOnClickListener {

            var folderName = "GenericICAO"

            var certificateFolder = (applicationContext.getExternalFilesDir(null)?.absolutePath ?: "") + "/" +  folderName

            val file = File(certificateFolder)
            if(file.exists()) {
                val fileList: Array<out File>? = file.listFiles()
                if (fileList != null) {
                    pushCertificateToSDK(applicationContext, certificateFolder)
                }
            }else{
                this.createFile(folderName)
          }
        }

    }

    //This function gives example how to push the certificates
    // required by PACE protocol to the SDK.
    fun pushCertificateToSDK(context: Context?, folderPath: String) {
        var icaoCertificateType: ServiceConstants.ICAO.IcaoCertificateType? = ServiceConstants.ICAO.IcaoCertificateType.csca
        if (folderPath.contains("ds", ignoreCase = true)) {
            icaoCertificateType = ServiceConstants.ICAO.IcaoCertificateType.ds
        }
        if (folderPath.contains("dv_and_cvca_link", ignoreCase = true)) {
            icaoCertificateType = ServiceConstants.ICAO.IcaoCertificateType.dv_and_cvca_link
        }
        if (folderPath.contains("csca", ignoreCase = true)) {
            icaoCertificateType = ServiceConstants.ICAO.IcaoCertificateType.csca
        }
        if (folderPath.contains("isTaCert", ignoreCase = true)) {
            icaoCertificateType = ServiceConstants.ICAO.IcaoCertificateType.isTaCert
        }
        val cardProfile = "GenericICAO"
        val file = File(folderPath)
        val fileList: Array<out File>? = file.listFiles()
        if (fileList != null) {
            for (f in fileList) {
                try {
                    Log.d(TAG, "fileName = " + f.getAbsolutePath())
                    //Log.d(TAG, "icaoCertificateType = " + icaoCertificateType.toString())
                    val data: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        readBytes(f.absolutePath)
                    } else {
                        TODO("VERSION.SDK_INT < O")
                    }
                    Log.d(TAG, "ByteSize = " + data.size.toString())
                    val resultCode = App.BioManager!!.registerSmartCardCertificates(icaoCertificateType, cardProfile, f.getName(), data, true)
                    Log.d(TAG, "Result Code = " + resultCode.resultCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            Log.e(TAG, "No file available at: " + file.absolutePath)
        }

    }

    fun readBytes(absFilePath: String?): ByteArray {
        try {
            FileInputStream(absFilePath).use { fis ->
                val bos = ByteArrayOutputStream(0x20000)
                val buf = ByteArray(1024)
                var readNum: Int = 0
                while (fis.read(buf).also { readNum = it } != -1) {
                    bos.write(buf, 0, readNum)
                }
                return bos.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "readBytes(String): Unable to read byes from file.")
            return byteArrayOf()
        }
    }

    //This function gives example how to power the card reader and start to communicate
    // with a card if available
    // This is for CredenceECO and C3.
    private fun openCardReader() {

        binding.icaoDG2ImageView.setImageBitmap(null)
        binding.icaoTextView1.setText("")
        binding.icaoTextView2.setText("")
        binding.statusTextView.text = getString(R.string.cardreader_opening)

        App.BioManager!!.cardOpenCommand(object : CardReaderStatusListener {
            override fun onCardReaderOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isCardReaderOpen = true

                        App.BioManager!!.registerCardStatusListener(onCardStatusListener)

                        binding.statusTextView.text = getString(R.string.card_opened)
                        binding.openCardBtn.text = getString(R.string.close_card)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        binding.statusTextView.text = getString(R.string.card_open_fail)
                    }

                    else -> {}
                }
            }

            override fun onCardReaderClosed(resultCode: ResultCode,
                                            closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isCardReaderOpen = false

                        binding.statusTextView.text = getString(R.string.card_closed)
                        binding.openCardBtn.text = getString(R.string.open_cardreader)
                        setReadButtons (false)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> binding.statusTextView.text = getString(R.string.card_close_fail)
                    else -> {}
                }
            }
        })
    }

    //This function gives example how to power the e-passport reader and start to read
    //MRZ data.
    // This is for CredenceTAB.
    private fun openMrzReader() {

        binding.icaoDG2ImageView.setImageBitmap(null)
        binding.icaoTextView1.setText("")
        binding.icaoTextView2.setText("")
        binding.statusTextView.text = getString(R.string.mrz_opening)

        App.BioManager!!.openMRZ(object : MRZStatusListener {
            override fun onMRZOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */

                        binding.statusTextView.text = getString(R.string.mrz_opened)
                        openEPassportReader();
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        binding.statusTextView.text = getString(R.string.mrz_open_failed)
                    }

                    else -> {}
                }
            }

            override fun onMRZClose(resultCode: ResultCode,
                                                 closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */

                        binding.statusTextView.text = getString(R.string.mrz_closed)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> binding.statusTextView.text = getString(R.string.mrz_failed_close)
                    else -> {}
                }
            }
        })
    }

    //This function gives example how to power the e-passport reader and start to communicate
    //with NFC chip of the e-document.
    // This is for CredenceTAB.
    private fun openEPassportReader() {

        binding.icaoDG2ImageView.setImageBitmap(null)
        binding.icaoTextView1.setText("")
        binding.icaoTextView2.setText("")
        binding.statusTextView.text = getString(R.string.epassport_opening)

        App.BioManager!!.registerEPassportStatusListener(onEPassportStatusListener)

        App.BioManager!!.ePassportOpenCommand(object : EPassportReaderStatusListener {
            override fun onEPassportReaderOpen(resultCode: ResultCode?) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openCardBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isPassportReaderOpen = true

                        binding.statusTextView.text = getString(R.string.epassport_opened)
                        binding.openEpassBtn.text = getString(R.string.close_epassport)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        binding.statusTextView.text = getString(R.string.epassport_open_failed)
                    }

                    else -> {}
                }
            }

            override fun onEPassportReaderClosed(resultCode: ResultCode,
                                            closeReasonCode: CloseReasonCode?) {
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openCardBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isPassportReaderOpen = false

                        binding.statusTextView.text = getString(R.string.epassport_closed)
                        binding.openEpassBtn.text = getString(R.string.open_epassport)
                        setReadButtons (false)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }
                    FAIL -> binding.statusTextView.text = getString(R.string.epassport_open_failed)
                    else -> {}
                }
            }
        })
    }

    //This function gives example how to read e-id document
    // using the PACE with CAN protocol.
    @OptIn(ExperimentalStdlibApi::class)
    @SuppressLint("SetTextI18n")
    private fun  readGenericIcaoIdDocument(can: String?) {

        /* If any one of three parameters is bad then do not proceed with document reading. */
        if (null == can || can.isEmpty()) {
            Log.w(TAG, "DateOfBirth parameter INVALID, will not read ICAO document.")
            return
        }

        Log.d(TAG, "Reading ID document: $can")

        /* Disable button so user does not initialize another readICAO document API call. */
        setReadButtons (false)
        binding.statusTextView.text = getString(R.string.reading)

        var start = SystemClock.elapsedRealtime()
        Log.d(TAG, "start(elapsedRealtime) = " + start)
        App.BioManager!!.readICAODocument(can,"GenericIcao", false)
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: ICAODocumentData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d(TAG, "ICAODocumentData: $data")

            binding.statusTextView.text = "Finished reading stage: " + stage.name
            if (ICAOReadIntermediateCode.BAC == stage) {
                if (FAIL == rc) {
                    binding.statusTextView.text = getString(R.string.bac_failed)
                    setReadButtons(isDocumentPresent)
                }

            } else if (ICAOReadIntermediateCode.DG1 == stage) {
                if (OK == rc) {
                    Log.d("CID", "DG1 DATA = "+ data.DG1.toString());
                    binding.icaoTextView2.text = data.DG1.toString()
                }

            } else if (ICAOReadIntermediateCode.DG2 == stage) {
                if (OK == rc) {
                    //binding.icaoTextView1.text = data.DG2.toString()
                    binding.icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                }

            } else if (ICAOReadIntermediateCode.DG3 == stage) {
                if (OK == rc) {
                    binding.icaoTextView1.text = "Number of fingers" + data.DG3.fingers.size
                    binding.icaoTextView1.text = binding.icaoTextView1.text.toString() + "\n" +
                            data.DG3.toString()
                    if(data.DG3.fingers.size > 0)

                        binding.icaoTextView1.text = binding.icaoTextView1.text.toString() + "\n" +
                                data.DG3.fingers[0].bytes.size
                    binding.icaoDG2ImageView.setImageBitmap(data.DG3.fingers[0].bitmap)
                }


                Log.d(TAG, "ICAO Profile read time = " + (SystemClock.elapsedRealtime() - start))
                binding.statusTextView.text = getString(R.string.icao_done)
                setReadButtons(isDocumentPresent)
            } else if (ICAOReadIntermediateCode.DG11 == stage) {
                Log.d(TAG, "DG11 read")
                Log.d(TAG, "DG11 data => \n ${data.DG11.toString()}")
            } else if (ICAOReadIntermediateCode.DG12 == stage) {
                Log.d(TAG, "DG12 read")
                Log.d(TAG, "DG12 data => \n ${data.DG12.toString()}")
            }else if (ICAOReadIntermediateCode.DG13 == stage) {
                Log.d(TAG, "DG13 read")
                Log.d(TAG, "DG13 data => \n ${data.DG13.dgData.toHexString(HexFormat.UpperCase)}")
            }else if (ICAOReadIntermediateCode.DG14 == stage) {
                Log.d(TAG, "DG14 read")
                Log.d(TAG, "DG14 data => \n ${ data.DG14.dgData.toHexString(HexFormat.UpperCase)}")
            }else {
                Log.d(TAG, "DG read => ${stage.name}")
            }
        }
    }

    //This function gives example how to read e-id document
    // using the BAC with MRZ protocol.
    @SuppressLint("SetTextI18n")
    private fun readICAODocument(mrz: String?) {

        Log.d(TAG, "Reading ICAO document: $mrz")

        //Hemingway card
        val documentNumber = "L898902C<"
        val dateOfBirth = "800622"
        val dateOfExpiry = "200101"

        /* Disable button so user does not initialize another readICAO document API call. */
        setReadButtons(false)
        binding.statusTextView.text = getString(R.string.reading)

        val start = SystemClock.elapsedRealtime()
        App.BioManager!!.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry)
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: ICAODocumentData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d(TAG, "ICAODocumentData: $data")

            binding.statusTextView.text = "Finished reading stage: " + stage.name
            if (ICAOReadIntermediateCode.BAC == stage) {
                if (FAIL == rc) {
                    binding.statusTextView.text = getString(R.string.bac_failed)
                    setReadButtons(isDocumentPresent)
                }

            } else if (ICAOReadIntermediateCode.DG1 == stage) {
                if (OK == rc) {
                    Log.d("CID", "DG1 DATA = "+ data.DG1.toString());
                    binding.icaoTextView2.text = data.DG1.toString()
                }

            } else if (ICAOReadIntermediateCode.DG2 == stage) {
                if (OK == rc) {
                    //binding.icaoTextView1.text = data.DG2.toString()
                    binding.icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                }

            } else if (ICAOReadIntermediateCode.DG3 == stage) {
                if (OK == rc) {
                    binding.icaoTextView1.text = "Number of fingers" + data.DG3.fingers.size
                    binding.icaoTextView1.text = binding.icaoTextView1.text.toString() + "\n" +
                            data.DG3.toString()
                    if(data.DG3.fingers.size > 0)

                        binding.icaoTextView1.text = binding.icaoTextView1.text.toString() + "\n" +
                                data.DG3.fingers[0].bytes.size
                        binding.icaoDG2ImageView.setImageBitmap(data.DG3.fingers[0].bitmap)
                }


                Log.d(TAG, "ICAO Profile read time = " + (SystemClock.elapsedRealtime() - start))
                binding.statusTextView.text = getString(R.string.icao_done)
                setReadButtons(isDocumentPresent)
            }
        }
    }

    private fun setReadButtons(enable: Boolean){
        binding.bacWithMrzBtn.isEnabled = enable
        binding.paceWithCanBtn.isEnabled = enable
    }

    @Throws(Exception::class)
    fun createFile(folder: String) {

        try {
            var certificateFolder = (applicationContext.getExternalFilesDir(null)?.absolutePath ?: "") + "/" +  folder
            var certificateFolderFile = File(certificateFolder)
            if (!certificateFolderFile.exists()) {
                certificateFolderFile.mkdirs()
            }
            val file = certificateFolderFile.absolutePath + "/CredenceId.txt"
            val toWrite = File(file)
            if (!toWrite.exists()) {
                Log.d("CID-TEST", "createFile = " + file );
                if (!toWrite.createNewFile())
                    throw Exception("Fail to create file")
            }
            val fOut = FileOutputStream(file)
            val data = "This is a fake certificate".toByteArray()
            fOut.write(data)
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

