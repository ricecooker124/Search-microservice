package se.kth.searchservice.dto;

import java.time.LocalDate;

public record PatientDto(
        Long id,
        String username,
        String firstName,
        String lastName,
        String ssn,
        LocalDate birthDate,
        String gender
) {}