package com.example.pneumonia.service;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.Patient;
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
import java.time.LocalDate;
import java.time.Period;
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
    private static final DateTimeFormatter BIRTH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            double p = a.getProbabilityPneumonia();
            Patient patient = a.getPatient();

            // 1. Bandeau d'avertissement en tête de la première page
            doc.add(banner(BANNER, bannerFont, new Color(0x9B, 0x1C, 0x1C)));

            Paragraph heading = new Paragraph("Rapport d'analyse radiographique", title);
            heading.setSpacingBefore(14);
            heading.setSpacingAfter(12);
            doc.add(heading);

            // 2. Patient
            addSection(doc, "Patient", h2);
            if (patient != null) {
                int months = ageMonths(patient);
                PdfPTable pt = new PdfPTable(2);
                pt.setWidthPercentage(100);
                pt.setWidths(new float[]{35, 65});
                addRow(pt, "Nom", patient.getLastName() + " " + patient.getFirstName(), bold, normal);
                addRow(pt, "N° de dossier", patient.getRecordNumber(), bold, normal);
                addRow(pt, "Naissance", BIRTH.format(patient.getBirthDate()) + " (" + ageText(months) + ")", bold, normal);
                addRow(pt, "Sexe", "F".equals(patient.getSex()) ? "Féminin" : "Masculin", bold, normal);
                doc.add(pt);
                if (months < 12 || months >= 72) {
                    Paragraph gap = new Paragraph(" ", small);
                    doc.add(gap);
                    doc.add(banner("ÂGE HORS DE LA PLAGE DU MODÈLE (1 à 5 ans) : résultat non validé pour cet âge, "
                            + "à interpréter avec une grande prudence.", bannerFont, new Color(0xB5, 0x47, 0x08)));
                }
            } else {
                doc.add(new Paragraph("Patient non renseigné (analyse antérieure à la gestion des patients).", normal));
            }

            // 3. Analyse et résultat du modèle
            addSection(doc, "Résultat du modèle", h2);
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35, 65});
            addRow(table, "Analyse n°", String.valueOf(a.getId()), bold, normal);
            addRow(table, "Date", DATE.format(a.getCreatedAt()), bold, normal);
            addRow(table, "Fichier", a.getFilename() == null ? "-" : a.getFilename(), bold, normal);
            double pPneu = p * 100;
            addRow(table, "Résultat",
                    pneumonia
                            ? String.format(Locale.FRANCE, "Pneumonie suspectée %.1f %%", pPneu)
                            : String.format(Locale.FRANCE,
                                    "Pas de pneumonie détectée (probabilité de pneumonie : %.1f %%)", pPneu),
                    bold, normal);
            addRow(table, "Type de pneumonie", typeText(a, reco), bold, normal);
            addRow(table, "Version du modèle",
                    a.getModelVersion() == null ? "non renseignée" : a.getModelVersion(), bold, normal);
            doc.add(table);
            if (p >= 0.4 && p <= 0.6) {
                Paragraph near = new Paragraph("Résultat proche du seuil de décision (50 %) : peu discriminant, "
                        + "à confronter à la clinique.", small);
                near.setSpacingBefore(6);
                doc.add(near);
            }
            Paragraph reliab = new Paragraph("Le type de pneumonie est une orientation à fiabilité limitée. "
                    + "L'indice de confiance du modèle n'est pas une probabilité calibrée.", small);
            reliab.setSpacingBefore(4);
            doc.add(reliab);

            // 4. Contexte clinique
            if (a.getTemperatureC() != null || a.getSpo2Percent() != null || a.getSymptomDays() != null) {
                addSection(doc, "Contexte clinique", h2);
                PdfPTable ct = new PdfPTable(2);
                ct.setWidthPercentage(100);
                ct.setWidths(new float[]{35, 65});
                if (a.getTemperatureC() != null)
                    addRow(ct, "Température", String.format(Locale.FRANCE, "%.1f °C", a.getTemperatureC()), bold, normal);
                if (a.getSpo2Percent() != null)
                    addRow(ct, "Saturation en oxygène", a.getSpo2Percent() + " %", bold, normal);
                if (a.getSymptomDays() != null)
                    addRow(ct, "Durée des symptômes", a.getSymptomDays() + " jour(s)", bold, normal);
                doc.add(ct);
            }

            // 5. Pistes de prise en charge : seulement s'il y a pneumonie
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

            // 6. Grad-CAM
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

            // 7. Avis du médecin, séparé du résultat du modèle
            addSection(doc, "Avis du médecin (distinct du résultat du modèle)", h2);
            if (a.getDoctorOpinion() == null) {
                doc.add(new Paragraph("Aucun avis enregistré.", normal));
            } else {
                PdfPTable ot = new PdfPTable(2);
                ot.setWidthPercentage(100);
                ot.setWidths(new float[]{35, 65});
                addRow(ot, "Avis", "AGREE".equals(a.getDoctorOpinion())
                        ? "D'accord avec l'orientation du modèle" : "Pas d'accord avec l'orientation du modèle", bold, normal);
                if (a.getDoctorComment() != null)
                    addRow(ot, "Commentaire", a.getDoctorComment(), bold, normal);
                if (a.getOpinionAt() != null)
                    addRow(ot, "Enregistré le", DATE.format(a.getOpinionAt()), bold, normal);
                doc.add(ot);
            }

            // 8. Avertissement complet en bas
            Paragraph disclaimer = new Paragraph("Avertissement. " + DISCLAIMER, bold);
            disclaimer.setSpacingBefore(24);
            doc.add(disclaimer);

            doc.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
        return out.toByteArray();
    }

    private static int ageMonths(Patient p) {
        Period per = Period.between(p.getBirthDate(), LocalDate.now());
        return per.getYears() * 12 + per.getMonths();
    }

    private static String ageText(int months) {
        if (months < 24) return months + " mois";
        int y = months / 12, m = months % 12;
        return m == 0 ? y + " ans" : y + " ans " + m + " mois";
    }

    private static String typeText(Analysis a, Recommendation reco) {
        String t = a.getPneumoniaType();
        String base = reco == null ? "Non évalué" : reco.typeLabel();
        if (("BACTERIAL".equals(t) || "VIRAL".equals(t)) && a.getTypeConfidence() != null) {
            return base + String.format(Locale.FRANCE, " (indice de confiance du modèle : %.0f %%)",
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
        p.setSpacingBefore(16);
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