package com.example.weather.model;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Weather {

	private BigDecimal temp; // 온도

    private BigDecimal rainAmount; // 강수량

    private BigDecimal humid; // 습도

    private String lastUpdateTime; // 마지막 갱신 시각 (시간 단위)
    
    public Weather(BigDecimal temp, BigDecimal rainAmount, BigDecimal humid, String lastUpdateTime) {
       this.temp = temp;
       this.rainAmount = rainAmount;
       this.humid = humid;
       this.lastUpdateTime = lastUpdateTime;
    }
    
}
