package com.example.myschemes.data.repository

import com.example.myschemes.data.database.SchemeDao
import com.example.myschemes.data.model.Scheme

class SchemeRepository(private val schemeDao: SchemeDao) {
    suspend fun getAllSchemes(): List<Scheme> = schemeDao.getAllSchemes()
    suspend fun saveSchemes(schemes: List<Scheme>) {
        schemeDao.deleteAll()
        schemeDao.insertAll(schemes)
    }
}