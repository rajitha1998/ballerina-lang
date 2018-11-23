/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.test.transaction;

import org.ballerinalang.test.BaseTest;
import org.ballerinalang.test.context.BServerInstance;
import org.ballerinalang.test.context.BallerinaTestException;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;

/**
 * Test cases for remote participant annotation.
 *
 * @since 0.985.0
 */
@Test(groups = "transactions-test")
public class RemoteParticipantTransactionTest extends BaseTest {
    private static BServerInstance serverInstance;
    private final int initiatorServicePort = 8888;
    private final int participant1ServicePort = 8889;

    @BeforeClass(groups = "transactions-test", alwaysRun = true)
    public void start() throws BallerinaTestException, IOException {
        int[] requiredPorts = new int[]{initiatorServicePort, participant1ServicePort /*, participant2ServicePort*/};
        String basePath = new File("src" + File.separator + "test" + File.separator + "resources" +
                File.separator + "transaction").getAbsolutePath();
        String[] args = new String[]{"-e", "http.coordinator.host=127.0.0.1"};

        serverInstance = new BServerInstance(balServer);
        HashMap<String, String> envProperties = new HashMap<>();
//        envProperties.put("BAL_JAVA_DEBUG", "5005");
        serverInstance.startServer(basePath, "participantService", args, envProperties, requiredPorts);
    }


    @AfterClass(groups = "transactions-test", alwaysRun = true)
    public void stop() throws Exception {
        serverInstance.removeAllLeechers();
        serverInstance.shutdownServer();
    }

    @Test
    public void remoteParticipantTransactionSuccessTest() throws IOException {
        String url = serverInstance.getServiceURLHttp(initiatorServicePort,
                "remoteParticipantTransactionSuccessTest");
        HttpResponse response = HttpClientRequest.doPost(url, "", new HashMap<>());
        assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        String target = " in-trx-block in-remote <payload-from-remote> in-trx-lastline " +
                "in-baz[oncommittedFunc] committed-block after-trx";
        assertEquals(response.getData(), target, "payload mismatched");
    }

    @Test
    public void remoteParticipantTransactionFailSuccessTest() throws IOException {
        String url = serverInstance.getServiceURLHttp(initiatorServicePort,
                "remoteParticipantTransactionFailSuccessTest");
        HttpResponse response = HttpClientRequest.doPost(url, "", new HashMap<>());
        assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        String target = " in-trx-block in-remote <payload-from-remote> throw-1 onretry-block " +
                "in-trx-block in-remote <payload-from-remote> in-trx-lastline " +
                "in-baz[oncommittedFunc] committed-block after-trx";
        assertEquals(response.getData(), target, "payload mismatched");
    }

    @Test
    public void remoteParticipantTransactionExceptionInRemoteNoSecondRemoteCall() throws IOException {
        String url = serverInstance.getServiceURLHttp(initiatorServicePort,
                "remoteParticipantTransactionExceptionInRemoteNoSecondRemoteCall");
        HttpResponse response = HttpClientRequest.doPost(url, "", new HashMap<>());
        assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        String target = " in-trx-block in-remote remote1-blown in-trx-lastline onretry-block " +
                "in-trx-block in-trx-lastline committed-block after-trx";
        assertEquals(response.getData(), target, "payload mismatched");
    }

    @Test
    public void remoteParticipantTransactionExceptionInRemoteThenSuccessInRemote() throws IOException {
        String url = serverInstance.getServiceURLHttp(initiatorServicePort,
                "remoteParticipantTransactionExceptionInRemoteThenSuccessInRemote");
        HttpResponse response = HttpClientRequest.doPost(url, "", new HashMap<>());
        assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        String target = " in-trx-block in-remote remote1-blown in-trx-lastline " +
                "onretry-block in-trx-block in-remote <payload-from-remote> in-trx-lastline " +
                "in-baz[oncommittedFunc] committed-block after-trx";
        assertEquals(response.getData(), target, "payload mismatched");

    }

    @Test
    public void remoteParticipantTransactionExceptionInRemoteThenExceptionInRemote() throws IOException {
        String url = serverInstance.getServiceURLHttp(initiatorServicePort,
                "remoteParticipantTransactionExceptionInRemoteThenExceptionInRemote");
        HttpResponse response = HttpClientRequest.doPost(url, "", new HashMap<>());
        assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        String target = " in-trx-block in-remote remote1-blown in-trx-lastline onretry-block " +
                "in-trx-block in-remote remote2-blown in-trx-lastline aborted-block after-trx";
        assertEquals(response.getData(), target, "payload mismatched");
    }
}
