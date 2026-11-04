package re.edu.thuchanh.job.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import re.edu.thuchanh.job.JobStatus;

public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @PositiveOrZero Long salaryMin,
        @PositiveOrZero Long salaryMax,
        @Size(max = 255) String location,
        @NotNull JobStatus status,
        @NotNull Long companyId) {

    @AssertTrue(message = "salaryMin must be less than or equal to salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
