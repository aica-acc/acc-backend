package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.AssetDetail;
import com.assistant.acc.domain.file.AssetElement;

import java.util.List;

public interface FileService {

    List<AssetElement> getAssetElementsList(String memberNo, String type);
    AssetDetail getAssetDetail(Integer filePathNo, Integer promptNo);
}
