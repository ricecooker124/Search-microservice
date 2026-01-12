package se.kth.searchservice.api;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import se.kth.searchservice.dto.EncounterDto;
import se.kth.searchservice.dto.PatientDto;
import se.kth.searchservice.service.SearchService;

import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    SearchService service;

    /**
     * Search patients by various criteria.
     * Only PRACTITIONER and ADMIN can search patients.
     */
    @GET
    @Path("/patients")
    @RolesAllowed({"PRACTITIONER", "ADMIN"})
    public Uni<List<PatientDto>> searchPatients(
            @QueryParam("name") String name,
            @QueryParam("ssn") String ssn,
            @QueryParam("condition") String condition,
            @QueryParam("gender") String gender,
            @DefaultValue("50") @QueryParam("limit") int limit,
            @DefaultValue("0") @QueryParam("offset") int offset
    ) {
        limit = Math.min(Math.max(limit, 1), 200);
        offset = Math.max(offset, 0);
        return service.searchPatients(name, ssn, condition, gender, limit, offset);
    }

    /**
     * Get all patients for a specific practitioner (based on encounters).
     * Only PRACTITIONER and ADMIN can access.
     */
    @GET
    @Path("/practitioners/{username}/patients")
    @RolesAllowed({"PRACTITIONER", "ADMIN"})
    public Uni<List<PatientDto>> practitionerPatients(@PathParam("username") String username) {
        return service.practitionerPatients(username);
    }

    /**
     * Get all encounters for a practitioner on a specific date.
     * Only PRACTITIONER and ADMIN can access.
     */
    @GET
    @Path("/practitioners/{username}/encounters")
    @RolesAllowed({"PRACTITIONER", "ADMIN"})
    public Uni<List<EncounterDto>> practitionerEncountersByDate(
            @PathParam("username") String username,
            @QueryParam("date") String date
    ) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Missing query param: date=YYYY-MM-DD");
        }
        return service.practitionerEncountersByDate(username, LocalDate.parse(date));
    }

    /**
     * Get a specific patient by ID.
     * PRACTITIONER can look up patient details.
     */
    @GET
    @Path("/patients/{id}")
    @RolesAllowed({"PRACTITIONER", "ADMIN"})
    public Uni<PatientDto> getPatientById(@PathParam("id") Long id) {
        if (id == null) throw new BadRequestException("Missing patient id");
        return service.getPatientById(id);
    }
}