package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.AssetDetail;
import com.assistant.acc.domain.file.AssetElement;

import java.util.List;

public interface FileService {

    List<AssetElement> getAssetElementsList(String memberNo, String type);
    /** 특정 프로젝트의 생성 이미지 목록 (소유권 검사는 호출부에서) */
    List<AssetElement> getAssetElementsListByProject(Integer projectNo, String type);
    AssetDetail getAssetDetail(Integer filePathNo, Integer promptNo);
}
