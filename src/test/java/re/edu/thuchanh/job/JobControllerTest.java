package re.edu.thuchanh.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import re.edu.thuchanh.common.BadRequestException;
import re.edu.thuchanh.common.PageResponse;
import re.edu.thuchanh.job.dto.JobResponse;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JobService jobService;

    private JobResponse sample() {
        return new JobResponse(1L, "Backend Dev", "Build APIs", 1000L, 2000L, "Hanoi",
                JobStatus.OPEN, 1L, "Acme", Instant.now());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(jobService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Dev","salaryMin":1000,"salaryMax":2000,
                                 "location":"Hanoi","status":"OPEN","companyId":1}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/jobs/1"))
                .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    void createRejectsInvertedSalaryRangeWith400() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Dev","salaryMin":5000,"salaryMax":1000,
                                 "status":"OPEN","companyId":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(jobService);
    }

    @Test
    void createRejectsUnknownStatusWith400() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Dev","status":"BANANA","companyId":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(jobService);
    }

    @Test
    void getReturnsJob() throws Exception {
        when(jobService.get(1L)).thenReturn(sample());

        mockMvc.perform(get("/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(jobService.update(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(put("/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Dev","status":"CLOSED","companyId":1}"""))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/jobs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchReturnsEnvelope() throws Exception {
        when(jobService.search(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void searchRejectsNegativePageWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(jobService);
    }

    @Test
    void searchRejectsZeroSizeWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobService);
    }

    @Test
    void searchRejectsUnknownSortWith400() throws Exception {
        when(jobService.search(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new BadRequestException("Unsupported sort field: password"));

        mockMvc.perform(get("/jobs").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchRejectsUnknownStatusWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(jobService);
    }

    @Test
    void searchRejectsNegativePageWithFieldDetails() throws Exception {
        mockMvc.perform(get("/jobs").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }
}
