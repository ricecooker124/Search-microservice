package se.kth.searchservice.api;

import io.smallrye.mutiny.Uni;
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

    @GET
    @Path("/patients")
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

    @GET
    @Path("/practitioners/{username}/patients")
    public Uni<List<PatientDto>> practitionerPatients(@PathParam("username") String username) {
        return service.practitionerPatients(username);
    }

    @GET
    @Path("/practitioners/{username}/encounters")
    public Uni<List<EncounterDto>> practitionerEncountersByDate(
            @PathParam("username") String username,
            @QueryParam("date") String date
    ) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Missing query param: date=YYYY-MM-DD");
        }
        return service.practitionerEncountersByDate(username, LocalDate.parse(date));
    }
}