package com.example.booklend.lending.api.cli;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.domain.BookId;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.lending.application.port.in.BorrowUseCase;
import com.example.booklend.lending.application.port.in.LoanQueryUseCase;
import com.example.booklend.lending.application.port.in.ReserveUseCase;
import com.example.booklend.lending.application.port.in.ReturnUseCase;
import com.example.booklend.lending.domain.Loan;
import com.example.booklend.lending.domain.LoanId;
import com.example.booklend.lending.domain.Reservation;
import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.domain.MemberId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/**
 * Second inbound adapter for BorrowBookUseCase — same port as LoanController, different driving mechanism.
 * Activate: java -jar booklend.jar --booklend.cli.enabled=true --spring.main.web-application-type=none
 */
@Component
@ConditionalOnProperty(name = "booklend.cli.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BookLendCliRunner implements ApplicationRunner {

    private final BorrowUseCase borrowUseCase;
    private final ReturnUseCase returnUseCase;
    private final ReserveUseCase reserveUseCase;
    private final CatalogAdminUseCase catalogAdminUseCase;
    private final MemberAdminUseCase memberAdminUseCase;
    private final LoanQueryUseCase loanQueryUseCase;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== BookLend CLI ===");
        printHelp();

        while (scanner.hasNextLine()) {
            System.out.print("\n> ");
            System.out.flush();
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "borrow"            -> handleBorrow(parts);
                    case "return"            -> handleReturn(parts);
                    case "reserve"           -> handleReserve(parts);
                    case "add-book"          -> handleAddBook(parts);
                    case "add-member"        -> handleAddMember(parts);
                    case "clear-restriction" -> handleClearRestriction(parts);
                    case "loans"             -> handleLoans(parts);
                    case "help"              -> printHelp();
                    case "quit", "exit"      -> { System.out.println("Bye."); return; }
                    default -> System.out.println("Unknown command: " + command + ". Type 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handleBorrow(String[] parts) {
        requireArgs(parts, 3, "borrow <memberId> <bookId>");
        Loan loan = borrowUseCase.borrow(new BorrowUseCase.BorrowCommand(
                MemberId.of(parts[1]), BookId.of(parts[2])));
        System.out.printf("Loan created: id=%s  due=%s%n", loan.getId(), loan.getPeriod().dueDate());
    }

    private void handleReturn(String[] parts) {
        requireArgs(parts, 2, "return <loanId>");
        Loan loan = returnUseCase.returnBook(
                new ReturnUseCase.ReturnCommand(LoanId.of(parts[1])));
        System.out.printf("Returned loan %s  (late=%s)%n", loan.getId(), loan.wasReturnedLate());
    }

    private void handleReserve(String[] parts) {
        requireArgs(parts, 3, "reserve <memberId> <bookId>");
        Reservation r = reserveUseCase.reserve(new ReserveUseCase.ReserveCommand(
                MemberId.of(parts[1]), BookId.of(parts[2])));
        System.out.printf("Reservation created: id=%s%n", r.getId());
    }

    private void handleAddBook(String[] parts) {
        requireArgs(parts, 4, "add-book <isbn> <title> <author>");
        var book = catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN(parts[1]), parts[2], parts[3]));
        System.out.printf("Book added: id=%s  title=%s%n", book.getId(), book.getTitle());
    }

    private void handleAddMember(String[] parts) {
        requireArgs(parts, 3, "add-member <name> <email>");
        var member = memberAdminUseCase.addMember(new MemberAdminUseCase.AddMemberCommand(parts[1], parts[2]));
        System.out.printf("Member added: id=%s  name=%s%n", member.getId(), member.getName());
    }

    private void handleClearRestriction(String[] parts) {
        requireArgs(parts, 2, "clear-restriction <memberId>");
        var member = memberAdminUseCase.clearRestriction(
                new MemberAdminUseCase.ClearRestrictionCommand(MemberId.of(parts[1])));
        System.out.printf("Restriction cleared for member %s%n", member.getId());
    }

    private void handleLoans(String[] parts) {
        requireArgs(parts, 2, "loans <memberId>");
        List<Loan> loans = loanQueryUseCase.findAllLoans(MemberId.of(parts[1]));
        if (loans.isEmpty()) {
            System.out.println("No loans found.");
        } else {
            loans.forEach(l -> System.out.printf(
                    "  loan=%s  book=%s  status=%s  due=%s%n",
                    l.getId(), l.getBookId(), l.getStatus(), l.getPeriod().dueDate()));
        }
    }

    private void requireArgs(String[] parts, int required, String usage) {
        if (parts.length < required) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private void printHelp() {
        System.out.println("""
                Commands:
                  add-book <isbn> <title> <author>
                  add-member <name> <email>
                  borrow <memberId> <bookId>
                  return <loanId>
                  reserve <memberId> <bookId>
                  clear-restriction <memberId>
                  loans <memberId>
                  help | quit
                """);
    }
}
