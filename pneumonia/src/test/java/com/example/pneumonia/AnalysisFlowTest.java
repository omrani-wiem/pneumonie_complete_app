package com.example.pneumonia;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.ml.MlResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisFlowTest extends IntegrationTestBase {

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder req(
            Long patientId, MockMultipartFile file, AppUser u) {
        return (org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder)
                multipart("/api/analyses").file(file).param("patientId", String.valueOf(patientId))
                        .header("Authorization", bearer(u));
    }

    private MockMultipartFile image() throws Exception {
        return new MockMultipartFile("file", "x.png", "image/png", png());
    }

    @Test
    void unknownPatientReturns404() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        mvc.perform(req(9999L, image(), u)).andExpect(status().isNotFound());
    }

    @Test
    void archivedPatientReturns409() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        Patient p = patient(u, "A-1");
        p.setArchived(true);
        patients.save(p);
        mvc.perform(req(p.getId(), image(), u)).andExpect(status().isConflict());
    }

    @Test
    void fileThatIsNotAnImageReturns415() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        Patient p = patient(u, "A-2");
        MockMultipartFile fake = new MockMultipartFile("file", "x.png", "image/png", "pas une image".getBytes());
        mvc.perform(req(p.getId(), fake, u)).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void mlServiceDownReturns502WithoutSavingAnything() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        Patient p = patient(u, "A-3");
        when(ml.predict(ArgumentMatchers.any())).thenThrow(new RestClientException("http://interne:7860 refuse"));
        var res = mvc.perform(req(p.getId(), image(), u)).andExpect(status().isBadGateway())
                .andReturn().getResponse();
        assertEquals(0, analyses.count());
        org.junit.jupiter.api.Assertions.assertFalse(res.getContentAsString().contains("interne"));
    }

    @Test
    void successfulAnalysisIsSavedAndPdfIsGenerated() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        Patient p = patient(u, "A-4");
        MlResult r = mock(MlResult.class);
        when(r.label()).thenReturn("NORMAL");
        when(r.probabilityPneumonia()).thenReturn(0.12);
        when(ml.predict(ArgumentMatchers.any())).thenReturn(r);

        mvc.perform(req(p.getId(), image(), u)).andExpect(status().isOk());
        assertEquals(1, analyses.count());
        Long id = analyses.findAll().get(0).getId();
        mvc.perform(get("/api/analyses/" + id + "/pdf").header("Authorization", bearer(u)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidClinicalValuesAreRefused() throws Exception {
        AppUser u = user("d@test.local", Role.USER, true);
        Patient p = patient(u, "A-5");
        mvc.perform(req(p.getId(), image(), u).param("spo2Percent", "300")).andExpect(status().isBadRequest());
    }
}