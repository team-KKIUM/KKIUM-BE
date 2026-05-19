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

  @Query("SELECT e FROM Experience e JOIN FETCH e.piece p WHERE e.id = :experienceId")
  Optional<Experience> findByIdWithPiece(@Param("experienceId") Long experienceId);

  // 전체 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

  // 전체 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND e.id < :cursor "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndCursor(
      @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

  // type 필터 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND p.type = :type "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndType(
      @Param("userId") Long userId, @Param("type") PieceType type, Pageable pageable);

  // type 필터 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND p.type = :type "
          + "AND e.id < :cursor "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndTypeAndCursor(
      @Param("userId") Long userId,
      @Param("type") PieceType type,
      @Param("cursor") Long cursor,
      Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 없음)
  @Query(
      "SELECT DISTINCT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "WHERE p.user.id = :userId "
          + "AND (e.title LIKE CONCAT('%', :keyword, '%') OR t.field LIKE CONCAT('%', :keyword, '%')) "
          + "ORDER BY e.id DESC")
  List<Long> findIdsByKeyword(
      @Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

  // 키워드 검색 - Step 1: id 목록 추출 (cursor 있음)
  @Query(
      "SELECT DISTINCT e.id FROM Experience e JOIN e.piece p LEFT JOIN Tag t ON t.experience = e "
          + "WHERE p.user.id = :userId "
          + "AND e.id < :cursor "
          + "AND (e.title LIKE CONCAT('%', :keyword, '%') OR t.field LIKE CONCAT('%', :keyword, '%')) "
          + "ORDER BY e.id DESC")
  List<Long> findIdsByKeywordAndCursor(
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("cursor") Long cursor,
      Pageable pageable);

  // 키워드 검색 - Step 2: id IN으로 fetch
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE e.id IN :ids ORDER BY e.id DESC")
  List<Experience> findAllByIdIn(@Param("ids") List<Long> ids);
}
