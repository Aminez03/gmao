package tn.lamg.gmao.modules.reports;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Presentation of maintenance exports; technical API keys never become report labels. */
final class MaintenanceReportDocument {

    private static final Color NAVY = new Color(22, 43, 65);
    private static final Color TEAL = new Color(0, 113, 120);
    private static final Color MUTED = new Color(91, 108, 125);
    private static final Color PALE = new Color(242, 246, 249);
    private static final Locale FRENCH = Locale.FRANCE;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
        "dd/MM/yyyy 'à' HH:mm",
        FRENCH
    ).withZone(ZoneId.of("Africa/Tunis"));
    private static final String COST_NOTE =
        "Ce total inclut uniquement les pièces consommées. La main-d’œuvre et les prestations sont exclues.";

    private MaintenanceReportDocument() {}

    record Indicator(String section, String label, String value) {}

    static List<Indicator> indicators(Map<?, ?> data) {
        var rows = new ArrayList<Indicator>();
        rows.add(
            new Indicator("Activité", "Ordres de travail actifs", number(data.get("active"), 0, ""))
        );
        rows.add(
            new Indicator(
                "Activité",
                "Ordres de travail en retard",
                number(data.get("overdue"), 0, "")
            )
        );
        rows.add(
            new Indicator(
                "Activité",
                "Ordres de travail urgents (P1)",
                number(data.get("critical"), 0, "")
            )
        );
        rows.add(
            new Indicator(
                "Activité",
                "Ordres de travail validés sur la période",
                number(data.get("completed"), 0, "")
            )
        );
        rows.add(
            new Indicator(
                "Performance",
                "Durée moyenne de réparation",
                number(data.get("mttrMinutes"), 1, " min")
            )
        );
        rows.add(
            new Indicator(
                "Performance",
                "Temps moyen entre deux pannes",
                number(data.get("mtbfHours"), 1, " h")
            )
        );
        rows.add(
            new Indicator(
                "Performance",
                "Maintenance préventive réalisée à temps",
                number(data.get("preventiveRate"), 2, " %")
            )
        );
        rows.add(
            new Indicator(
                "Performance",
                "Disponibilité des équipements",
                number(data.get("availability"), 2, " %")
            )
        );
        rows.add(
            new Indicator(
                "Ressources",
                "Articles sous le seuil de stock disponible",
                number(data.get("lowStock"), 0, "")
            )
        );
        rows.add(
            new Indicator(
                "Ressources",
                "Équipements indisponibles",
                number(data.get("unavailableAssets"), 0, "")
            )
        );
        if (data.containsKey("partsCost")) {
            rows.add(
                new Indicator(
                    "Coûts",
                    "Coût des pièces consommées",
                    number(data.get("partsCost"), 3, " TND")
                )
            );
            rows.add(new Indicator("Coûts", "Périmètre du coût", COST_NOTE));
        }
        return List.copyOf(rows);
    }

    static String date(Object value) {
        return value instanceof Instant instant ? DATE.format(instant) : "Non disponible";
    }

    private static String number(Object value, int digits, String unit) {
        if (!(value instanceof Number)) return "Non disponible";
        var format = NumberFormat.getNumberInstance(FRENCH);
        format.setMinimumFractionDigits(digits);
        format.setMaximumFractionDigits(digits);
        // Ordinary spaces remain legible with the portable PDF font and text extraction.
        return format.format(value).replace('\u202f', ' ').replace('\u00a0', ' ') + unit;
    }

    static byte[] pdf(Map<?, ?> data) throws Exception {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 40, 40, 34, 46);
        var writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(
            new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter pdf, Document page) {
                    var canvas = pdf.getDirectContent();
                    canvas.setColorStroke(new Color(218, 227, 234));
                    canvas.moveTo(page.left(), 35);
                    canvas.lineTo(page.right(), 35);
                    canvas.stroke();
                    ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_LEFT,
                        new Phrase(
                            "POLYCLINIQUE  /  Rapport de maintenance",
                            font(8, Font.NORMAL, MUTED)
                        ),
                        page.left(),
                        23,
                        0
                    );
                    ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_RIGHT,
                        new Phrase("Page " + pdf.getPageNumber(), font(8, Font.NORMAL, MUTED)),
                        page.right(),
                        23,
                        0
                    );
                }
            }
        );
        document.addTitle("Rapport de maintenance — Polyclinique");
        document.addAuthor("GMAO Polyclinique");
        document.open();

        var banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        var heading = new PdfPCell();
        heading.setBorder(Rectangle.NO_BORDER);
        heading.setBackgroundColor(NAVY);
        heading.setPadding(14);
        heading.addElement(
            new Paragraph("POLYCLINIQUE  /  GMAO", font(9, Font.BOLD, new Color(156, 221, 221)))
        );
        heading.addElement(
            new Paragraph("Rapport de maintenance", font(24, Font.BOLD, Color.WHITE))
        );
        var subtitle = new Paragraph(
            "Synthèse de l’activité et des indicateurs",
            font(10, Font.NORMAL, Color.WHITE)
        );
        subtitle.setSpacingBefore(4);
        heading.addElement(subtitle);
        banner.addCell(heading);
        document.add(banner);

        var period = new Paragraph(
            "Période : du " + date(data.get("from")) + " au " + date(data.get("to")),
            font(10, Font.BOLD, NAVY)
        );
        period.setSpacingBefore(14);
        document.add(period);
        var generated = new Paragraph(
            "Édité le " + date(Instant.now()) + " · Heure de Tunis",
            font(8, Font.NORMAL, MUTED)
        );
        generated.setSpacingBefore(4);
        generated.setSpacingAfter(10);
        document.add(generated);

        var rows = indicators(data);
        var cards = new PdfPTable(4);
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(4);
        for (var row : rows.subList(0, 4)) {
            var card = new PdfPCell();
            card.setBorderColor(Color.WHITE);
            card.setBorderWidth(3);
            card.setBackgroundColor(PALE);
            card.setPadding(9);
            card.addElement(new Paragraph(row.value(), font(21, Font.BOLD, TEAL)));
            card.addElement(new Paragraph(row.label(), font(8, Font.NORMAL, NAVY)));
            cards.addCell(card);
        }
        document.add(cards);
        section(
            document,
            "Performance de maintenance",
            rows
                .stream()
                .filter(r -> r.section().equals("Performance"))
                .toList()
        );
        section(
            document,
            "Stock et équipements",
            rows
                .stream()
                .filter(r -> r.section().equals("Ressources"))
                .toList()
        );

        if (data.containsKey("partsCost")) {
            title(document, "Coûts de maintenance");
            var costs = new PdfPTable(1);
            costs.setWidthPercentage(100);
            costs.setSpacingBefore(10);
            var cost = new PdfPCell();
            cost.setBorder(Rectangle.NO_BORDER);
            cost.setBackgroundColor(new Color(233, 246, 243));
            cost.setPadding(11);
            cost.addElement(new Paragraph("Coût des pièces consommées", font(10, Font.BOLD, NAVY)));
            cost.addElement(
                new Paragraph(number(data.get("partsCost"), 3, " TND"), font(22, Font.BOLD, TEAL))
            );
            var note = new Paragraph(COST_NOTE, font(9, Font.NORMAL, MUTED));
            note.setSpacingBefore(6);
            cost.addElement(note);
            costs.addCell(cost);
            document.add(costs);
        }
        var note = new Paragraph(
            "Lecture du rapport : les indicateurs de flux portent sur la période sélectionnée. Les compteurs de stock et les équipements indisponibles décrivent l’état actuel. « Non disponible » indique qu’aucune donnée ne permet de calculer l’indicateur.",
            font(8, Font.NORMAL, MUTED)
        );
        note.setSpacingBefore(10);
        note.setLeading(11);
        document.add(note);
        document.close();
        return out.toByteArray();
    }

    private static void section(
        Document document,
        String label,
        List<Indicator> rows
    ) throws DocumentException {
        title(document, label);
        var table = new PdfPTable(new float[] { 3.5f, 1.2f });
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setHeaderRows(1);
        table.addCell(cell("Indicateur", NAVY, Color.WHITE, true, Element.ALIGN_LEFT));
        table.addCell(cell("Valeur", NAVY, Color.WHITE, true, Element.ALIGN_RIGHT));
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            var background = i % 2 == 0 ? PALE : Color.WHITE;
            table.addCell(cell(row.label(), background, NAVY, false, Element.ALIGN_LEFT));
            table.addCell(cell(row.value(), background, TEAL, true, Element.ALIGN_RIGHT));
        }
        document.add(table);
    }

    private static void title(Document document, String text) throws DocumentException {
        var title = new Paragraph(text, font(12, Font.BOLD, NAVY));
        title.setSpacingBefore(11);
        title.setSpacingAfter(6);
        title.setKeepTogether(true);
        document.add(title);
    }

    private static PdfPCell cell(
        String text,
        Color background,
        Color foreground,
        boolean bold,
        int align
    ) {
        var cell = new PdfPCell(
            new Phrase(text, font(10, bold ? Font.BOLD : Font.NORMAL, foreground))
        );
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(background);
        cell.setPadding(7);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private static Font font(float size, int style, Color color) {
        return FontFactory.getFont(
            FontFactory.HELVETICA,
            BaseFont.WINANSI,
            false,
            size,
            style,
            color
        );
    }
}
