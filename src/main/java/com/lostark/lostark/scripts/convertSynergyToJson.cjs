
/**
 * synergyDataMerged.js → merged_25_jobs.json 변환 스크립트
 * 실행: node src/main/java/com/lostark/lostark/scripts/convertSynergyToJson.cjs
 */
const fs = require('fs');
const path = require('path');

// JS 파일을 직접 읽어서 export const 선언을 require 가능하게 처리
const jsFilePath = path.resolve('C:/Users/kimdw/IdeaProjects/LostArk/src/main/resources/static/js/character/synergyDataMerged.js');
let jsContent = fs.readFileSync(jsFilePath, 'utf-8');

// 'export const jobData = [...]' → 'module.exports = [...]' 치환
jsContent = jsContent.replace('export const jobData =', 'module.exports =');

// 임시 파일 생성
const tmpFilePath = path.resolve(__dirname, '__tmp_jobData.cjs');
fs.writeFileSync(tmpFilePath, jsContent, 'utf-8');

const jobData = require(tmpFilePath);
fs.unlinkSync(tmpFilePath);

// ─── classGroup 매핑 ───────────────────────────────────────────────
// 로스트아크 직업 계열 분류
const CLASS_GROUP_MAP = {
    '기상술사':  '스페셜리스트',
    '도화가':    '스페셜리스트',
    '알케미스트': '스페셜리스트',

    '바드':     '마법사',
    '소서리스': '마법사',
    '서머너':   '마법사',
    '아르카나': '마법사',

    '블레이드':   '암살자',
    '데모닉':     '암살자',
    '소울이터':   '암살자',
    '리퍼':       '암살자',

    '건슬링어':   '헌터',
    '데빌헌터':   '헌터',
    '블래스터':   '헌터',
    '스카우터':   '헌터',
    '스트라이커': '헌터',  // 주의: 무도가 계열도 있음

    '배틀마스터': '무도가',
    '인파이터':   '무도가',
    '기공사':     '무도가',
    '창술사':     '무도가',
    '브레이커':   '무도가',

    '버서커':     '전사',
    '워로드':     '전사',
    '디스트로이어': '전사',
    '슬레이어':   '전사',

    '홀리나이트':  '전사',

    '호크아이':    '헌터',
    '가디언나이트': '전사',
    '차원술사':    '마법사',
};

// type이 배열인 경우 쉼표 구분 문자열로 변환
function normalizeType(type) {
    if (Array.isArray(type)) return type.join(',');
    return type || '딜러';
}

const result = jobData.map(job => ({
    className:      job.className,
    classGroup:     CLASS_GROUP_MAP[job.className] || null,
    type:           normalizeType(job.type),
    classEngravings: (job.classEngravings || []).map(e => ({
        engravingName: e.engravingName,
        role: e.role
    })),
    skills: (job.skills || []).map(s => ({
        name:     s.name,
        priority: s.priority || null,
        sources: (s.sources || []).map(src => {
            const obj = {};
            if (src.skillName)        obj.skillName        = src.skillName;
            if (src.skillImg)         obj.skillImg         = src.skillImg;
            if (src.skillDescription) obj.skillDescription = src.skillDescription;
            if (src.tripodName)       obj.tripodName       = src.tripodName;
            if (src.tripodImg)        obj.tripodImg        = src.tripodImg;
            if (src.tripodDescription) obj.tripodDescription = src.tripodDescription;
            if (src.tripodRate)       obj.tripodRate       = src.tripodRate;
            if (src.baseRate)         obj.baseRate         = src.baseRate;
            if (src.isAwakening)      obj.isAwakening      = src.isAwakening;
            return obj;
        })
    }))
}));

// 출력 경로: src/main/resources/merged_25_jobs.json
const outDir = path.resolve('C:/Users/kimdw/IdeaProjects/LostArk/src/main/resources');
const outPath = path.join(outDir, 'merged_25_jobs.json');

fs.writeFileSync(outPath, JSON.stringify(result, null, 2), { encoding: 'utf8', flag: 'w' });

console.log(`✅ 변환 완료: ${result.length}개 직업 → ${outPath}`);
result.forEach(j => console.log(`  - ${j.className} (${j.classGroup || '분류없음'})`));
