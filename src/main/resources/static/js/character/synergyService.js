/**
 * DB(API)로부터 직업 시너지 데이터를 가져오고 캐싱하는 클라이언트 서비스 모듈
 */
let cachedJobData = null;
let fetchPromise = null;

export const synergyService = {
    /**
     * 서버(/character/api/synergies)로부터 시너지 데이터를 조회하고 메모리에 캐싱합니다.
     */
    async fetchJobData() {
        if (cachedJobData) {
            return cachedJobData;
        }

        if (fetchPromise) {
            return fetchPromise;
        }

        fetchPromise = (async () => {
            try {
                const response = await fetch('/character/api/synergies');
                if (!response.ok) {
                    throw new Error(`시너지 데이터 조회 실패 (HTTP ${response.status})`);
                }
                const data = await response.json();
                cachedJobData = Array.isArray(data) ? data : [];
                return cachedJobData;
            } catch (error) {
                console.error('[synergyService] 시너지 데이터 로드 오류:', error);
                cachedJobData = [];
                throw error;
            } finally {
                fetchPromise = null;
            }
        })();

        return fetchPromise;
    },

    /**
     * 캐시된 전체 직업 시너지 목록을 반환합니다.
     */
    getJobData() {
        return cachedJobData || [];
    },

    /**
     * 클래스명으로 직업 시너지 정보를 조회합니다.
     */
    findJobByName(className) {
        if (!className || !cachedJobData) return null;
        return cachedJobData.find(job => job.className === className) || null;
    }
};
