package com.poom.backend.api.service.fundraiser;

import com.poom.backend.api.dto.donation.FundraiserDonationDto;
import com.poom.backend.api.dto.fundraiser.*;
import com.poom.backend.api.service.donation.DonationService;
import com.poom.backend.api.service.ipfs.IpfsService;
import com.poom.backend.api.service.member.MemberService;
import com.poom.backend.db.entity.Shelter;
import com.poom.backend.db.repository.ShelterRepository;
import com.poom.backend.exception.BadRequestException;
import com.poom.backend.solidity.fundraiser.FundraiserContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class FundraiserServiceImpl implements FundraiserService{
    private final MemberService memberService;
    private final IpfsService ipfsService;
    private final FundraiserContractService fundraiserContractService;
    private final DonationService donationService;
    private final ShelterRepository shelterRepository;


    @Override
    public MyFundraiserListRes getMyFundraiserList(HttpServletRequest request, int size, int page, boolean isClosed) {
        String memberId = memberService.getMemberIdFromHeader(request);
        Shelter shelter = shelterRepository.findShelterByAdminId(memberId)
                .orElseThrow(()->new BadRequestException("보호소 정보가 없습니다"));

        // 스마트 컨트랙트 호출 부분
        List<SmartContractFundraiserDto> smartContractFundraiserDtoList= fundraiserContractService.getFundraiserList()
                .orElseThrow(()->new RuntimeException());

        // 페이지네이션
        int startIdx = page*size;
        int endIdx = startIdx+size;
        int listLength = endIdx > smartContractFundraiserDtoList.size() ? smartContractFundraiserDtoList.size() : endIdx;
        int count = 0;

        List<FundraiserDto> fundraiserDtoList = new ArrayList<>();


        int listIdx = 0;
        while(count<smartContractFundraiserDtoList.size() && listIdx<listLength) {
            SmartContractFundraiserDto smartContractFundraiserDto = smartContractFundraiserDtoList.get(listIdx);
            count++;
            // 모집중, 종료 구분, 나의 후원 요청만 반환
            if(smartContractFundraiserDto.getIsEnded()==isClosed && smartContractFundraiserDto.getShelterId().equals(shelter.getId())){

                // IPFS에 저장된 정보들 -> 객체로
                IPFSFundraiserDto ipfsFundraiserDto = null;
                try {
                    ipfsFundraiserDto =
                            IPFSFundraiserDto.fromJson(ipfsService.downloadJson(smartContractFundraiserDto.getHashString()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                
                // FundraiserDto에 담기
                FundraiserDto fundraiserDto = FundraiserDto.builder()
                        .fundraiserId(smartContractFundraiserDto.getFundraiserId())
                        .dogName(ipfsFundraiserDto.getDogName())
                        .dogGender(ipfsFundraiserDto.getDogGender())
                        .mainImgUrl(ipfsFundraiserDto.getMainImage())
                        .nftImgUrl(ipfsFundraiserDto.getNftImage())
                        .endDate(ipfsFundraiserDto.getEndDate())
                        .currentAmount(smartContractFundraiserDto.getCurrentAmount())
                        .targetAmount(smartContractFundraiserDto.getTargetAmount())
                        .build();

                fundraiserDtoList.add(fundraiserDto);
                listIdx++;
            }


        }

        MyFundraiserListRes myFundraiserListRes = MyFundraiserListRes.builder()
                .shelterName(shelter.getShelterName())
                .hasMore(!(listLength==smartContractFundraiserDtoList.size()))
                .fundraisers(fundraiserDtoList)
                .build();


        return myFundraiserListRes;
    }

    @Override
    public void createFundraiser(HttpServletRequest request, List<MultipartFile> dogImages, MultipartFile nftImage, MultipartFile mainImage, OpenFundraiserCond openFundraiserCond) {
        String memberId = memberService.getMemberIdFromHeader(request);
        Shelter shelter = shelterRepository.findShelterByAdminId(memberId)
                .orElseThrow(()->new BadRequestException("보호소 정보가 없습니다"));

        // 이미지들을 IPFS에 저장한다.
        List<String> dogImageHash = dogImages.stream().map(file ->
            ipfsService.uploadImage(file)
        ).collect(Collectors.toList());
        String nftImageHash = ipfsService.uploadImage(nftImage);
        String mainImageHash = ipfsService.uploadImage(mainImage);

        // 후원 모집 JSON을 IPFS에 저장한다.
        String hashString = null;
        try {
            // 이미지 해시, 후원 정보들을 합쳐 저장
            IPFSFundraiserDto ipfsDto = IPFSFundraiserDto.toIPFSFundraiseDto(openFundraiserCond, dogImageHash, nftImageHash, mainImageHash);
            hashString = ipfsService.uploadJson(ipfsDto.toJson());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 스마트 컨트랙트에 등록하기 위한 객체 생성
        SmartContractFundraiserDto sc = SmartContractFundraiserDto.from(openFundraiserCond, hashString, shelter.getId());

        // 스마트 컨트랙트 호출해 저장한다.
        fundraiserContractService.createFundraiser(sc);

    }

    @Override
    public FundraiserListRes getFundraiserList(int size, int page, boolean isClosed) {
        // 스마트 컨트랙트 호출 부분
        List<SmartContractFundraiserDto> smartContractFundraiserDtoList= fundraiserContractService.getFundraiserList()
                .orElseThrow(()->new RuntimeException());

        // 페이지네이션
        int startIdx = page*size;
        int endIdx = startIdx+size;
        int listLength = endIdx > smartContractFundraiserDtoList.size() ? smartContractFundraiserDtoList.size() : endIdx;
        int count = 0;

        List<FundraiserDto> fundraiserDtoList = new ArrayList<>();


        int listIdx = 0;
        while(count<smartContractFundraiserDtoList.size() && listIdx<listLength) {

            SmartContractFundraiserDto smartContractFundraiserDto = smartContractFundraiserDtoList.get(listIdx);
            count++;
            if(smartContractFundraiserDto.getIsEnded()==isClosed){ // 모집중, 종료 구분

                // IPFS에 저장된 정보들 -> 객체로
                IPFSFundraiserDto ipfsFundraiserDto = null;
                try {
                    ipfsFundraiserDto =
                            IPFSFundraiserDto.fromJson(ipfsService.downloadJson(smartContractFundraiserDto.getHashString()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
//
//                Shelter shelter = shelterRepository.findShelterByAdminId(smartContractFundraiserDto.getShelterId())
//                        .orElseThrow(()->new BadRequestException("보호소 정보가 없습니다"));
//                String shelterName = shelter.getShelterName();


                // FundraiserDto에 담기
                FundraiserDto fundraiserDto = FundraiserDto.builder()
                        .fundraiserId(smartContractFundraiserDto.getFundraiserId())
                        .shelterName("shelterName") // TODO: 보호소 이름 받아야함
                        .dogName(ipfsFundraiserDto.getDogName())
                        .dogGender(ipfsFundraiserDto.getDogGender())
                        .mainImgUrl(ipfsFundraiserDto.getMainImage())
                        .nftImgUrl(ipfsFundraiserDto.getNftImage())
                        .endDate(ipfsFundraiserDto.getEndDate())
                        .currentAmount(smartContractFundraiserDto.getCurrentAmount())
                        .targetAmount(smartContractFundraiserDto.getTargetAmount())
                        .build();

                fundraiserDtoList.add(fundraiserDto);
                listIdx++;
            }


        }
        FundraiserListRes fundraiserDto = FundraiserListRes.builder()
                .hasMore(!(listLength==smartContractFundraiserDtoList.size()))
                .fundraiser(fundraiserDtoList)
                .build();


        return fundraiserDto;
    }

    @Override
    public FundraiserDetailRes getFundraiserDetail(Long fundraiserId) {
        // 스마트 컨트랙트 호출 부분
        SmartContractFundraiserDto smartContractFundraiserDto =
                fundraiserContractService.getFundraiserDetail(fundraiserId).orElseThrow(() -> new BadRequestException("모금 정보가 없습니다."));

        // IPFS에 저장된 정보들 -> 객체로
        IPFSFundraiserDto ipfsFundraiserDto = null;
        try {
            ipfsFundraiserDto =
                    IPFSFundraiserDto.fromJson(ipfsService.downloadJson(smartContractFundraiserDto.getHashString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<FundraiserDonationDto> donationList = donationService.getFundraiserDonationList(fundraiserId);

        FundraiserDetailRes fundraiserDetailRes =
                FundraiserDetailRes.builder()
                        .shelterId(smartContractFundraiserDto.getShelterId())
                        .shelterAddress(smartContractFundraiserDto.getShelterAddress())
                        .dogName(ipfsFundraiserDto.getDogName())
                        .mainImgUrl(ipfsFundraiserDto.getMainImage())
                        .nftImgUrl(ipfsFundraiserDto.getNftImage())
                        .dogImgUrls(ipfsFundraiserDto.getDogImage())
                        .dogGender(ipfsFundraiserDto.getDogGender())
                        .dogAge(ipfsFundraiserDto.getDogAge())
                        .ageIsEstimated(ipfsFundraiserDto.getAgeIsEstimated())
                        .dogFeature(ipfsFundraiserDto.getDogFeature())
                        .currentAmount(smartContractFundraiserDto.getCurrentAmount())
                        .targetAmount(smartContractFundraiserDto.getTargetAmount())
                        .donations(donationList)
                        .isClosed(smartContractFundraiserDto.getIsEnded())
                        .build();

        return fundraiserDetailRes;
    }

    // 후원 종료
    @Override
    public void endFundraiser() {

        List<SmartContractFundraiserDto> smartContractFundraiserDtoList= fundraiserContractService.getFundraiserList()
            .stream()
            .flatMap(List::stream).filter(fundraiser->fundraiser.getIsEnded()==false)
            .collect(Collectors.toList());

        IPFSFundraiserDto ipfsFundraiserDto = null;

        for(SmartContractFundraiserDto fundraiserDto:smartContractFundraiserDtoList){
            try {
                ipfsFundraiserDto =
                    IPFSFundraiserDto.fromJson(ipfsService.downloadJson(fundraiserDto.getHashString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(ipfsFundraiserDto.getEndDate().isBefore(LocalDate.now())) {
                fundraiserContractService.endFundraiser(fundraiserDto.getFundraiserId());
                donationService.setDonationSort(fundraiserDto.getFundraiserId());

            }


        }


    }
}