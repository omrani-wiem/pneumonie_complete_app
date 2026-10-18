package com.example.pneumonia;

import com.example.pneumonia.entity.Analysis;
import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends IntegrationTestBase {

    private static final String BODY = """
            {"recordNumber":"X1","lastName":"A","firstName":"B","birthDate":"2023-01-01","sex":"F"}""";

    @Test
    void withoutCredentialsApiReturns401() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotReadAdminRoutes() throws Exception {
        AppUser doctor = user("doc@test.local", Role.USER, true);
        mvc.perform(get("/api/admin/audit").header("Authorization", bearer(doctor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAuditLog() throws Exception {
        AppUser admin = user("admin@test.local", Role.ADMIN, true);
        mvc.perform(get("/api/admin/audit").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void disabledAccountWithValidTokenIsRejected() throws Exception {
        AppUser u = user("off@test.local", Role.USER, false);
        mvc.perform(get("/api/patients").header("Authorization", bearer(u)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherDoctorCannotReadUpdateArchiveEraseOrExport() throws Exception {
        AppUser owner = user("owner@test.local", Role.USER, true);
        AppUser other = user("other@test.local", Role.USER, true);
        Patient p = patient(owner, "R-1");
        String auth = bearer(other);
        String base = "/api/patients/" + p.getId();

        mvc.perform(get(base).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/analyses").header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/export").header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(put(base).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
        mvc.perform(put(base + "/archived").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"archived\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(base + "?confirm=R-1").header("Authorization", auth))
                .andExpect(status().isNotFound());

        assertEquals(1, patients.count());
    }

    @Test
    void otherDoctorCannotReadAnalysisOrPdf() throws Exception {
        AppUser owner = user("owner@test.local", Role.USER, true);
        AppUser other = user("other@test.local", Role.USER, true);
        Analysis a = new Analysis();
        a.setOwner(owner);
        a.setPatient(patient(owner, "R-2"));
        a.setLabel("NORMAL");
        a.setProbabilityPneumonia(0.1);
        a = analyses.save(a);

        mvc.perform(get("/api/analyses/" + a.getId()).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/analyses/" + a.getId() + "/pdf").header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/analyses/" + a.getId() + "/opinion").header("Authorization", bearer(other))
                .contentType(MediaType.APPLICATION_JSON).content("{\"opinion\":\"AGREE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerErasesPatientOnlyWithCorrectRecordNumber() throws Exception {
        AppUser owner = user("owner@test.local", Role.USER, true);
        Patient p = patient(owner, "R-3");
        Analysis a = new Analysis();
        a.setOwner(owner);
        a.setPatient(p);
        a.setLabel("NORMAL");
        a.setProbabilityPneumonia(0.2);
        analyses.save(a);
        String auth = bearer(owner);

        mvc.perform(delete("/api/patients/" + p.getId() + "?confirm=faux").header("Authorization", auth))
                .andExpect(status().isBadRequest());
        assertEquals(1, patients.count());
        assertEquals(1, analyses.count());

        mvc.perform(delete("/api/patients/" + p.getId() + "?confirm=R-3").header("Authorization", auth))
                .andExpect(status().isNoContent());
        assertEquals(0, patients.count());
        assertEquals(0, analyses.count());
    }

    @Test
    void foreignOriginOnWriteRequestIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").header("Origin", "http://evil.example")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}