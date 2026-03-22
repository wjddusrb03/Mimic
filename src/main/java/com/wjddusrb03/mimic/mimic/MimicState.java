package com.wjddusrb03.mimic.mimic;

/**
 * 미믹의 상태를 나타낸다.
 */
public enum MimicState {
    /** 블록으로 위장 중 (엔티티 없음) */
    DORMANT,
    /** 플레이어 접근 감지, 힌트 표시 중 */
    ALERTING,
    /** 변신 시퀀스 진행 중 */
    TRIGGERING,
    /** 전투 중 (커스텀 몹 활성) */
    ACTIVE,
    /** 재위장 시도 중 (Lv.4+) */
    RE_DISGUISING,
    /** 처치됨 */
    DEAD
}
