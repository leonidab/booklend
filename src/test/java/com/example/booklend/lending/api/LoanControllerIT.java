package com.example.booklend.lending.api;

import com.example.booklend.catalog.application.port.in.CatalogAdminUseCase;
import com.example.booklend.catalog.domain.Book;
import com.example.booklend.catalog.domain.ISBN;
import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoanControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired CatalogAdminUseCase catalogAdminUseCase;
    @Autowired MemberAdminUseCase memberAdminUseCase;

    private Book addBook() {
        return catalogAdminUseCase.addBook(new CatalogAdminUseCase.AddBookCommand(
                new ISBN("9780201633610"), "Clean Code", "Martin"));
    }

    private Member addMember() {
        return memberAdminUseCase.addMember(new MemberAdminUseCase.AddMemberCommand("Alice", "alice@test.com"));
    }

    @Test
    void POST_loans_returns201_withActiveLoan() throws Exception {
        Book book = addBook();
        Member member = addMember();

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","bookId":"%s"}
                                """.formatted(member.getId(), book.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.memberId").value(member.getId().toString()))
                .andExpect(jsonPath("$.bookId").value(book.getId().toString()));
    }

    @Test
    void POST_loans_returns409_whenBookUnavailable() throws Exception {
        Book book = addBook();
        Member member1 = addMember();
        Member member2 = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand("Bob", "bob@test.com"));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","bookId":"%s"}
                                """.formatted(member1.getId(), book.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","bookId":"%s"}
                                """.formatted(member2.getId(), book.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void POST_loans_loanId_return_returns200_withReturnedLoan() throws Exception {
        Book book = addBook();
        Member member = addMember();

        String body = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","bookId":"%s"}
                                """.formatted(member.getId(), book.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String loanId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void POST_loans_returns422_whenMaxLoansExceeded() throws Exception {
        Member member = addMember();
        for (int i = 0; i < 3; i++) {
            Book book = addBook();
            mockMvc.perform(post("/api/loans")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"memberId":"%s","bookId":"%s"}
                                    """.formatted(member.getId(), book.getId())))
                    .andExpect(status().isCreated());
        }

        Book extra = addBook();
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":"%s","bookId":"%s"}
                                """.formatted(member.getId(), extra.getId())))
                .andExpect(status().isUnprocessableEntity());
    }
}
