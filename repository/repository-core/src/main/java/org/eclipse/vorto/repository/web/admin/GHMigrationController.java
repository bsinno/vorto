/**
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.vorto.repository.web.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.vorto.model.ModelContent;
import org.eclipse.vorto.model.ModelId;
import org.eclipse.vorto.repository.conversion.ModelIdToModelContentConverter;
import org.eclipse.vorto.repository.core.Attachment;
import org.eclipse.vorto.repository.core.FileContent;
import org.eclipse.vorto.repository.core.IModelRepositoryFactory;
import org.eclipse.vorto.repository.core.ModelInfo;
import org.eclipse.vorto.repository.search.ISearchService;
import org.eclipse.vorto.repository.web.GenericApplicationException;
import org.eclipse.vorto.repository.web.api.v1.dto.ModelLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This small REST endpoint "flattens" data about models given a search expression (defaulting to all public models if
 * absent), and attaches a zip archive to the response with the following data structure:
 * <ul>
 *     <li>
 *         root
 *         <ul>
 *             <li>
 *                 folder named after fully qualified model name, e.g. {@literal com.bosch.iot.suite.example:VirtualDemoDevice:1.0.0}
 *                 <ul>
 *                     <li>
 *                         vortolang file named after simple model name and model type, e.g.
 *                         {@literal VirtualDemoDevice.infomodel}, containing the DSL for that model
 *                     </li>
 *                     <li>
 *                         model content JSON file named {@literal content.json}, and containing the output of e.g.
 *                         {@literal [host[:port]]/api/v1/models/com.bosch.iot.suite.example:VirtualDemoDevice:1.0.0/content}
 *                     </li>
 *                     <li>
 *                         model pictures if any, with their original name and extension e.g. {@literal vorto_screenshot_final.png}
 *                     </li>
 *                     <li>
 *                         model links JSON file named {@literal links.json}, and containing the output of e.g.
 *                         {@literal [host[:port]]/api/v1/attachments/com.bosch.iot.suite.example:VirtualDemoDevice:1.0.0/links}
 *                     </li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 * Each model returned by the search expression has its own folder directly under the root. <br/>
 * Given that this feature is implemented in a branch representing the currently deployed Vorto application in production
 * and therefore far behind the development branch, there are no capabilities to load the content mentioned above
 * asynchronously. <br/>
 * Moreover, since this operation is considered a "one time run", no particular performance adjustments (e.g.
 * asynchronizing the REST call through a token system) have been designed. <br/>
 * As a result, this is intended to be run on a local or dockerized Vorto, whereas the default settings in e.g.
 * productive Vorto would likely cause a gateway timeout before the request could respond.
 */
@RestController
@RequestMapping(value = "/rest/gh")
public class GHMigrationController {

    protected static final String ATTACHMENT_FILENAME = "attachment; filename = repository.zip";
    protected static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    protected static final String CONTENT_DISPOSITION = "content-disposition";
    public static final String DEFAULT_SEARCH_EXPRESSION = "visibility:Public";
    public static final String FORMAT_MODEL_FQNAME_FOLDER = "%s/";
    public static final String FORMAT_DSL_FILENAME = "%s%s.%s";
    public static final String FORMAT_JSON_FILENAME = "%s%s.json";
    public static final String FORMAT_IMAGE_UNDER_FOLDER = "%s%s.%s";

    private static final Logger LOGGER = Logger.getLogger(GHMigrationController.class);

    @Autowired
    private ISearchService searchService;

    @Autowired
    private IModelRepositoryFactory factory;

