package tn.lamg.gmao.modules.reports;

import static org.junit.jupiter.api.Assertions.*;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceReportDocumentTest {

    @Test
    void rendersFrenchLabelsAccentsUnitsAndLocalDatesOnOnePage() throws Exception {
        var data = sample();
        byte[] pdf = MaintenanceReportDocument.pdf(data);
        try (var reader = new PdfReader(pdf)) {
            assertEquals(1, reader.getNumberOfPages());
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("Rapport de maintenance"));
            assertTrue(text.contains("Équipements indisponibles"));
            assertTrue(text.contains("Durée moyenne de réparation"));
            assertTrue(text.contains("62,500 TND"));
            assertTrue(text.contains("99,86 %"));
            assertTrue(text.contains("239,7 h"));
            assertTrue(text.contains("23/11/2026 à 14:10"));
            assertTrue(text.contains("pièces consommées"));
            assertFalse(text.contains("partsCost"));
            assertFalse(text.contains("mttrMinutes"));
            assertFalse(text.contains("costScope"));
            assertFalse(text.contains("2026-11-23T"));
        }
        // Reviewable sample matching the user's original report; contains no live data.
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/maintenance-apercu.pdf"), pdf);
    }

    @Test
    void handlesMissingMetricsAndDoesNotInventOrExposeCosts() throws Exception {
        var data = sample();
        data.put("mttrMinutes", null);
        data.put("availability", null);
        data.remove("partsCost");
        try (var reader = new PdfReader(MaintenanceReportDocument.pdf(data))) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("Non disponible"));
            assertFalse(text.contains("Coût des pièces consommées"));
            assertFalse(text.contains("TND"));
            assertFalse(text.contains("null"));
        }
    }

    private Map<String, Object> sample() {
        var data = new LinkedHashMap<String, Object>();
        data.put("active", 2);
        data.put("overdue", 1);
        data.put("critical", 0);
        data.put("completed", 2);
        data.put("lowStock", 1);
        data.put("unavailableAssets", 0);
        data.put("mttrMinutes", 60.0);
        data.put("preventiveRate", 100.0);
        data.put("availability", 99.86111111111111);
        data.put("mtbfHours", 239.66666666666666);
        data.put("partsCost", new BigDecimal("62.500000"));
        data.put("from", Instant.parse("2026-10-24T13:10:59Z"));
        data.put("to", Instant.parse("2026-11-23T13:10:59Z"));
        return data;
    }
}
