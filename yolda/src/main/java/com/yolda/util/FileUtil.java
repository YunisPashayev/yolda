az.turboclone.util;

import org.springframework.stereotype.Component;
import java.util.List; 

public class FileUtil {
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp",
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private FileUtil(){

    }
    public static boolean isAllowedType(MultipartFile file){
        if (file == null || file.getContentType() == null){
            return false;
        }
        return ALLOWED_CONTENT_TYPES.contains(file.getContentType());
    }
    public static boolean isWithinSizeLimit(MultipartFile file){
        if (file == null){
            return false;
        }
        return file.getSize() <= MAX_FILE_SIZE;
    }
    public static String getExtension(MultipartFile file){
        if (file == null || file.getOriginalFilename() == null){
            return "";
        }
        String originalFilename = file.getOriginalFilename();
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1){
            return "";
        }
        return originalFilename.substring(lastDotIndex + 1);
    }
}
   
