package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.Patient;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
@Service
public class PatientRecordPdfService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter BIRTH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Patient p, List<Analysis> analysesNewestFirst) {
        List<Analysis> list = new ArrayList<>(analysesNewestFirst);
        Collections.reverse(list); // ordre chronologique
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font bannerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bc = new PdfPCell(new Phrase(PdfReportService.BANNER, bannerFont));
            bc.setBackgroundColor(new Color(0x9B, 0x1C, 0x1C));
            bc.setPadding(8);
            bc.setBorder(0);
            bc.setHorizontalAlignment(Element.ALIGN_CENTER);
            banner.addCell(bc);
            doc.add(banner);

            Paragraph heading = new Paragraph("Dossier patient : synthèse des analyses", title);
            heading.setSpacingBefore(12);
            heading.setSpacingAfter(10);
            doc.add(heading);

            Period per = Period.between(p.getBirthDate(), LocalDate.now());
            int months = per.getYears() * 12 + per.getMonths();
            PdfPTable id = new PdfPTable(2);
            id.setWidthPercentage(100);
            id.setWidths(new float[]{30, 70});
            row(id, "Nom", p.getLastName() + " " + p.getFirstName(), bold, normal);
            row(id, "N° de dossier", p.getRecordNumber(), bold, normal);
            row(id, "Naissance", BIRTH.format(p.getBirthDate()) + " (" + age(months) + ")", bold, normal);
            row(id, "Sexe", "F".equals(p.getSex()) ? "Féminin" : "Masculin", bold, normal);
            if (months < 12 || months >= 72)
                row(id, "Attention", "Âge hors de la plage du modèle (1 à 5 ans) : résultats non validés.", bold, normal);
            if (p.getNotes() != null) row(id, "Notes", p.getNotes(), bold, normal);
            doc.add(id);

            Paragraph sec = new Paragraph("Analyses (" + list.size() + ")", h2);
            sec.setSpacingBefore(16);
            sec.setSpacingAfter(8);
            doc.add(sec);

            if (list.isEmpty()) {
                doc.add(new Paragraph("Aucune analyse pour ce patient.", normal));
            } else {
                PdfPTable t = new PdfPTable(5);
                t.setWidthPercentage(100);
                t.setWidths(new float[]{15, 25, 22, 20, 18});
                t.setHeaderRows(1);
                for (String h : new String[]{"Date", "Résultat", "Type (indice du modèle)", "Contexte clinique", "Avis du médecin"})
                    t.addCell(cell(h, small, new Color(0xE2, 0xE8, 0xF0)));
                for (Analysis a : list) {
                    boolean pn = "PNEUMONIA".equals(a.getLabel());
                    double pct = a.getProbabilityPneumonia() * 100;
                    t.addCell(cell(DATE.format(a.getCreatedAt()), normal, null));
                    t.addCell(cell(pn
                            ? String.format(Locale.FRANCE, "Pneumonie suspectée %.1f %%", pct)
                            : String.format(Locale.FRANCE, "Pas de pneumonie (%.1f %%)", pct), normal, null));
                    t.addCell(cell(typeText(a), normal, null));
                    t.addCell(cell(context(a), normal, null));
                    t.addCell(cell(opinion(a), normal, null));
                }
                doc.add(t);
            }

            Paragraph note = new Paragraph("Le type de pneumonie est une orientation à fiabilité limitée ; l'indice de "
                    + "confiance du modèle n'est pas une probabilité calibrée. Les heatmaps ne sont pas incluses.", normal);
            note.setSpacingBefore(10);
            doc.add(note);
            Paragraph disc = new Paragraph("Avertissement. " + PdfReportService.DISCLAIMER, bold);
            disc.setSpacingBefore(18);
            doc.add(disc);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
        return out.toByteArray();
    }

    private static String typeText(Analysis a) {
        String t = a.getPneumoniaType();
        if (t == null) return "non évalué";
        return switch (t) {
            case "VIRAL" -> conf(a, "probablement virale");
            case "BACTERIAL" -> conf(a, "probablement bactérienne");
            case "UNDETERMINED" -> "indéterminé (abstention)";
            default -> "non évalué";
        };
    }

    private static String conf(Analysis a, String label) {
        return a.getTypeConfidence() == null ? label
                : label + String.format(Locale.FRANCE, " (%.0f %%)", a.getTypeConfidence() * 100);
    }

    private static String context(Analysis a) {
        List<String> parts = new ArrayList<>();
        if (a.getTemperatureC() != null) parts.add(String.format(Locale.FRANCE, "%.1f °C", a.getTemperatureC()));
        if (a.getSpo2Percent() != null) parts.add("SpO2 " + a.getSpo2Percent() + " %");
        if (a.getSymptomDays() != null) parts.add(a.getSymptomDays() + " j");
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private static String opinion(Analysis a) {
        if (a.getDoctorOpinion() == null) return "Aucun";
        String base = "AGREE".equals(a.getDoctorOpinion()) ? "D'accord" : "Pas d'accord";
        return a.getDoctorComment() == null ? base : base + " : " + a.getDoctorComment();
    }

    private static String age(int months) {
        if (months < 24) return months + " mois";
        int y = months / 12, m = months % 12;
        return m == 0 ? y + " ans" : y + " ans " + m + " mois";
    }

    private static PdfPCell cell(String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(5);
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private static void row(PdfPTable t, String l, String v, Font b, Font n) {
        t.addCell(cell(l, b, null));
        t.addCell(cell(v, n, null));
    }
}