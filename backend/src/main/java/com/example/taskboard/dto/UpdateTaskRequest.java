package com.example.taskboard.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

/**
 * A5 PATCH /api/tasks/:id で受け取る内容。送られなかった項目（null）は変えない。
 * 期限だけは「送らない＝変えない」と「null を送る＝期限なしにする」を分ける必要があるので、
 * setDue が呼ばれたか（JSON に due があったか）を dueSpecified で覚えておく。
 */
public class UpdateTaskRequest {

	/** 前後の空白は保存時に取り除く。空白だけかどうかは isTitleFilled で確かめる */
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

	/**
	 * タイトルを送ったなら、空白だけではないこと（docs/screens.md 3章）。
	 * 保存時の strip() と同じ空白の定義（全角スペースも含む）で確かめる。登録の @NotBlank も同じ判定。
	 * 正規表現の \S は全角スペースを空白とみなさず、strip() で空になって 500 になっていた（#78）
	 */
	@AssertTrue(message = "タイトルを入力してください")
	public boolean isTitleFilled() {
		return title == null || !title.isBlank();
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
