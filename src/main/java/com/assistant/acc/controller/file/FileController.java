package com.assistant.acc.controller.file;

import com.assistant.acc.domain.file.AssetDetail;
import com.assistant.acc.domain.file.AssetElement;
import com.assistant.acc.service.file.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    // 세션에만 저장해서 사용 할 것. (F/E ASSET Detail 조회용)
    @GetMapping("/list")
    public ResponseEntity<List<AssetElement>> getAssetList(
        @RequestParam(defaultValue = "포스터") String type,
        HttpServletRequest request
    ){
        String m_no = (String) request.getAttribute("m_no");
        if(m_no == null) m_no = "M000001";

        List<AssetElement> result = fileService.getAssetElementsList(m_no, type);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/detail/{filePathNo}/{promptNo}")
    public ResponseEntity<AssetDetail> getAssetDetail(
            @PathVariable Integer filePathNo,
            @PathVariable Integer promptNo,
            HttpServletRequest request
    ) {
        System.out.println("컨트롤러 요청 파일번호: " + filePathNo + " 프롬프트번호: " + promptNo);
        String m_no = (String) request.getAttribute("m_no");
        if(m_no == null) m_no = "M000001";

        AssetDetail result = fileService.getAssetDetail(filePathNo, promptNo);
        return ResponseEntity.ok(result);
    }
}
