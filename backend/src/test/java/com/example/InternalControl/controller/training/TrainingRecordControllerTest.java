package com.example.InternalControl.controller.training;

import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.training.TrainingRecordService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = TrainingRecordController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TrainingRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingRecordService trainingRecordService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    private TrainingRecord mockTraining;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockTraining = TrainingRecord.builder()
                .trainingRecordId(1L)
                .orgNumber(937219997)
                .build();
    }

    @Test
    void getAllTrainingRecords_AsAdmin_ReturnsOk() throws Exception {
        List<TrainingRecord> records = Arrays.asList(mockTraining);
        when(trainingRecordService.getTrainingRecordsByOrg(anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/training")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAllTrainingRecords_AsManager_ReturnsOk() throws Exception {
        List<TrainingRecord> records = Arrays.asList(mockTraining);
        when(trainingRecordService.getTrainingRecordsByOrg(anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/training")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllTrainingRecords_ReturnsOk() throws Exception {
        List<TrainingRecord> records = Arrays.asList(mockTraining);
        when(trainingRecordService.getTrainingRecordsByOrg(anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/training")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getTrainingRecordById_Existing_ReturnsOk() throws Exception {
        when(trainingRecordService.getTrainingRecord(anyLong(), anyInt())).thenReturn(mockTraining);

        mockMvc.perform(get("/api/v1/training/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getTrainingRecordsByUser_ReturnsUserRecords() throws Exception {
        List<TrainingRecord> records = Arrays.asList(mockTraining);
        when(trainingRecordService.getTrainingRecordsByUserAndOrg(anyLong(), anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/training/user/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getExpiringTraining_ReturnsExpiringRecords() throws Exception {
        List<TrainingRecord> records = Arrays.asList(mockTraining);
        when(trainingRecordService.getExpiringTrainingRecords(anyInt(), anyInt())).thenReturn(records);

        mockMvc.perform(get("/api/v1/training/expiring")
                        .param("orgNumber", "937219997")
                        .param("days", "30"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTrainingRecord_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/training/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTrainingStatistics_AsAdmin_ReturnsOk() throws Exception {
        when(trainingRecordService.getExpiringCount(anyInt())).thenReturn(5L);

        mockMvc.perform(get("/api/v1/training/expiring/count")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }
}
