package com.example.weather.model;

import lombok.Getter;
import org.springframework.context.annotation.Description;

import lombok.Builder;

@Getter
@Builder
@Description("Weather Open API 호출할 때 사용하는 Model")
public class OpenApiSearchTimeDTO {

	/**
	 * 발표한 날짜
	 */
	private String date;
	/**
	 * 발표한 시각
	 */
	private String hour;

}
