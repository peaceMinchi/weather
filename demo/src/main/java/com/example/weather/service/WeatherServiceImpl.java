package com.example.weather.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.weather.mapper.WeatherMapper;
import com.example.weather.model.Region;

@Service
public class WeatherServiceImpl implements WeatherService {

	@Autowired
	private WeatherMapper weatherMapper;
	
	@Override
	public int insertRegionData(Region region) {
		return weatherMapper.insertRegionData(region);
	}

	@Override
	public Region selectRegionData(int id) {
		return weatherMapper.selectRegionData(id);
	}
	

	@Override
	public List<Region> selectSeoulList() {
		return weatherMapper.selectSeoulList();
	}

	@Override
	public void updateWeather(Region region) {
		weatherMapper.updateWeather(region);
	}

}
