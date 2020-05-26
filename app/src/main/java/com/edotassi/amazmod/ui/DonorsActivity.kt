package com.edotassi.amazmod.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.DonorsAdapter
import com.edotassi.amazmod.ui.model.Donor
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_donors.*
import okhttp3.*
import org.tinylog.kotlin.Logger
import java.io.IOException

class DonorsActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setTitle(R.string.donors)
        } catch (exception: NullPointerException) {
            Logger.error(exception, "AboutActivity onCreate exception: {}", exception.message)
        }
        setContentView(R.layout.activity_donors)
        activity_donors_list.layoutManager = LinearLayoutManager(this)
        //recyclerViewDonorList.setLayoutManager(new GridLayoutManager(this, 2));
        listDonors()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun listDonors() {
        val donorsURL = "https://opencollective.com/amazmod-33/members/users.json"
        val request = Request.Builder()
                .url(donorsURL)
                .build()
        Logger.info("listDonors: checking donors list in OpenCollective")
        val client = OkHttpClient()
        client.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Logger.debug("listDonors: failed to check donors")
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        val json = response.body()?.string()
                        val d: Array<Donor> = Gson().fromJson(json, Array<Donor>::class.java)
                        val donors = listOf(*d)
                            .onlyBackers()
                            .removeDuplicates()
                            .sortedByDescending { it.totalAmountDonated }

                        val adapter = DonorsAdapter(this@DonorsActivity, donors)
                        runOnUiThread {
                            activity_donors_list.adapter = adapter
                            activity_donors_list.visibility = View.VISIBLE
                            activity_donors_progress.visibility = View.GONE
                        }
                    }
                })
    }

    private fun List<Donor>.onlyBackers(): List<Donor> =
        this.filter { (it.role == "BACKER") && (it.totalAmountDonated > 0) }

    private fun List<Donor>.removeDuplicates(): List<Donor> = this.distinctBy { it.name }

}