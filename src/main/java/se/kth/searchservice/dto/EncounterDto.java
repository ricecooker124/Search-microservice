package se.kth.searchservice.dto;

import java.time.LocalDateTime;

public record EncounterDto(
        Long id,
        LocalDateTime date,
        String reason,
        Long imageId,

        Long patientId,
        String patientFirstName,
        String patientLastName,

        Long locationId,
        String locationName,
        String locationAddress,

        Long organizationId,
        String organizationName
) {}