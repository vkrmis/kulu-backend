package nilenso.com.kulu_mobile2;

import org.apache.commons.io.FilenameUtils;

public class FileUtils {
    public static String getLastPartOfFile(String file) {
        return FilenameUtils.getBaseName(file) + "." + FilenameUtils.getExtension(file);
    }
}
