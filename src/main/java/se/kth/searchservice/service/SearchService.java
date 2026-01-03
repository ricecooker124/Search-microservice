package se.kth.searchservice.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import se.kth.searchservice.dto.EncounterDto;
import se.kth.searchservice.dto.PatientDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SearchService {

    @Inject
    MySQLPool client;

    public Uni<List<PatientDto>> searchPatients(
            String name,
            String ssn,
            String condition,
            String gender,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT
              pat.person_id,
              pat.username,
              p.first_name,
              p.last_name,
              p.ssn,
              p.birth_date,
              p.gender
            FROM patient pat
            JOIN person p ON p.person_id = pat.person_id
            LEFT JOIN condition_entry ce ON ce.patient_id = pat.person_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            sql.append(" AND (LOWER(p.first_name) LIKE ? OR LOWER(p.last_name) LIKE ?) ");
            String like = "%" + name.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }

        if (ssn != null && !ssn.isBlank()) {
            sql.append(" AND p.ssn LIKE ? ");
            params.add("%" + ssn + "%");
        }

        if (condition != null && !condition.isBlank()) {
            sql.append(" AND LOWER(ce.name) LIKE ? ");
            params.add("%" + condition.toLowerCase() + "%");
        }

        if (gender != null && !gender.isBlank()) {
            sql.append(" AND p.gender = ? ");
            params.add(gender);
        }

        sql.append(" ORDER BY p.last_name, p.first_name LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        return client.preparedQuery(sql.toString())
                .execute(Tuple.from(params))
                .map(rows -> {
                    List<PatientDto> out = new ArrayList<>();
                    for (Row r : rows) {
                        out.add(new PatientDto(
                                r.getLong("person_id"),
                                r.getString("username"),
                                r.getString("first_name"),
                                r.getString("last_name"),
                                r.getString("ssn"),
                                r.getLocalDate("birth_date"),
                                r.getString("gender")
                        ));
                    }
                    return out;
                });
    }

    public Uni<List<PatientDto>> practitionerPatients(String practitionerUsername) {
        String sql = """
            SELECT DISTINCT
              pat.person_id,
              pat.username,
              p.first_name,
              p.last_name,
              p.ssn,
              p.birth_date,
              p.gender
            FROM encounter e
            JOIN practitioner pr ON pr.person_id = e.practitioner_id
            JOIN patient pat ON pat.person_id = e.patient_id
            JOIN person p ON p.person_id = pat.person_id
            WHERE pr.username = ?
            ORDER BY p.last_name, p.first_name
            """;

        return client.preparedQuery(sql)
                .execute(Tuple.of(practitionerUsername))
                .map(rows -> {
                    List<PatientDto> out = new ArrayList<>();
                    for (Row r : rows) {
                        out.add(new PatientDto(
                                r.getLong("person_id"),
                                r.getString("username"),
                                r.getString("first_name"),
                                r.getString("last_name"),
                                r.getString("ssn"),
                                r.getLocalDate("birth_date"),
                                r.getString("gender")
                        ));
                    }
                    return out;
                });
    }

    public Uni<List<EncounterDto>> practitionerEncountersByDate(String practitionerUsername, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        String sql = """
        SELECT
          e.id,
          e.date,
          e.reason,
          e.image_id,

          e.patient_id,
          pp.first_name AS patient_first_name,
          pp.last_name  AS patient_last_name,

          e.location_id,
          l.name        AS location_name,
          l.address     AS location_address,

          o.id          AS organization_id,
          o.name        AS organization_name
        FROM encounter e
        JOIN practitioner pr ON pr.person_id = e.practitioner_id

        JOIN patient pat ON pat.person_id = e.patient_id
        JOIN person pp   ON pp.person_id  = pat.person_id

        LEFT JOIN location l     ON l.id = e.location_id
        LEFT JOIN organization o ON o.id = l.organization_id

        WHERE pr.username = ?
          AND e.date >= ?
          AND e.date < ?
        ORDER BY e.date DESC
        """;

        return client.preparedQuery(sql)
                .execute(Tuple.of(practitionerUsername, start, end))
                .map(rows -> {
                    List<EncounterDto> out = new ArrayList<>();
                    for (Row r : rows) {
                        out.add(new EncounterDto(
                                r.getLong("id"),
                                r.getLocalDateTime("date"),
                                r.getString("reason"),
                                r.getLong("image_id"),

                                r.getLong("patient_id"),
                                r.getString("patient_first_name"),
                                r.getString("patient_last_name"),

                                r.getLong("location_id"),
                                r.getString("location_name"),
                                r.getString("location_address"),

                                r.getLong("organization_id"),
                                r.getString("organization_name")
                        ));
                    }
                    return out;
                });
    }

    public Uni<PatientDto> getPatientById(Long patientId) {
        String sql = """
        SELECT
          pat.person_id,
          pat.username,
          p.first_name,
          p.last_name,
          p.ssn,
          p.birth_date,
          p.gender
        FROM patient pat
        JOIN person p ON p.person_id = pat.person_id
        WHERE pat.person_id = ?
        LIMIT 1
        """;

        return client.preparedQuery(sql)
                .execute(Tuple.of(patientId))
                .map(rows -> {
                    Row r = rows.iterator().hasNext() ? rows.iterator().next() : null;
                    if (r == null) {
                        throw new NotFoundException("Patient not found: " + patientId);
                    }

                    return new PatientDto(
                            r.getLong("person_id"),
                            r.getString("username"),
                            r.getString("first_name"),
                            r.getString("last_name"),
                            r.getString("ssn"),
                            r.getLocalDate("birth_date"),
                            r.getString("gender")
                    );
                });
    }
}