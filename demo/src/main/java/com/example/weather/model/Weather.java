package com.example.weather.model;

import lombok.Data;

@Data
public class Weather {

	private Double temp; // 온도

    private Double rainAmount; // 강수량

    private Double humid; // 습도

    private String lastUpdateTime; // 마지막 갱신 시각 (시간 단위)
    
    public Weather(Double temp, Double rainAmount, Double humid, String lastUpdateTime) {
       this.temp = temp;
       this.rainAmount = rainAmount;
       this.humid = humid;
       this.lastUpdateTime = lastUpdateTime;
    }
    
}
