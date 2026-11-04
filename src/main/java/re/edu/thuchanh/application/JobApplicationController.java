package re.edu.thuchanh.application;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import re.edu.thuchanh.application.dto.ApplicationRequest;
import re.edu.thuchanh.application.dto.ApplicationResponse;

import java.net.URI;

@RestController
@RequestMapping("/jobs/{jobId}/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> apply(@PathVariable Long jobId,
                                                     @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse created = applicationService.apply(jobId, request);
        return ResponseEntity
                .created(URI.create("/jobs/%d/applications/%d".formatted(jobId, created.id())))
                .body(created);
    }
}
