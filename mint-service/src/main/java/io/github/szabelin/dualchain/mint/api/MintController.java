package io.github.szabelin.dualchain.mint.api;

import io.github.szabelin.dualchain.mint.api.dto.MintRequest;
import io.github.szabelin.dualchain.mint.api.dto.MintResponse;
import io.github.szabelin.dualchain.mint.domain.EthAddress;
import io.github.szabelin.dualchain.mint.domain.MintReceipt;
import io.github.szabelin.dualchain.mint.domain.MintService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mints")
public class MintController {

    private static final Logger log = LoggerFactory.getLogger(MintController.class);

    private final MintService mintService;

    public MintController(MintService mintService) {
        this.mintService = mintService;
    }

    @PostMapping
    public ResponseEntity<MintResponse> mint(@Valid @RequestBody MintRequest request) {
        log.info("Mint request received: to={}, amount={}", request.to(), request.amount());
        MintReceipt receipt = mintService.mint(new EthAddress(request.to()), request.amount());
        return ResponseEntity.accepted().body(MintResponse.from(receipt));
    }
}
