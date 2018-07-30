package org.eclipse.vorto.repository.web.attachment;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.vorto.repository.core.FileContent;
import org.eclipse.vorto.repository.web.exception.AttachmentException;
import org.eclipse.vorto.repository.web.exception.FileLengthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.function.Predicate;

@Component
public class AttachmentValidator  {

    private static int ONE_KB = 1024;

    @Value("#{'${repo.allowed.extension}'.split(',')}")
    private List<String> allowedExtension;

    @Value("${repo.allowed.fileSize: 2}")
    private int fileSize;

    public boolean validateAttachment(FileContent file) throws AttachmentException {

        if(getFileSizeInMegaBytes(file.getSize()) > fileSize){
            throw new AttachmentException("File size exceeded");
        }
        if(!allowedExtension.stream()
                .anyMatch(isExtensionAllowed(file.getFileName()))){
            throw new AttachmentException("File type not supported");
        }else{
            return true;
        }

    }

    public boolean validateFileLength(FileContent file) throws UnsupportedEncodingException {
        String fileName = URLDecoder.decode(file.getFileName(), "UTF-8");
        if (fileName.length() > 100) {
            throw new FileLengthException("Name of File exceeds 100 Characters");
        }
        return true;
    }

    private long getFileSizeInMegaBytes(long size) {
        return size / (ONE_KB * ONE_KB);
    }

    private Predicate<String> isExtensionAllowed(String fileName) {
        return  extension -> FilenameUtils.getExtension(
                fileName).equals(extension.trim());

    }
}
