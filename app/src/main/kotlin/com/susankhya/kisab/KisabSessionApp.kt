package com.susankhya.kisab

import android.content.Context
import com.susankhya.foundation.session.AndroidKeystoreSessionStorage
import com.susankhya.foundation.session.SessionStorage

class KisabSessionApp {
    fun storage(context: Context): SessionStorage = AndroidKeystoreSessionStorage(context)
}
