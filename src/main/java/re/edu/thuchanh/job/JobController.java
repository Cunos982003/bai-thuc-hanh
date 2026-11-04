package re.edu.thuchanh.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import re.edu.thuchanh.common.PageResponse;
import re.edu.thuchanh.job.dto.JobRequest;
import re.edu.thuchanh.job.dto.JobResponse;

import java.net.URI;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Validated
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
        JobResponse created = jobService.create(request);
        return ResponseEntity.created(URI.create("/jobs/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<JobResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "title") String sort) {
        return jobService.search(keyword, location, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable Long id) {
        return jobService.get(id);
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
