package com.assistant.acc.service.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.assistant.acc.mapper.file.GenerateFilePathMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;

class FileStorageServiceTest {

    @TempDir
    Path front;

    private AiPromotionFiles ai;
    private GeneratedAssetMapper assets;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        ai = mock(AiPromotionFiles.class);
        assets = mock(GeneratedAssetMapper.class);
        service = new FileStorageService(assets, mock(GenerateFilePathMapper.class), ai);
        ReflectionTestUtils.setField(service, "baseDir", front.toString());
    }

    private Path dest(String name) {
        return front.resolve("public/data/promotion/M1/7/mascot").resolve(name);
    }

    @Test
    void 저장_AI에서_받고_원본을_지운다() throws Exception {
        when(ai.download(eq("mascot"), eq("M1_7"), eq("mascot_1_0.png"), any())).thenAnswer(inv -> {
            Files.write(inv.getArgument(3, Path.class), new byte[] {9});
            return true;
        });

        service.saveGeneratedPosterImage("M1", 7, "mascot_1_0.png", 1, 2, "mascot", "M1_7");

        assertThat(dest("mascot_1_0.png")).exists();
        verify(ai).delete("mascot", "M1_7", "mascot_1_0.png");
        verify(assets).AssetInsert(any());
    }

    @Test
    void 저장_AI에_파일이_없으면_DB에_쓰지_않는다() {
        when(ai.download(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.saveGeneratedPosterImage("M1", 7, "x.png", 1, 2, "mascot", "M1_7"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(assets);
    }

    @Test
    void 저장_원본_삭제_실패면_받은_파일도_지운다() {
        when(ai.download(any(), any(), any(), any())).thenAnswer(inv -> {
            Files.write(inv.getArgument(3, Path.class), new byte[] {9});
            return true;
        });
        doThrow(new RuntimeException("ai down")).when(ai).delete(any(), any(), any());

        assertThatThrownBy(() -> service.saveGeneratedPosterImage("M1", 7, "mascot_1_0.png", 1, 2, "mascot", "M1_7"))
                .hasMessageContaining("롤백");
        assertThat(dest("mascot_1_0.png")).doesNotExist();
        verifyNoInteractions(assets);
    }
}
