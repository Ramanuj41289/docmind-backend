
package com.ramanuj.docmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "spring.profiles.active=test",
		"spring.datasource.url=jdbc:h2:mem:docmind-context;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
		"jwt.secret=DocMindTestSecretKeyForJwtSigningMustBeAtLeast32Bytes", "jwt.expiration=3600000",
		"spring.ai.openai.api-key=test-api-key", "spring.ai.vectorstore.pgvector.initialize-schema=false" })
class DocmindBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}
