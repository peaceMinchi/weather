package com.example.weather.model;

import java.math.BigDecimal;

import org.springframework.context.annotation.Description;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Description("지역 날씨 정보를 수정할 때 사용하는 Model")
public class Region {
	
    private int id; // 지역 순번

    private String regionParent; // 시, 도

    private String regionChild; // 시, 군, 구

    private int nx; // x좌표

    private int ny; // y좌표

	private BigDecimal temp; // 온도

    private BigDecimal rainAmount; // 강수량

    private BigDecimal humid; // 습도
    
    private String requestTime; // 서버 요청 시각
    
    public Region(int id, String regionParent, String regionChild, int nx, int ny) {
        this.id = id;
        this.regionParent = regionParent;
        this.regionChild = regionChild;
        this.nx = nx;
        this.ny = ny;
     }
}
