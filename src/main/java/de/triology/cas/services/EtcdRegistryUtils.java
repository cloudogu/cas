/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import mousio.etcd4j.responses.EtcdKeysResponse;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.services.RegisteredService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO document public API
// TODO Favor object orientation over util classes. That is, move the methods used here to RegistryEtcd/EtcdServicesManager
// TODO unit test this
public final class EtcdRegistryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdRegistryUtils.class);

    private static final JSONParser PARSER = new JSONParser();

    public static List<String> convertNodesToStringList(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd) {
        List<String> stringList = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode entry : nodesFromEtcd) {
            JSONObject json;
            try {
                json = getCurrentDoguNode(entry);
                if (hasCasDependency(json)) {
                    stringList.add(json.get("Name").toString());
                }
            } catch (ParseException ex) {
                LOGGER.warn("failed to parse EtcdNode to json", ex);
            }

        }
        return stringList;
    }

    private static boolean hasCasDependency(JSONObject json) {
        return json != null && json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas");
    }

    private static JSONObject getCurrentDoguNode(EtcdKeysResponse.EtcdNode doguNode) throws ParseException {
        String version = "";
        JSONObject json = null;
        // get used dogu version
        for (EtcdKeysResponse.EtcdNode leaf : doguNode.getNodes()) {
            if (leaf.getKey().equals(doguNode.getKey() + "/current")) {
                version = leaf.getValue();
            }

        }
        // empty if dogu isnt used
        if (!version.isEmpty()) {
            for (EtcdKeysResponse.EtcdNode leaf : doguNode.getNodes()) {
                if (leaf.getKey().equals(doguNode.getKey() + "/" + version)) {

                    json = (JSONObject) PARSER.parse(leaf.getValue());
                }
            }
        }

        return json;

    }

    public static long findHighestId(Map<Long, RegisteredService> map) {
        long id = 0;

        for (Map.Entry<Long, RegisteredService> entry : map.entrySet()) {
            if (entry.getKey() > id) {
                id = entry.getKey();
            }
        }
        return id;
    }

    public static String getEtcdUri() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("/etc/ces/node_master"))) {
            String nodeMaster = reader.readLine();
            if (StringUtils.isBlank(nodeMaster)) {
                throw new IOException("failed to read node_master file");
            }
            return "http://".concat(nodeMaster).concat(":4001");
        }
    }

}
