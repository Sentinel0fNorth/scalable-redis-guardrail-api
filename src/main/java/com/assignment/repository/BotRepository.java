package com.assignment.repository;

import com.assignment.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRepository extends JpaRepository<Bot, Long> {
    Bot findByName(String name);
}
