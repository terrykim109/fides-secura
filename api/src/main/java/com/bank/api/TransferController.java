// package com.bank.api;

// import com.bank.api.dto.CreateTransferRequest;
// import com.bank.api.dto.TransferResponse;
// import com.bank.security.UserPrincipal;
// import com.bank.service.TransferService;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.validation.Valid;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestHeader;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.List;

// @RestController
// @RequestMapping("/api/v1/transfers")
// public class TransferController {

//     private final TransferService transferService;

//     public TransferController(TransferService transferService) {
//         this.transferService = transferService;
//     }

//     @PostMapping
//     public ResponseEntity<TransferResponse> create(
//             @AuthenticationPrincipal UserPrincipal principal,
//             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
//             @Valid @RequestBody CreateTransferRequest request,
//             HttpServletRequest httpRequest
//     ) {
//         TransferResponse body = transferService.transfer(
//                 principal.getId(),
//                 idempotencyKey,
//                 request,
//                 clientIp(httpRequest),
//                 httpRequest.getHeader("User-Agent")
//         );
//         return ResponseEntity.status(HttpStatus.CREATED).body(body);
//     }

//     @GetMapping
//     public ResponseEntity<List<TransferResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
//         return ResponseEntity.ok(transferService.listMine(principal.getId()));
//     }

//     private static String clientIp(HttpServletRequest request) {
//         String forwarded = request.getHeader("X-Forwarded-For");
//         if (forwarded != null && !forwarded.isBlank()) {
//             return forwarded.split(",")[0].trim();
//         }
//         return request.getRemoteAddr();
//     }
// }

package com.bank.api;

import com.bank.api.dto.CreateTransferRequest;
import com.bank.api.dto.TransferResponse;
import com.bank.security.UserPrincipal;
import com.bank.service.TransferService;
import com.bank.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        return transferService.transfer(
                principal.getId(),
                idempotencyKey,
                request,
                RequestUtils.clientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
    }

    @GetMapping
    public List<TransferResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return transferService.listMine(principal.getId());
    }
}