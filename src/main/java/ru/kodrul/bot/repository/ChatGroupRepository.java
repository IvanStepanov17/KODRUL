package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.kodrul.bot.entity.ChatGroup;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    Optional<ChatGroup> findByChatIdAndName(Long chatId, String name);

    boolean existsByChatIdAndName(Long chatId, String name);

    @Query("SELECT DISTINCT g FROM ChatGroup g LEFT JOIN FETCH g.members WHERE g.chatId = :chatId")
    List<ChatGroup> findByChatIdWithMembers(@Param("chatId") Long chatId);

    @Query("SELECT DISTINCT g FROM ChatGroup g LEFT JOIN FETCH g.members WHERE g.chatId = :chatId AND g.name = :name")
    Optional<ChatGroup> findByChatIdAndNameWithMembers(@Param("chatId") Long chatId, @Param("name") String name);

    @Query("SELECT DISTINCT g FROM ChatGroup g LEFT JOIN FETCH g.members WHERE g.id = :id")
    Optional<ChatGroup> findByIdWithMembers(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM ChatGroup g " +
            "LEFT JOIN FETCH g.members gm " +
            "LEFT JOIN FETCH gm.user " +
            "WHERE g.chatId = :chatId AND g.name = :name")
    Optional<ChatGroup> findByChatIdAndNameWithMembersAndUsers(@Param("chatId") Long chatId, @Param("name") String name);

    @Query("SELECT DISTINCT g FROM ChatGroup g " +
            "LEFT JOIN FETCH g.members gm " +
            "LEFT JOIN FETCH gm.user " +
            "WHERE g.id = :id")
    Optional<ChatGroup> findByIdWithMembersAndUsers(@Param("id") Long id);
}

