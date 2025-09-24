package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.GroupMember;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.userId = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.userId = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group.id = :groupId AND gm.user.userId = :userId")
    Optional<GroupMember> findByGroupIdAndUserIdWithUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}