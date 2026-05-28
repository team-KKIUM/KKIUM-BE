package com.kusitms.kkium.experience.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.type.PieceType;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE e.id = :experienceId AND p.deleteAt IS NULL")
  Optional<Experience> findByIdWithPiece(@Param("experienceId") Long experienceId);

  // 전체 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = com.kusitms.kkium.experience.domain.type.PieceType.ALL "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "ORDER BY eo.sortOrder ASC")
  List<Experience> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

  // 공고분석용 전체 조회 (페이지네이션 없음, experience_order 조인 없음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "ORDER BY e.id ASC")
  List<Experience> findAllByUserIdForAnalysis(@Param("userId") Long userId);

  // 공고분석용 전체 조회 (페이지네이션 없음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = com.kusitms.kkium.experience.domain.type.PieceType.ALL "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "ORDER BY eo.sortOrder ASC")
  List<Experience> findAllByUserIdNoPage(@Param("userId") Long userId);

  // 전체 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = com.kusitms.kkium.experience.domain.type.PieceType.ALL "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND eo.sortOrder > :cursor "
          + "ORDER BY eo.sortOrder ASC")
  List<Experience> findAllByUserIdAndCursor(
      @Param("userId") Long userId, @Param("cursor") Integer cursor, Pageable pageable);

  // type 필터 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = :type "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "ORDER BY eo.sortOrder ASC")
  List<Experience> findAllByUserIdAndType(
      @Param("userId") Long userId, @Param("type") PieceType type, Pageable pageable);

  // type 필터 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = :type "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND eo.sortOrder > :cursor "
          + "ORDER BY eo.sortOrder ASC")
  List<Experience> findAllByUserIdAndTypeAndCursor(
      @Param("userId") Long userId,
      @Param("type") PieceType type,
      @Param("cursor") Integer cursor,
      Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 없음, ALL 타입)
  @Query(
      "SELECT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = com.kusitms.kkium.experience.domain.type.PieceType.ALL "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.field) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "GROUP BY e.id, eo.sortOrder "
          + "ORDER BY eo.sortOrder ASC")
  List<Long> findIdsByKeyword(
      @Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 없음, 특정 type)
  @Query(
      "SELECT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = :type "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND p.type = :type "
          + "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.field) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "GROUP BY e.id, eo.sortOrder "
          + "ORDER BY eo.sortOrder ASC")
  List<Long> findIdsByKeywordAndType(
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("type") PieceType type,
      Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 있음, ALL 타입)
  @Query(
      "SELECT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = com.kusitms.kkium.experience.domain.type.PieceType.ALL "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND eo.sortOrder > :cursor "
          + "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.field) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "GROUP BY e.id, eo.sortOrder "
          + "ORDER BY eo.sortOrder ASC")
  List<Long> findIdsByKeywordAndCursor(
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("cursor") Integer cursor,
      Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 있음, 특정 type)
  @Query(
      "SELECT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "JOIN ExperienceOrder eo ON eo.experience = e AND eo.user.id = :userId AND eo.pieceType = :type "
          + "WHERE p.user.id = :userId "
          + "AND p.deleteAt IS NULL "
          + "AND p.type = :type "
          + "AND eo.sortOrder > :cursor "
          + "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.field) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "GROUP BY e.id, eo.sortOrder "
          + "ORDER BY eo.sortOrder ASC")
  List<Long> findIdsByKeywordAndTypeAndCursor(
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("type") PieceType type,
      @Param("cursor") Integer cursor,
      Pageable pageable);

  // 키워드 검색 - Step 2: id IN으로 fetch (sort_order 유지를 위해 호출부에서 순서 보장)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE e.id IN :ids AND p.deleteAt IS NULL")
  List<Experience> findAllByIdIn(@Param("ids") List<Long> ids);
}
