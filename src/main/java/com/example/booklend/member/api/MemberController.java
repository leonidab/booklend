package com.example.booklend.member.api;

import com.example.booklend.member.api.dto.CreateMemberRequest;
import com.example.booklend.member.application.port.in.MemberAdminUseCase;
import com.example.booklend.member.application.port.out.LoadMemberPort;
import com.example.booklend.member.domain.Member;
import com.example.booklend.member.domain.MemberId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberAdminUseCase memberAdminUseCase;
    private final LoadMemberPort loadMemberPort;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MemberResponse addMember(@RequestBody CreateMemberRequest request) {
        Member member = memberAdminUseCase.addMember(
                new MemberAdminUseCase.AddMemberCommand(request.name(), request.email()));
        return MemberResponse.from(member);
    }

    @GetMapping("/{memberId}")
    MemberResponse getMember(@PathVariable String memberId) {
        Member member = loadMemberPort.loadMember(MemberId.of(memberId));
        return MemberResponse.from(member);
    }

    @DeleteMapping("/{memberId}/restriction")
    MemberResponse clearRestriction(@PathVariable String memberId) {
        Member member = memberAdminUseCase.clearRestriction(
                new MemberAdminUseCase.ClearRestrictionCommand(MemberId.of(memberId)));
        return MemberResponse.from(member);
    }

    record MemberResponse(String id, String name, String email, String status,
                          int activeLoansCount, int lateReturnCount) {
        static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getId().toString(),
                    member.getName(),
                    member.getEmail(),
                    member.getStatus().name(),
                    member.getActiveLoansCount(),
                    member.getLateReturnCount()
            );
        }
    }
}
