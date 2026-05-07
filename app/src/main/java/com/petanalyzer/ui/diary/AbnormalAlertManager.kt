package com.petanalyzer.ui.diary

import android.content.Context
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.data.repository.PetDiaryRepository

class AbnormalAlertManager {

    private var repository: PetDiaryRepository? = null

    fun attach(context: Context) {
        repository = (context.applicationContext as? PetAnalyzerApp)?.repository
    }

    suspend fun check(currentHour: Int): List<String> {
        return repository?.checkAbnormalPatterns(currentHour) ?: emptyList()
    }
}
