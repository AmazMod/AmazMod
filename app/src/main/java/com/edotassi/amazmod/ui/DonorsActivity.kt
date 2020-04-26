package com.edotassi.amazmod.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.DonorsAdapter
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_donors.*
import okhttp3.*
import org.tinylog.Logger
import java.io.IOException
import java.util.*

class DonorsActivity : BaseAppCompatActivity() {
    private lateinit var donors: List<Donor>
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
        Logger.info("listDonors: checking donors list in OpenCollective", null)
        val client = OkHttpClient()
        client.newCall(request)
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Logger.debug("listDonors: failed to check donors", null)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        val json = response.body()!!.string()
                        val d = Gson().fromJson(json, Array<Donor>::class.java)
                        donors = listOf(*d)
                        donors = onlyBackers(donors)
                        donors = removeDuplicates(donors)

                        //Sort Donors by total amount
                        Collections.sort(donors) { o1, o2 -> (o2.totalAmountDonated - o1.totalAmountDonated).toInt() }
                        val adapter = DonorsAdapter(this@DonorsActivity, donors)
                        runOnUiThread {
                            activity_donors_list.adapter = adapter
                            activity_donors_list.visibility = View.VISIBLE
                            activity_donors_progress.visibility = View.GONE
                        }
                    }
                })
    }

    private fun onlyBackers(originalList: List<Donor>?): List<Donor> {
        //This code is faster and optimized for Java 8 but needs, at least, Android N (api 25)
        // Predicate<Donor> byRole = donor -> donor.role.equals("BACKER");
        //Predicate<Donor> byAmount = donor -> donor.totalAmountDonated > 0;
        //List<Donor> filtered = originalList.stream().filter(byRole).filter(byAmount)
        //        .collect(Collectors.toList());

        //Code compatible with Java 7
        val filtered: MutableList<Donor> = ArrayList()
        for (donor in originalList!!) {
            if ((donor.role == "BACKER") and (donor.totalAmountDonated > 0)) {
                filtered.add(donor)
            }
        }
        return filtered
    }

    private fun removeDuplicates(originalList: List<Donor>?): List<Donor> {
        val listDonor: MutableList<Donor> = ArrayList()
        for (donor in originalList!!) {
            if (!listDonor.contains(donor)) {
                listDonor.add(donor)
            }
        }
        return listDonor
    }

    inner class Donor internal constructor() {
        var MemberId = 0
        var createdAt: String? = null
        var type: String? = null

        @JvmField
        var role: String? = null
        var isActive = false

        @JvmField
        var totalAmountDonated = 0f
        var lastTransactionAt: String? = null
        var lastTransactionAmount = 0f
        var profile: String? = null

        @JvmField
        var name: String? = null
        var company: String? = null
        var description: String? = null

        @JvmField
        var image: String? = null
        var email: String? = null
        var twitter: String? = null
        var github: String? = null
        var website: String? = null

        @JvmField
        var currency: String? = null
        override fun equals(obj: Any?): Boolean {
            if (obj === this) {
                return true
            }
            if (obj !is Donor) {
                return false
            }
            return name == obj.name
        }
    }
}