package re.edu.thuchanh.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import re.edu.thuchanh.application.dto.ApplicationResponse;
import re.edu.thuchanh.common.ConflictException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobApplicationController.class)
class JobApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JobApplicationService applicationService;

    private ApplicationResponse sample() {
        return new ApplicationResponse(7L, 1L, "Nguyen Van A", "a@example.com",
                "https://cv.example/a.pdf", Instant.now());
    }

    @Test
    void applyReturns201WithLocation() throws Exception {
        when(applicationService.apply(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(post("/jobs/1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyen Van A","email":"a@example.com",
                                 "cvUrl":"https://cv.example/a.pdf"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/jobs/1/applications/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void applyRejectsMissingFullNameWith400() throws Exception {
        mockMvc.perform(post("/jobs/1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"a@example.com",
                                 "cvUrl":"https://cv.example/a.pdf"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fullName"));

        verifyNoInteractions(applicationService);
    }

    @Test
    void applyRejectsMalformedEmailWith400() throws Exception {
        mockMvc.perform(post("/jobs/1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyen Van A","email":"not-an-email",
                                 "cvUrl":"https://cv.example/a.pdf"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(applicationService);
    }

    @Test
    void applyReturns409WhenServiceReportsConflict() throws Exception {
        when(applicationService.apply(eq(1L), any()))
                .thenThrow(new ConflictException("Job 1 is closed and no longer accepts applications"));

        mockMvc.perform(post("/jobs/1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Nguyen Van A","email":"a@example.com",
                                 "cvUrl":"https://cv.example/a.pdf"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
