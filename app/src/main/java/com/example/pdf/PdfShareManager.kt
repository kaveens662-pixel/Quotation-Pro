package com.example.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.Quotation
import java.io.File
import java.io.OutputStream

object PdfShareManager {
    fun shareQuotationPdf(context: Context, quotation: Quotation) {
        val pdfFile = PdfGenerator.generatePdf(context, quotation)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Quotation ${quotation.quotationNumber} from ${quotation.businessSnapshotName}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Please find attached quotation ${quotation.quotationNumber} for ${quotation.clientSnapshotName}.\n\nTotal: ${quotation.currency} ${quotation.grandTotal.setScale(2)}\nValid Until: ${quotation.formattedValidUntil()}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Quotation PDF via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun savePdfToStream(context: Context, quotation: Quotation, outputStream: OutputStream) {
        outputStream.use { out ->
            PdfGenerator.writePdfToStream(quotation, out)
        }
    }
}
