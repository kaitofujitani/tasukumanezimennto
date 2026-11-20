package com.example.taskboard.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;

/**
 * A5 PATCH /api/tasks/:id で受け取る内容。送られなかった項目（null）は変えない。
 * 期限だけは「送らない＝変えない」と「null を送る＝期限なしにする」を分ける必要があるので、
 * setDue が呼ばれたか（JSON に due があったか）を dueSpecified で覚えておく。
 */
public class UpdateTaskRequest {

	/** 空白だけは空とみなす（docs/screens.md 3章）。前後の空白は保存時に取り除く */
	@Pattern(regexp = "(?s).*\\S.*", message = "タイトルを入力してください")
	private String title;

	private String description;

	@Pattern(regexp = "high|mid|low", message = "優先度は high・mid・low のどれかにしてください")
	private String priority;

	private LocalDate due;

	private boolean dueSpecified;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public LocalDate getDue() {
		return due;
	}

	/** JSON に due があれば（null でも）呼ばれる */
	public void setDue(LocalDate due) {
		this.due = due;
		this.dueSpecified = true;
	}

	/** JSON に due が含まれていたか */
	public boolean isDueSpecified() {
		return dueSpecified;
	}

}
