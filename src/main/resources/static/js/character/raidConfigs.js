/**
 * 레이드별 구성 설정 데이터
 * - 카테고리별로 분류하여 관리 (군단장, 카제로스, 어비스, 그림자 등)
 * - maxPlayers: 최대 인원수 (4, 8, 16 등)
 * - partyCount: 파티 개수 (1, 2, 4 등)
 */
export const raidConfigs = {
    // 군단장 레이드
    'valtan': { 
        name: '발탄', 
        category: '군단장',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'vykas': { 
        name: '비아키스', 
        category: '군단장',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'kouku-saton': { 
        name: '쿠크세이튼', 
        category: '군단장',
        maxPlayers: 4, 
        partyCount: 1 
    },
    'abrelshud': { 
        name: '아브렐슈드', 
        category: '군단장',
        maxPlayers: 8, 
        partyCount: 2 
    },

    // 카제로스 레이드
    'kazeros-1': { 
        name: '카제로스 1막', 
        category: '카제로스',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'kazeros-2': { 
        name: '카제로스 2막', 
        category: '카제로스',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'kazeros-3': { 
        name: '카제로스 3막', 
        category: '카제로스',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'kazeros-4': { 
        name: '카제로스 4막', 
        category: '카제로스',
        maxPlayers: 8, 
        partyCount: 2 
    },
    'kazeros-final': { 
        name: '카제로스 종막', 
        category: '카제로스',
        maxPlayers: 8, 
        partyCount: 2 
    },

    // 어비스 레이드
    'kayangel': { 
        name: '카양겔', 
        category: '어비스',
        maxPlayers: 4, 
        partyCount: 1 
    },
    'ivory-tower': { 
        name: '상아탑', 
        category: '어비스',
        maxPlayers: 4, 
        partyCount: 1 
    },
    'horizon-cathedral': { 
        name: '지평의 성당', 
        category: '어비스',
        maxPlayers: 4, 
        partyCount: 1 
    },

    // 그림자 레이드
    'serca': { 
        name: '세르카', 
        category: '그림자',
        maxPlayers: 4, 
        partyCount: 1 
    },

    // 초기 상태용
    'none': { 
        name: '레이드 선택', 
        category: '기본',
        maxPlayers: 8, 
        partyCount: 2 
    }
};
