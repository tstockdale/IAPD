import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XMLProcessingServiceTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("iapd-xml-test");
        // Ensure Data/Input exists for output
        Files.createDirectories(Paths.get(Config.BROCHURE_INPUT_PATH));
    }

    @AfterEach
    void tearDown() throws Exception {
        // Cleanup temp files
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
    }

    @Test
    void writesCsvUsingCommonsCsv() throws Exception {
        // Minimal XML with one Firm containing required elements
        String xml = "" +
            "<Root>" +
            "  <Firm>" +
            "    <Info SECRgnCD=\"01\" FirmCrdNb=\"123\" SECNb=\"ABC\" BusNm=\"Biz\" LegalNm=\"Legal\"/>" +
            "    <Rgstn FirmType=\"X\" St=\"NY\" Dt=\"2025-01-01\"/>" +
            "    <Filing Dt=\"2025-01-02\" FormVrsn=\"1\"/>" +
            "    <MainAddr Strt1=\"S1\" Strt2=\"S2\" City=\"City\" State=\"NY\" Cntry=\"US\" PostlCd=\"10001\" PhNb=\"111\" FaxNb=\"222\"/>" +
            "    <Item5A TtlEmp=\"10\"/>" +
            "    <Item5F Q5F2C=\"AUM\" Q5F2F=\"ACC\"/>" +
            "  </Firm>" +
            "</Root>";

        Path xmlFile = tempDir.resolve("test.xml");
        try (FileWriter fw = new FileWriter(xmlFile.toFile())) {
            fw.write(xml);
        }

        ProcessingContext ctx = ProcessingContext.builder().urlRatePerSecond(1000).downloadRatePerSecond(1000).build();
        XMLProcessingService service = new XMLProcessingService();

        Path out = service.processXMLFile(xmlFile.toFile(), ctx);
        assertNotNull(out);
        assertTrue(Files.exists(out));

        String content = Files.readString(out);
        assertTrue(content.startsWith(Config.FIRM_HEADER));
        assertTrue(content.contains("Biz"));
    }
}
