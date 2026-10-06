package com.businessmanager.backend.printing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;

@Service
@Slf4j
public class PdfGeneratorService {

    /**
     * Converts a raw HTML string into a PDF byte array using OpenHTMLToPDF.
     *
     * @param htmlContent The fully rendered HTML string
     * @return The raw PDF byte array
     */
    public byte[] generatePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            registerUnicodeFonts(builder);

            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF from HTML: {}", e.getMessage(), e);
            throw new RuntimeException("PDF Generation Failed", e);
        }
    }

    private void registerUnicodeFonts(PdfRendererBuilder builder) {
        String[] fontPaths = {
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/calibri.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf"
        };

        for (String path : fontPaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    builder.useFont(file, "Helvetica");
                    builder.useFont(file, "Arial");
                    builder.useFont(file, "sans-serif");
                    log.info("Registered Unicode PDF font from {}", path);
                    break;
                } catch (Exception e) {
                    log.warn("Could not register font from {}: {}", path, e.getMessage());
                }
            }
        }
    }
}
