package se.kth.searchservice.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowIterator;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.kth.searchservice.dto.PatientDto;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    MySQLPool client;

    @Mock
    PreparedQuery<RowSet<Row>> preparedQuery;

    @Mock
    RowSet<Row> rowSet;

    @Mock
    RowIterator<Row> rowIterator;

    @Mock
    Row row;

    @InjectMocks
    SearchService searchService;

    @BeforeEach
    void setUp() {
        // Common mock setup
        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
    }

    @Test
    @DisplayName("searchPatients - returnerar patienter när sökresultat finns")
    void searchPatients_returnsPatients() {
        // Given
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Uni.createFrom().item(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(true, false); // One row
        when(rowIterator.next()).thenReturn(row);

        when(row.getLong("person_id")).thenReturn(1L);
        when(row.getString("username")).thenReturn("patient1");
        when(row.getString("first_name")).thenReturn("Anna");
        when(row.getString("last_name")).thenReturn("Andersson");
        when(row.getString("ssn")).thenReturn("199001011234");
        when(row.getLocalDate("birth_date")).thenReturn(LocalDate.of(1990, 1, 1));
        when(row.getString("gender")).thenReturn("FEMALE");

        // When
        List<PatientDto> result = searchService.searchPatients(
                "Anna", null, null, null, 50, 0
        ).await().indefinitely();

        // Then
        assertEquals(1, result.size());
        assertEquals("Anna", result.get(0).firstName());
        assertEquals("Andersson", result.get(0).lastName());
        verify(client, times(1)).preparedQuery(anyString());
    }

    @Test
    @DisplayName("getPatientById - kastar NotFoundException när patient saknas")
    void getPatientById_throwsNotFoundException() {
        // Given
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Uni.createFrom().item(rowSet));
        when(rowSet.iterator()).thenReturn(rowIterator);
        when(rowIterator.hasNext()).thenReturn(false); // No rows

        // When & Then
        assertThrows(NotFoundException.class, () ->
                searchService.getPatientById(999L).await().indefinitely()
        );
    }
}