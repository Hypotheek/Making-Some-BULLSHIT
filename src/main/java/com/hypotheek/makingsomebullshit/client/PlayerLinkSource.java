package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.net.ClientLinkData;
import com.hypotheek.makingsomebullshit.net.LinkSnapshot;
import com.hypotheek.makingsomebullshit.net.Network;
import com.hypotheek.makingsomebullshit.net.RequestLinksPacket;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.client.Minecraft;

import java.math.BigInteger;
import java.util.List;

public final class PlayerLinkSource implements LinkSource {
    @Override
    public void requestRefresh() {
        Network.CHANNEL.sendToServer(new RequestLinksPacket());
    }

    @Override
    public List<LinkSnapshot> getLinks() {
        return ClientLinkData.getLinks();
    }

    @Override
    public BigInteger getBalance() {
        if (Minecraft.getInstance().player == null) return BigInteger.ZERO;
        return Minecraft.getInstance().player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                .map(IKnowledgeProvider::getEmc)
                .orElse(BigInteger.ZERO);
    }
}
