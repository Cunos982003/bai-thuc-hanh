package re.edu.thuchanh.job.dto;

import re.edu.thuchanh.job.Job;
import re.edu.thuchanh.job.JobStatus;

import java.time.Instant;

public record JobResponse(
        Long id,
        String title,
        String description,
        Long salaryMin,
        Long salaryMax,
        String location,
        JobStatus status,
        Long companyId,
        String companyName,
        Instant createdAt) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getLocation(),
                job.getStatus(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCreatedAt());
    }
}
