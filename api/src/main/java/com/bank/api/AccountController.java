// package com.bank.api;

// import com.bank.api.dto.AccountResponse;
// import com.bank.api.dto.CreateAccountRequest;
// import com.bank.api.dto.DepositRequest;
// import com.bank.security.UserPrincipal;
// import com.bank.service.AccountService;
// import jakarta.validation.Valid;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.List;

// @RestController
// @RequestMapping("/api/v1/accounts")
// public class AccountController {

//     private final AccountService accountService;

//     public AccountController(AccountService accountService) {
//         this.accountService = accountService;
//     }

//     @PostMapping
//     public ResponseEntity<AccountResponse> open(
//             @AuthenticationPrincipal UserPrincipal principal,
//             @Valid @RequestBody CreateAccountRequest request
//     ) {
//         return ResponseEntity.status(HttpStatus.CREATED).body(accountService.open(principal.getId(), request));
//     }

//     @GetMapping
//     public ResponseEntity<List<AccountResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
//         return ResponseEntity.ok(accountService.listMine(principal.getId()));
//     }

//     @GetMapping("/{id}")
//     public ResponseEntity<AccountResponse> get(
//             @AuthenticationPrincipal UserPrincipal principal,
//             @PathVariable Long id
//     ) {
//         return ResponseEntity.ok(accountService.getMine(principal.getId(), id));
//     }

//     @PostMapping("/{id}/deposit")
//     public ResponseEntity<AccountResponse> deposit(
//             @AuthenticationPrincipal UserPrincipal principal,
//             @PathVariable Long id,
//             @Valid @RequestBody DepositRequest request
//     ) {
//         return ResponseEntity.ok(accountService.deposit(principal.getId(), id, request));
//     }
// }

package com.bank.api;

import com.bank.api.dto.AccountResponse;
import com.bank.api.dto.CreateAccountRequest;
import com.bank.api.dto.DepositRequest;
import com.bank.security.UserPrincipal;
import com.bank.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(principal.getId(), request);
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return accountService.listFor(principal.getId());
    }

    @GetMapping("/{id}")
    public AccountResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long id) {
        return accountService.get(principal.getId(), id);
    }

    // TODO: add an idempotency key so retries don't create another deposit
    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody DepositRequest request) {
        return accountService.deposit(principal.getId(), id, request);
    }
}