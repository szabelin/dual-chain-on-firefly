package io.github.szabelin.dualchain.mint.api;

import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.firefly.FireFlyClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceController.class)
@Import(GlobalExceptionHandler.class)
class BalanceControllerTest {

    private static final String ADDR = "0x0000000000000000000000000000000000000001";

    @Autowired MockMvc mvc;
    @MockitoBean FireFlyClient fireFly;

    @Test
    void returns_balance_from_firefly() throws Exception {
        when(fireFly.balance(new EthAddress(ADDR))).thenReturn(Optional.of(new BigInteger("42")));

        mvc.perform(get("/api/v1/balances/{addr}", ADDR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(ADDR))
                .andExpect(jsonPath("$.balance").value("42"));
    }

    @Test
    void returns_zero_when_address_has_no_balance() throws Exception {
        when(fireFly.balance(new EthAddress(ADDR))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/balances/{addr}", ADDR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(ADDR))
                .andExpect(jsonPath("$.balance").value("0"));
    }

    @Test
    void invalid_address_returns_400() throws Exception {
        mvc.perform(get("/api/v1/balances/{addr}", "not-an-address"))
                .andExpect(status().isBadRequest());
    }
}
