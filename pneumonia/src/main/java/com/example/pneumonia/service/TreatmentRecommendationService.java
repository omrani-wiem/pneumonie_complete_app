package com.example.pneumonia.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pistes de prise en charge à discuter selon le type estimé de pneumonie.
 *
 * ATTENTION : rappels généraux inspirés de recommandations publiques (OMS, IDSA/ATS).
 * Aucune posologie. Ne remplacent pas le jugement du médecin : âge, poids, allergies,
 * fonction rénale, grossesse, comorbidités, gravité et résistances locales changent la décision.
 * Le contenu doit être relu et validé par un médecin avant tout usage réel.
 */
@Service
public class TreatmentRecommendationService {

    public record Recommendation(String typeLabel, String summary,
                                 List<String> confirmatoryWork, List<String> treatment,
                                 List<String> supportive, List<String> followUp,
                                 List<String> redFlags, String limits) {}

    static final String LIMITS =
            "Le type est estimé à partir de l'image seule. Même pour des radiologues, distinguer "
                    + "une pneumonie bactérienne d'une pneumonie virale sur une radiographie est difficile, et "
                    + "le germe exact (pneumocoque, Mycoplasma, virus de la grippe, SARS-CoV-2, VRS, aspiration, "
                    + "champignon...) ne peut pas être identifié ainsi. Le modèle a été entraîné sur des "
                    + "radiographies pédiatriques d'un seul centre : sa fiabilité chez l'adulte n'est pas établie.";

    private static final List<String> RED_FLAGS = List.of(
            "Saturation en oxygène basse (environ moins de 92 à 94 %) ou besoin d'oxygène",
            "Respiration rapide, tirage, geignement, cyanose, signes de détresse respiratoire",
            "Confusion, somnolence, hypotension, déshydratation, impossibilité de boire ou de s'alimenter",
            "Nourrisson très jeune, immunodépression, comorbidité grave, échec d'un traitement après 48 à 72 h",
            "Un ou plusieurs de ces signes justifient une évaluation en urgence et souvent une hospitalisation");

    private static final List<String> SUPPORTIVE = List.of(
            "Hydratation, repos, antipyrétique/antalgique adapté à l'âge",
            "Oxygénothérapie si hypoxémie");

    public Recommendation recommend(String type) {
        if ("BACTERIAL".equals(type)) return bacterial();
        if ("VIRAL".equals(type)) return viral();
        return undetermined(type);
    }

    private Recommendation bacterial() {
        return new Recommendation(
                "Pneumonie probablement bactérienne",
                "Le profil de l'image évoque plutôt une origine bactérienne. Une antibiothérapie est "
                        + "généralement envisagée, à décider par le médecin selon la clinique.",
                List.of("Examen clinique, constantes et saturation en oxygène",
                        "NFS, CRP (± procalcitonine) pour appuyer l'origine bactérienne",
                        "Hémocultures et recherche étiologique si forme sévère ou hospitalisation",
                        "Rechercher épanchement pleural ou complication si évolution défavorable"),
                List.of("Antibiothérapie probabiliste à discuter. Forme non sévère : amoxicilline par voie orale "
                                + "est classiquement la première intention chez l'enfant, et une option chez "
                                + "l'adulte selon le terrain (alternatives : macrolide ou doxycycline, par exemple en cas d'allergie).",
                        "Forme sévère ou hospitalisée : bêta-lactamine par voie intraveineuse, associée ou non à un macrolide.",
                        "Adapter au germe, à l'antibiogramme et aux résistances locales dès que les résultats sont connus."),
                SUPPORTIVE,
                List.of("Réévaluer la réponse clinique à 48-72 h",
                        "Si pas d'amélioration : revoir le diagnostic, chercher une complication (épanchement, empyème) "
                                + "ou un germe atypique (Mycoplasma, Chlamydophila)"),
                RED_FLAGS, LIMITS);
    }

    private Recommendation viral() {
        return new Recommendation(
                "Pneumonie probablement virale",
                "Le profil de l'image évoque plutôt une origine virale. La prise en charge est le plus "
                        + "souvent symptomatique ; les antibiotiques ne sont pas systématiques.",
                List.of("Examen clinique, constantes et saturation en oxygène",
                        "Recherche virale selon la saison et le contexte : grippe, SARS-CoV-2, VRS (PCR ou test rapide)",
                        "CRP/NFS en cas de doute sur une surinfection bactérienne"),
                List.of("Traitement principalement symptomatique.",
                        "Antiviral spécifique si un agent est identifié et si les critères sont remplis "
                                + "(par exemple oseltamivir pour la grippe, idéalement tôt, ou en cas de forme sévère). "
                                + "COVID-19 : selon les protocoles en vigueur.",
                        "Antibiotiques seulement si suspicion de surinfection bactérienne (fièvre persistante ou "
                                + "qui repart, CRP/procalcitonine élevées, condensation lobaire, aggravation)."),
                SUPPORTIVE,
                List.of("Surveiller l'évolution, notamment l'apparition d'une détresse respiratoire",
                        "Réévaluer en cas de fièvre persistante au-delà de quelques jours ou d'aggravation"),
                RED_FLAGS, LIMITS);
    }

    private Recommendation undetermined(String type) {
        boolean notAssessed = type == null;
        return new Recommendation(
                notAssessed ? "Type de pneumonie non évalué" : "Type de pneumonie indéterminé",
                notAssessed
                        ? "Le modèle déployé détecte la pneumonie mais n'estime pas son type."
                        : "Le modèle n'a pas assez de confiance pour distinguer une origine bactérienne d'une origine virale.",
                List.of("Examen clinique, constantes et saturation en oxygène",
                        "NFS, CRP (± procalcitonine)",
                        "Recherche virale selon le contexte (grippe, SARS-CoV-2, VRS)",
                        "Hémocultures et autres examens si forme sévère"),
                List.of("Ne pas décider du traitement sur la seule base de ce rapport.",
                        "La décision d'antibiothérapie probabiliste revient au médecin, selon la gravité, "
                                + "l'âge, le terrain et les résultats biologiques."),
                SUPPORTIVE,
                List.of("Réévaluer à 48-72 h ou plus tôt en cas d'aggravation"),
                RED_FLAGS, LIMITS);
    }
}