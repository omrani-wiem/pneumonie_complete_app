package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@SuppressWarnings("deprecation")
@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] generate(Analysis a) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font small = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

            Paragraph heading = new Paragraph("Rapport d'analyse radiographique", title);
            heading.setSpacingAfter(16);
            doc.add(heading);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35, 65});
            addRow(table, "Analyse n°", String.valueOf(a.getId()), bold, normal);
            addRow(table, "Date", DATE.format(a.getCreatedAt()), bold, normal);
            addRow(table, "Fichier", a.getFilename() == null ? "-" : a.getFilename(), bold, normal);
            addRow(table, "Résultat", a.getLabel(), bold, normal);
            addRow(table, "Probabilité de pneumonie",
                    String.format(Locale.FRANCE, "%.1f %%", a.getProbabilityPneumonia() * 100),
                    bold, normal);
            doc.add(table);

            Paragraph mapTitle = new Paragraph("Zones influençant la prédiction (Grad-CAM)", bold);
            mapTitle.setSpacingBefore(20);
            mapTitle.setSpacingAfter(8);
            doc.add(mapTitle);

            if (a.getHeatmapBase64() != null && !a.getHeatmapBase64().isBlank()) {
                Image img = Image.getInstance(Base64.getDecoder().decode(a.getHeatmapBase64()));
                img.scaleToFit(320, 320);
                img.setAlignment(Element.ALIGN_CENTER);
                doc.add(img);
                Paragraph legend = new Paragraph(
                        "Les zones rouges et jaunes sont celles qui ont le plus pesé dans la décision du modèle.",
                        small);
                legend.setAlignment(Element.ALIGN_CENTER);
                legend.setSpacingBefore(6);
                doc.add(legend);
            } else {
                doc.add(new Paragraph("Heatmap non disponible pour cette analyse.", normal));
            }

            Paragraph disclaimer = new Paragraph(
                    "Projet éducatif. Ce rapport est généré par un modèle d'intelligence artificielle "
                            + "et ne constitue pas un diagnostic médical.", small);
            disclaimer.setSpacingBefore(30);
            doc.add(disclaimer);

            doc.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
        return out.toByteArray();
    }

    private void addRow(PdfPTable table, String label, String value, Font bold, Font normal) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, bold));
        PdfPCell c2 = new PdfPCell(new Phrase(value, normal));
        c1.setPadding(6);
        c2.setPadding(6);
        table.addCell(c1);
        table.addCell(c2);
    }
}