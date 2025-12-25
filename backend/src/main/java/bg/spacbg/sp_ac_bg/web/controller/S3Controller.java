package bg.spacbg.sp_ac_bg.web.controller;

import bg.spacbg.sp_ac_bg.model.dto.S3ObjectDto;
import bg.spacbg.sp_ac_bg.service.S3Service;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @QueryMapping
    public List<S3ObjectDto> s3Objects(@Argument String prefix) {
        return s3Service.listFiles(prefix).stream()
                .map(this::toS3ObjectDto)
                .collect(Collectors.toList());
    }

    @MutationMapping
    public String uploadFile(@Argument MultipartFile file) throws IOException {
        return s3Service.uploadFile(file.getOriginalFilename(), file.getInputStream(), file.getSize(), file.getContentType());
    }

    private S3ObjectDto toS3ObjectDto(S3Object s3Object) {
        S3ObjectDto dto = new S3ObjectDto();
        dto.setKey(s3Object.key());
        dto.setSize(s3Object.size());
        dto.setLastModified(s3Object.lastModified().toString());
        return dto;
    }
}
