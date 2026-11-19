package com.example.taskboard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskboard.entity.BoardColumn;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Integer> {

	/** 表示順（左から）で全列を返す */
	List<BoardColumn> findAllByOrderByPositionAsc();

}
