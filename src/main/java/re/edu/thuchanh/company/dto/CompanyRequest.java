package re.edu.thuchanh.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String address,
        @Size(max = 255) String website) {
}
