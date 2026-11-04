package re.edu.thuchanh.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import re.edu.thuchanh.application.dto.ApplicationRequest;
import re.edu.thuchanh.application.dto.ApplicationResponse;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.job.Job;
import re.edu.thuchanh.job.JobRepository;
import re.edu.thuchanh.job.JobStatus;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    @Transactional
    public ApplicationResponse apply(Long jobId, ApplicationRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job %d not found".formatted(jobId)));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException(
                    "Job %d is closed and no longer accepts applications".formatted(jobId));
        }

        String email = request.email().trim().toLowerCase();

        if (applicationRepository.existsByJobIdAndEmail(jobId, email)) {
            throw new ConflictException(
                    "Email %s has already applied to job %d".formatted(email, jobId));
        }

        JobApplication application =
                JobApplication.create(job, request.fullName(), email, request.cvUrl());
        return ApplicationResponse.from(applicationRepository.save(application));
    }
}
