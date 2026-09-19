package com.adrianojlt.logistics.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkDataSeederTest {

    @Mock
    private JdbcTemplate jdbc;

    /** Nothing at all must happen unless the property asks for it. */
    @Test
    void doesNothingWhenTheCountIsZero() {
        new BulkDataSeeder(jdbc, 0).run();

        verifyNoInteractions(jdbc);
    }

    @Test
    void doesNothingWhenTheCountIsNegative() {
        new BulkDataSeeder(jdbc, -5).run();

        verifyNoInteractions(jdbc);
    }

    /** Running twice against a persistent database must not double the data. */
    @Test
    void skipsWhenGeneratedDataIsAlreadyThere() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(4_000);

        new BulkDataSeeder(jdbc, 10_000).run();

        verify(jdbc, never()).batchUpdate(anyString(), any(List.class));
    }

    @Test
    void writesShipmentsIncomesCostsAndCalculations() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        when(jdbc.queryForList(anyString(), eq(Long.class), any())).thenReturn(List.of(1L, 2L));

        new BulkDataSeeder(jdbc, 2).run();

        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        verify(jdbc, atLeast(4)).batchUpdate(statements.capture(), any(List.class));

        String all = String.join(" ", statements.getAllValues());
        assertThat(all).contains("INSERT INTO shipment");
        assertThat(all).contains("INSERT INTO income");
        assertThat(all).contains("INSERT INTO cost");
        assertThat(all).contains("INSERT INTO profit_calculation");
    }

    @Test
    void generatesThreeIncomesAndTwoCostsPerShipment() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(0);
        when(jdbc.queryForList(anyString(), eq(Long.class), any())).thenReturn(List.of(1L, 2L, 3L));

        new BulkDataSeeder(jdbc, 3).run();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> rows = ArgumentCaptor.forClass(List.class);
        verify(jdbc, atLeast(4)).batchUpdate(sql.capture(), rows.capture());

        int incomes = 0;
        int costs = 0;

        for (int i = 0; i < sql.getAllValues().size(); i++) {
            if (sql.getAllValues().get(i).contains("INTO income")) {
                incomes += rows.getAllValues().get(i).size();
            }
            if (sql.getAllValues().get(i).contains("INTO cost")) {
                costs += rows.getAllValues().get(i).size();
            }
        }

        assertThat(incomes).isEqualTo(9);
        assertThat(costs).isEqualTo(6);
    }
}
