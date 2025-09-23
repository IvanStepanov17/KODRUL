package ru.kodrul.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kodrul.bot.entity.RandomizeEntity;

public interface RandomizeRepository extends JpaRepository<RandomizeEntity, Long> {
}
