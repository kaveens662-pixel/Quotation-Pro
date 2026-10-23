package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.AppCurrency
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.math.BigDecimal

object PdfGenerator {
    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 806f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT

    // Colors
    private val COLOR_PRIMARY = Color.rgb(15, 23, 42) // Slate 900
    private val COLOR_SECONDARY = Color.rgb(71, 85, 105) // Slate 600
    private val COLOR_MUTED = Color.rgb(148, 163, 184) // Slate 400
    private val COLOR_BORDER = Color.rgb(226, 232, 240) // Slate 200
    private val COLOR_BG_HEADER = Color.rgb(248, 250, 252) // Slate 50
    private val COLOR_ACCENT_BG = Color.rgb(241, 245, 249) // Slate 100
    private val COLOR_EMERALD = Color.rgb(16, 185, 129) // Emerald 500

    fun generatePdf(context: Context, quotation: Quotation): File {
        val cacheDir = File(context.cacheDir, "quotations")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val safeNumber = quotation.quotationNumber.replace("[^a-zA-Z0-9-_]".toRegex(), "_")
        val outputFile = File(cacheDir, "Quotation_${safeNumber}.pdf")

        FileOutputStream(outputFile).use { out ->
            writePdfToStream(quotation, out)
        }
        return outputFile
    }

    fun writePdfToStream(quotation: Quotation, outputStream: OutputStream) {
        val document = PdfDocument()

        val items = quotation.items
        val itemsPerPage = 7 // Safe item count per page with descriptions
        val totalPages = if (items.isEmpty()) 1 else ((items.size - 1) / itemsPerPage) + 1

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = COLOR_PRIMARY
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = MARGIN_TOP

            // Draw Header on first page or simplified header on continuation
            if (pageIndex == 0) {
                currentY = drawFirstPageHeader(canvas, quotation, currentY)
            } else {
                currentY = drawContinuationHeader(canvas, quotation, currentY, pageIndex + 1, totalPages)
            }

            // Draw Items Table for this page
            val startIdx = pageIndex * itemsPerPage
            val endIdx = (startIdx + itemsPerPage).coerceAtMost(items.size)
            val pageItems = if (items.isNotEmpty()) items.subList(startIdx, endIdx) else emptyList()

            currentY = drawItemsTable(canvas, pageItems, quotation.currency, currentY)

            // If last page, draw totals, terms, notes, and signature
            if (pageIndex == totalPages - 1) {
                currentY = drawTotalsAndDetails(canvas, quotation, currentY)
            }

            // Draw Footer on every page
            drawPageFooter(canvas, quotation, pageIndex + 1, totalPages)

            document.finishPage(page)
        }

