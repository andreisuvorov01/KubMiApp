package com.example.kubmi.domain.model

import android.content.Context
import com.example.kubmi.R

enum class LessonType {
    LECTURE,
    PRACTICE,
    LAB;

    fun localizedName(context: Context): String {
        return when (this) {
            LECTURE -> context.getString(R.string.lecture)
            PRACTICE -> context.getString(R.string.practice)
            LAB -> context.getString(R.string.lab_work)
        }
    }
}