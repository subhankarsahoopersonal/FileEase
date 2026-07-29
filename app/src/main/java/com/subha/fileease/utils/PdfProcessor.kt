package com.subha.fileease.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import java.io.File

object PdfProcessor {

    /**
     * Merges multiple PDF files into a single PDF locally on the device.
     *
     * @param context The application context needed to read files securely.
     * @param pdfUris A list of Uris representing the PDFs the user selected.
     * @param outputFileName The desired name of the final merged file (e.g., "Merged_Project.pdf").
     * @return The merged PDF File object, or null if an error occurred.
     */
    fun mergePdfs(context: Context, pdfUris: List<Uri>, outputFileName: String): File? {
        return try {
            // 1. Initialize the PDFBox Merger Tool
            val merger = PDFMergerUtility()

            // 2. Define where to temporarily save the output (App's secure cache)
            val outputFile = File(context.cacheDir, outputFileName)
            merger.destinationFileName = outputFile.absolutePath

            // 3. Loop through every PDF the user selected
            for (uri in pdfUris) {
                // Safely open a stream to read the file using Android's ContentResolver
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    merger.addSource(inputStream)
                }
            }

            // 4. Perform the merge operation using the device's main memory
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

            Log.d("PdfProcessor", "Success! Merged PDF saved to: ${outputFile.absolutePath}")

            // Return the final file so the UI can show it to the user or let them share it
            outputFile

        } catch (e: Exception) {
            // If anything goes wrong (e.g., corrupted PDF), catch the error so the app doesn't crash
            Log.e("PdfProcessor", "Error merging PDFs: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}