        document.writeTo(outputStream)
        document.close()
    }

    private fun drawFirstPageHeader(canvas: Canvas, quotation: Quotation, startY: Float): Float {
        var y = startY
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top decorative accent bar
        val barPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_LEFT + 60f, y + 4f), 2f, 2f, barPaint)
        y += 18f

        // Business Name & Title row
        textPaint.apply {
            color = COLOR_PRIMARY
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bizName = quotation.businessSnapshotName.ifBlank { "Quotation Pro Business" }
        canvas.drawText(bizName, MARGIN_LEFT, y, textPaint)

        // "QUOTATION" label on right
        textPaint.apply {
            color = COLOR_PRIMARY
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("QUOTATION", MARGIN_RIGHT, y, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        y += 14f

        // Business Details (Left column)
        textPaint.apply {
            color = COLOR_SECONDARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val bizOwner = quotation.businessSnapshotOwner
        if (bizOwner.isNotBlank()) {
            canvas.drawText(bizOwner, MARGIN_LEFT, y, textPaint)
            y += 11f
        }

        val bizEmail = quotation.businessSnapshotEmail
        val bizPhone = quotation.businessSnapshotPhone
        val contactLine = listOf(bizEmail, bizPhone).filter { it.isNotBlank() }.joinToString(" • ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, MARGIN_LEFT, y, textPaint)
            y += 11f
        }

        val bizAddress = quotation.businessSnapshotAddress
        if (bizAddress.isNotBlank()) {
            val lines = bizAddress.split("\n")
            for (line in lines.take(2)) {
                canvas.drawText(line.trim(), MARGIN_LEFT, y, textPaint)
                y += 11f
            }
        }

        val bizReg = quotation.businessSnapshotRegNumber
        val bizTax = quotation.businessSnapshotTaxNumber
        val regLine = listOf(
            if (bizReg.isNotBlank()) "Reg: $bizReg" else "",
            if (bizTax.isNotBlank()) "Tax: $bizTax" else ""
        ).filter { it.isNotBlank() }.joinToString(" | ")
        if (regLine.isNotBlank()) {
            canvas.drawText(regLine, MARGIN_LEFT, y, textPaint)
            y += 11f
        }

        // Quotation Metadata Box (Right column, aligned with top)
        val metaBoxWidth = 175f
        val metaBoxHeight = 58f
        val metaBoxLeft = MARGIN_RIGHT - metaBoxWidth
        val metaBoxTop = startY + 24f

        val boxPaint = Paint().apply {
            color = COLOR_BG_HEADER
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(metaBoxLeft, metaBoxTop, MARGIN_RIGHT, metaBoxTop + metaBoxHeight), 6f, 6f, boxPaint)
        canvas.drawRoundRect(RectF(metaBoxLeft, metaBoxTop, MARGIN_RIGHT, metaBoxTop + metaBoxHeight), 6f, 6f, borderPaint)

        var metaY = metaBoxTop + 14f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Quotation No:", metaBoxLeft + 10f, metaY, labelPaint)
        canvas.drawText(quotation.quotationNumber, MARGIN_RIGHT - 10f, metaY, valPaint)
        metaY += 14f

        canvas.drawText("Issue Date:", metaBoxLeft + 10f, metaY, labelPaint)
        canvas.drawText(quotation.formattedIssueDate(), MARGIN_RIGHT - 10f, metaY, valPaint)
        metaY += 14f

        canvas.drawText("Valid Until:", metaBoxLeft + 10f, metaY, labelPaint)
        canvas.drawText(quotation.formattedValidUntil(), MARGIN_RIGHT - 10f, metaY, valPaint)

        // Ensure vertical space is adequate
        val maxHeaderY = maxOf(y, metaBoxTop + metaBoxHeight) + 16f
        y = maxHeaderY

        // Client Info Card ("Billed To / Recipient")
        val clientCardHeight = 48f
        val clientCardPaint = Paint().apply {
            color = COLOR_BG_HEADER
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + clientCardHeight), 6f, 6f, clientCardPaint)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + clientCardHeight), 6f, 6f, borderPaint)

        var clientY = y + 14f
        val clientTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("BILLED TO / CLIENT:", MARGIN_LEFT + 12f, clientY, clientTitlePaint)
        clientY += 12f

        val clientNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cName = quotation.clientSnapshotCompany.ifBlank { quotation.clientSnapshotName }.ifBlank { "Valued Client" }
        canvas.drawText(cName, MARGIN_LEFT + 12f, clientY, clientNamePaint)
        clientY += 11f

        val cDetailsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY
            textSize = 8f
        }
        val cDetails = listOf(
            quotation.clientSnapshotContact,
            quotation.clientSnapshotEmail,
            quotation.clientSnapshotPhone,
            quotation.clientSnapshotAddress.replace("\n", ", ")
        ).filter { it.isNotBlank() }.joinToString(" • ")
        if (cDetails.isNotBlank()) {
            val truncatedDetails = if (cDetails.length > 90) cDetails.take(87) + "..." else cDetails
            canvas.drawText(truncatedDetails, MARGIN_LEFT + 12f, clientY, cDetailsPaint)
        }

        y += clientCardHeight + 16f
        return y
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        quotation: Quotation,
        startY: Float,
        currentPage: Int,
        totalPages: Int
    ): Float {
        var y = startY
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.apply {
            color = COLOR_PRIMARY
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("${quotation.businessSnapshotName} — Quotation ${quotation.quotationNumber}", MARGIN_LEFT, y + 10f, paint)

        paint.apply {
            color = COLOR_MUTED
            textSize = 9f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Page $currentPage of $totalPages", MARGIN_RIGHT, y + 10f, paint)

        // Divider
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_LEFT, y + 18f, MARGIN_RIGHT, y + 18f, linePaint)
        return y + 28f
    }

    private fun drawItemsTable(
        canvas: Canvas,
        items: List<QuotationItem>,
        currencyCode: String,
        startY: Float
    ): Float {
        var y = startY
        val headerHeight = 22f

        // Table Header Background
        val headerPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + headerHeight), 4f, 4f, headerPaint)

        // Header Columns: Description (width 220), Unit (40), Qty (35), Unit Price (75), Disc (45), Total (80)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val headerTextY = y + 14f
        canvas.drawText("ITEM / DESCRIPTION", MARGIN_LEFT + 8f, headerTextY, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("UNIT", MARGIN_LEFT + 245f, headerTextY, textPaint)
        canvas.drawText("QTY", MARGIN_LEFT + 285f, headerTextY, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("UNIT PRICE", MARGIN_LEFT + 365f, headerTextY, textPaint)
        canvas.drawText("DISC", MARGIN_LEFT + 425f, headerTextY, textPaint)
        canvas.drawText("LINE TOTAL", MARGIN_RIGHT - 8f, headerTextY, textPaint)

        y += headerHeight

        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.8f
        }

        val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val itemDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val colTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8.5f
        }

        val symbol = AppCurrency.find(currencyCode).symbol

        for (item in items) {
            val hasDesc = item.description.isNotBlank()
            val rowHeight = if (hasDesc) 34f else 24f

            // Bottom line for row
            canvas.drawLine(MARGIN_LEFT, y + rowHeight, MARGIN_RIGHT, y + rowHeight, borderPaint)

            var rowY = y + 13f
            val itemName = if (item.name.length > 38) item.name.take(35) + "..." else item.name
            canvas.drawText(itemName, MARGIN_LEFT + 8f, rowY, itemTitlePaint)

            if (hasDesc) {
                rowY += 12f
                val desc = if (item.description.length > 55) item.description.take(52) + "..." else item.description
                canvas.drawText(desc, MARGIN_LEFT + 8f, rowY, itemDescPaint)
            }

            // Unit & Qty
            colTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.unitType, MARGIN_LEFT + 245f, y + 14f, colTextPaint)
            canvas.drawText(item.quantity.toPlainString(), MARGIN_LEFT + 285f, y + 14f, colTextPaint)

            // Price, Disc, Total
            colTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$symbol ${item.unitPrice.setScale(2)}", MARGIN_LEFT + 365f, y + 14f, colTextPaint)

            val discText = if (item.lineDiscountAmount > BigDecimal.ZERO) "-$symbol ${item.lineDiscountAmount.setScale(2)}" else "-"
            canvas.drawText(discText, MARGIN_LEFT + 425f, y + 14f, colTextPaint)

            colTextPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText("$symbol ${item.lineTotal.setScale(2)}", MARGIN_RIGHT - 8f, y + 14f, colTextPaint)
            colTextPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

            y += rowHeight
        }

        return y + 14f
    }

    private fun drawTotalsAndDetails(canvas: Canvas, quotation: Quotation, startY: Float): Float {
        var y = startY
        val symbol = AppCurrency.find(quotation.currency).symbol

        // Left section: Terms & Notes (width 280)
        // Right section: Totals Table (width 220)
        val totalsWidth = 220f
        val totalsLeft = MARGIN_RIGHT - totalsWidth

        var leftY = y
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        if (quotation.paymentTerms.isNotBlank()) {
            canvas.drawText("PAYMENT TERMS", MARGIN_LEFT, leftY, titlePaint)
            leftY += 10f
            val terms = if (quotation.paymentTerms.length > 60) quotation.paymentTerms.take(57) + "..." else quotation.paymentTerms
            canvas.drawText(terms, MARGIN_LEFT, leftY, bodyPaint)
            leftY += 16f
        }

        if (quotation.deliveryTerms.isNotBlank()) {
            canvas.drawText("DELIVERY TERMS", MARGIN_LEFT, leftY, titlePaint)
            leftY += 10f
            val dTerms = if (quotation.deliveryTerms.length > 60) quotation.deliveryTerms.take(57) + "..." else quotation.deliveryTerms
            canvas.drawText(dTerms, MARGIN_LEFT, leftY, bodyPaint)
            leftY += 16f
        }

        if (quotation.notes.isNotBlank()) {
            canvas.drawText("NOTES & CONDITIONS", MARGIN_LEFT, leftY, titlePaint)
            leftY += 10f
            val notes = if (quotation.notes.length > 70) quotation.notes.take(67) + "..." else quotation.notes
            canvas.drawText(notes, MARGIN_LEFT, leftY, bodyPaint)
            leftY += 16f
        }

        // Signature box on left
        leftY += 10f
        val sigLinePaint = Paint().apply {
            color = COLOR_MUTED
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_LEFT, leftY + 28f, MARGIN_LEFT + 160f, leftY + 28f, sigLinePaint)
        canvas.drawText("Authorized Signature / Date", MARGIN_LEFT, leftY + 38f, bodyPaint)

        // Right side: Totals box
        var rightY = y
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SECONDARY
            textSize = 8.5f
        }
        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Subtotal
        canvas.drawText("Subtotal:", totalsLeft + 10f, rightY, totalLabelPaint)
        canvas.drawText("$symbol ${quotation.subtotal.setScale(2)}", MARGIN_RIGHT - 8f, rightY, totalValPaint)
        rightY += 14f

        // Line Discounts if any
        if (quotation.lineDiscountsTotal > BigDecimal.ZERO) {
            canvas.drawText("Line Discounts:", totalsLeft + 10f, rightY, totalLabelPaint)
            canvas.drawText("-$symbol ${quotation.lineDiscountsTotal.setScale(2)}", MARGIN_RIGHT - 8f, rightY, totalValPaint)
            rightY += 14f
        }

        // Overall Discount if any
        if (quotation.overallDiscount > BigDecimal.ZERO) {
            canvas.drawText("Overall Discount:", totalsLeft + 10f, rightY, totalLabelPaint)
            canvas.drawText("-$symbol ${quotation.overallDiscount.setScale(2)}", MARGIN_RIGHT - 8f, rightY, totalValPaint)
            rightY += 14f
        }

        // Tax Total
        if (quotation.taxTotal > BigDecimal.ZERO) {
            canvas.drawText("Tax (VAT / Sales):", totalsLeft + 10f, rightY, totalLabelPaint)
            canvas.drawText("+$symbol ${quotation.taxTotal.setScale(2)}", MARGIN_RIGHT - 8f, rightY, totalValPaint)
            rightY += 14f
        }

        // Additional Fee
        if (quotation.additionalFee > BigDecimal.ZERO) {
            canvas.drawText("Additional Fee:", totalsLeft + 10f, rightY, totalLabelPaint)
            canvas.drawText("+$symbol ${quotation.additionalFee.setScale(2)}", MARGIN_RIGHT - 8f, rightY, totalValPaint)
            rightY += 14f
        }

        rightY += 4f
        // Grand Total Highlight Card
        val grandCardHeight = 28f
        val grandBgPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(totalsLeft, rightY, MARGIN_RIGHT, rightY + grandCardHeight), 4f, 4f, grandBgPaint)

        val grandLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val grandValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("GRAND TOTAL", totalsLeft + 10f, rightY + 18f, grandLabelPaint)
        canvas.drawText("$symbol ${quotation.grandTotal.setScale(2)} ${quotation.currency}", MARGIN_RIGHT - 8f, rightY + 19f, grandValPaint)

        rightY += grandCardHeight + 10f
        return maxOf(leftY + 50f, rightY)
    }

    private fun drawPageFooter(canvas: Canvas, quotation: Quotation, currentPage: Int, totalPages: Int) {
        val y = MARGIN_BOTTOM - 14f
        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val footerText = quotation.businessSnapshotFooter.ifBlank { "Generated by Quotation Pro • All rights reserved." }
        val truncatedFooter = if (footerText.length > 75) footerText.take(72) + "..." else footerText
        canvas.drawText(truncatedFooter, MARGIN_LEFT, y + 12f, footerPaint)

        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $currentPage of $totalPages", MARGIN_RIGHT, y + 12f, footerPaint)
    }
}
