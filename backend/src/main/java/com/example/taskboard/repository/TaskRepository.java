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

	// 以下の3つは A8 の並べ替え用（docs/features.md の F8）。列ごとに分けて使うので列の id で先に並べる。
	// 条件が同じタスクは今の並び（position）を保つ

	/** 優先度順：優先度が高い順 → 期限が近い順（期限なしは後ろ） */
	@Query("""
			select t from Task t
			order by t.column.id,
			  case t.priority when 'high' then 0 when 'mid' then 1 else 2 end,
			  t.due asc nulls last,
			  t.position""")
	List<Task> findAllOrderByPriority();

	/** 期限順：期限が近い順（期限なしは最後） → 優先度が高い順 */
	@Query("""
			select t from Task t
			order by t.column.id,
			  t.due asc nulls last,
			  case t.priority when 'high' then 0 when 'mid' then 1 else 2 end,
			  t.position""")
	List<Task> findAllOrderByDue();

	/** 登録順：登録した順 */
	@Query("""
			select t from Task t
			order by t.column.id,
			  t.createdAt,
			  t.position""")
	List<Task> findAllOrderByCreated();

	/** 指定した列の一番下のタスクの position。列が空なら -1 */
	@Query("select coalesce(max(t.position), -1) from Task t where t.column.id = :columnId")
	int findMaxPositionByColumnId(@Param("columnId") Integer columnId);

}
