package com.petanalyzer

import android.app.Application
import com.petanalyzer.data.PetDatabase
import com.petanalyzer.data.repository.PetDiaryRepository

class PetAnalyzerApp : Application() {

    lateinit var repository: PetDiaryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = PetDatabase.getInstance(this)
        repository = PetDiaryRepository(db.behaviorLogDao())
    }
}
