package re.edu.thuchanh.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.Company;
import re.edu.thuchanh.company.CompanyRepository;
import re.edu.thuchanh.job.dto.JobRequest;
import re.edu.thuchanh.job.dto.JobResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock CompanyRepository companyRepository;
    @InjectMocks JobService jobService;

    private Company acme() {
        return Company.create("Acme", "Hanoi", null);
    }

    private JobRequest openJobRequest(Long companyId) {
        return new JobRequest("Backend Dev", "Build APIs", 1000L, 2000L,
                "Hanoi", JobStatus.OPEN, companyId);
    }

    @Test
    void createAttachesCompanyAndStampsCreatedAt() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(acme()));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.create(openJobRequest(1L));

        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createThrowsNotFoundWhenCompanyMissing() {
        when(companyRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.create(openJobRequest(5L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("5");

        verify(jobRepository, never()).save(any());
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateReplacesFields() {
        Job existing = Job.create("Old", null, null, null, null, JobStatus.OPEN, acme());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(acme()));
        when(jobRepository.save(existing)).thenReturn(existing);

        JobResponse response = jobService.update(1L,
                new JobRequest("New", "desc", 10L, 20L, "Da Nang", JobStatus.CLOSED, 1L));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void deleteRemovesJobWithNoApplications() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(
                Job.create("t", null, null, null, null, JobStatus.OPEN, acme())));
        when(jobRepository.existsApplicationByJobId(1L)).thenReturn(false);

        jobService.delete(1L);

        verify(jobRepository).delete(any(Job.class));
    }

    @Test
    void deleteThrowsConflictWhenJobStillHasApplications() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(
                Job.create("t", null, null, null, null, JobStatus.OPEN, acme())));
        when(jobRepository.existsApplicationByJobId(1L)).thenReturn(true);

        assertThatThrownBy(() -> jobService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(jobRepository, never()).delete(any(Job.class));
    }
}
