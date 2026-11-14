package com.sarbar.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class SupportApplicationTest {

  @Test
  void contextLoads() {}
}
