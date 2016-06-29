package io.dgraph.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for Dgraph client.
 *
 * @author Edgar Rodriguez-Diaz
 * @version 0.0.1
 */
public class DgraphClientTest {

    private static DgraphClient dgraphClient;

    private static final String TEST_HOSTNAME = "localhost";
    private static final int TEST_PORT = 8081;

    private static final Logger logger = LoggerFactory.getLogger(DgraphClientTest.class);


    @BeforeClass
    public static void beforeClass() {
        dgraphClient = GrpcDgraphClient.newInstance(TEST_HOSTNAME, TEST_PORT);
    }

    @AfterClass
    public static void afterClass() {
        dgraphClient.close();
    }


    @Test
    public void testMutationAndQuery() {
        final DgraphResult result = dgraphClient.query("mutation {\n" +
                                                 "  set {\n" +
                                                 "        <alice> <name> \"Alice\" .\n " +
                                                 "        <greg> <name> \"Greg\" .\n" +
                                                 "        <alice> <follows> <greg> . \n" +
                                                 "    }\n" +
                                                 "}\n" +
                                                 "query {\n" +
                                                 "    me(_xid_: alice) {\n" +
                                                 "        follows { \n" +
                                                 "            name _xid_  \n" +
                                                 "        }\n" +
                                                 "    }\n" +
                                                 "}");
        assertNotNull(result);
        final JsonObject jsonResult = result.toJsonObject();
        logger.info(jsonResult.toString());
        assertEquals(1, jsonResult.getAsJsonArray("_root_").size());

        final JsonObject resNode = jsonResult.getAsJsonArray("_root_").get(0)
                                              .getAsJsonObject();

        assertEquals("0x8c84811dffd0a905", resNode.get("_uid_").getAsString());

        final JsonArray childrenNodes = resNode.get("follows").getAsJsonArray();
        assertEquals("Bob", childrenNodes.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("Greg", childrenNodes.get(1).getAsJsonObject().get("name").getAsString());
    }

    @Test
    public void testMutationAndQueryTwoLevel() {
        final DgraphResult result = dgraphClient.query("mutation { \n" +
                                                       "    set { \n" +
                                                       "        <alice> <follows> <bob> . \n" +
                                                       "        <alice> <name> \"Alice\" . \n" +
                                                       "        <bob> <name> \"Bob\" . \n" +
                                                       "    } \n" +
                                                       "} \n" +
                                                       "query { \n" +
                                                       "    me(_xid_: alice) { \n" +
                                                       "        name _xid_ follows { \n" +
                                                       "            name _xid_ follows { \n" +
                                                       "                name _xid_ \n" +
                                                       "            } \n" +
                                                       "        } \n" +
                                                       "    } \n" +
                                                       "}");
        assertNotNull(result);
        final JsonObject jsonResult = result.toJsonObject();
        logger.info(jsonResult.toString());
        assertEquals(1, jsonResult.getAsJsonArray("_root_").size());

        final JsonObject gregNode = jsonResult.getAsJsonArray("_root_").get(0)
                                              .getAsJsonObject();
        assertEquals("alice", gregNode.getAsJsonPrimitive("_xid_")
                                     .getAsString());
        assertEquals("Alice", gregNode.getAsJsonPrimitive("name")
                                     .getAsString());
    }
}
