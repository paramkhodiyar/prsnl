package com.github.barteksc.pdfviewer;

import android.graphics.RectF;
import com.shockwave.pdfium.util.SizeF;

public final class PdfViewUtils {
    private PdfViewUtils() {}

    public static RectF getPageRect(PDFView pdfView, int pageIndex) {
        if (pdfView == null || pdfView.pdfFile == null) return null;
        if (pageIndex < 0 || pageIndex >= pdfView.getPageCount()) return null;

        float zoom = pdfView.getZoom();
        float pageOffset = pdfView.pdfFile.getPageOffset(pageIndex, zoom);
        float secondaryOffset = pdfView.pdfFile.getSecondaryPageOffset(pageIndex, zoom);

        float left = pdfView.getCurrentXOffset() + secondaryOffset;
        float top = pdfView.getCurrentYOffset() + pageOffset;

        SizeF size = pdfView.pdfFile.getScaledPageSize(pageIndex, zoom);
        return new RectF(left, top, left + size.getWidth(), top + size.getHeight());
    }

    public static float getSecondaryOffset(PDFView pdfView, int pageIndex) {
        if (pdfView == null || pdfView.pdfFile == null) return 0f;
        return pdfView.pdfFile.getSecondaryPageOffset(pageIndex, pdfView.getZoom());
    }
}
