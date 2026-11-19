package com.example.taskboard.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ボードの列（columns テーブル）。未着手・進行中・完了の3行は Flyway の初期データで入れる。
 * jakarta.persistence.Column と名前がぶつかるため、クラス名は BoardColumn にしている。
 */
@Entity
@Table(name = "columns")
public class BoardColumn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;

	/** 列の表示順（0から、小さいほど左） */
	private int position;

	protected BoardColumn() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getPosition() {
		return position;
	}

}
