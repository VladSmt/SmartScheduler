package com.cr0w.smartplanner.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cr0w.smartplanner.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {


    Page<Event> findByUserId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndUserId(Long id, Long userId);
}
