package com.poom.backend.api.controller;

import com.poom.backend.api.dto.shelter.ShelterAuthCond;
import com.poom.backend.api.service.mattermost.MattermostService;
import com.poom.backend.api.service.oauth.OauthService;
import com.poom.backend.api.service.shelter.ShelterService;
import com.poom.backend.db.entity.Shelter;
import com.poom.backend.db.repository.ShelterRepository;
import com.poom.backend.enums.ShelterStatus;
import com.poom.backend.exception.UnAuthorizationException;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@Api(tags = "보호소 관련 API")
@RequiredArgsConstructor
public class ShelterController {

    private final OauthService oauthService;
    private final ShelterRepository shelterRepository;
    private final MattermostService mattermostService;
    private final ShelterService shelterService;

    @GetMapping("/shelters/{shelterId}")
    @ApiOperation(value = "보호소 정보를 조회합니다.", notes = "<strong>보호소의 id</strong>를 입력받아 보호소의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK(조회 성공)"),
            @ApiResponse(code = 204, message = "NO CONTENT(보호소 정보 없음)"),
            @ApiResponse(code = 401, message = "UNAUTHORIZED(권한 없음)"),
            @ApiResponse(code = 500, message = "서버에러")
    })
    public ResponseEntity<?> getShelterInfo(@PathVariable String shelterId){
        return ResponseEntity.status(200)
                .body(shelterService.getShelterInfo(shelterId));
    }

    @PostMapping(value = "/shelters/auth" , consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ApiOperation(value = "보호소 심사 등록", notes = "<strong>보호소의 id</strong>를 입력받아 보호소의 정보를 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cond", value = "cond", dataType = "com.poom.backend.api.dto.shelter.ShelterAuthCond", paramType = "body")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK(등록 성공)"),
            @ApiResponse(code = 400, message = "BAD REQUEST(요청 실패)"),
            @ApiResponse(code = 401, message = "UNAUTHORIZED(권한 없음)"),
            @ApiResponse(code = 500, message = "서버에러")
    })
    public ResponseEntity<?> requestShelterAuth(HttpServletRequest request,
                                                @RequestPart("certificateImages") List<MultipartFile> certificateImages,
                                                @ModelAttribute("cond") ShelterAuthCond shelterAuthCond){
        shelterService.requestShelterAuth(request, certificateImages, shelterAuthCond);
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/admin/shelter/auth")
    @ApiOperation(value = "보호소 심사 승인 및 거절", notes = "<strong>보호소의 id와 심사 결과</strong>를 입력받아 보호소의 인증 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK(수정 성공)"),
            @ApiResponse(code = 204, message = "NO CONTENT(보호소 정보 없음)"),
            @ApiResponse(code = 401, message = "UNAUTHORIZED(권한 없음)"),
            @ApiResponse(code = 500, message = "서버에러")
    })
    public ResponseEntity<?> updateShelterAuth(@RequestParam String shelterId, @RequestParam boolean isApproved, @RequestParam String token){
        // token validation check
        if(!oauthService.checkAdmin(token)) throw new UnAuthorizationException("관리자가 아닙니다.");

        Optional<Shelter> shelter = shelterRepository.findById(shelterId);

        if(shelter.isEmpty()) mattermostService.sendColorMessage(" \"ID :"+shelterId+"\"로 검색되는 보호소 정보가 없습니다.", "#ff0000");
        if(!shelter.get().getStatus().equals(ShelterStatus.UNDER_REVIEW)) mattermostService.sendColorMessage("이미 처리된 요청입니다.", "#ff0000");
        if(isApproved){
            shelterService.approveShelterAuth(shelter.get());
        }else {
            shelterService.rejectShelterAuth(shelter.get());
        }
        return ResponseEntity.status(200).build();
    }
}
