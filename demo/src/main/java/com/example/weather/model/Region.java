package com.example.weather.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Region {
	
    private int id; // 지역 순번

    private String regionParent; // 시, 도

    private String regionChild; // 시, 군, 구

    private int nx; // x좌표

    private int ny; // y좌표

    private Weather weather; // 지역 날씨 정보

    // 날씨 정보 제외하고 지역 생성
    public Region(int id, String regionParent, String regionChild, int nx, int ny) {
        this.id = id;
        this.regionParent = regionParent;
        this.regionChild = regionChild;
        this.nx = nx;
        this.ny = ny;
    }
}
