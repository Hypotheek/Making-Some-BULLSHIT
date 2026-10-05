package com.hypotheek.makingsomebullshit.net;

import java.util.List;

public class ClientLinkData {
    private static volatile List<LinkSnapshot> links = List.of();

    private ClientLinkData() {

    }

    public static List<LinkSnapshot> getLinks() {
        return links;
    }

    public static void setLinks(List<LinkSnapshot> newLinks) {
        links = List.copyOf(newLinks);
    }


}
