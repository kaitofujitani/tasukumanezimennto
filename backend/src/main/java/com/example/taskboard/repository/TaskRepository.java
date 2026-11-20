package com.example.taskboard.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.taskboard.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	/** 指定した列のタスクを表示順（上から）で返す */
	List<Task> findByColumnIdOrderByPositionAsc(Integer columnId);

	/** 全タスクを、列の表示順（左から）→列の中の表示順（上から）で返す */
	List<Task> findAllByOrderByColumnPositionAscPositionAsc();

	/** 指定した列の一番下のタスクの position。列が空なら -1 */
	@Query("select coalesce(max(t.position), -1) from Task t where t.column.id = :columnId")
	int findMaxPositionByColumnId(@Param("columnId") Integer columnId);

}
