package com.poom.backend.api.dto.fundraiser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poom.backend.api.dto.donation.FundraiserDonationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FundraiserDetailRes {

    private String dogName;
    private String shelterId;
    private String memberId;
    private String shelterName;
    private String shelterAddress;
    private String shelterEthWalletAddress;
    private String mainImgUrl;
    private String nftImgUrl;
    private List<String> dogImgUrls;
    private int dogGender;
    private int dogAge;
    private Boolean ageIsEstimated;
    private String dogFeature;
    private Double targetAmount;
    private Double currentAmount;
    private Boolean isClosed;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd", timezone = "Asia/Seoul")
    private LocalDate endDate;
    private List<FundraiserDonationDto> donations;

}
