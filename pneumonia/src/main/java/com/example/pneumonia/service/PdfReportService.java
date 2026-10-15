package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.service.TreatmentRecommendationService.Recommendation;
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    static final String BANNER =
            "OUTIL D'AIDE À LA DÉCISION - CE DOCUMENT N'EST PAS UN DIAGNOSTIC";
    static final String DISCLAIMER =
            "Ce rapport est généré automatiquement par un modèle d'intelligence artificielle. Il constitue une aide "
                    + "à la lecture destinée à un professionnel de santé et ne constitue en aucun cas un diagnostic médical "
                    + "ni une prescription. Seul un médecin, après examen clinique et analyse de la radiographie, peut "
                    + "poser un diagnostic et décider d'un traitement. Les pistes de prise en charge sont des rappels "
                    + "généraux, sans posologie, à adapter au patient.";

    public byte[] generate(Analysis a, Recommendation reco) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font small = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
            Font bannerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            boolean pneumonia = "PNEUMONIA".equals(a.getLabel());

            // Bandeaux d'avertissement en haut
            doc.add(banner(BANNER, bannerFont, new Color(0x9B, 0x1C, 0x1C)));

            Paragraph heading = new Paragraph("Rapport d'analyse radiographique", title);
            heading.setSpacingBefore(14);
            heading.setSpacingAfter(16);
            doc.add(heading);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35, 65});
            addRow(table, "Analyse n°", String.valueOf(a.getId()), bold, normal);
            addRow(table, "Date", DATE.format(a.getCreatedAt()), bold, normal);
            addRow(table, "Fichier", a.getFilename() == null ? "-" : a.getFilename(), bold, normal);
            addRow(table, "Résultat", pneumonia ? "Pneumonie suspectée" : "Pas de pneumonie détectée", bold, normal);
            addRow(table, "Probabilité de pneumonie",
                    String.format(Locale.FRANCE, "%.1f %%", a.getProbabilityPneumonia() * 100),
                    bold, normal);
            if (pneumonia) {
                addRow(table, "(indice de confiance du modèle : %.0f %%)", typeText(a, reco), bold, normal);
            }
            doc.add(table);

            // Pistes de prise en charge : seulement s'il y a pneumonie
            if (pneumonia && reco != null) {
                addSection(doc, "Pistes de prise en charge à discuter (suggestions)", h2);
                doc.add(new Paragraph(reco.summary(), normal));
                addList(doc, "Examens pour confirmer et préciser", reco.confirmatoryWork(), bold, normal);
                addList(doc, "Traitement : points à considérer", reco.treatment(), bold, normal);
                addList(doc, "Mesures de soutien", reco.supportive(), bold, normal);
                addList(doc, "Suivi", reco.followUp(), bold, normal);
                addList(doc, "Signes de gravité : évaluation urgente", reco.redFlags(), bold, normal);
                Paragraph limits = new Paragraph("Limites : " + reco.limits(), small);
                limits.setSpacingBefore(8);
                doc.add(limits);
            }

            addSection(doc, "Zones influençant la prédiction (Grad-CAM)", h2);
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

            // Avertissement complet en bas
            Paragraph disclaimer = new Paragraph("Avertissement. " + DISCLAIMER, bold);
            disclaimer.setSpacingBefore(24);
            doc.add(disclaimer);

            doc.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
        return out.toByteArray();
    }

    private static String typeText(Analysis a, Recommendation reco) {
        String t = a.getPneumoniaType();
        String base = reco == null ? "Non évalué" : reco.typeLabel();
        if (("BACTERIAL".equals(t) || "VIRAL".equals(t)) && a.getTypeConfidence() != null) {
            return base + String.format(Locale.FRANCE, " (confiance du modèle : %.0f %%)",
                    a.getTypeConfidence() * 100);
        }
        return base;
    }

    private static PdfPTable banner(String text, Font font, Color bg) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(0);
        t.addCell(c);
        return t;
    }

    private static void addSection(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(20);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private static void addList(Document doc, String title, List<String> items, Font bold, Font normal)
            throws DocumentException {
        if (items == null || items.isEmpty()) return;
        Paragraph t = new Paragraph(title, bold);
        t.setSpacingBefore(8);
        doc.add(t);
        for (String it : items) {
            Paragraph p = new Paragraph("- " + it, normal);
            p.setIndentationLeft(12);
            doc.add(p);
        }
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