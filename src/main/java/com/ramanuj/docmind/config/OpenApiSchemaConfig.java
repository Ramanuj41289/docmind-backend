
package com.ramanuj.docmind.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ramanuj.docmind.dto.AdminStatusDto;
import com.ramanuj.docmind.dto.ChatMessageDto;
import com.ramanuj.docmind.dto.ChatRequestDto;
import com.ramanuj.docmind.dto.ChatResponseDto;
import com.ramanuj.docmind.dto.CitationDto;
import com.ramanuj.docmind.dto.ConversationDto;
import com.ramanuj.docmind.dto.DocumentMetadataDto;
import com.ramanuj.docmind.dto.DocumentResponseDto;
import com.ramanuj.docmind.dto.LoginRequestDto;
import com.ramanuj.docmind.dto.LoginResponseDto;
import com.ramanuj.docmind.dto.RegisterUserRequest;
import com.ramanuj.docmind.dto.SearchRequestDto;
import com.ramanuj.docmind.dto.SearchResultDto;
import com.ramanuj.docmind.dto.UpdateUserRoleRequest;
import com.ramanuj.docmind.dto.UserDto;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

@Configuration
public class OpenApiSchemaConfig {

	private static final List<Class<?>> DTO_CLASSES = List.of(AdminStatusDto.class, ChatMessageDto.class,
			ChatRequestDto.class, ChatResponseDto.class, CitationDto.class, ConversationDto.class,
			DocumentMetadataDto.class, DocumentResponseDto.class, LoginRequestDto.class, LoginResponseDto.class,
			RegisterUserRequest.class, SearchRequestDto.class, SearchResultDto.class, UpdateUserRoleRequest.class,
			UserDto.class);

	@Bean
	public OpenApiCustomizer registerDtoSchemas() {
		return openApi -> {
			if (openApi.getComponents() == null) {
				openApi.setComponents(new Components());
			}
			Components components = openApi.getComponents();
			if (components.getSchemas() == null) {
				components.setSchemas(new LinkedHashMap<>());
			}
			for (Class<?> dtoClass : DTO_CLASSES) {
				ModelConverters.getInstance().readAll(dtoClass)
						.forEach((schemaName, schema) -> components.getSchemas().putIfAbsent(schemaName, schema));
			}
			configureVoidResponseSchema(openApi);
			configureMultipleDocumentUploadSchema(openApi);
		};
	}

	private void configureVoidResponseSchema(OpenAPI openApi) {
		Schema<?> responseSchema = openApi.getComponents().getSchemas().get("ApiResponseVoid");
		if (responseSchema == null) {
			return;
		}
		Schema<Object> nullSchema = new Schema<>();
		nullSchema.setType("null");
		nullSchema.setDescription("No response payload. Always null.");
		responseSchema.addProperty("data", nullSchema);
		Map<String, Object> example = new LinkedHashMap<>();
		example.put("success", true);
		example.put("message", "Document deleted successfully");
		example.put("data", null);
		example.put("timestamp", "2026-11-26T16:30:00");
		responseSchema.setExample(example);
	}

	private void configureMultipleDocumentUploadSchema(OpenAPI openApi) {
		var paths = openApi.getPaths();
		if (paths == null) {
			return;
		}
		var pathItem = paths.get("/api/v1/documents/upload-multiple");
		if (pathItem == null || pathItem.getPost() == null) {
			return;
		}
		var operation = pathItem.getPost();
		var requestBody = operation.getRequestBody();
		if (requestBody == null) {
			requestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
		}
		StringSchema fileSchema = new StringSchema();
		fileSchema.setFormat("binary");
		ArraySchema filesArraySchema = new ArraySchema();
		filesArraySchema.setItems(fileSchema);
		ObjectSchema requestSchema = new ObjectSchema();
		requestSchema.addProperty("files", filesArraySchema);
		requestSchema.addRequiredItem("files");
		MediaType multipartMediaType = new MediaType();
		multipartMediaType.setSchema(requestSchema);
		Content content = new Content();
		content.addMediaType("multipart/form-data", multipartMediaType);
		requestBody.setRequired(true);
		requestBody.setContent(content);
		operation.setRequestBody(requestBody);
	}
}
