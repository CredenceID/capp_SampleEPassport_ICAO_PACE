package com.credenceid.sample.epassport

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.biometrics.DeviceFamily


private const val REQUEST_ALL_PERMISSIONS = 0
/**
 * List of all permissions we will request.
 */
private val PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE)

class LaunchActivity : Activity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        this.requestPermissions()
        this.initBiometrics()
    }

    /**
     * Checks if permissions stated in manifest have been granted, if not it then requests them.
     */
    private fun requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                    ||checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS)
            }
        }
    }

    private fun initBiometrics() {

        /*  Create new biometrics object. */
        App.BioManager = BiometricsManager(this)

        /* Initialize object, meaning tell CredenceService to bind to this application. */
        App.BioManager!!.initializeBiometrics { rc: Biometrics.ResultCode,
                                                _: String,
                                                _: String ->

            when (rc) {
                OK -> {
                    Toast.makeText(this, getString(R.string.bio_init), LENGTH_SHORT).show()

                    App.DevFamily = App.BioManager!!.deviceFamily
                    App.DevType = App.BioManager!!.deviceType

                    val intent = when (App.DevFamily) {
                        DeviceFamily.CredenceTAB ->
                            Intent(this, MRZActivity::class.java)
                        DeviceFamily.CredenceTwo ->
                            Intent(this, MRZActivity::class.java)
                        DeviceFamily.CredenceECO ->
                            Intent(this, MRZActivity::class.java)
                        DeviceFamily.Generic_Android ->
                            Intent(this, MRZActivity::class.java)
                        else -> return@initializeBiometrics
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    this.finish()

                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Toast.makeText(this, getString(R.string.bio_ini_fail), LENGTH_LONG).show()
                else -> {}
            }
        }
    }
}
