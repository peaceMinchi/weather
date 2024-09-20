package com.example.weather.model;

import java.math.BigDecimal;

import org.springframework.context.annotation.Description;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Description("지역 날씨 정보를 조회할 때 사용하는 Model")
public class RegionWeatherSelectDTO {
	
    private int id; // 지역 순번

    private String regionParent; // 시, 도

    private String regionChild; // 시, 군, 구

    private int nx; // x좌표

    private int ny; // y좌표

	private BigDecimal temp; // 온도

    private BigDecimal rainAmount; // 강수량

    private BigDecimal humid; // 습도

    private String lastUpdateTime; // 마지막 갱신 시각 (시간 단위)
}
