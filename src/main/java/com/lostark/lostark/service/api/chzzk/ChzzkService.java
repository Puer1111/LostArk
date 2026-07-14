package com.lostark.lostark.service.api.chzzk;

import com.lostark.lostark.model.dto.chzzk.ChzzkResponse;
import java.util.List;

public interface ChzzkService {
    /**
     * 치지직 로스트아크 카테고리 실시간 방송 목록 조회
     * @return List<ChzzkResponse.LiveDetail>
     */
    List<ChzzkResponse.LiveDetail> getLostArkLives();
}
