package com.yeshwanth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@RestController
@RequestMapping("/products")
public class ProductDataController {
    private static final Logger log = LoggerFactory.getLogger(ProductDataController.class);
    private static final String JOB_PARAM_INPUT_FILE_NAME = "input.file.name";

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("productImportJob")
    private Job importProductJob;

    @Autowired
    @Qualifier("exportProductJob")
    private Job exportProductJob;

    @PostMapping("/import")
    public ResponseEntity<String> importProductFile(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Processing file: {}", filename);

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            File targetFile = saveFileToTempDirectory(file);
            executeBatchJob(targetFile.getAbsolutePath());
            return ResponseEntity.ok("Batch job executed successfully");
        } catch (Exception e) {
            log.error("Exception during file processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed: " + e.getMessage());
        }
    }

    private File saveFileToTempDirectory(MultipartFile file) throws IOException {
        Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "uploads");
        if (!Files.exists(tempDirectory)) {
            Files.createDirectories(tempDirectory);
        }
        File savedFile = tempDirectory.resolve(Objects.requireNonNull(file.getOriginalFilename())).toFile();
        file.transferTo(savedFile);
        return savedFile;
    }

    private void executeBatchJob(String filePath) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString(JOB_PARAM_INPUT_FILE_NAME, filePath)
                .toJobParameters();
        JobExecution jobExecution = jobLauncher.run(importProductJob, jobParameters);
        log.info("Batch job status: {}", jobExecution.getStatus());
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Batch job did not complete successfully");
        }
    }

    @GetMapping("/export")
    public void downloadCsv(HttpServletResponse response) throws IOException {
        File file = new File("products.csv");
        try {
            jobLauncher.run(exportProductJob, new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters());

            response.setContentType("text/csv");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.csv");

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush(); // Ensure all content is written to the response
            }
        } catch (Exception e) {
            log.error("Error during CSV download", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error occurred during file processing: " + e.getMessage());
        } finally {
            if (file.exists() && !file.delete()) {
                log.error("Failed to delete the file after download: {}", file.getAbsolutePath());
            }
        }
    }
}