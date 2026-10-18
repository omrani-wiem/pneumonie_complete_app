package com.example.pneumonia;

import com.example.pneumonia.entity.AppUser;
import com.example.pneumonia.entity.Patient;
import com.example.pneumonia.entity.Role;
import com.example.pneumonia.ml.MlClient;
import com.example.pneumonia.repository.AnalysisRepository;
import com.example.pneumonia.repository.PatientRepository;
import com.example.pneumonia.repository.UserRepository;
import com.example.pneumonia.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret=test-secret-test-secret-test-secret-1234",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.admin.email=",
        "app.admin.password=",
        "app.login.max-attempts=100",
        "app.analysis.max-per-minute=1000",
        "ml.service.url=http://localhost:1",
        "ml.service.secret=test"
})
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected PatientRepository patients;
    @Autowired protected AnalysisRepository analyses;
    @Autowired protected JwtService jwt;
    @Autowired protected PasswordEncoder encoder;
    @MockitoBean protected MlClient ml;

    @BeforeEach
    void cleanDatabase() {
        analyses.deleteAll();
        patients.deleteAll();
        users.deleteAll();
    }

    protected AppUser user(String email, Role role, boolean enabled) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("Mot-de-passe-1!"));
        u.setRole(role);
        u.setEnabled(enabled);
        return users.save(u);
    }

    protected String bearer(AppUser u) {
        return "Bearer " + jwt.generate(u);
    }

    protected Patient patient(AppUser owner, String record) {
        Patient p = new Patient();
        p.setOwner(owner);
        p.setRecordNumber(record);
        p.setLastName("Test");
        p.setFirstName("Patient");
        p.setBirthDate(LocalDate.now().minusYears(3));
        p.setSex("F");
        return patients.save(p);
    }
}