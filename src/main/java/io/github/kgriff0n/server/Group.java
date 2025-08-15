package io.github.kgriff0n.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Group {

    private final String name;
    private final List<ServerInfo> serversList;
    private final Settings settings;
    private final HashMap<String, Settings> rules;

    public Group(String name, Settings settings) {
        this.name = name;
        this.serversList = new ArrayList<>();
        this.settings = settings;
        this.rules = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void addServer(ServerInfo server) {
        this.serversList.add(server);
    }

    public List<ServerInfo> getServersList() {
        return serversList;
    }

    public void addRule(String groupId, Settings rule) {
        rules.put(groupId, rule);
    }

    public Settings getSettings() {
        return settings;
    }

    public HashMap<String, Settings> getRules() {
        return rules;
    }

}
