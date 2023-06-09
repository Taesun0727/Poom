package com.poom.backend.api.service.donation;

import com.poom.backend.api.dto.donation.DonationRes;
import com.poom.backend.api.dto.donation.FundraiserDonationDto;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface DonationService {
    DonationRes getMyDonationList(HttpServletRequest request, int size, int page);
    List<FundraiserDonationDto> getFundraiserDonationList(Long fundraiserId);
    int getNftIsIssued(Long donationId);
    String setDonationSort(Long fundraiserId);
    int getMyRank(Long fundraiserId, String memberId);
    Double getMyAmount(Long fundraiserId, String memberId);
}
