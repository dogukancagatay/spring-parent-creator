package com.dgkncgty.spring.parentcreator;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;

@Service
public class ParentCreatorService {
    private static final Logger logger = LoggerFactory.getLogger(ParentCreatorService.class);

    private static final MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
    private static final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();

    @Value("${maven.repository.url:https://repo1.maven.org/maven2}")
    String repositoryUrl;

    @Value("${spring_boot.version:2.3.3.RELEASE}")
    String springBootVersion;

    @Value("${output.filename:new_parent_pom.xml}")
    String outputFilename;

    public static Model parsePomFile(String pomFilePath) {
        try (Reader reader = new FileReader(pomFilePath)) {
            return xpp3Reader.read(reader);
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Unexpected error while reading pom file", e);
        }

    }

    public static String downloadFromUrl(String downloadUrlStr, String localFilename) {
        logger.info("Will download URL: {}", downloadUrlStr);

        String tempDir = System.getProperty("java.io.tmpdir");
        logger.debug("java.io.tmpdir: {}", tempDir);

        String outputPath = new File(tempDir, localFilename).getAbsolutePath();
        logger.info("Will download file to : {}", outputPath);

        try (
                FileOutputStream fos = new FileOutputStream(outputPath);
                InputStream is = new URL(downloadUrlStr).openConnection().getInputStream()
        ) {
            // 4KB buffer
            byte[] buffer = new byte[4096];
            int length;

            // read from source and write into local file
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            logger.info("Downloaded URL: {}", downloadUrlStr);
            return outputPath;
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error while downloading pom file", e);
        }
    }

    public void createParentPom() {

        logger.info("Maven repository URL: {}", repositoryUrl);
        logger.info("New parent POM Spring Boot version: {}", springBootVersion);
        logger.info("Output POM filename: {}", outputFilename);

        // https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/2.5.7/spring-boot-dependencies-2.5.7.pom
        String dependenciesPomUrl = repositoryUrl
                + "/org/springframework/boot/spring-boot-dependencies/"
                + springBootVersion
                + "/spring-boot-dependencies-"
                + springBootVersion
                + ".pom";

        // https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/2.5.7/spring-boot-starter-parent-2.5.7.pom
        String parentPomUrl = repositoryUrl
                + "/org/springframework/boot/spring-boot-starter-parent/"
                + springBootVersion
                + "/spring-boot-starter-parent-"
                + springBootVersion
                + ".pom";

        String dependencyPomPath = downloadFromUrl(dependenciesPomUrl, "dependencies_pom.xml");
        String parentPomPath = downloadFromUrl(parentPomUrl, "parent_pom.xml");
        logger.info("Downloaded pom files");

        Model dependenciesPom = parsePomFile(dependencyPomPath);
        Model parentPom = parsePomFile(parentPomPath);
        logger.info("Parsed pom files");

        // populate plugin versions on parent
        parentPom.getBuild().getPluginManagement().getPlugins().forEach(plugin -> {
            if (dependenciesPom.getBuild().getPluginManagement().getPluginsAsMap().containsKey(plugin.getKey())) {
                plugin.setVersion(
                        dependenciesPom
                                .getBuild()
                                .getPluginManagement()
                                .getPluginsAsMap()
                                .get(plugin.getKey())
                                .getVersion()
                );
            }
        });

        // transfer missing plugins from dependencies
        dependenciesPom.getBuild().getPluginManagement().getPlugins().forEach(plugin -> {
            if (!parentPom.getBuild().getPluginManagement().getPluginsAsMap().containsKey(plugin.getKey())) {
                parentPom.getBuild().getPluginManagement().addPlugin(plugin);
            }
        });

        // populate plugin properties
        parentPom.getBuild().getPluginManagement().getPlugins().forEach(plugin -> {
            String versionStr = plugin.getVersion();
            if (versionStr.startsWith("${") && versionStr.endsWith("}")) {
                String propertyName = versionStr.substring(2, versionStr.length() - 1);

                if (dependenciesPom.getProperties().containsKey(propertyName)) {
                    parentPom.getProperties().setProperty(propertyName, dependenciesPom.getProperties().getProperty(propertyName));
                }
            }
        });

        // unset some values
        parentPom.setGroupId(parentPom.getParent().getGroupId());
        parentPom.setScm(null);
        parentPom.setDevelopers(null);
        parentPom.setLicenses(null);
        parentPom.setUrl(null);
        parentPom.setVersion("1-0-SNAPSHOT");
        parentPom.setName(null);
        parentPom.setDescription(null);
        parentPom.setParent(null);

        logger.info("Modifications are done");

        try {
            logger.info("Will write to new POM file: {}", outputFilename);
            xpp3Writer.write(new FileWriter(outputFilename), parentPom);
        } catch (IOException e) {
            logger.error("Unexpected error when writing new pom.xml", e);
        }
    }
}
