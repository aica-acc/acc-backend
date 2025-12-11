package com.assistant.acc.service.editor;

import java.util.List;

public interface PromotionPathService {

    /**
     * Python에서 내려준 dbFilePath / dbFileType 리스트를
     * promotion_path 테이블에 저장한다.
     *
     * @param pNo   프로젝트 번호
     * @param paths db_file_path 리스트
     * @param types db_file_type 리스트
     */
    void savePromotionPaths(int pNo, List<String> paths, List<String> types);
}
