package bg.spacbg.sp_ac_bg.model.dto;

import lombok.Data;

@Data
public class S3ObjectDto {
    private String key;
    private Long size;
    private String lastModified;
}
