package com.example.sheetmusicapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CloudFileListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_file_list)
        actionBar?.hide()
        initList()
    }

    fun initList() {
        val database = Firebase.database.reference
        val user = Firebase.auth.currentUser ?: throw IllegalStateException("User must login!")
        database.child("storage").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val cloudList = mutableListOf<String>()
            for (snapshot in dataSnapshot.children) {
                snapshot.key?.let { cloudList.add(it) }
            }
            val adapter = ArrayAdapter(this@CloudFileListActivity,
                R.layout.listview_item, R.id.label, cloudList)

            val listView: ListView = findViewById(R.id.list_view)
            listView.adapter = adapter
            listView.setOnItemClickListener { parent, view, position, id ->
                val data = Intent().apply { putExtra("title", adapter.getItem(position)) }
                setResult(RESULT_OK, data)
                finish()
            }
        }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
    //            Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })
    }
}

