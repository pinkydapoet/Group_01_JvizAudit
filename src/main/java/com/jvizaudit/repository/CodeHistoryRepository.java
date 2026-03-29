package com.jvizaudit.repository;
import com.jvizaudit.entity.CodeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodeHistoryRepository extends JpaRepository<CodeHistory, Integer> {
    List<CodeHistory> findByUser_UserIdOrderByUpdatedAtDesc(Integer userId);
    Optional<CodeHistory> findByHistoryIdAndUser_UserId(Integer historyId, Integer userId);
}