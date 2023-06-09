// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0 <0.9.0;

import "./FundraiserContract.sol";
import "./DonationContract.sol";
import "./NftContract.sol";

contract PoomContract is FundraiserProcess, DonationProcess, NftProcess{

    /*
        fundraiser
    */
    // 후원 요청 등록
    function createFundraiser(Fundraiser memory _fundraiser) external{
        _createFundraiser(_fundraiser);
    }

    function getFundraiserId() external view returns (uint64){
        return _getFundraiserId();
    }

    // 모든 후원 요청 목록 조회
    function getFundraiserList() external view returns(Fundraiser[] memory){
        return _getFundraiserList();
    }

    // 후원 요청 상세 조회
    function getFundraiserDetail(uint64 _fundraiserId) external view returns(Fundraiser memory){
        Fundraiser memory fundraiser = _getFundraiserDetail(_fundraiserId);
        return fundraiser;
    }

    // 한 후원에 대한 후원자 목록 조회
    function getDonationList() external view returns(Donation[] memory){
        Donation[] memory donationList = _getDonationList();
        return donationList;
    }

    // 후원 요청 종료
    function endFundraiser(uint64 _fundraiserId) external{
        _endFundraiser(_fundraiserId); // 종료
        _setNftFundraiserEnded(_fundraiserId); // isIssued 1로 변경
    }


    /*
        Donation
    */

    // 후원
    function donate(uint64 _fundraiserId, string memory _memberId, uint256 _donationTime) external payable{
        _donate(_fundraiserId, _memberId, _donationTime, msg.value);
    }

    function setDonationSort(uint64 _fundraiserId, string memory _sortHash) external{
        _setDonationSort(_fundraiserId, _sortHash);
    }

    function getDonationSort(uint64 _fundraiserId) external view returns(string memory){
        return _getDonationSort(_fundraiserId);
    }


    function getDonation(uint64 _id) external view returns (Donation memory){
        return _getDonation(_id);
    }



    /*
        NFT
    */
    // NFT 리스트 조회
    function getNftList(string memory _memberId) external view returns(NFT[] memory){
        return _getNftList(_memberId);
    }

    // 마감된 후원 NFT 발급
    function mintNft(NFT memory _nft, address _memberAddress, string memory _memberId, uint64 _donationId, uint64 _fundraiserId) external{
        _mintNft(_nft, _memberAddress, _memberId, _donationId, _fundraiserId);
    }

    function getNftId() external view returns (uint64){
        return _getNftId();
    }

}
