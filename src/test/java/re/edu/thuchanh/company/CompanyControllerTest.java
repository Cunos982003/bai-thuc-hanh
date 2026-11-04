package re.edu.thuchanh.company;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.dto.CompanyResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CompanyService companyService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(companyService.create(any())).thenReturn(new CompanyResponse(1L, "Acme", null, null));

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/companies/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        verifyNoInteractions(companyService);
    }

    @Test
    void createRejectsOverlongNameWith400NotServerError() throws Exception {
        String longName = "a".repeat(300);

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(companyService);
    }

    @Test
    void getReturns404InStandardShapeWhenServiceThrows() throws Exception {
        when(companyService.get(9L)).thenThrow(new NotFoundException("Company 9 not found"));

        mockMvc.perform(get("/companies/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/companies/9"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/companies/3"))
                .andExpect(status().isNoContent());
    }
}
