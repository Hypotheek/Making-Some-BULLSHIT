package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.net.LinkSnapshot;

import java.math.BigInteger;
import java.util.List;

public interface LinkSource {

    void requestRefresh();

    List<LinkSnapshot> getLinks();

    BigInteger getBalance();
}