    /**
     * No pre-authorization here. <br/>
     * This endpoint is mainly designed to use the default search term for all public models, meaning it does not
     * required the caller to be even authenticated.<br/>
     * Usages of this endpoint with custom search expression may fail or yield unexpected results should the calling
     * user not have sufficient privileges to view the models targeted.
     * @param searchExpression
     * @param response
     */
    @GetMapping("/download")
    public void downloadModel(@RequestParam(required = false) final String searchExpression, final HttpServletResponse response) {
        response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME);
        response.setContentType(APPLICATION_OCTET_STREAM);
        String expression = Strings.isNullOrEmpty(searchExpression) ? DEFAULT_SEARCH_EXPRESSION : searchExpression;
        LOGGER.info(String.format("Starting model packaging with search expression: [%s]", searchExpression));
        byte[] zipContent = createZipWithEverything(expression);
        try {
            IOUtils.copy(new ByteArrayInputStream(zipContent), response.getOutputStream());
            response.flushBuffer();
            LOGGER.info(String.format("Model packaging and download with search expression: [%s] -- success.", expression));
        } catch (IOException e) {
            LOGGER.warn(
                    String.format(
                            "Model packaging and download with search expression: [%s] -- failure - see exception logs.",
                            searchExpression
                    )
            );
            throw new GenericApplicationException("Could not export the repository model as zip.", e);
        }
    }

    private byte[] createZipWithEverything(String expression) {
        List<ModelInfo> models = searchService.search(expression);
        LOGGER.info(String.format("Search expression: [%s] yields %d results", expression, models.size()));
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            for (ModelInfo model : models) {
                // adds folder with model's fully qualified name
                LOGGER.info(String.format("Model [%s]: creating folder", model.getFullQualifiedFileName()));
                ZipEntry folder = new ZipEntry(
                        String.format(FORMAT_MODEL_FQNAME_FOLDER, model.getFullQualifiedFileName())
                );
                zos.putNextEntry(folder);

                // adds DSL entry
                LOGGER.info(String.format("Model [%s]: creating DSL file", model.getFullQualifiedFileName()));
                ModelId id = model.getId();
                FileContent modelFile = factory
                        .getRepositoryByModel(id)
                        .getFileContent(id, Optional.empty())
                        .get();
                // model.getFileName() can and usually does return null :|
                ZipEntry dslEntry = new ZipEntry(
                        String.format(
                                FORMAT_DSL_FILENAME, folder, "model", model.getType().toString().toLowerCase()
                        )
                );
                zos.putNextEntry(dslEntry);
                zos.write(modelFile.getContent());

                // fetches JSON as from the [model id]/content endpoint
                LOGGER.info(String.format("Model [%s]: creating content file", model.getFullQualifiedFileName()));
                ModelIdToModelContentConverter converter = new ModelIdToModelContentConverter(factory);
                ModelContent content = converter.convert(id, Optional.empty());
                ZipEntry contentEntry = new ZipEntry(
                        String.format(
                                FORMAT_JSON_FILENAME, folder, "model"
                        )
                );
                zos.putNextEntry(contentEntry);
                ObjectMapper om = new ObjectMapper();
                om.configure(SerializationFeature.INDENT_OUTPUT, true);
                zos.write(om.writeValueAsBytes(content));

                // fetches model images if any
                // first searches by "display image" tag
                LOGGER.info(String.format("Model [%s]: creating image file(s) if any", model.getFullQualifiedFileName()));
                List<Attachment> imageAttachments = factory
                        .getRepositoryByModel(id)
                        .getAttachmentsByTag(id, Attachment.TAG_DISPLAY_IMAGE);

                // if none present, searches just by "image" tag (for backwards compatibility)
                if (imageAttachments.isEmpty()) {
                    imageAttachments = factory
                            .getRepositoryByModel(id)
                            .getAttachmentsByTag(id, Attachment.TAG_IMAGE);
                }
                //for (Attachment a : imageAttachments) {
                if (!imageAttachments.isEmpty()) {
                    String fileName = imageAttachments.get(0).getFilename();
                    LOGGER.info(
                            String.format(
                                    "Model [%s]: creating image file for [%s]", model.getFullQualifiedFileName(),
                                    fileName
                            )
                    );
                    factory
                            .getRepositoryByModel(id)
                            .getAttachmentContent(id, fileName)
                            .ifPresent(
                                    fc -> {
                                        try {
                                            ZipEntry imageEntry = new ZipEntry(
                                                    String.format(
                                                            FORMAT_IMAGE_UNDER_FOLDER,
                                                            folder,
                                                            "image",
                                                            fileName.substring(fileName.lastIndexOf(".") + 1)
                                                    )
                                            );
                                            zos.putNextEntry(imageEntry);
                                            zos.write(fc.getContent());
                                        } catch (IOException ioe) {
                                            LOGGER.warn(
                                                    String.format(
                                                            "Model [%s]: could not create image file for [%s] -- aborting (see exception logs)",
                                                            model.getFullQualifiedFileName(),
                                                            fileName
                                                    )
                                            );
                                            throw new RuntimeException(ioe);
                                        }
                                    }
                            );

                }
                // fetches links
                LOGGER.info(String.format("Model [%s]: creating links file if applicable", model.getFullQualifiedFileName()));
                Set<ModelLink> links = factory.getRepositoryByModel(id).getLinks(id);
                if (!links.isEmpty()) {
                    zos.putNextEntry(
                            new ZipEntry(
                                    String.format(
                                            FORMAT_JSON_FILENAME, folder, "links"
                                    )
                            )
                    );
                    zos.write(om.writeValueAsBytes(links));
                }
            }
            zos.flush();
            baos.flush();
            zos.close();
            baos.close();
            LOGGER.info(String.format("Finished packing archive for search expression: [%s]", expression));
            return baos.toByteArray();
        } catch (Exception ex) {
            LOGGER.warn(String.format("Failed to pack archive for search expression: [%s] - see exception logs", expression));
            throw new GenericApplicationException("Error while generating zip file.", ex);
        }
    }
}
