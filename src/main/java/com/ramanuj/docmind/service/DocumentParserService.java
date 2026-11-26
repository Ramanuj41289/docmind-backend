
package com.ramanuj.docmind.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ramanuj.docmind.exception.DocumentProcessingException;

@Service
public class DocumentParserService {

	private static final Logger logger = LoggerFactory.getLogger(DocumentParserService.class);

	public List<Document> parse(MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw new DocumentProcessingException("Uploaded file is empty");
		}

		String fileName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();

		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

		String lowerName = fileName.toLowerCase(Locale.ROOT);

		try {

			Resource resource = new ByteArrayResource(file.getBytes()) {
				@Override
				public @Nullable String getFilename() {
					return fileName;
				}
			};

			if (lowerName.endsWith(".pdf") || contentType.equals("application/pdf")) {

				return parsePdf(resource);
			}

			if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc") || lowerName.endsWith(".txt")
					|| lowerName.endsWith(".md") || lowerName.endsWith(".csv") || contentType.startsWith("text/")) {

				return parseGenericFile(resource);
			}

			throw new DocumentProcessingException(
					"Unsupported file type. Supported formats: PDF, DOC, DOCX, TXT, MD and CSV");

		} catch (DocumentProcessingException ex) {
			throw ex;

		} catch (IOException ex) {

			logger.error("Unable to read uploaded file: {}", fileName, ex);

			throw new DocumentProcessingException("Could not read uploaded file", ex);

		} catch (Exception ex) {

			logger.error("Document parsing failed: {}", fileName, ex);

			throw new DocumentProcessingException("Could not parse uploaded file", ex);
		}
	}

	private List<Document> parsePdf(Resource resource) {

		PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPageBottomMargin(0).withPageTopMargin(0)
				.build();

		return new PagePdfDocumentReader(resource, config).read();
	}

	private List<Document> parseGenericFile(Resource resource) {

		TikaDocumentReader reader = new TikaDocumentReader(resource);

		return reader.read();
	}
}
