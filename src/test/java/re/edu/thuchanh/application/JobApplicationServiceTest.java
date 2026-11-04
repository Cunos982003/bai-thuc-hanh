package re.edu.thuchanh.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import re.edu.thuchanh.application.dto.ApplicationRequest;
import re.edu.thuchanh.application.dto.ApplicationResponse;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.Company;
import re.edu.thuchanh.job.Job;
import re.edu.thuchanh.job.JobRepository;
import re.edu.thuchanh.job.JobStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock JobRepository jobRepository;
    @InjectMocks JobApplicationService applicationService;

    private Job job(JobStatus status) {
        return Job.create("Backend Dev", null, null, null, null, status,
                Company.create("Acme", null, null));
    }

    private ApplicationRequest request(String email) {
        return new ApplicationRequest("Nguyen Van A", email, "https://cv.example/a.pdf");
    }

    @Test
    void applySavesApplicationAndStampsAppliedAt() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(false);
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.apply(1L, request("a@example.com"));

        assertThat(response.appliedAt()).isNotNull();
        assertThat(response.email()).isEqualTo("a@example.com");
    }

    @Test
    void applyThrowsNotFoundWhenJobMissing() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.apply(99L, request("a@example.com")))
                .isInstanceOf(NotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyThrowsConflictWhenJobIsClosed() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.CLOSED)));

        assertThatThrownBy(() -> applicationService.apply(1L, request("a@example.com")))
                .isInstanceOf(ConflictException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyThrowsConflictWhenEmailAlreadyAppliedToSameJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(1L, request("a@example.com")))
                .isInstanceOf(ConflictException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyNormalisesEmailBeforeCheckingAndSaving() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(false);
        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        when(applicationRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.apply(1L, request("  A@Example.COM  "));

        assertThat(captor.getValue().getEmail()).isEqualTo("a@example.com");
    }
}
