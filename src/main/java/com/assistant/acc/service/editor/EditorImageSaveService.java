package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.SaveImageResponse;

public interface EditorImageSaveService {
    /**
     * 에디터에서 수정한 이미지를 서버에 저장하고 DB에 저장
     * 
     * @param pNo 프로젝트 번호
     * @param imageBase64 base64 이미지 데이터 (data:image/png;base64,...) - 선택적, 하위 호환성 유지
     * @param imagePath 이미지 경로 (상대 경로: /data/promotion/... 또는 절대 경로) - 우선 사용
     * @param dbFileType 파일 타입 (예: "poster", "mascot", "banner" 등)
     * @return SaveImageResponse 저장된 경로 포함
     */
    SaveImageResponse saveEditorImage(Integer pNo, String imageBase64, String imagePath, String dbFileType);
}

