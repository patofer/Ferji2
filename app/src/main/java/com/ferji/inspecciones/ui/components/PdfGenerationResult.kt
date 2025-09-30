package com.ferji.inspecciones.ui.components

import android.net.Uri

sealed class PdfGenerationResult {

    data class Success(val fileName: String, val filePath: String?, val fileUri: Uri?) : PdfGenerationResult()
    data class Error(val message: String) : PdfGenerationResult()
    object InProgress : PdfGenerationResult()
    object Idle : PdfGenerationResult()
}