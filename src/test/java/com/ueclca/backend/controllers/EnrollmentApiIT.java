package com.ueclca.backend.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.EnrollmentRepository;
import com.ueclca.backend.repositories.StudentRepository;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;

@AutoConfigureMockMvc
class EnrollmentApiIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ueclca.edu.ve";
    private static final String EDITOR_EMAIL = "editor@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";
    private static final int YEAR = LocalDate.now().getYear();
    private static final String SCHOOL_YEAR = YEAR + "-" + (YEAR + 1);
    private static final String BIRTH_DATE = (YEAR - 4) + "-03-15";
    private static final String ENROLLMENT_DATE = YEAR + "-09-15";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String editorToken;

    @BeforeEach
    void setUp() {
        seedUser(ADMIN_EMAIL, Role.ADMIN);
        seedUser(EDITOR_EMAIL, Role.EDITOR);
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();

        adminToken = authService.login(new LoginRequest(ADMIN_EMAIL, PASSWORD)).token();
        editorToken = authService.login(new LoginRequest(EDITOR_EMAIL, PASSWORD)).token();
    }

    @Test
    void publicCreateReturnsCreatedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Ana", "Pérez", BIRTH_DATE, "GROUP_II")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.schoolYear").value(SCHOOL_YEAR))
                .andExpect(jsonPath("$.groupName").value("GROUP_II"))
                .andExpect(jsonPath("$.student.firstNames").value("Ana"))
                .andExpect(jsonPath("$.mother.nationalId").value("V-12345678"))
                .andExpect(jsonPath("$.father.fullName").value("José Pérez"))
                .andExpect(jsonPath("$.documents", hasSize(0)));
    }

    @Test
    void listWithoutTokenReturnsUnauthorizedAndEditorForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListAndDetailReturnEnrollment() throws Exception {
        createValidEnrollment("Ana", "Pérez", BIRTH_DATE, "GROUP_II");

        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentFullName").value("Pérez, Ana"))
                .andExpect(jsonPath("$.content[0].motherName").value("María Pérez"));

        String body = mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.content[0].id");

        mockMvc.perform(get("/api/v1/enrollments/" + id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.lastNames").value("Pérez"))
                .andExpect(jsonPath("$.student.address").value("Calle India, Villa Asia"));
    }

    @Test
    void duplicateEnrollmentForSameStudentAndYearReturnsConflict() throws Exception {
        createValidEnrollment("Ana", "Pérez", BIRTH_DATE, "GROUP_II");

        mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Ana", "Pérez", BIRTH_DATE, "GROUP_II")))
                .andExpect(status().isConflict());
    }

    @Test
    void ageThatDoesNotMatchGroupReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Ana", "Pérez", (YEAR - 15) + "-03-15", "MATERNAL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidNationalIdReturnsBadRequest() throws Exception {
        String payload = validPayload("Ana", "Pérez", BIRTH_DATE, "GROUP_II")
                .replace("V-12345678", "12345678");

        mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesFieldsAndKeepsStudentIdentity() throws Exception {
        String created = createValidEnrollment("Ana", "Pérez", BIRTH_DATE, "GROUP_II");
        Integer id = JsonPath.read(created, "$.id");

        String payload = validPayload("Ana Sofía", "Pérez", BIRTH_DATE, "GROUP_II")
                .replace("\"address\":\"Calle India, Villa Asia\"", "\"address\":\"Calle Nueva, UD-236\"")
                .replace("\"responsibleType\":\"MOTHER\"", "\"responsibleType\":\"FATHER\"");

        mockMvc.perform(put("/api/v1/enrollments/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.firstNames").value("Ana Sofía"))
                .andExpect(jsonPath("$.student.address").value("Calle Nueva, UD-236"))
                .andExpect(jsonPath("$.responsibleType").value("FATHER"));
    }

    @Test
    void deleteRemovesEnrollmentAndStudent() throws Exception {
        String created = createValidEnrollment("Ana", "Pérez", BIRTH_DATE, "GROUP_II");
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/api/v1/enrollments/" + id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/enrollments/" + id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThat(studentRepository.count()).isZero();
    }

    @Test
    void publicUploadAndAdminDownloadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "partida.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        MvcResult upload = mockMvc.perform(multipart("/api/v1/public/enrollments/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("partida.pdf"))
                .andExpect(jsonPath("$.storageKey").isString())
                .andReturn();

        String storageKey = JsonPath.read(upload.getResponse().getContentAsString(), "$.storageKey");
        String payload = validPayload("Ana", "Pérez", BIRTH_DATE, "GROUP_II")
                .replace("\"documents\":[]", """
                        "documents":[{
                          "documentType":"BIRTH_CERTIFICATE","presented":true,
                          "storageKey":"%s","fileName":"partida.pdf",
                          "mimeType":"application/pdf","sizeBytes":8
                        }]
                        """.formatted(storageKey));

        String created = mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].fileName").value("partida.pdf"))
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/v1/enrollments/" + id + "/documents/BIRTH_CERTIFICATE")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    private String createValidEnrollment(String firstNames, String lastNames,
            String birthDate, String groupName) throws Exception {
        return mockMvc.perform(post("/api/v1/public/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(firstNames, lastNames, birthDate, groupName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String validPayload(String firstNames, String lastNames, String birthDate, String groupName) {
        return """
                {
                  "schoolYear":"%s",
                  "shift":"MORNING",
                  "groupName":"%s",
                  "enrollmentDate":"%s",
                  "student":{
                    "lastNames":"%s",
                    "firstNames":"%s",
                    "birthDate":"%s",
                    "birthPlace":"Puerto Ordaz",
                    "nationality":"Venezolana",
                    "sex":"FEMALE",
                    "heightCm":98.5,
                    "weightKg":16.2,
                    "schoolRegistryNumber":"RE-001",
                    "phone":"0414-1234567",
                    "address":"Calle India, Villa Asia"
                  },
                  "previousSchooling":"HOME",
                  "mother":{
                    "fullName":"María Pérez",
                    "nationalId":"V-12345678",
                    "address":"Calle India",
                    "birthDate":"1990-01-01",
                    "occupation":"Docente",
                    "works":true,
                    "employer":"Colegio",
                    "email":"maria@example.com",
                    "homePhone":"0286-1234567",
                    "mobilePhone":"0414-1234567"
                  },
                  "father":{
                    "fullName":"José Pérez",
                    "nationalId":"V-87654321",
                    "occupation":"Ingeniero",
                    "works":false,
                    "mobilePhone":"0416-1234567"
                  },
                  "responsibleType":"MOTHER",
                  "livesWithMother":true,
                  "livesWithFather":true,
                  "pickupType":"BOTH",
                  "allergies":"Polen",
                  "operated":false,
                  "psychologicalTreatment":false,
                  "commitmentAccepted":true,
                  "commitmentSignatureName":"María Pérez",
                  "commitmentSignatureDate":"%s",
                  "documents":[]
                }
                """.formatted(SCHOOL_YEAR, groupName, ENROLLMENT_DATE,
                lastNames, firstNames, birthDate, ENROLLMENT_DATE);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void seedUser(String email, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .fullName("Usuario de prueba " + role)
                    .role(role)
                    .enabled(true)
                    .build());
        }
    }
}
