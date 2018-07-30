package org.eclipse.vorto.repository.web.service;

import org.eclipse.vorto.repository.TestConfig;
import org.eclipse.vorto.repository.core.FileContent;
import org.eclipse.vorto.repository.web.attachment.AttachmentValidator;
import org.eclipse.vorto.repository.web.exception.AttachmentException;
import org.eclipse.vorto.repository.web.exception.FileLengthException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class, AttachmentValidator.class}, loader = AnnotationConfigContextLoader.class)
public class AttachmentValidatorTest {

    private static int ONE_KB = 1024;

    @Autowired
    private AttachmentValidator attachmentValidator;

    private FileContent file;

    @Before
    public void setUp(){
        file = mock(FileContent.class);
    }

    @Test
    public void should_allow_attachment() throws Exception{
        when(file.getFileName()).thenReturn("test.doc");
        when(file.getSize()).thenReturn((long)(2360 * ONE_KB * ONE_KB));

        assertThat(attachmentValidator.validateAttachment(file)).isEqualTo(true);
    }

    @Test(expected = AttachmentException.class)
    public void should_not_allow_attachment_exceed_filesize() throws Exception{
        when(file.getFileName()).thenReturn("test.doc");
        when(file.getSize()).thenReturn((long)(5360 * ONE_KB * ONE_KB));

        assertThat(attachmentValidator.validateAttachment(file)).isEqualTo(false);
    }

    @Test (expected = AttachmentException.class)
    public void should_not_allow_attachment_illegal_file_type() throws Exception{
        when(file.getFileName()).thenReturn("test.hack");
        when(file.getSize()).thenReturn((long)(2360 * ONE_KB * ONE_KB));

        assertThat(attachmentValidator.validateAttachment(file)).isEqualTo(false);
    }

    @Test (expected = FileLengthException.class)
    public void should_size_more_then_100() throws Exception {
        when(file.getFileName()).thenReturn("thisisbigfilename_thisisbigfilename_thisisbigfilename_thisisbigfilename_thisisbigfilename_thisisbigfilename_.txt");

        assertThat(attachmentValidator.validateFileLength(file)).isEqualTo(false);
    }


}
