package com.bank.config;

import com.bank.domain.Account;
import com.bank.domain.AccountType;
import com.bank.domain.Role;
import com.bank.domain.SecurityEvent;
import com.bank.domain.SecuritySeverity;
import com.bank.domain.User;
import com.bank.repository.AccountRepository;
import com.bank.repository.SecurityEventRepository;
import com.bank.repository.UserRepository;
import com.bank.security.SecurityEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Local/demo bootstrap so recruiters can sign in without SQL.
 * Disable with {@code app.demo.seed=false}.
 */
@Component
@ConditionalOnProperty(prefix = "app.demo", name = "seed", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    public static final String CUSTOMER_EMAIL = "customer@fidessecura.bank";
    public static final String MERCHANT_EMAIL = "merchant@fidessecura.bank";
    public static final String ANALYST_EMAIL = "analyst@fidessecura.bank";
    public static final String DEMO_PASSWORD = "password1";
    public static final String HOME_IP = "192.168.1.10";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SecurityEventRepository securityEventRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            SecurityEventRepository securityEventRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.securityEventRepository = securityEventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User customer = ensureUser(CUSTOMER_EMAIL, "Demo Customer", Role.CUSTOMER);
        User merchant = ensureUser(MERCHANT_EMAIL, "Demo Merchant", Role.CUSTOMER);
        ensureUser(ANALYST_EMAIL, "Demo Analyst", Role.ANALYST);

        ensureAccount(customer.getId(), "FSDEMOCUSTOMER0001", AccountType.CHECKING, new BigDecimal("10000.00"));
        ensureAccount(merchant.getId(), "FSDEMOMERCHANT0001", AccountType.CHECKING, new BigDecimal("100.00"));

        // Prior successful login from "home" IP so a later foreign IP counts as new.
        boolean hasHomeLogin = securityEventRepository.findTop20BySubjectUserIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .anyMatch(e -> SecurityEventTypes.LOGIN_SUCCESS.equals(e.getEventType())
                        && HOME_IP.equals(e.getIpAddress()));
        if (!hasHomeLogin) {
            securityEventRepository.save(new SecurityEvent(
                    SecurityEventTypes.LOGIN_SUCCESS,
                    SecuritySeverity.INFO,
                    customer.getId(),
                    customer.getId(),
                    HOME_IP,
                    "demo-seeder",
                    null,
                    "{\"seeded\":true}"
            ));
        }

        log.info(
                "Demo users ready: {} / {} / {} (password '{}')",
                CUSTOMER_EMAIL,
                MERCHANT_EMAIL,
                ANALYST_EMAIL,
                DEMO_PASSWORD
        );
    }

    private User ensureUser(String email, String fullName, Role role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(new User(email, passwordEncoder.encode(DEMO_PASSWORD), fullName, role))
        );
    }

    private void ensureAccount(Long customerId, String accountNumber, AccountType type, BigDecimal balance) {
        boolean exists = accountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .anyMatch(a -> accountNumber.equals(a.getAccountNumber()));
        if (!exists) {
            accountRepository.save(new Account(customerId, accountNumber, type, balance));
        }
    }
}
