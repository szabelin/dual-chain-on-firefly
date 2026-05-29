package io.github.szabelin.dualchain.mint.api;

import io.github.szabelin.dualchain.mint.api.dto.BalanceResponse;
import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.firefly.FireFlyClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;

@RestController
@RequestMapping("/api/v1/balances")
public class BalanceController {

    private final FireFlyClient fireFly;

    public BalanceController(FireFlyClient fireFly) {
        this.fireFly = fireFly;
    }

    @GetMapping("/{address}")
    public BalanceResponse balance(@PathVariable String address) {
        EthAddress addr = new EthAddress(address);
        BigInteger balance = fireFly.balance(addr).orElse(BigInteger.ZERO);
        return new BalanceResponse(addr.value(), balance.toString());
    }
}
