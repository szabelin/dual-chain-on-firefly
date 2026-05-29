package io.github.szabelin.dualchain.mint.api;

import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.domain.MintReceipt;
import io.github.szabelin.dualchain.mint.domain.MintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MintController.class)
@Import(GlobalExceptionHandler.class)
class MintControllerTest {

    private static final String VALID_ADDR = "0x0000000000000000000000000000000000000001";

    @Autowired MockMvc mvc;
    @MockitoBean MintService mintService;

    @Test
    void post_mint_returns_202_with_receipt() throws Exception {
        when(mintService.mint(any(), any())).thenReturn(
                new MintReceipt("op-1", new EthAddress(VALID_ADDR), BigInteger.ONE, "tx-1")
        );

        mvc.perform(post("/api/v1/mints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "to": "%s", "amount": 1 }
                                """.formatted(VALID_ADDR)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.operationId").value("op-1"))
                .andExpect(jsonPath("$.to").value(VALID_ADDR))
                .andExpect(jsonPath("$.amount").value("1"))
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void invalid_address_returns_400_with_problem_details() throws Exception {
        mvc.perform(post("/api/v1/mints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "to": "not-an-address", "amount": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("to"));
    }

    @Test
    void zero_amount_returns_400() throws Exception {
        mvc.perform(post("/api/v1/mints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "to": "%s", "amount": 0 }
                                """.formatted(VALID_ADDR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    @Test
    void missing_amount_returns_400() throws Exception {
        mvc.perform(post("/api/v1/mints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "to": "%s" }
                                """.formatted(VALID_ADDR)))
                .andExpect(status().isBadRequest());
    }
}
