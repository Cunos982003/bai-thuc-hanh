package re.edu.thuchanh.company.dto;

import re.edu.thuchanh.company.Company;

public record CompanyResponse(Long id, String name, String address, String website) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(), company.getName(), company.getAddress(), company.getWebsite());
    }
}
