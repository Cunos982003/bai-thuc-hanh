package re.edu.thuchanh.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import re.edu.thuchanh.common.BadRequestException;
import re.edu.thuchanh.common.PageResponse;
import re.edu.thuchanh.company.Company;
import re.edu.thuchanh.company.CompanyRepository;
import re.edu.thuchanh.job.dto.JobResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceSearchTest {

    @Mock JobRepository jobRepository;
    @Mock CompanyRepository companyRepository;
    @InjectMocks JobService jobService;

    @Test
    void searchPassesFiltersToRepositoryAndWrapsPage() {
        Job job = Job.create("Backend Dev", "Build APIs", 1000L, 2000L,
                "Hanoi", JobStatus.OPEN, Company.create("Acme", null, null));
        when(jobRepository.search(eq("%backend%"), eq("hanoi"), eq(JobStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        PageResponse<JobResponse> result =
                jobService.search("Backend", "Hanoi", JobStatus.OPEN, 0, 20, "title");

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void searchTreatsBlankKeywordAndLocationAsNoFilter() {
        when(jobRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.search("  ", "", null, 0, 20, "title");

        verify(jobRepository).search(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void searchAcceptsEveryWhitelistedSortField() {
        when(jobRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        for (String field : List.of("title", "salaryMin", "salaryMax", "location", "createdAt")) {
            jobService.search(null, null, null, 0, 20, field);
        }
    }

    @Test
    void searchRejectsUnknownSortFieldWithBadRequest() {
        assertThatThrownBy(() -> jobService.search(null, null, null, 0, 20, "password"))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(jobRepository);
    }

    @Test
    void searchAppliesDescendingPrefix() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(jobRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.search(null, null, null, 0, 20, "-createdAt");

        verify(jobRepository).search(any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }
}
