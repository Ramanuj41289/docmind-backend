
package com.ramanuj.docmind.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class AppPropertiesTest {

	@Test
	void appProperties_shouldHaveDefaultValues() {

		AppProperties properties = new AppProperties();

		assertNotNull(properties.getRag());
		assertNotNull(properties.getCors());

		assertEquals(700, properties.getRag().getChunkSize());
		assertEquals(350, properties.getRag().getMinChunkSizeChars());
		assertEquals(5, properties.getRag().getMinChunkLengthToEmbed());
		assertEquals(10000, properties.getRag().getMaxNumChunks());
		assertEquals(5, properties.getRag().getTopK());
		assertEquals(0.0, properties.getRag().getSimilarityThreshold());

		assertEquals("*", properties.getCors().getAllowedOrigin());
		assertEquals("GET, POST, PUT, DELETE, OPTIONS", properties.getCors().getAllowedMethods());
		assertEquals("*", properties.getCors().getAllowedHeaders());
	}

	@Test
	void appProperties_shouldBindCustomRagProperties() {

		MockEnvironment environment = new MockEnvironment().withProperty("app.rag.chunk-size", "1000")
				.withProperty("app.rag.min-chunk-size-chars", "400")
				.withProperty("app.rag.min-chunk-length-to-embed", "10").withProperty("app.rag.max-num-chunks", "5000")
				.withProperty("app.rag.top-k", "8").withProperty("app.rag.similarity-threshold", "0.75");

		AppProperties properties = Binder.get(environment).bind("app", Bindable.of(AppProperties.class)).get();

		assertEquals(1000, properties.getRag().getChunkSize());
		assertEquals(400, properties.getRag().getMinChunkSizeChars());
		assertEquals(10, properties.getRag().getMinChunkLengthToEmbed());
		assertEquals(5000, properties.getRag().getMaxNumChunks());
		assertEquals(8, properties.getRag().getTopK());
		assertEquals(0.75, properties.getRag().getSimilarityThreshold());
	}

	@Test
	void appProperties_shouldBindCustomCorsProperties() {

		MockEnvironment environment = new MockEnvironment()
				.withProperty("app.cors.allowed-origin", "https://example.com")
				.withProperty("app.cors.allowed-methods", "GET, POST")
				.withProperty("app.cors.allowed-headers", "Authorization, Content-Type");

		AppProperties properties = Binder.get(environment).bind("app", Bindable.of(AppProperties.class)).get();

		assertEquals("https://example.com", properties.getCors().getAllowedOrigin());

		assertEquals("GET, POST", properties.getCors().getAllowedMethods());

		assertEquals("Authorization, Content-Type", properties.getCors().getAllowedHeaders());
	}

	@Test
	void appProperties_shouldPreserveDefaultsForUnspecifiedProperties() {

		MockEnvironment environment = new MockEnvironment().withProperty("app.rag.chunk-size", "1200");

		AppProperties properties = Binder.get(environment).bind("app", Bindable.of(AppProperties.class)).get();

		assertEquals(1200, properties.getRag().getChunkSize());

		assertEquals(350, properties.getRag().getMinChunkSizeChars());
		assertEquals(5, properties.getRag().getMinChunkLengthToEmbed());
		assertEquals(10000, properties.getRag().getMaxNumChunks());
		assertEquals(5, properties.getRag().getTopK());
		assertEquals(0.0, properties.getRag().getSimilarityThreshold());

		assertEquals("*", properties.getCors().getAllowedOrigin());
		assertEquals("GET, POST, PUT, DELETE, OPTIONS", properties.getCors().getAllowedMethods());
		assertEquals("*", properties.getCors().getAllowedHeaders());
	}

	@Test
	void appProperties_shouldBindRagAndCorsTogether() {

		MockEnvironment environment = new MockEnvironment().withProperty("app.rag.chunk-size", "900")
				.withProperty("app.rag.top-k", "10").withProperty("app.rag.similarity-threshold", "0.85")
				.withProperty("app.cors.allowed-origin", "https://docmind.example.com")
				.withProperty("app.cors.allowed-methods", "GET, POST, OPTIONS");

		AppProperties properties = Binder.get(environment).bind("app", Bindable.of(AppProperties.class)).get();

		assertEquals(900, properties.getRag().getChunkSize());
		assertEquals(10, properties.getRag().getTopK());
		assertEquals(0.85, properties.getRag().getSimilarityThreshold());

		assertEquals("https://docmind.example.com", properties.getCors().getAllowedOrigin());

		assertEquals("GET, POST, OPTIONS", properties.getCors().getAllowedMethods());

		assertEquals("*", properties.getCors().getAllowedHeaders());
	}

	@Test
	void appProperties_shouldAllowUpdatingNestedProperties() {

		AppProperties properties = new AppProperties();

		AppProperties.RagProperties ragProperties = new AppProperties.RagProperties();

		ragProperties.setChunkSize(1500);
		ragProperties.setMinChunkSizeChars(500);
		ragProperties.setMinChunkLengthToEmbed(8);
		ragProperties.setMaxNumChunks(2000);
		ragProperties.setTopK(12);
		ragProperties.setSimilarityThreshold(0.9);

		AppProperties.CorsProperties corsProperties = new AppProperties.CorsProperties();

		corsProperties.setAllowedOrigin("https://localhost:3000");
		corsProperties.setAllowedMethods("GET, POST");
		corsProperties.setAllowedHeaders("Authorization");

		properties.setRag(ragProperties);
		properties.setCors(corsProperties);

		assertEquals(1500, properties.getRag().getChunkSize());
		assertEquals(500, properties.getRag().getMinChunkSizeChars());
		assertEquals(8, properties.getRag().getMinChunkLengthToEmbed());
		assertEquals(2000, properties.getRag().getMaxNumChunks());
		assertEquals(12, properties.getRag().getTopK());
		assertEquals(0.9, properties.getRag().getSimilarityThreshold());

		assertEquals("https://localhost:3000", properties.getCors().getAllowedOrigin());

		assertEquals("GET, POST", properties.getCors().getAllowedMethods());

		assertEquals("Authorization", properties.getCors().getAllowedHeaders());
	}
}
