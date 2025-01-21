package com.yeshwanth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
    private Job job;

    @PostMapping("/import")
    public ResponseEntity<String> importProductFile(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Processing file: {}", filename);

        try {
            File targetFile = saveFileToTempDirectory(file);
            executeBatchJob(targetFile.getAbsolutePath());
            return ResponseEntity.ok("Batch job executed successfully");
        } catch (Exception e) {
            log.error("Exception during file processing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed");
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
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        log.info("Batch job status: {}", jobExecution.getStatus());
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Batch job did not complete successfully");
        }
    }
}