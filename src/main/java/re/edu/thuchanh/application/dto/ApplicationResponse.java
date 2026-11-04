package re.edu.thuchanh.application.dto;

import re.edu.thuchanh.application.JobApplication;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String fullName,
        String email,
        String cvUrl,
        Instant appliedAt) {

    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getFullName(),
                application.getEmail(),
                application.getCvUrl(),
                application.getAppliedAt());
    }
}
