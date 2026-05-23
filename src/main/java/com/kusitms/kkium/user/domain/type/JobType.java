package com.kusitms.kkium.user.domain.type;

public enum JobType {
  GOAL_DESIGNER("목표 설계자", "전략적 실행·계획력"),
  DRIVEN_EXECUTOR("추진형 실행가", "추진력·빠른 결단"),
  PRECISION_ANALYST("정밀 분석가", "데이터 기반 사고·정확성"),
  RELATIONSHIP_CONNECTOR("관계 연결자", "공감·소통·팀 화합"),
  STABLE_SUPPORTER("안정적 지지자", "일관성·신뢰감·서포터"),
  IDEA_EXPLORER("아이디어 탐험가", "창의성·새로운 시각"),
  PRINCIPLE_GUARDIAN("원칙 수호자", "윤리·완벽주의·기준 준수"),
  GROWTH_SEEKER("성장 지향자", "학습·자기계발·피드백 수용"),
  BALANCE_COORDINATOR("균형 조율자", "중재·유연성·맥락 파악");

  private final String label;
  private final String description;

  JobType(String label, String description) {
    this.label = label;
    this.description = description;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }
}
