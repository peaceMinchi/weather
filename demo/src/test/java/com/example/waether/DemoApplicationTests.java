package com.example.waether;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@SpringBootTest
@SpringBootConfiguration
class DemoApplicationTests {

	@Value("${resources.location}")
	private String location;

	@Test
	void contextLoads() throws IOException {
		// 클래스패스에서 파일을 읽기
		String fileLocation = String.format("%s/%s", location, "REGION_LIST.csv"); // 위도 경도 파일 경로
		Resource resource = new FileSystemResource(fileLocation);

		System.out.println("resource = " + resource.getFile().getPath());
		System.out.println("resource = " + resource.getURI());
		System.out.println("resource = " + resource.getURL());
	}

}
