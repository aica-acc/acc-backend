package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.AssetDetail;
import com.assistant.acc.domain.file.AssetElement;
import com.assistant.acc.dto.file.AssetDetailRequestDto;
import com.assistant.acc.dto.file.AssetQueryRequest;
import com.assistant.acc.mapper.file.AssetsMapper;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService{
    private final AssetsMapper assetsMapper;
    private final ProjectMapper projectMapper;
    private final GeneratedAssetMapper generatedAssetMapper;

    @Override
    public List<AssetElement> getAssetElementsList(String memberNo, String type) {
        // 1. 최신 프로젝트번호 조회
        Integer projectNo = projectMapper.selectLatestProjectNo(memberNo);
        if (projectNo == null) {
            throw new IllegalStateException("해당 회원의 프로젝트가 존재하지 않습니다: " + memberNo);
        }
        // 2. DTO 구성
        AssetQueryRequest param = AssetQueryRequest.builder()
                .projectNo(projectNo).type(type)
                .build();
        // 3. 쿼리 결과 반환
        return assetsMapper.selectAssetElementList(param);
    }

    @Override
    public AssetDetail getAssetDetail(Integer filePathNo, Integer promptNo) {
        System.out.println("서비스 실행");
        System.out.println("파일번호" + filePathNo);

        AssetDetailRequestDto param = AssetDetailRequestDto.builder()
                .filePathNo(filePathNo).promptNo(promptNo).build();
        System.out.println("디테일"+assetsMapper.getAssetDetail(param));
        return assetsMapper.getAssetDetail(param);
    }
}
