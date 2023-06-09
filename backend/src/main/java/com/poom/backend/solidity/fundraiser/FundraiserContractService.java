package com.poom.backend.solidity.fundraiser;

import com.poom.backend.api.dto.fundraiser.SmartContractFundraiserDto;

import java.util.List;
import java.util.Optional;

public interface FundraiserContractService {

    Long createFundraiser(SmartContractFundraiserDto smartContractFundraiserDto);
    Optional<List<SmartContractFundraiserDto>> getFundraiserList();
    Optional<SmartContractFundraiserDto> getFundraiserDetail(Long fundraiserId);
    void endFundraiser(Long fundraiserId);

}
